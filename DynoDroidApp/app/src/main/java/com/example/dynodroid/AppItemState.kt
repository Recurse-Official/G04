package com.example.dynodroid

import androidx.compose.ui.graphics.ImageBitmap

data class AppItemState(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap // Replace with the appropriate type for your app icons
)