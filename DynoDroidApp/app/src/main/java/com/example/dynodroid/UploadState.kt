package com.example.dynodroid

sealed class UploadState {
    data object Idle : UploadState()
    data class InProgress(val message: String) : UploadState()
    data class Success(val message: String) : UploadState()
    data class Failed(val message: String) : UploadState()
}