package com.itshedi.pricetagscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itshedi.pricetagscanner.R
import com.itshedi.pricetagscanner.models.ScannedProduct
import com.itshedi.pricetagscanner.ui.theme.*
import java.text.DecimalFormat

@Composable
fun BottomSheetHeader(
    modifier: Modifier,
    scannedProductCount: Int,
    total: Double?,
    onClear: () -> Unit
) {
    Row(
        modifier = modifier.padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Row(
                modifier = Modifier, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = DecimalFormat("#,###.00").format(total),
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 16.sp,
                    color = when (isSystemInDarkTheme()) {
                        true -> greenTextDarkColor
                        else -> greenTextColor
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "$scannedProductCount " + stringResource(R.string.scanned),
                color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium),
                fontSize = 14.sp
            )
        }

        Box(modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClear() }
            .border(
                color = MaterialTheme.colors.onBackground.copy(0.6f),
                shape = CircleShape,
                width = 1.dp
            )
            .padding(12.dp), contentAlignment = Center) {
            Icon(
                imageVector = Icons.Outlined.ClearAll,
                contentDescription = stringResource(R.string.clear_all_products),
                tint = MaterialTheme.colors.onBackground.copy(0.6f)
            )
        }


    }
}

@Composable
fun BottomSheetHandle(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.onBackground.copy(0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .width(32.dp)
            .height(4.dp)
    )
}

@Composable
fun BottomSheetESP(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Center) {
        Text(
            text = stringResource(R.string.no_price_tags),
            color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium),
            fontSize = 16.sp
        )
    }
}

@Composable
fun BottomSheetContent(mainViewModel: MainViewModel) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
    ) {


        when (mainViewModel.scannedPriceTags.size == 0) {
            true -> BottomSheetESP(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .padding(
                        16.dp
                    )
            )
            false -> {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .background(
                            when (isSystemInDarkTheme()) {
                                true -> sheetBackgroundDarkColor
                                else -> sheetBackgroundColor
                            }
                        )
                ) {


                    BottomSheetHeader(modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (isSystemInDarkTheme()) {
                                true -> sheetSurfaceDarkColor
                                else -> sheetSurfaceColor
                            }
                        )
                        .padding(top = 20.dp) // for handle after background to include it to the handle
                        .padding(
                            start = 16.dp, end = 16.dp
                        ),
                        scannedProductCount = mainViewModel.scannedPriceTags.size,
                        total = mainViewModel.scannedPriceTags.sumOf { it.price * it.multiplier },
                        onClear = { mainViewModel.clearScannedTagsConfirmDialog = true })


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                when (isSystemInDarkTheme()) {
                                    true -> sheetSurfaceDarkColor
                                    else -> sheetSurfaceColor
                                }
                            )
                    ) {

                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(mainViewModel.scannedPriceTags) { index, item ->
                                Column {
                                    ScannedProductItem(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                        product = item,
                                        onDelete = {
                                            mainViewModel.scannedPriceTags.removeAt(index)
                                        })
                                    if (index + 1 < mainViewModel.scannedPriceTags.size) {
                                        Divider(
                                            color = MaterialTheme.colors.onBackground.copy(0.2f)
                                        )
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            BottomSheetHandle(
                modifier = Modifier
                    .align(Center)
                    .padding(top = 8.dp)
            )
        }// 20 padding
    }
}

@Composable
fun ScannedProductItem(modifier: Modifier, product: ScannedProduct, onDelete: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${product.multiplier}x ${product.productName ?: stringResource(R.string.unknown_product)}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                Text(
                    text = "${DecimalFormat("#,###.00").format(product.price)} x ${product.multiplier} = ${
                        DecimalFormat(
                            "#,###.00"
                        ).format(product.price * product.multiplier)
                    }",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
                )
            }
        }

        Box(modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable { onDelete() }
            .border(
                color = MaterialTheme.colors.onBackground.copy(0.2f),
                shape = CircleShape,
                width = 1.dp
            )
            .padding(8.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.remove_product),
                tint = MaterialTheme.colors.onBackground.copy(0.2f)
            )
        }

    }
}