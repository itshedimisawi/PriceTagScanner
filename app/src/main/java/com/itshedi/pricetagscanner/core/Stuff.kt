package com.itshedi.pricetagscanner.core

import android.graphics.Rect
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


fun getAllRects(contours: ArrayList<MatOfPoint>): List<Rect> { //opencv
    return contours.mapNotNull{ contour ->
        if (Imgproc.contourArea(contour) > 1000) {
            // Minimum size allowed for consideration
            val approxCurve = MatOfPoint2f()
            val mat2f = MatOfPoint2f()
            contour.convertTo(mat2f, CvType.CV_32FC2)
            val contour2f = MatOfPoint2f(mat2f)
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true)

            // Get bounding rect of contour
            val boundingRect = Imgproc.boundingRect(contour)

           return@mapNotNull Rect(
                boundingRect.x,
                boundingRect.y,
                boundingRect.x + boundingRect.width,
                boundingRect.y + boundingRect.height
            )
        }else{
            return@mapNotNull null
        }
    }
}


fun findImageContours(image: Mat): ArrayList<MatOfPoint> {
    val grayMat = Mat()
    Imgproc.cvtColor(image.clone(), grayMat, Imgproc.COLOR_BGR2GRAY)

    Imgproc.threshold(grayMat, grayMat, 177.0, 200.0, Imgproc.THRESH_BINARY)

    val blurredMat = Mat()
    Imgproc.GaussianBlur(grayMat, blurredMat, Size(3.0, 3.0), 0.0, 0.0)

    val rectKernel =
        Imgproc.getStructuringElement(Imgproc.MORPH_RECT, org.opencv.core.Size(3.0, 3.0))

    val dilateMat = Mat()
    Imgproc.dilate(blurredMat, dilateMat, rectKernel)

    val cannyMat = Mat()
    Imgproc.Canny(dilateMat, cannyMat, 100.0, 200.3)

    val contours = ArrayList<MatOfPoint>()
    Imgproc.findContours(
        cannyMat,
        contours,
        Mat(),
        Imgproc.RETR_EXTERNAL,
        Imgproc.CHAIN_APPROX_SIMPLE
    )

    return contours
}
