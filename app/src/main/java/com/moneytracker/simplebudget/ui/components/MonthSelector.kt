package com.moneytracker.simplebudget.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneytracker.simplebudget.R
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit = {},
    yearOnly: Boolean = false
) {
    val displayText = if (yearOnly) {
        selectedMonth.year.toString()
    } else {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        selectedMonth.format(formatter).replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onMonthClick)
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    yearOnly: Boolean = false
) {
    var selectedYear by remember { mutableIntStateOf(selectedMonth.year) }
    var selectedMonthValue by remember { mutableIntStateOf(selectedMonth.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(if (yearOnly) R.string.period_select_year else R.string.period_select_month_year),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            if (yearOnly) {
                YearGridContent(
                    selectedYear = selectedYear,
                    onYearChange = { selectedYear = it }
                )
            } else {
                Column {
                    // Year stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
                        }
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next year")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month grid — names derived from current locale
                    val months = remember(Locale.getDefault()) {
                        (1..12).map { Month.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..3) {
                                    val monthIndex = row * 4 + col + 1
                                    val isSelected = monthIndex == selectedMonthValue
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedMonthValue = monthIndex },
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = months[monthIndex - 1],
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onMonthSelected(YearMonth.of(selectedYear, if (yearOnly) 1 else selectedMonthValue))
            }) {
                Text(stringResource(R.string.button_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
private fun YearGridContent(
    selectedYear: Int,
    onYearChange: (Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    val years = (currentYear - 10)..(currentYear + 5)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        years.toList().chunked(4).forEach { rowYears ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowYears.forEach { year ->
                    val isSelected = year == selectedYear
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onYearChange(year) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = year.toString(),
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                repeat(4 - rowYears.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
