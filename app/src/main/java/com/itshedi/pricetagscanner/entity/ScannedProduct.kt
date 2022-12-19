package com.itshedi.pricetagscanner.entity

import com.google.gson.annotations.SerializedName

data class ScannedProduct(
    @SerializedName("index")
    var index: Int?=null,
    @SerializedName("productName")
    val productName: String?,
    @SerializedName("price")
    val price: Double,
    @SerializedName("multiplier")
    var multiplier: Int
)
