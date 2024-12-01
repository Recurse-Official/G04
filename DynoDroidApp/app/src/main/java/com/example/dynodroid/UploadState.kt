package com.example.dynodroid

sealed class UploadState {
    object Idle : UploadState()
    data class InProgress(val message: String) : UploadState()
    data class DynamicAnalysis(val stage: String) : UploadState()
    data class Success(val message: String, val analysisResults: AnalysisResult? = null) : UploadState()
    data class Failed(val message: String) : UploadState()
}