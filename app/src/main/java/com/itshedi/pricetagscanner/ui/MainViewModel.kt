package com.itshedi.pricetagscanner.ui

import android.app.Application
import android.content.Context
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itshedi.pricetagscanner.models.ImageTextData
import com.itshedi.pricetagscanner.models.Product
import com.itshedi.pricetagscanner.models.ScannedProduct


class MainViewModel(application: Application) : AndroidViewModel(application) {

    val sharedPreferences = getApplication<Application>()
        .getSharedPreferences("main_prefs", Context.MODE_PRIVATE)


    var foundPrice by mutableStateOf<ImageTextData?>(null)

    var priceTagContour by mutableStateOf<Rect?>(null)

    var foundProductName by mutableStateOf<ImageTextData?>(null)

    var isScanning by mutableStateOf(false)

    var lastScanResult by mutableStateOf<Product?>(null)

    var cameraFocusPoint by mutableStateOf<Offset?>(null)

    var isFlashOn by mutableStateOf(false)

    var isScanButtonExtended by mutableStateOf(false)


    var showMultiplierPicker by mutableStateOf(false)
    var multiplierValue by mutableStateOf(1)

    var scannedPriceTags = mutableStateListOf<ScannedProduct>()


    fun addPriceTag(priceTag: ScannedProduct){
        scannedPriceTags.add(0,priceTag)
        scannedPriceTags.mapIndexed { index, scannedProduct -> scannedProduct.index = index }
        val gson = Gson()
        val jsonData = gson.toJson(scannedPriceTags)
        sharedPreferences.edit().putString("SAVED_PRICE_TAGS", jsonData).apply()
    }

    fun getPriceTags(){
        scannedPriceTags.clear()
        val gson = Gson()
        val listType = object : TypeToken<ArrayList<ScannedProduct>>() {}.type
        val scannedProducts = sharedPreferences.getString("SAVED_PRICE_TAGS", "[]")
        val listObjects = gson.fromJson<ArrayList<ScannedProduct>>(scannedProducts,listType)
        scannedPriceTags.addAll(listObjects.sortedBy { it.index })
    }

    fun clearScannedTags(){
        scannedPriceTags.clear()
        sharedPreferences.edit().putString("SAVED_PRICE_TAGS", "[]").apply()
    }

}