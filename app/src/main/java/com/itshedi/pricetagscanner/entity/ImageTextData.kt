package com.itshedi.pricetagscanner.entity

import android.graphics.Rect
import androidx.compose.ui.geometry.Size


data class ImageTextData(
    val id: Int,
    val imageSize: Size,
    val rect: Rect,
    val text: String,
    val size: Int,
    val blocNumber: Int,
    val lineNumber: Int,
    val confidence: Float,
)
