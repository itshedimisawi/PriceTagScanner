package com.itshedi.pricetagscanner.core

import android.graphics.Rect
import com.itshedi.pricetagscanner.models.ImageTextData
import kotlin.math.abs


fun findBadlyFormattedPrice(
    founShapes: List<ImageTextData>,
    confidence: Float
): Pair<ImageTextData, Float>? {

    founShapes.filter {
        it.text.trim().isNumberWithLeadingCurrency()
    }.maxByOrNull { it.size }?.let { bigNumber -> //find biggest text

        founShapes
            .filter { possibleSmallNumber ->

                possibleSmallNumber.id != bigNumber.id &&
                        possibleSmallNumber.text.replace(" ", "")
                            .isNumberOnly() /* todo: check for currency sign*/ &&
                        possibleSmallNumber.rect.left > bigNumber.rect.left + bigNumber.rect.width() / 2 &&
//                        possibleSmallNumber.lineNumber == bigNumber.lineNumber &&
                        abs(possibleSmallNumber.blocNumber - bigNumber.blocNumber) <= 1
            }.minByOrNull { it.rect.left }?.let { //closest to the left / bigNumber
                if (bigNumber.confidence < confidence || it.confidence < confidence) {
                    return null
                }


                return Pair(
                    bigNumber.copy(
                        text = "${bigNumber.text}.${it.text}",
                        rect = Rect(
                            bigNumber.rect.left, bigNumber.rect.top, it.rect.right,
                            bigNumber.rect.bottom
                        )
                    ),
                    bigNumber.size.toFloat()
                )
            }

    }
    return null
}

fun findPrice(
    founShapes: List<ImageTextData>,
    confidence: Float
): ImageTextData? { // shapes are sorted by size desc


    founShapes.firstOrNull()?.let { if (it.text.isValidPrice()) return it }

    val badlyFormattedPrice = findBadlyFormattedPrice(founShapes.toList(), confidence)

    val price: ImageTextData? = founShapes.firstOrNull { it.text.isValidPrice() }

    //if both bfp and price are found
    //return bfp if it has higher average size than price
    badlyFormattedPrice?.let { bfp ->

        price?.let { p -> //todo: price shouldn't be bigger by small fraction

            if (bfp.second > p.size) return bfp.first
            else return price
        }
        return bfp.first
    }


    return price
}

fun findPriceTagContour(
    foundContours: List<Rect>,
    priceContour: Rect,
): Rect? {
    return foundContours.filter { //smallest possible pricetagcontour
        priceContour.isInsideOf(it, 0)
    }.sortedBy { it.width() * it.height() }.getOrNull(0)
}


fun findProductName(
    foundShapes: List<ImageTextData>,
    priceBlockNumber: Int,
    priceTagContour: Rect
): ImageTextData? { // shapes are sorted by size desc


    return foundShapes.firstOrNull { shape ->
        shape.blocNumber != priceBlockNumber &&
                shape.text.filter { it==' ' || it.isLetter() }.length > shape.text.length * 0.5f &&
                shape.rect.isInsideOf(priceTagContour, 10) // inside of pricetagcontour
    }


}

