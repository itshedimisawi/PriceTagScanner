package com.itshedi.pricetagscanner.core

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.regex.Pattern
import kotlin.math.max


@ExperimentalPermissionsApi
fun PermissionState.isPermanentlyDenied(): Boolean {
    return !shouldShowRationale && !hasPermission
}

fun Rect.expandBy(rect: Rect): Rect {
    return Rect(
        Integer.min(this.left, rect.left), Integer.min(this.top, rect.top),
        max(this.right, rect.right), max(this.bottom, rect.bottom)
    )
}

fun Rect.isInsideOf(rect: Rect, margin: Int): Boolean {
//    return this.left+margin >= rect.left && this.top+margin >= rect.top && this.width() <= rect.width()+margin && this.height() <= rect.height()+margin &&
    return this.left + margin >= rect.left && this.top + margin >= rect.top &&
            this.bottom <= rect.bottom + margin && this.right <= rect.right + margin


}


fun CharSequence.isValidPrice(): Boolean {
    val pattern = Pattern.compile(""".{0,2}\d{1,4}(?:[.,]\d{3})*(?:[.,]\d{1,3}).{0,2}""")
    return pattern.matcher(this).matches()
}

fun CharSequence.isNumberOnly(): Boolean {
    val pattern: Pattern = Pattern.compile("""^[0-9]*""")
    return pattern.matcher(this).matches()
}

fun CharSequence.isNumberWithLeadingCurrency(): Boolean {
    val pattern: Pattern = Pattern.compile(""".{0,2}\d{1,5}""")
    return pattern.matcher(this).matches()
}


fun Image.yuvToRgba(): Mat {
    val rgbaMat = Mat()

    if (format == ImageFormat.YUV_420_888
        && planes.size == 3
    ) {

        val chromaPixelStride = planes[1].pixelStride

        if (chromaPixelStride == 2) { // Chroma channels are interleaved
            assert(planes[0].pixelStride == 1)
            assert(planes[2].pixelStride == 2)
            val yPlane = planes[0].buffer
            val uvPlane1 = planes[1].buffer
            val uvPlane2 = planes[2].buffer
            val yMat = Mat(height, width, CvType.CV_8UC1, yPlane)
            val uvMat1 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane1)
            val uvMat2 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane2)
            val addrDiff = uvMat2.dataAddr() - uvMat1.dataAddr()
            if (addrDiff > 0) {
                assert(addrDiff == 1L)
                Imgproc.cvtColorTwoPlane(yMat, uvMat1, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV12)
            } else {
                assert(addrDiff == -1L)
                Imgproc.cvtColorTwoPlane(yMat, uvMat2, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21)
            }
        } else { // Chroma channels are not interleaved
            val yuvBytes = ByteArray(width * (height + height / 2))
            val yPlane = planes[0].buffer
            val uPlane = planes[1].buffer
            val vPlane = planes[2].buffer

            yPlane.get(yuvBytes, 0, width * height)

            val chromaRowStride = planes[1].rowStride
            val chromaRowPadding = chromaRowStride - width / 2

            var offset = width * height
            if (chromaRowPadding == 0) {
                // When the row stride of the chroma channels equals their width, we can copy
                // the entire channels in one go
                uPlane.get(yuvBytes, offset, width * height / 4)
                offset += width * height / 4
                vPlane.get(yuvBytes, offset, width * height / 4)
            } else {
                // When not equal, we need to copy the channels row by row
                for (i in 0 until height / 2) {
                    uPlane.get(yuvBytes, offset, width / 2)
                    offset += width / 2
                    if (i < height / 2 - 1) {
                        uPlane.position(uPlane.position() + chromaRowPadding)
                    }
                }
                for (i in 0 until height / 2) {
                    vPlane.get(yuvBytes, offset, width / 2)
                    offset += width / 2
                    if (i < height / 2 - 1) {
                        vPlane.position(vPlane.position() + chromaRowPadding)
                    }
                }
            }

            val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
            yuvMat.put(0, 0, yuvBytes)
            Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_I420, 4)
        }
    }
    return rgbaMat
}