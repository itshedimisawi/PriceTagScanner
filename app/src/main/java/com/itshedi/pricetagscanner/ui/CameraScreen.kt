package com.itshedi.pricetagscanner.ui

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.itshedi.pricetagscanner.R
import com.itshedi.pricetagscanner.models.ScannedProduct
import com.itshedi.pricetagscanner.ui.camera.BottomSheetContent
import com.itshedi.pricetagscanner.ui.camera.CameraCanvas
import com.itshedi.pricetagscanner.ui.camera.DismissScanButton
import com.itshedi.pricetagscanner.ui.camera.ScanButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Float.min


@Composable
fun NoticeText(
    modifier: Modifier,
    text: String,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color.White)
    }
}


@Composable
fun CameraScreen(
    permissionState: Int, // 0 granted 1 denied once 2 perma denied
    onRequestPermission: () -> Unit,
    onPreviewView: (PreviewView) -> Unit,
    mainViewModel: MainViewModel,
    onScan: () -> Unit,
    onFlash: () -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val cs = rememberCoroutineScope()
    BackHandler(scaffoldState.bottomSheetState.isExpanded) {
        cs.launch { scaffoldState.bottomSheetState.collapse() }
    }
    var sheetPeekHeight by remember { mutableStateOf<Dp>(0.dp) }
    var cameraContainerHeight by remember { mutableStateOf<Dp>(0.dp) }
    var maxContainerWidth by remember { mutableStateOf<Dp>(0.dp) }
    val densityValue = LocalDensity.current
    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned {
            with(densityValue) {
                val preferredRatio =
                    min(16f / 9f, (it.size.height.toFloat() * 0.8f) / it.size.width.toFloat())
                cameraContainerHeight = (it.size.width * preferredRatio).toDp()
                sheetPeekHeight = (it.size.height - (it.size.width * preferredRatio)).toDp()
                maxContainerWidth = it.size.width.toDp()
            }
        }) {
        BottomSheetScaffold(scaffoldState = scaffoldState,
            sheetPeekHeight = sheetPeekHeight.plus(16.dp),
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                BottomSheetContent(scannedPriceTags = mainViewModel.scannedPriceTags,
                    onRemovePriceTag = {
                        mainViewModel.scannedPriceTags.removeAt(it)
                    },
                    onClearScannedTags = {
                        mainViewModel.scannedPriceTags.clear()
                    })
            }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cameraContainerHeight)
            ) {
                when (permissionState) {
                    0 -> { // Permission granted
                        AndroidView(modifier = Modifier.matchParentSize(), factory = { context ->
                            val previewView = PreviewView(context).apply {
                                this.scaleType = scaleType
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            CoroutineScope(Dispatchers.IO).launch {
                                onPreviewView(previewView)
                            }
                            previewView
                        })

                        LaunchedEffect(mainViewModel.cameraFocusPoint) {
                            mainViewModel.cameraFocusPoint?.let {

                                delay(500)
                                mainViewModel.cameraFocusPoint = null
                            }
                        }
                        CameraCanvas(
                            modifier = Modifier.fillMaxSize(),
                            priceTagContour = mainViewModel.priceTagContour,
                            foundPrice = mainViewModel.foundPrice,
                            foundProductName = mainViewModel.foundProductName,
                            cameraFocusPoint = mainViewModel.cameraFocusPoint
                        )

                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (mainViewModel.isScanning) {
                                NoticeText(
                                    modifier = Modifier.padding(bottom = 16.dp), text = "Scanning.."
                                )
                            } else {
                                if (mainViewModel.isScanButtonExtended && mainViewModel.lastScanResult != null) {
                                    DismissScanButton(
                                        modifier = Modifier.padding(bottom = 16.dp),
                                        onDismiss = {
                                            mainViewModel.lastScanResult = null

                                            mainViewModel.showMultiplierPicker = false
                                        })
                                } else {
                                    if (mainViewModel.lastScanResult == null) {
                                        NoticeText(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .padding(bottom = 16.dp),
                                            text = "Press the shutter button to scan"
                                        )
                                    }
                                }
                            }


                            ScanButton(
                                modifier = Modifier.padding(bottom = 40.dp),
                                onScan = {
                                    onScan()
                                },
                                isScanning = mainViewModel.isScanning,
                                size = 70.dp,
                                maxWidth = maxContainerWidth,
                                maxHeight = 90.dp,
                                result = mainViewModel.lastScanResult,
                                onAccept = { item, multiplier ->
                                    mainViewModel.addPriceTag(
                                        ScannedProduct(
                                            productName = item.name,
                                            price = item.price,
                                            multiplier = multiplier
                                        )
                                    )
                                    mainViewModel.showMultiplierPicker = false
                                    mainViewModel.lastScanResult = null
                                },
                                onNext = {
                                    mainViewModel.showMultiplierPicker = true
                                },
                                onMultiplierValueChange = {
                                    mainViewModel.multiplierValue = it
                                },
                                multiplierValue = mainViewModel.multiplierValue,
                                onStateChanged = { mainViewModel.isScanButtonExtended = it },
                                showMultiplierPicker = mainViewModel.showMultiplierPicker
                            )
                        }
                    }

                    1 -> { // note: Can request again
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .align(Alignment.Center)
                                .clickable {
                                    onRequestPermission()
                                }, horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.camera_permission_needed),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                stringResource(R.string.camera_permission_needed),
                                textAlign = TextAlign.Center
                            )
                        }

                    }

                    2 -> { // note: Perma denied
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.camera_permission_perma_denied),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                stringResource(R.string.camera_permission_perma_denied),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(0.9f), Color.Transparent
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .systemBarsPadding()
                    ) { // overlay content
                        HomeActionBar(modifier = Modifier, onMenu = { }, onFlash = {
                            onFlash()
                        }, isFlashOn = mainViewModel.isFlashOn
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeActionBar(
    modifier: Modifier, onMenu: () -> Unit, onFlash: () -> Unit, isFlashOn: Boolean
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onMenu() }
            .padding(12.dp)) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.app_title),
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Box(modifier = modifier
            .align(Alignment.CenterEnd)
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onFlash() }
            .padding(12.dp)) {
            Icon(
                imageVector = when (isFlashOn) {
                    true -> Icons.Default.FlashOff
                    else -> Icons.Default.FlashOn
                }, contentDescription = stringResource(R.string.turn_on_flash), tint = Color.White
            )
        }
    }
}

