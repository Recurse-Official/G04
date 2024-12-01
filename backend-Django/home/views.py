from django.shortcuts import render

# Create your views here.
from django.shortcuts import render
from django.http import HttpResponse
from rest_framework import status
from rest_framework.views import APIView
from rest_framework.response import Response
from datetime import datetime
from django.conf import settings
from .models import *
from django.http import JsonResponse

from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator

import os
import shutil
import hashlib

from dynamic_analysis.dynamic_analysis import dyn_analysis;
from dynamic_analysis.search_db import search_apk_in_database;

@method_decorator(csrf_exempt, name='dispatch')
class receiveApp(APIView):  
    def post(self, request):
        file_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'files')
        chunk_dir = os.path.join(file_dir, 'chunks')

        chunk_file = request.FILES.get('chunk')
        chunk_index = int(request.data.get('chunkIndex', 0))
        total_chunks = int(request.data.get('totalChunks', 1))
        sha256_client = request.data.get('sha256', '').strip('"')

        # print(f"sha256 -> {sha256_client}")
        # Basic validation
        if not chunk_file:
            return Response({'error': 'No APK chunk provided'}, status=status.HTTP_400_BAD_REQUEST)

        # Ensure content type is APK (optional if client-side checks)
        if chunk_file.content_type != 'application/octet-stream':
            return Response({'error': 'Invalid file type, APK required'}, status=status.HTTP_400_BAD_REQUEST)

        # Calculate the SHA-256 of the received chunk before saving it
        calculated_sha256 = self.calculate_sha256(chunk_file)

        if calculated_sha256 != sha256_client:
            return Response({'error': 'Chunk hash mismatch. File is corrupted.'}, status=status.HTTP_400_BAD_REQUEST)
        
        print(f"File name : {chunk_file.name}\n\n Total Chunks : {total_chunks}\n\n Sha256 : {sha256_client}\n\n Chunk Number : {chunk_index}")

        # Ensure chunk directory exists
        if not os.path.exists(chunk_dir):
            os.makedirs(chunk_dir)

        # Save the chunk
        chunk_filename = os.path.join(chunk_dir, f"{chunk_file.name}_{chunk_index}.part")
        try:
            with open(chunk_filename, 'wb+') as destination:
                for chunk in chunk_file.chunks():
                    destination.write(chunk)
        except IOError as e:
            return Response({'error': f'Error saving chunk: {str(e)}'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        print(f"Chunk file has same sha256 {chunk_filename}")
        
        # Check if all chunks are uploaded
        if self.all_chunks_received(chunk_dir, chunk_file.name, total_chunks):
            try:
                # Combine chunks and validate APK integrity
                combined_apk_path = self.combine_chunks(chunk_dir, chunk_file.name, file_dir, total_chunks)

                print(f"combined_apk_path : {combined_apk_path}")
                
                # Verify APK hash to ensure integrity
                if not self.verify_file_integrity(combined_apk_path, chunk_file.name):
                    os.remove(combined_apk_path)  # Delete invalid file
                    return Response({'error': 'APK hash mismatch. File is corrupted.'}, status=status.HTTP_400_BAD_REQUEST)

                return Response({'message': 'APK successfully uploaded and verified.'}, status=status.HTTP_200_OK)
            except Exception as e:
                return Response({'error': f'Error combining chunks: {str(e)}'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        return Response({'message': f'Chunk {chunk_index} uploaded successfully'}, status=status.HTTP_200_OK)

    @staticmethod
    def all_chunks_received(chunk_dir, filename, total_chunks):
        """
        Check if all chunks have been uploaded by verifying their presence.
        """
        for i in range(total_chunks):
            if not os.path.exists(os.path.join(chunk_dir, f"{filename}_{i}.part")):
                return False
        return True

    @staticmethod
    def combine_chunks(chunk_dir, filename, folder, total_chunks):
        """
        Combine all the chunks into a single APK file.
        """
        output_file_path = os.path.join(folder, filename)

        try:
            with open(f"{output_file_path}.apk", 'wb') as output_file:
                for i in range(total_chunks):
                    chunk_file_path = os.path.join(chunk_dir, f"{filename}_{i}.part")
                    with open(chunk_file_path, 'rb') as chunk_file:
                        output_file.write(chunk_file.read())

                    # Cleaning up chunk after reading the file
                    os.remove(chunk_file_path)
        except IOError as e:
            raise Exception(f"Error combining chunks: {str(e)}")
        
        return f"{output_file_path}.apk"

    @staticmethod
    def verify_file_integrity(file_path, expected_sha256):
        """
        Verify the file's integrity by comparing its SHA-256 hash with the expected value.
        """
        sha256_hash = hashlib.sha256()
        try:
            with open(file_path, "rb") as f:
                for byte_block in iter(lambda: f.read(4096), b""):
                    sha256_hash.update(byte_block)
        except IOError as e:
            raise Exception(f"Error reading combined file for hash calculation: {str(e)}")

        calculated_hash = sha256_hash.hexdigest()
        return calculated_hash == expected_sha256

    @staticmethod
    def calculate_sha256(chunk_file):
        sha256_hash = hashlib.sha256()

        # Read the chunk file in small blocks to avoid memory issues with large files
        for byte_block in chunk_file.chunks():
            sha256_hash.update(byte_block)

        # Return the calculated SHA-256 hash in hexadecimal format
        return sha256_hash.hexdigest()

from django.core.files.storage import FileSystemStorage
from rest_framework.parsers import MultiPartParser, FormParser

@method_decorator(csrf_exempt, name='dispatch')  # For development, use proper CSRF handling in production
class uploadApp(APIView):
    def post(self, request):
        try:
            chunk = request.FILES.get('chunk')
            app_name = request.POST.get('app_name')
            package_name = request.POST.get('package_name')
            chunk_index = int(request.POST.get('chunk_index'))
            total_chunks = int(request.POST.get('total_chunks'))
            file_name = request.POST.get('file_name')

            # Create temp directory for chunks
            temp_dir = os.path.join(settings.MEDIA_ROOT, 'temp', package_name)
            os.makedirs(temp_dir, exist_ok=True)

            # Save chunk to temp file
            chunk_path = os.path.join(temp_dir, f"{file_name}.part{chunk_index}")
            with open(chunk_path, 'wb+') as destination:
                for chunk_data in chunk.chunks():
                    destination.write(chunk_data)

            # If this is the last chunk, combine all chunks
            if chunk_index == total_chunks - 1:
                # Create final directory if it doesn't exist
                final_dir = os.path.join(settings.MEDIA_ROOT, 'uploads', package_name)
                os.makedirs(final_dir, exist_ok=True)
                final_path = os.path.join(final_dir, file_name)
                final_path = os.path.abspath(final_path)

                # Combine chunks
                with open(final_path, 'wb') as outfile:
                    for i in range(total_chunks):
                        chunk_path = os.path.join(temp_dir, f"{file_name}.part{i}")
                        if os.path.exists(chunk_path):
                            with open(chunk_path, 'rb') as infile:
                                shutil.copyfileobj(infile, outfile, 1024*1024)  # 1MB buffer
                            os.remove(chunk_path)  # Clean up chunk
                
                # Clean up temp directory
                try:
                    shutil.rmtree(temp_dir)
                except Exception as e:
                    print(f"Error cleaning temp directory: {e}")
                print(f"{final_path}")
                
                if(search_apk_in_database(package_name) != []):
                    return JsonResponse({
                        'message': 'Upload complete',
                        'status': 'success',
                        'file_path': os.path.join('uploads', package_name, file_name),
                        'dyn_analysis_data' : search_apk_in_database(name)
                    })

                # for i in final_dir:
                #     print(i)

                name = dyn_analysis(final_path)

                return JsonResponse({
                    'message': 'Upload complete',
                    'status': 'success',
                    'file_path': os.path.join('uploads', package_name, file_name),
                    'dyn_analysis_data' : search_apk_in_database(name)
                })
            
            return JsonResponse({
                'message': f'Chunk {chunk_index + 1}/{total_chunks} received',
                'status': 'success'
            })

        except Exception as e:
            import traceback
            print(f"Error in receive_app_chunk: {e}")
            print(traceback.format_exc())
            return JsonResponse({
                'message': f'Error processing upload: {str(e)}',
                'status': 'error'
            }, status=500)

        return JsonResponse({
            'message': 'Method not allowed',
            'status': 'error'
        }, status=405)