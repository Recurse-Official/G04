package com.example.dynodroid

data class AnalysisResult(
    val dangerousPermissions: List<String> = listOf(),
    val potentialThreats: List<String> = listOf(),
    val networkActivity: List<String> = listOf(),
    val staticAnalysisFindings: List<String> = listOf()
)