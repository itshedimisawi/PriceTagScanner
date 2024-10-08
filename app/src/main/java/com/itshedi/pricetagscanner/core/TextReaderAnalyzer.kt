package com.itshedi.pricetagscanner.core

import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itshedi.pricetagscanner.models.FrameResult
import com.itshedi.pricetagscanner.models.ImageTextData
import com.itshedi.pricetagscanner.models.Product
import org.opencv.core.Core
import org.opencv.core.Mat
import java.io.IOException

@ExperimentalGetImage
class TextReaderAnalyzer(
    private val onResultFound: (Product) -> Unit,
    private val onScanStateChanged: (Boolean) -> Unit,
    private val onFoundPrice: (ImageTextData?) -> Unit,
    private val onFoundProductName: (ImageTextData?) -> Unit,
    private val onFoundPriceTagContour: (Rect?) -> Unit,
) : ImageAnalysis.Analyzer {

    var lastFrameTimestamp: Long = 0


    var frameResultList = ArrayList<FrameResult>()

    private var isScanning = false

    var scannedFrames = 0

    val INITIAL_CONFIDENCE = 0.7f
    val MINIMUM_CONFIDENCE = 0.5f
    var currentConfidence = INITIAL_CONFIDENCE


    fun startScan() {
        scannedFrames = 0
        frameResultList.clear()
        isScanning = true
        onScanStateChanged(true)
    }


    override fun analyze(imageProxy: ImageProxy) {
        if (isScanning) {
            val thisImageTimestamp = System.currentTimeMillis()

            if (thisImageTimestamp - lastFrameTimestamp > 50) { // interal between frames

                lastFrameTimestamp = thisImageTimestamp
                imageProxy.image?.let { image ->
                    process(image, imageProxy)
                }
                return
            }
        }
        imageProxy.close()
    }

    private fun process(image: Image, imageProxy: ImageProxy) {
        if (scannedFrames < 18) {
            try {
                collectFrameData(
                    InputImage.fromMediaImage(
                        image,
                        imageProxy.imageInfo.rotationDegrees
                    ), imageProxy
                )
                scannedFrames++
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            electMajority(frameResultList.map { it.price.text })?.let { price ->
                "\\d{1,5}(?:[.,]\\d{3})*(?:[.,]\\d{1,3})".toRegex().find(price)
                    ?.let { cleanedPrice ->
                        onResultFound(
                            Product(
                                price = cleanedPrice.value.replace(",", ".").toDouble(),
                                name = electMajority(frameResultList.filter { it.productName != null }
                                    .map { it.productName!! }) ?: ""
                            )
                        )
                    }
            }

            isScanning = false
            onScanStateChanged(false)
            onFoundPrice(null)
            onFoundProductName(null)
            onFoundPriceTagContour(null)

            imageProxy.close()
        }

        if (scannedFrames == 10 && frameResultList.size < 5) {
            currentConfidence = MINIMUM_CONFIDENCE
        }
    }


    private fun collectFrameData(image: InputImage, imageProxy: ImageProxy) {
        if (frameResultList.size % 6 == 0) { // text recognition frame
            imageProxy.image?.let {
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener { visionText ->

                        processPriceTagFromImage(
                            visionText,
                            Size(it.width.toFloat(), it.height.toFloat()),
                            rotation = image.rotationDegrees,
                            it.yuvToRgba()
                        )?.let { fr ->
                            frameResultList.add(fr)
                        }

                        imageProxy.close()
                    }
                    .addOnFailureListener { error ->

                        error.printStackTrace()
                        imageProxy.close()
                    }
            }

        } else { // product name only frames

            imageProxy.image?.let {
                processPriceTagFromImage(
                    frameResultList.last().visionText,
                    Size(it.width.toFloat(), it.height.toFloat()),
                    rotation = image.rotationDegrees,
                    it.yuvToRgba()
                )?.let { fr ->
                    frameResultList.add(fr)
                }

                imageProxy.close()
            }
        }
    }


    private fun processPriceTagFromImage(
        visionText: Text,
        image: Size,
        rotation: Int,
        mat: Mat
    ): FrameResult? {

        // check camera sensor rotation and adjust the captured image accordingly
        when (rotation) {
            90 -> Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(mat, mat, Core.ROTATE_180)
            270 -> Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE)
        }

        val contours = findImageContours(mat)
        val rects = getAllRects(contours = contours)


        var id = 0
        val imageTextData = ArrayList<ImageTextData>()
        val blockData = ArrayList<ImageTextData>()
        for ((blocNumber, block) in visionText.textBlocks.withIndex()) {
            block.boundingBox?.let { blockFrame ->
                blockData.add(
                    ImageTextData(
                        id = id,
                        imageSize = Size(
                            image.width,
                            image.height
                        ),
                        rect = blockFrame,
                        text = block.text,
                        size = blockFrame.bottom - blockFrame.top,
                        blocNumber = blocNumber,
                        lineNumber = -1,
                        confidence = (block.lines.sumOf { it.confidence.toDouble() } / block.lines.size).toFloat(),
                    )
                )
            }
            for ((lineNumber, line) in block.lines.withIndex()) {
                for (element in line.elements) {
                    val frame = element.boundingBox
                    frame?.let {
                        imageTextData.add(
                            ImageTextData(
                                id = id,
                                imageSize = Size(
                                    image.width,
                                    image.height
                                ),
                                rect = frame,
                                text = element.text,
                                size = it.bottom - it.top,
                                blocNumber = blocNumber,
                                lineNumber = lineNumber,
                                confidence = element.confidence,
                            )
                        )
                        id++
                    }
                }
            }
        }
        return analyzeFrame(Pair(imageTextData, rects), blockData, visionText)
    }


    fun analyzeFrame(
        imageTextData: Pair<ArrayList<ImageTextData>, List<Rect>>,
        blockData: ArrayList<ImageTextData>,
        visionText: Text
    ): FrameResult? {

        val foundSurfaces = imageTextData.second

        val foundPrice = findPrice(
            imageTextData.first.sortedByDescending { it.size },
            currentConfidence
        ) //look for price and stack it

        onFoundPrice(foundPrice)
        foundPrice?.let {
            val frameResult = FrameResult(visionText, it)

            val priceTagContour =
                findPriceTagContour(foundContours = foundSurfaces, priceContour = it.rect)
            onFoundPriceTagContour(priceTagContour)

            // look for product name within the contour
            priceTagContour?.let { ptc ->
                frameResult.priceTagContour = ptc
                val foundProductName = findProductName(
                    foundShapes = blockData.sortedByDescending { z -> z.size },
                    priceTagContour = ptc,
                    priceBlockNumber = it.blocNumber
                )
                onFoundProductName(foundProductName)
                foundProductName?.let { productName ->
                    frameResult.productName = productName.text
                }
            }
            return frameResult
        }
        return null

    }

    private fun electMajority(resultList: List<String>) = resultList.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

}

