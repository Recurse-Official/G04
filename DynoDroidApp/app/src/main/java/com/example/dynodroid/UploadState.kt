package com.example.dynodroid

sealed class UploadState {
    data object Idle : UploadState()
    data class InProgress(val message: String) : UploadState()
    data class DynamicAnalysis(val stage: String) : UploadState()
    data class Success(
        val message: String,
        val analysisResult: AnalysisStatusResponse,
        val staticAnalysisResult: StaticAnalysisResult? = null
    ) : UploadState()
    data class Failed(val errorMessage: String) : UploadState()
}