package com.itshedi.pricetagscanner.ui.camera

import android.graphics.Rect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.itshedi.pricetagscanner.models.ImageTextData


// This is a canvas overlay on top of the camera to show all sorts of indicators and guidelines

@Composable
fun CameraCanvas(
    modifier: Modifier,
    foundPrice: ImageTextData?,
    foundProductName: ImageTextData?,
    priceTagContour: Rect?,
    cameraFocusPoint: Offset? = null
) {
    val radius = if (cameraFocusPoint==null) 46f else 56f
    val animatedRadius = animateFloatAsState(radius,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing
        )
    )

    Canvas(
        modifier = modifier,
    ) {

        val left = size.width / 10f
        val top = size.height / 2.8f
        val right = size.width - left
        val bottom = size.height - top

        val lengthF = 50.0f // Length of stand out from corners
        val strokeF = 4f

        cameraFocusPoint?.let{
            drawCircle(color = Color.White, radius = animatedRadius.value, center = it , style = Stroke(width = 4f))
        }

        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(left, top),
            end = Offset(left + lengthF, top)
        ) // Top Left to right
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(left, top),
            end = Offset(left, top + lengthF)
        ) // Top Left to bottom
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(right, top),
            end = Offset(right - lengthF, top)
        ) // Top Right to left
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(right, top),
            end = Offset(right, top + lengthF)
        ) // Top Right to Bottom
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(left, bottom),
            end = Offset(left + lengthF, bottom)
        ) // Bottom Left to right
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(left, bottom),
            end = Offset(left, bottom - lengthF)
        ) // Bottom Left to top
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(right, bottom),
            end = Offset(right - lengthF, bottom)
        ) // Bottom right to left
        drawLine(
            color = Color.White,
            strokeWidth = strokeF,
            start = Offset(right, bottom),
            end = Offset(right, bottom - lengthF)
        ) // Bottom right to top




        // Draw a rectangle around the price
        foundPrice?.let {
            val horizontalRatio =
                size.width / it.imageSize.height //good
            val verticalRatio =
                size.height / it.imageSize.width //good

            val priceLeft =
                it.rect.left * horizontalRatio
            val priceTop = it.rect.top * verticalRatio

            val rectWidth =
                it.rect.width() * horizontalRatio

            val rectHeight =
                it.rect.height() * verticalRatio

            priceTagContour?.let { ptc ->
                val ptcLeft =
                    ptc.left * horizontalRatio
                val ptcTop = ptc.top * verticalRatio

                val ptcWidth =
                    ptc.width() * horizontalRatio

                val ptcHeight =
                    ptc.height() * verticalRatio

                drawRect(
                    color = Color.White.copy(0.5f),
                    topLeft = Offset(ptcLeft, ptcTop),
                    size = Size(ptcWidth, ptcHeight)
                )
            }
            drawRect(
                color = Color.Yellow.copy(0.5f),
                topLeft = Offset(priceLeft, priceTop),
                size = Size(rectWidth, rectHeight)
            )

        }

        // Draw a rectangle around the product name
        foundProductName?.let {
            val horizontalRatio =
                size.width / it.imageSize.height //good
            val verticalRatio =
                size.height / it.imageSize.width //good

            val productNameLeft =
                it.rect.left * horizontalRatio
            val productNameTop = it.rect.top * verticalRatio

            val rectWidth =
                it.rect.width() * horizontalRatio

            val rectHeight =
                it.rect.height() * verticalRatio

            drawRect(
                color = Color.Yellow.copy(0.5f),
                topLeft = Offset(productNameLeft, productNameTop),
                size = Size(rectWidth, rectHeight)
            )
        }
    }
}
