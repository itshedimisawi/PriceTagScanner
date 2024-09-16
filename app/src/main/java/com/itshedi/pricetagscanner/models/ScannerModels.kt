package com.itshedi.pricetagscanner.models

import android.graphics.Rect
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.text.Text


data class FrameResult(
    val visionText: Text,// for the next frame to use
    val price: ImageTextData,
    var productName: String? = null,
    var priceTagContour: Rect? = null
)


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