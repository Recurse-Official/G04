package com.example.dynodroid.network

import com.example.dynodroid.AnalysisResult
import com.example.dynodroid.CheckRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface DynoApiService {
    @Multipart
    @POST("/api/receiveApp/")
    suspend fun uploadApkChunk(
        @Part chunk: MultipartBody.Part,
        @Part("app_name") appName: RequestBody,
        @Part("package_name") packageName: RequestBody,
        @Part("chunk_index") chunkIndex: RequestBody,
        @Part("total_chunks") totalChunks: RequestBody,
        @Part("file_name") fileName: RequestBody,
        @Part("isLastChunk") isLastChunk: Boolean
    ): Response<AnalysisResult>


    @POST("/api/checkApp/")
    suspend fun checkAnalysisStatus(@Body checkRequestBody: CheckRequestBody): AnalysisResult
}