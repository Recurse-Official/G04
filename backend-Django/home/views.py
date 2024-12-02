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

from django.core.files.storage import FileSystemStorage
from rest_framework.parsers import MultiPartParser, FormParser

class uploadApp(APIView):
    def post(self, request):
        try:
            chunk = request.FILES.get('chunk')
            app_name = request.POST.get('app_name')
            package_name = request.POST.get('package_name')
            chunk_index = int(request.POST.get('chunk_index'))
            total_chunks = int(request.POST.get('total_chunks'))
            file_name = request.POST.get('file_name')

            answers = search_apk_in_database(package_name)

            if(answers != []):
                return JsonResponse({
                    'message': 'Upload complete',
                    'status': 'success',
                    'file_path': os.path.join('uploads', package_name, file_name),
                    'dyn_analysis_data' : answers
                })

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

                # List all files in the folder
                files = [f for f in os.listdir(final_dir) if os.path.isfile(os.path.join(final_dir, f))]

                name = dyn_analysis(final_path, files)

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
    
    @staticmethod
    def all_chunks_received(chunk_dir, filename, total_chunks):
        """
        Check if all chunks have been uploaded by verifying their presence.
        """
        for i in range(total_chunks):
            if not os.path.exists(os.path.join(chunk_dir, f"{filename}_{i}.part")):
                return False
        return True
