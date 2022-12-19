@file:OptIn(ExperimentalPermissionsApi::class)

package com.itshedi.pricetagscanner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itshedi.pricetagscanner.entity.ImageTextData
import com.itshedi.pricetagscanner.core.TextReaderAnalyzer
import com.itshedi.pricetagscanner.core.isPermanentlyDenied
import com.itshedi.pricetagscanner.ui.theme.PTScanTheme
import org.opencv.android.OpenCVLoader
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SuppressLint("UnsafeOptInUsageError")
class MainActivity : ComponentActivity() {


    private var viewModel: MainViewModel? = null

    private val textReaderAnalyzer by lazy {
        TextReaderAnalyzer(
            ::onResultFound,
            ::onScanStateChanged,
            ::onFoundPrice,
            ::onFoundProductName,
            ::onFoundPriceTagContour,
            ::onBenchmark
        )
    }


    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    textReaderAnalyzer
                )
            }
    }


    @OptIn(ExperimentalPermissionsApi::class)
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (OpenCVLoader.initDebug()) {
            Log.d("yayy", "OpenCv configured successfully")
        } else {
            Log.d("yayy", "OpenCv configuration failed")
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel?.getPriceTags()


        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


        viewModel?.let { mainViewModel ->

            setContent {
                PTScanTheme {

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val context = LocalContext.current
                        val permissionsState = rememberMultiplePermissionsState(
                            permissions = listOf(
                                Manifest.permission.CAMERA,
                            )
                        )
                        val lifecycleOwner = LocalLifecycleOwner.current

                        DisposableEffect(
                            key1 = lifecycleOwner,
                            effect = {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_START) {
                                        permissionsState.launchMultiplePermissionRequest()
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)

                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }
                        )
                        permissionsState.permissions.forEach { perm ->
                            when (perm.permission) {
                                Manifest.permission.CAMERA -> {
                                    val systemUiController = rememberSystemUiController()
                                    systemUiController.setSystemBarsColor(
                                        color = Color.Transparent
                                    )
                                    CameraScreen(
                                        onPreviewView = {
                                            startCamera(it)
                                        }, mainViewModel = mainViewModel,
                                        onScan = {
                                            textReaderAnalyzer.startScan()
                                        },
                                        onFlash = {
                                            imageAnalyzer.camera?.let {
                                                if (it.cameraInfo.hasFlashUnit()) {
                                                    it.cameraControl.enableTorch(!mainViewModel.isFlashOn)
                                                    mainViewModel.isFlashOn =
                                                        !mainViewModel.isFlashOn
                                                }
                                            }
                                        }, permissionState = (when {
                                            perm.hasPermission -> 0
                                            perm.shouldShowRationale -> 1
                                            perm.isPermanentlyDenied() -> 2
                                            else -> 1
                                        }),
                                        onRequestPermission = { permissionsState.launchMultiplePermissionRequest() })
                                }
                            }
                        }

                    }
                }


            }
        }

    }

    private fun onBenchmark(string: String) {
        viewModel?.bmv = string
    }

    private fun onResultFound(result: Pair<Double, String?>?) {
        if (result?.first == null) {
            viewModel?.lastScanResult = null
        } else {
            viewModel?.lastScanResult = result
        }
    }

    private fun onFoundPrice(price: ImageTextData?) {
        viewModel?.foundPrice = price
    }

    private fun onFoundProductName(productName: ImageTextData?) {
        viewModel?.foundProductName = productName
    }

    private fun onFoundPriceTagContour(contour: Rect?) {
        viewModel?.priceTagContour = contour
    }

    private fun onScanStateChanged(state: Boolean) {
        viewModel?.isScanning = state
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera(cameraPreview: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val preview = androidx.camera.core.Preview.Builder()
            .build()

        cameraProviderFuture.addListener(
            Runnable {
                preview.setSurfaceProvider(cameraPreview.surfaceProvider)
                cameraProviderFuture.get().bind(preview, imageAnalyzer)
            },
            ContextCompat.getMainExecutor(this)
        )

        cameraPreview.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                MotionEvent.ACTION_UP -> {
                    val factory =
                        cameraPreview.meteringPointFactory

                    val point = factory.createPoint(
                        motionEvent.x,
                        motionEvent.y
                    )

                    val action =
                        FocusMeteringAction.Builder(
                            point
                        ).build()

                    preview.camera?.cameraControl?.startFocusAndMetering(
                        action
                    )
                    view.performClick()
                    viewModel?.cameraFocusPoint = Offset(motionEvent.x, motionEvent.y)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }


    }

    private fun ProcessCameraProvider.bind(
        preview: androidx.camera.core.Preview,
        imageAnalyzer: ImageAnalysis
    ) = try {
        unbindAll()
        bindToLifecycle(
            this@MainActivity,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalyzer
        )
    } catch (ise: IllegalStateException) {
        // Thrown if binding is not done from the main thread
//        Log.e(TAG, "Binding failed", ise)
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


}


