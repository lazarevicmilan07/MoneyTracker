package com.expensetracker.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

enum class PeriodType {
    MONTH, YEAR
}

data class ExportPeriod(
    val type: PeriodType,
    val year: Int,
    val month: Int? = null // Only used when type is MONTH
) {
    fun getFileName(prefix: String, extension: String): String {
        return when (type) {
            PeriodType.MONTH -> {
                val monthName = Month.of(month!!).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                "${prefix}_${monthName}_${year}.${extension}"
            }
            PeriodType.YEAR -> "${prefix}_${year}.${extension}"
        }
    }

    fun getDisplayName(): String {
        return when (type) {
            PeriodType.MONTH -> {
                val monthName = Month.of(month!!).getDisplayName(TextStyle.FULL, Locale.getDefault())
                "$monthName $year"
            }
            PeriodType.YEAR -> year.toString()
        }
    }
}

@Composable
fun PeriodSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onPeriodSelected: (ExportPeriod) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf<PeriodType?>(null) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }

    val currentYear = LocalDate.now().year
    val years = (currentYear downTo currentYear - 10).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    IconButton(
                        onClick = { step-- },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when (step) {
                        1 -> title
                        2 -> if (selectedType == PeriodType.MONTH) "Select Month" else "Select Year"
                        3 -> "Select Year"
                        else -> title
                    }
                )
            }
        },
        text = {
            when (step) {
                1 -> {
                    // Step 1: Choose period type
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select period for export:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PeriodTypeOption(
                            icon = Icons.Default.CalendarToday,
                            title = "Monthly",
                            description = "Export data for a specific month",
                            onClick = {
                                selectedType = PeriodType.MONTH
                                step = 2
                            }
                        )

                        PeriodTypeOption(
                            icon = Icons.Default.CalendarMonth,
                            title = "Yearly",
                            description = "Export data for an entire year",
                            onClick = {
                                selectedType = PeriodType.YEAR
                                step = 2
                            }
                        )
                    }
                }

                2 -> {
                    if (selectedType == PeriodType.MONTH) {
                        // Step 2 for monthly: Select month first
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(Month.entries.toList()) { month ->
                                val monthName = month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = monthName,
                                            fontWeight = if (month.value == selectedMonth) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    trailingContent = {
                                        if (month.value == selectedMonth) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        selectedMonth = month.value
                                        step = 3
                                    }
                                )
                            }
                        }
                    } else {
                        // Step 2 for yearly: Select year
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(years) { year ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = year.toString(),
                                            fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    trailingContent = {
                                        if (year == selectedYear) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        selectedYear = year
                                        onPeriodSelected(ExportPeriod(PeriodType.YEAR, year))
                                    }
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // Step 3 for monthly: Select year (after month)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(years) { year ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = year.toString(),
                                        fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                trailingContent = {
                                    if (year == selectedYear) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    selectedYear = year
                                    onPeriodSelected(
                                        ExportPeriod(PeriodType.MONTH, year, selectedMonth)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PeriodTypeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
