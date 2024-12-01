package com.example.dynodroid


data class AnalysisStatusResponse(
    val alreadyAnalyzed: Boolean,
    val previousResults: AnalysisResult?
)