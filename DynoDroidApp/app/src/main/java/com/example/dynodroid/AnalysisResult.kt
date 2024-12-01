package com.example.dynodroid

data class AnalysisResult(
    val status: Boolean,
    val topSyscalls: List<List<Any>> = emptyList(),
    val topRedZonesyscalls: List<List<Any>> = emptyList()
)