package com.itshedi.pricetagscanner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itshedi.pricetagscanner.core.TextReaderAnalyzer
import com.itshedi.pricetagscanner.ui.theme.PTScanTheme
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SuppressLint("UnsafeOptInUsageError")
class MainActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val textReaderAnalyzer by lazy {
        TextReaderAnalyzer(
            onResultFound = {
                viewModel.lastScanResult = it
            },
            onScanStateChanged = {
                viewModel.isScanning = it
            },
            onFoundPrice = {
                viewModel.foundPrice = it
            },
            onFoundProductName = {
                viewModel.foundProductName = it
            },
            onFoundPriceTagContour = {
                viewModel.priceTagContour = it
            },
        )
    }

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also {
            it.setAnalyzer(
                cameraExecutor, textReaderAnalyzer
            )
        }
    }


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (OpenCVLoader.initDebug()) {
            // OpenCV configured correctly
        } else {
            // OpenCV is not configured correctly
        }

        viewModel.getPriceTags()

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        setContent {
            PTScanTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    var isCameraPermissionGranted by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this@MainActivity, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val lifecycleOwner = LocalLifecycleOwner.current

                    DisposableEffect(key1 = lifecycleOwner, effect = {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) {
                                isCameraPermissionGranted = ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)

                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    })
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        isCameraPermissionGranted = isGranted
                    }

                    LaunchedEffect(true) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity, Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }

                    CameraScreen(onPreviewView = {
                        startCamera(it)
                    }, mainViewModel = viewModel, onScan = {
                        textReaderAnalyzer.startScan()
                    }, onFlash = {
                        imageAnalyzer.camera?.let {
                            if (it.cameraInfo.hasFlashUnit()) {
                                it.cameraControl.enableTorch(!viewModel.isFlashOn)
                                viewModel.isFlashOn = !viewModel.isFlashOn
                            }
                        }
                    }, permissionState = (when {
                        isCameraPermissionGranted -> 0
                        else -> 1
                    }), onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    })
                }
            }
        }
    }


    @SuppressLint("RestrictedApi")
    private fun startCamera(cameraPreview: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val preview = androidx.camera.core.Preview.Builder().build()

        cameraProviderFuture.addListener(
            {
                preview.setSurfaceProvider(cameraPreview.surfaceProvider)
                cameraProviderFuture.get().bind(preview, imageAnalyzer)
            }, ContextCompat.getMainExecutor(this)
        )

        cameraPreview.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                MotionEvent.ACTION_UP -> {
                    val factory = cameraPreview.meteringPointFactory

                    val point = factory.createPoint(
                        motionEvent.x, motionEvent.y
                    )

                    val action = FocusMeteringAction.Builder(
                        point
                    ).build()

                    preview.camera?.cameraControl?.startFocusAndMetering(
                        action
                    )
                    view.performClick()
                    viewModel.cameraFocusPoint = Offset(motionEvent.x, motionEvent.y)
                    return@setOnTouchListener true
                }

                else -> return@setOnTouchListener false
            }
        }
    }

    private fun ProcessCameraProvider.bind(
        preview: androidx.camera.core.Preview, imageAnalyzer: ImageAnalysis
    ) = try {
        unbindAll()
        bindToLifecycle(
            this@MainActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
        )
    } catch (ise: IllegalStateException) {
        // Thrown if binding is not done from the main thread
        // Log.e(TAG, "Binding failed", ise)
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}


