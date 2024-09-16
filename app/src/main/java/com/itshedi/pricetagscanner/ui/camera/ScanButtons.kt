package com.itshedi.pricetagscanner.ui.camera

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.itshedi.pricetagscanner.R
import com.itshedi.pricetagscanner.models.Product
import com.itshedi.pricetagscanner.ui.theme.AccentColor
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


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

    val buttonAlpha = if (isScanning && result == null) {
        animatedAlpha
    } else 1f
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
        modifier = modifier, contentAlignment = Alignment.Center
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
                .background(
                    Color.White.copy(buttonAlpha), shape = RoundedCornerShape(animatedRadius)
                )
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
                            DisposableEffect(Unit) {
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
                                    count = 100,
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = maxHeight)
                                ) { multiplier ->
                                    Box(modifier = Modifier
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
                                        .padding(2.dp)) {
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
                            Text(text = result.name.takeIf { it.trim().isNotEmpty() }
                                ?: stringResource(R.string.unknown_product),
                                color = Color.Black,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
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
                            color = AccentColor.copy(0.7f), shape = RoundedCornerShape(5.dp)
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
                                false -> Icons.AutoMirrored.Filled.ArrowRightAlt
                            },
                            contentDescription = stringResource(R.string.confirm),
                            tint = MaterialTheme.colors.contentColorFor(AccentColor),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DismissScanButton(
    modifier: Modifier, onDismiss: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
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