package com.example.dynodroid

import androidx.compose.ui.graphics.ImageBitmap

data class AppDetail(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap,
    val sourceDir: String,
    val splitSourceDirs: Array<String>?
)