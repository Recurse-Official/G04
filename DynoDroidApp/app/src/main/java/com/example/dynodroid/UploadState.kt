package com.example.dynodroid

sealed class UploadState {
    object Idle : UploadState()
    data class InProgress(val message: String) : UploadState()
    data class DynamicAnalysis(val stage: String) : UploadState()
    data class Success(
        val message: String,
        val analysisResult: AnalysisStatusResponse // Changed from AnalysisResult
    ) : UploadState()
    data class Failed(val errorMessage: String) : UploadState()
}