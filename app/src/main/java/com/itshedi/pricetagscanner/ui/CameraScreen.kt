package com.itshedi.pricetagscanner.ui

import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.itshedi.pricetagscanner.R
import com.itshedi.pricetagscanner.core.Product
import com.itshedi.pricetagscanner.entity.ImageTextData
import com.itshedi.pricetagscanner.entity.ScannedProduct
import com.itshedi.pricetagscanner.ui.theme.AccentColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Float.min
import kotlin.math.absoluteValue

@Composable
fun DismissScanButton(
    modifier: Modifier,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(0.4f))
            .padding(horizontal = 12.dp , vertical = 8.dp)
            .clickable { onDismiss() }) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.scan_again),
            tint = Color.White,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(20.dp)
        )
        Text(text = stringResource(R.string.scan_again), color = Color.White)
    }
}


@Composable
fun NoticeText(
    modifier: Modifier,
    text:String,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(0.4f))
            .padding(horizontal = 12.dp , vertical = 8.dp)) {
        Text(text = text, color = Color.White)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScanButton(
    modifier: Modifier, onScan: () -> Unit,
    isScanning: Boolean,
    result: Product?,
    size: Dp,
    maxWidth: Dp,
    maxHeight: Dp,
    onAccept: (Product, Int) -> Unit,
    onNext: () -> Unit,
    onStateChanged: (Boolean) -> Unit,
    showMultiplierPicker: Boolean,
    multiplierValue: Int,
    onMultiplierValueChange: (Int) -> Unit,
    content: @Composable (() -> Unit),
    ) {

    val transition = rememberInfiniteTransition()

    val animatedAlpha by transition.animateValue(
        initialValue = 1f,
        targetValue = 0.5f,
        typeConverter = Float.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
            // Use the default RepeatMode.Restart to start from 0.dp after each iteration
        )
    )

    val buttonAlpha = if (isScanning && result==null){
        animatedAlpha
    }else 1f
    val radius = if (result == null) {
        size / 2f
    } else 5.dp
    val animatedRadius by animateDpAsState(targetValue = radius, finishedListener = {
        onStateChanged(result != null)
    })

    val width = if (result == null) {
        size
    } else maxWidth
    val animatedWidth by animateDpAsState(targetValue = width)

    val height = if (result == null) {
        size
    } else maxHeight
    val animatedHeight by animateDpAsState(targetValue = height)


    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size = size.plus(14.dp))
                .border(width = 4.dp, color = Color.White, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .width(animatedWidth)
                .height(animatedHeight)
                .clip(RoundedCornerShape(animatedRadius))
                .background(Color.White.copy(buttonAlpha), shape = RoundedCornerShape(animatedRadius))
                .clickable(enabled = !isScanning && result == null) { onScan() },
            contentAlignment = Alignment.Center
        ) {
            if (result != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (showMultiplierPicker) {
                            DisposableEffect(Unit){
                                onDispose {
                                    onMultiplierValueChange(1)
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {

                                val cs = rememberCoroutineScope()
                                val pagerState = rememberPagerState()

                                if (pagerState.isScrollInProgress) {
                                    DisposableEffect(Unit) {
                                        onDispose {
                                            onMultiplierValueChange(pagerState.currentPage + 1)
                                        }
                                    }
                                }
                                HorizontalPager(
                                    count = 100, state = pagerState,
                                    contentPadding = PaddingValues(horizontal = maxHeight)
                                ) { multiplier ->
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                val pageOffset =
                                                    calculateCurrentOffsetForPage(multiplier).absoluteValue

                                                lerp(
                                                    start = 0.85f,
                                                    stop = 1f,
                                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                                ).also { scale ->
                                                    scaleX = scale
                                                    scaleY = scale
                                                }

                                                alpha = lerp(
                                                    start = 0.5f,
                                                    stop = 1f,
                                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                                )
                                            }
                                            .fillMaxHeight()
                                            .aspectRatio(1f)
                                            .clickable(enabled = multiplier != pagerState.currentPage) {
                                                cs.launch {
                                                    pagerState.animateScrollToPage(multiplier)
                                                }
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            text = (multiplier + 1).toString(),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.Center),
                                            color = when (multiplier == pagerState.currentPage) {
                                                true -> Color.Black
                                                else -> Color.Black.copy(ContentAlpha.disabled)
                                            }
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(1f)
                                        .align(Alignment.Center)
                                        .background(
                                            color = Color.Black.copy(0.2f),
                                            shape = RoundedCornerShape(5)
                                        )
                                        .clip(shape = RoundedCornerShape(5))
                                )
                            }

                        } else {
                            Text(
                                text = result.name.takeIf { it.trim().isNotEmpty() } ?: stringResource(R.string.unknown_product),
                                color = Color.Black,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = result.price.toString(),
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box(
                        Modifier
                            .size(maxHeight)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                color = AccentColor.copy(0.7f),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .clickable {
                                if (showMultiplierPicker) {
                                    onAccept(result, multiplierValue)
                                } else {
                                    onNext()
                                }
                            }
                            .padding(22.dp)) {
                        Icon(
                            imageVector = when (showMultiplierPicker) {
                                true -> Icons.Default.Check
                                false -> Icons.Default.ArrowRightAlt
                            },
                            contentDescription = stringResource(R.string.confirm),
                            tint = MaterialTheme.colors.contentColorFor(AccentColor),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(),
                        )
                    }
                }
            } else {
                content()
            }
        }
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
                BottomSheetContent(mainViewModel = mainViewModel)
            }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cameraContainerHeight)
            ) {

                ChoiceConfirmDialog(
                    showDialog = mainViewModel.clearScannedTagsConfirmDialog,
                    message = { Text(stringResource(R.string.confirm_delete_tags)) },
                    okMessage = stringResource(R.string.confirm),
                    cancelMessage = stringResource(R.string.cancel),
                    onOk = { mainViewModel.clearScannedTags()
                        mainViewModel.clearScannedTagsConfirmDialog = false},
                    onCancel = { mainViewModel.clearScannedTagsConfirmDialog = false},
                onDismissRequest = {mainViewModel.clearScannedTagsConfirmDialog = false })
                when (permissionState) {
                    0 -> { // note: Permission granted
                        AndroidView(modifier = Modifier.matchParentSize(),
                            factory = { context ->
                                val previewView =
                                    PreviewView(context).apply {
                                        this.scaleType =
                                            scaleType
                                        layoutParams =
                                            ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                    }

                                CoroutineScope(Dispatchers.IO).launch {
                                    onPreviewView(previewView)
                                }
                                previewView
                            })


                        LaunchedEffect(mainViewModel.cameraFocusPoint){
                            mainViewModel.cameraFocusPoint?.let{

                                delay(500)
                                mainViewModel.cameraFocusPoint = null
                            }
                        }
                        CameraCanvas(
                            modifier = Modifier
                                .fillMaxSize(), priceTagContour = mainViewModel.priceTagContour,
                            foundPrice = mainViewModel.foundPrice,
                            foundProductName = mainViewModel.foundProductName,
                            cameraFocusPoint = mainViewModel.cameraFocusPoint
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (mainViewModel.isScanning){
                                NoticeText(modifier = Modifier.padding(bottom = 16.dp),
                                    text = "Scanning..")
                            }else{
                                if (mainViewModel.isScanButtonExtended && mainViewModel.lastScanResult != null) {
                                    DismissScanButton(modifier = Modifier.padding(bottom = 16.dp),
                                        onDismiss = {
                                            mainViewModel.lastScanResult = null

                                            mainViewModel.showMultiplierPicker = false
                                        })
                                }else{
                                    if (mainViewModel.lastScanResult == null){
                                        NoticeText(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                                            text = "Press the shutter button to scan")
                                    }
                                }
                            }


                            ScanButton(
                                modifier = Modifier
                                    .padding(bottom = 40.dp),
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
                            ) {
                            }
                        }
                    }
                    1 -> { // note: Can request again
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .align(Alignment.Center)
                            .clickable {
                                onRequestPermission()
                            }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = stringResource(R.string.camera_permission_needed), modifier = Modifier.padding(bottom = 12.dp))
                            Text(stringResource(R.string.camera_permission_needed), textAlign = TextAlign.Center)
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
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = stringResource(R.string.camera_permission_perma_denied), modifier = Modifier.padding(bottom = 12.dp))
                            Text(stringResource(R.string.camera_permission_perma_denied), textAlign = TextAlign.Center)
                        }
                    }
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(0.9f),
                                    Color.Transparent
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
                        HomeActionBar(
                            modifier = Modifier,
                            onMenu = {  },
                            onFlash = {
                                onFlash()
                            },
                            isFlashOn = mainViewModel.isFlashOn
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeActionBar(
    modifier: Modifier, onMenu: () -> Unit, onFlash: () -> Unit,
    isFlashOn: Boolean
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onMenu() }
            .padding(12.dp)) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = stringResource(R.string.menu), tint = Color.White)
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
                }, contentDescription = stringResource(R.string.turn_on_flash),
                tint = Color.White
            )
        }
    }
}

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
    ))



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

