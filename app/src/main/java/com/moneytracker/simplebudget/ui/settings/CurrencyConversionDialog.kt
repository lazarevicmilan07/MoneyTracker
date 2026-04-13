package com.moneytracker.simplebudget.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneytracker.simplebudget.R

// Removes the default 12dp start padding so button labels align with body text
private val ButtonContentPadding = PaddingValues(start = 0.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)

@Composable
fun CurrencyConversionDialog(
    fromCurrency: String,
    toCurrency: String,
    uiState: CurrencyConversionUiState,
    onFetchRate: () -> Unit,
    onManualRateChanged: (String) -> Unit,
    onEnterManually: () -> Unit,
    onConfirmConvert: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val isConvertEnabled = !uiState.isConverting && when {
        uiState.rateState is RateState.Success && !uiState.isManualEntry -> true
        uiState.rateState is RateState.Loading -> false
        else -> uiState.manualRateInput.toDoubleOrNull()?.let { it > 0.0 } == true
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isManualEntry) {
        if (uiState.isManualEntry) {
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title row with X button on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dialog_convert_currency_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !uiState.isConverting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.button_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body description
                Text(
                    text = stringResource(
                        R.string.dialog_convert_currency_body,
                        uiState.transactionCount,
                        fromCurrency,
                        toCurrency
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rate content
                when (val rs = uiState.rateState) {
                    is RateState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = stringResource(R.string.dialog_convert_rate_fetching),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is RateState.Success -> {
                        if (uiState.isManualEntry) {
                            OutlinedTextField(
                                value = uiState.manualRateInput,
                                onValueChange = onManualRateChanged,
                                label = { Text(stringResource(R.string.dialog_convert_rate_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.dialog_convert_rate_label,
                                    fromCurrency,
                                    "%.4f".format(rs.rate),
                                    toCurrency
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = onEnterManually,
                                contentPadding = ButtonContentPadding
                            ) {
                                Text(stringResource(R.string.button_enter_rate_manually))
                            }
                        }
                    }

                    is RateState.Failed -> {
                        Text(
                            text = stringResource(R.string.dialog_convert_rate_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.manualRateInput,
                            onValueChange = onManualRateChanged,
                            label = { Text(stringResource(R.string.dialog_convert_rate_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp).padding(top = 1.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.dialog_convert_rate_tooltip,
                                    fromCurrency,
                                    toCurrency
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onFetchRate,
                            contentPadding = ButtonContentPadding
                        ) {
                            Text(stringResource(R.string.dialog_convert_rate_retry))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row {
                    TextButton(
                        onClick = onSkip,
                        enabled = !uiState.isConverting,
                        contentPadding = ButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.button_keep_amounts),
                            color = if (uiState.isConverting)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onConfirmConvert,
                        enabled = isConvertEnabled,
                        contentPadding = ButtonContentPadding
                    ) {
                        if (uiState.isConverting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.button_convert))
                        }
                    }
                }
            }
        }
    }
}
