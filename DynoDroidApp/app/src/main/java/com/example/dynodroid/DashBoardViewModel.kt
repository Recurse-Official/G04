package com.example.dynodroid

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dynodroid.network.DynoApi
import com.example.dynodroid.network.MinimalMemoryRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                // Update upload stages
                _uploadState.value = UploadState.InProgress("Collecting APK files...")
                val apkFiles = getApkFilesForPackage(packageName, context)

                if (apkFiles.isEmpty()) {
                    _uploadState.value = UploadState.Failed("No APK files found for $packageName")
                    return@launch
                }

                var uploadedSize = 0L
                val totalSize = apkFiles.sumOf { it.length() }

                // Update stages for dynamic analysis
                val analysisStages = listOf(
                    "Uploading APK files...",
                    "Initializing static analysis...",
                    "Performing network behavior analysis...",
                    "Scanning for potential security threats...",
                    "Generating comprehensive report..."
                )

                apkFiles.forEach { file ->
                    try {
                        uploadLargeApk(file, appName, packageName) { chunkSize ->
                            uploadedSize += chunkSize
                            val progress = ((uploadedSize.toFloat() / totalSize) * 100).toInt()
                            _uploadState.value = UploadState.InProgress("Uploading: $progress%")
                        }
                    } catch (e: Exception) {
                        _uploadState.value = UploadState.Failed("Error uploading file: ${e.message}")
                        throw e
                    }
                }

                // Simulate dynamic analysis stages
                analysisStages.forEach { stage ->
                    _uploadState.value = UploadState.DynamicAnalysis(stage)
                    delay(1500) // Simulated processing time
                }

                // Mock analysis results (replace with actual API call)
                val mockResults = AnalysisResult(
                    dangerousPermissions = listOf(
                        "android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO"
                    ),
                    potentialThreats = listOf(
                        "Potential data exfiltration",
                        "Suspicious network connections"
                    ),
                    networkActivity = listOf(
                        "Outbound connection to unknown IP",
                        "Frequent background data transfer"
                    ),
                    staticAnalysisFindings = listOf(
                        "Obfuscated code detected",
                        "Potential code injection vulnerability"
                    )
                )

                _uploadState.value = UploadState.Success(
                    "Dynamic analysis completed for $appName",
                    mockResults
                )

            } catch (e: Exception) {
                _uploadState.value = UploadState.Failed("Analysis failed: ${e.message}")
            }
        }
    }

    private suspend fun uploadLargeApk(
        file: File,
        appName: String,
        packageName: String,
        onChunkUploaded: (Long) -> Unit
    ) {
        val chunkSize = 10 * 1024 * 1024L // 10MB chunks
        val totalChunks = (file.length() + chunkSize - 1) / chunkSize

        try {
            for (chunkIndex in 0 until totalChunks) {
                val startByte = chunkIndex * chunkSize
                val endByte = min((chunkIndex + 1) * chunkSize, file.length()) - 1

                withContext(Dispatchers.IO) {
                    uploadChunk(
                        file = file,
                        startByte = startByte,
                        endByte = endByte,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        appName = appName,
                        packageName = packageName
                    )
                    onChunkUploaded(endByte - startByte + 1)
                }
            }
        } catch (e: Exception) {
            _uploadState.value = UploadState.Failed("Upload failed for chunk: ${e.message}")
            throw e
        }
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
        packageName: String
    ) {
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
            fileName = fileNameBody
        )
        if (!response.isSuccessful) {
            throw IOException("Chunk upload failed: ${response.code()}")
        }
    }
}