package com.itshedi.pricetagscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.itshedi.pricetagscanner.ui.theme.AccentColor
import com.itshedi.pricetagscanner.ui.theme.sheetSurfaceColor
import com.itshedi.pricetagscanner.ui.theme.sheetSurfaceDarkColor


@Composable
fun ChoiceConfirmDialog(
    showDialog: Boolean,
    message: @Composable (() -> Unit),
    okMessage: String,
    cancelMessage: String,
    onOk: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = { onDismissRequest() },
            DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (isSystemInDarkTheme()) {
                            true -> sheetSurfaceDarkColor
                            false -> sheetSurfaceColor
                        }
                    )
            ) {

                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    message()
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { onCancel() },
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = (when (isSystemInDarkTheme()) {
                                true -> sheetSurfaceDarkColor
                                false -> sheetSurfaceColor
                            }), contentColor = (when (isSystemInDarkTheme()) {
                                true -> AccentColor
                                false -> MaterialTheme.colors.onBackground
                            })
                        )
                    ) {
                        Text(
                            text = cancelMessage
                        )
                    }
                    Button(
                        onClick = { onOk() }, modifier = Modifier
                            .weight(1f)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = okMessage
                        )
                    }
                }
            }
        }
    }
}