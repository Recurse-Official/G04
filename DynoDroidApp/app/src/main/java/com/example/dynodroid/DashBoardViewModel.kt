package com.example.dynodroid

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dynodroid.network.DynoApi
import com.example.dynodroid.network.MinimalMemoryRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import kotlin.math.min

class DashBoardViewModel : ViewModel() {
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
    fun scanAndSendApk(appName: String, packageName: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uploadState.value = UploadState.InProgress("Checking analysis status...")
                val analysisStatus = checkAnalysisStatus(packageName)

                // Assuming you want to check if analysis is NOT already done
                if (analysisStatus.alreadyAnalyzed) {
                    _uploadState.value = UploadState.Success(
                        "Analysis already completed for $appName",
                        analysisStatus
                    )
                    return@launch
                }

                _uploadState.value = UploadState.InProgress("Collecting APK files...")
                val apkFiles = getApkFilesForPackage(packageName, context)

                if (apkFiles.isEmpty()) {
                    _uploadState.value = UploadState.Failed("No APK files found for $packageName")
                    return@launch
                }

                var uploadedSize = 0L
                val totalSize = apkFiles.sumOf { it.length() }

                apkFiles.forEachIndexed { index, file ->
                    try {
                        // Check if this is the last file
                        val isLastChunk = index == apkFiles.size - 1
                        val analysisResult = uploadLargeApk(
                            file,
                            appName,
                            packageName,
                            isLastChunk
                        ) { chunkSize ->
                            uploadedSize += chunkSize
                            val progress = ((uploadedSize.toFloat() / totalSize) * 100).toInt()
                            _uploadState.value = UploadState.InProgress("Uploading: $progress%")

                            // Switch to dynamic analysis state after second-to-last file
                            if (index == apkFiles.size - 2) {
                                _uploadState.value = UploadState.DynamicAnalysis("Preparing for dynamic analysis...")
                            }
                        }

                        // If this is the last chunk and status is success, update state
                        if (isLastChunk && analysisResult.alreadyAnalyzed) {
                            _uploadState.value = UploadState.Success(
                                "Dynamic analysis completed for $appName",
                                analysisResult
                            )
                            return@launch
                        }

                    } catch (e: Exception) {
                        _uploadState.value = UploadState.Failed("Error uploading file: ${e.message}")
                        throw e
                    }
                }

                // Fallback in case no results were received
                _uploadState.value = UploadState.Failed("No analysis results received")

            } catch (e: Exception) {
                _uploadState.value = UploadState.Failed("Analysis failed: ${e.message}")
            }
        }
    }

    private suspend fun checkAnalysisStatus(packageName: String): AnalysisStatusResponse {
        return try {
            Log.d("Package",packageName)
            val response = DynoApi.retrofitService.checkAnalysisStatus(CheckRequestBody(packageName))
            Log.d("Check response", response.status.toString()+response.topSyscalls.toString()+response.topRedZonesyscalls.toString())
            AnalysisStatusResponse(
                alreadyAnalyzed = response.status,
                previousResults = response
            )
        } catch (e: Exception) {
            // If API call fails, assume the app needs to be analyzed
            AnalysisStatusResponse(false, null)
        }
    }

    private suspend fun uploadLargeApk(
        file: File,
        appName: String,
        packageName: String,
        isLastChunk: Boolean,
        progressCallback: (Long) -> Unit
    ): AnalysisStatusResponse {
        val chunkSize = 10 * 1024 * 1024L // 10MB chunks
        val totalChunks = (file.length() + chunkSize - 1) / chunkSize
        var lastChunkResult: AnalysisResult? = null

        try {
            for (chunkIndex in 0 until totalChunks) {
                val startByte = chunkIndex * chunkSize
                val endByte = min((chunkIndex + 1) * chunkSize, file.length()) - 1

                val chunkResult = withContext(Dispatchers.IO) {
                    uploadChunk(
                        file = file,
                        startByte = startByte,
                        endByte = endByte,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        appName = appName,
                        packageName = packageName,
                        isLastChunk,
                    )
                }

                // Update progress
                progressCallback(endByte - startByte + 1)

                // Store the last chunk's result
                if (chunkIndex == totalChunks - 1) {
                    lastChunkResult = chunkResult
                }
            }
        } catch (e: Exception) {
            _uploadState.value = UploadState.Failed("Upload failed for chunk: ${e.message}")
            throw e
        }

        // Create and return AnalysisStatusResponse
        return AnalysisStatusResponse(
            alreadyAnalyzed = lastChunkResult?.status == true,
            previousResults = lastChunkResult
        )
    }

    private fun getApkFilesForPackage(packageName: String, context: Context): List<File> {
        val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        val apkFiles = mutableListOf<File>()

        packageInfo.applicationInfo?.sourceDir?.let {
            apkFiles.add(File(it))
        }

        packageInfo.applicationInfo?.splitSourceDirs?.let { splitDirs ->
            splitDirs.forEach { splitPath ->
                apkFiles.add(File(splitPath))
            }
        }

        return apkFiles
    }

    private suspend fun uploadChunk(
        file: File,
        startByte: Long,
        endByte: Long,
        chunkIndex: Long,
        totalChunks: Long,
        appName: String,
        packageName: String,
        isLastChunk: Boolean,
    ): AnalysisResult {
        val requestBody = MinimalMemoryRequestBody(
            file = file,
            contentType = "application/vnd.android.package-archive",
            startByte = startByte,
            endByte = endByte
        )
        val part = MultipartBody.Part.createFormData(
            "chunk",
            file.name,
            requestBody
        )

        val appNameBody = appName.toRequestBody("text/plain".toMediaTypeOrNull())
        val packageNameBody = packageName.toRequestBody("text/plain".toMediaTypeOrNull())
        val chunkIndexBody = chunkIndex.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val totalChunksBody = totalChunks.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val fileNameBody = file.name.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = DynoApi.retrofitService.uploadApkChunk(
            chunk = part,
            appName = appNameBody,
            packageName = packageNameBody,
            chunkIndex = chunkIndexBody,
            totalChunks = totalChunksBody,
            fileName = fileNameBody,
            isLastChunk = isLastChunk
        )

        if (!response.isSuccessful) {
            throw IOException("Chunk upload failed: ${response.code()}")
        }

        // Extract the body (AnalysisResult) from the response
        return response.body() ?: throw IOException("Empty response body")
    }
}