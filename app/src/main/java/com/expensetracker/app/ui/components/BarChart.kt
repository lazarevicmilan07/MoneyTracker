package com.expensetracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.expensetracker.app.ui.reports.MonthData
import com.expensetracker.app.ui.theme.ExpenseRed
import com.expensetracker.app.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

@Composable
fun MonthlyBarChart(
    monthlyData: List<MonthData>,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    val maxValue = monthlyData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
    val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    var selectedMonth by remember { mutableIntStateOf(-1) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(monthlyData) {
                    detectTapGestures { offset ->
                        val barGroupWidth = size.width / 12f
                        val tappedIndex = (offset.x / barGroupWidth).toInt().coerceIn(0, 11)
                        val data = monthlyData[tappedIndex]
                        if (data.income > 0 || data.expense > 0) {
                            selectedMonth = if (tappedIndex == selectedMonth) -1 else tappedIndex
                        } else {
                            selectedMonth = -1
                        }
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height - 24.dp.toPx()
            val barGroupWidth = chartWidth / 12
            val barWidth = barGroupWidth * 0.35f
            val spacing = barGroupWidth * 0.1f

            monthlyData.forEachIndexed { index, data ->
                val groupX = index * barGroupWidth
                val isSelected = index == selectedMonth

                // Income bar
                val incomeHeight = if (maxValue > 0) (data.income / maxValue * chartHeight).toFloat() else 0f
                drawRect(
                    color = if (isSelected) IncomeGreen else IncomeGreen.copy(alpha = if (selectedMonth == -1) 1f else 0.4f),
                    topLeft = Offset(
                        x = groupX + spacing,
                        y = chartHeight - incomeHeight
                    ),
                    size = Size(barWidth, incomeHeight)
                )

                // Expense bar
                val expenseHeight = if (maxValue > 0) (data.expense / maxValue * chartHeight).toFloat() else 0f
                drawRect(
                    color = if (isSelected) ExpenseRed else ExpenseRed.copy(alpha = if (selectedMonth == -1) 1f else 0.4f),
                    topLeft = Offset(
                        x = groupX + spacing + barWidth + 2.dp.toPx(),
                        y = chartHeight - expenseHeight
                    ),
                    size = Size(barWidth, expenseHeight)
                )
            }

            // Baseline
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, chartHeight),
                end = Offset(chartWidth, chartHeight),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            monthLabels.forEach { month ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Tooltip
        if (selectedMonth in monthlyData.indices) {
            val data = monthlyData[selectedMonth]
            val monthName = Month.of(selectedMonth + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Income: ${formatBarCurrency(data.income, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = IncomeGreen
                    )
                    Text(
                        text = "Expense: ${formatBarCurrency(data.expense, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ExpenseRed
                    )
                }
            }
        } else {
            // Legend
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = IncomeGreen)
                    }
                }
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.width(24.dp))

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = ExpenseRed)
                    }
                }
                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatBarCurrency(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        "$${String.format("%.2f", amount)}"
    }
}
