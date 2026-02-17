package com.expensetracker.app.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.domain.model.TransactionType
import com.expensetracker.app.domain.model.CategoryBreakdown
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.components.CollapsibleSummaryCard
import com.expensetracker.app.ui.components.MonthSelector
import com.expensetracker.app.ui.components.MonthYearPickerDialog
import com.expensetracker.app.ui.components.CurrencyAmountText
import com.expensetracker.app.ui.components.ScrollToTopButton
import com.expensetracker.app.ui.components.formatCurrency
import com.expensetracker.app.ui.components.formatNumber
import com.expensetracker.app.ui.components.rememberCollapseProgress
import com.expensetracker.app.ui.theme.ExpenseRed
import com.expensetracker.app.ui.theme.IncomeGreen
import com.expensetracker.app.ui.theme.TransferBlue
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onViewTransaction: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = selectedMonth,
            onMonthSelected = { yearMonth ->
                viewModel.selectMonth(yearMonth)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScrollToTopButton(
                    listState = listState,
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    }
                )
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val swipeThreshold = 100f
            val dragOffset = remember { Animatable(0f) }
            val collapseProgress = rememberCollapseProgress(listState)

            // Scroll to top when screen is navigated to
            LaunchedEffect(Unit) {
                listState.scrollToItem(0)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Pinned month selector
                Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                    MonthSelector(
                        selectedMonth = selectedMonth,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onMonthClick = { showMonthPicker = true }
                    )
                }

                // Pinned collapsible hero — swipes with content
                CollapsibleSummaryCard(
                    income = uiState.monthlyStats.totalIncome,
                    expense = uiState.monthlyStats.totalExpense,
                    balance = uiState.monthlyStats.balance,
                    currency = currency,
                    collapseProgress = collapseProgress,
                    modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedMonth) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        val currentOffset = dragOffset.value
                                        if (currentOffset > swipeThreshold) {
                                            dragOffset.animateTo(
                                                targetValue = size.width.toFloat(),
                                                animationSpec = tween(150)
                                            )
                                            viewModel.previousMonth()
                                            dragOffset.snapTo(-size.width.toFloat())
                                            dragOffset.animateTo(0f, animationSpec = tween(200))
                                        } else if (currentOffset < -swipeThreshold) {
                                            dragOffset.animateTo(
                                                targetValue = -size.width.toFloat(),
                                                animationSpec = tween(150)
                                            )
                                            viewModel.nextMonth()
                                            dragOffset.snapTo(size.width.toFloat())
                                            dragOffset.animateTo(0f, animationSpec = tween(200))
                                        } else {
                                            dragOffset.animateTo(0f, animationSpec = tween(200))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, animationSpec = tween(200))
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        dragOffset.snapTo(dragOffset.value + dragAmount)
                                    }
                                }
                            )
                        },
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Content with interactive drag offset
                    item {
                        Column(
                            modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Transactions grouped by date
                            if (uiState.recentTransactions.isNotEmpty()) {
                                val groupedByDate = uiState.recentTransactions
                                    .sortedByDescending { it.expense.date }
                                    .groupBy { it.expense.date }

                                groupedByDate.forEach { (date, transactions) ->
                                    Text(
                                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                    )

                                    transactions.forEach { transaction ->
                                        CompactTransactionItem(
                                            transaction = transaction,
                                            currency = currency,
                                            onClick = { onViewTransaction(transaction.expense.id) }
                                        )
                                    }
                                }
                            } else {
                                EmptyState()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseBreakdownCard(
    breakdown: List<CategoryBreakdown>,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Expense Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple Pie Chart
            SimplePieChart(
                breakdown = breakdown,
                currency = currency,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            breakdown.take(5).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(item.category?.color ?: Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.category?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatCurrency(item.amount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SimplePieChart(
    breakdown: List<CategoryBreakdown>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val total = breakdown.sumOf { it.amount }.toFloat()
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val gapDegrees = if (breakdown.size > 1) 2.5f else 0f

    val sliceAngles = remember(breakdown, total) {
        var cumulative = -90f
        breakdown.map { item ->
            val sweep = if (total > 0f) (item.amount.toFloat() / total * 360f) else 0f
            val start = cumulative
            cumulative += sweep
            start to sweep
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = modifier.pointerInput(breakdown) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        val outerR = minOf(size.width, size.height) / 2f * 0.85f
                        val innerR = outerR * 0.55f
                        if (dist in innerR..outerR) {
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < -90f) angle += 360f
                            val tapped = sliceAngles.indexOfFirst { (start, sweep) ->
                                angle >= start && angle < start + sweep
                            }
                            selectedIndex = if (tapped == selectedIndex) -1 else tapped
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
            ) {
                val canvasSize = size.minDimension
                val outerRadius = canvasSize / 2f * 0.85f
                val strokeWidth = outerRadius * 0.45f
                val arcRadius = outerRadius - strokeWidth / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                sliceAngles.forEachIndexed { index, (start, sweep) ->
                    val sliceColor = breakdown[index].category?.color ?: Color.Gray
                    val isSelected = index == selectedIndex
                    val drawStroke = if (isSelected) strokeWidth * 1.15f else strokeWidth
                    val adjustedSweep = (sweep - gapDegrees).coerceAtLeast(0.5f)
                    val adjustedStart = start + gapDegrees / 2f
                    drawArc(
                        color = if (selectedIndex != -1 && !isSelected) sliceColor.copy(alpha = 0.4f) else sliceColor,
                        startAngle = adjustedStart,
                        sweepAngle = adjustedSweep,
                        useCenter = false,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = drawStroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Butt
                        )
                    )
                }
            }
        }

        // Tooltip
        if (selectedIndex in breakdown.indices) {
            val item = breakdown[selectedIndex]
            Text(
                text = "${item.category?.name ?: "Uncategorized"}: ${formatCurrency(item.amount, currency)} (${item.percentage.toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CompactTransactionItem(
    transaction: ExpenseWithCategory,
    currency: String,
    onClick: () -> Unit
) {
    val isExpense = transaction.expense.type == TransactionType.EXPENSE
    val isTransfer = transaction.expense.type == TransactionType.TRANSFER
    val amountColor by animateColorAsState(
        targetValue = when (transaction.expense.type) {
            TransactionType.EXPENSE -> ExpenseRed
            TransactionType.INCOME -> IncomeGreen
            TransactionType.TRANSFER -> TransferBlue
        },
        label = "amount_color"
    )
    val hasSubcategory = transaction.subcategory != null
    val hasNote = transaction.expense.note.isNotBlank()
    val accountName = transaction.account?.name
    val toAccountName = transaction.toAccount?.name

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                icon = if (isTransfer) "swap_horiz" else (transaction.subcategory?.icon ?: transaction.category?.icon ?: "more_horiz"),
                color = if (isTransfer) TransferBlue else (transaction.subcategory?.color ?: transaction.category?.color ?: Color.Gray),
                size = 30.dp,
                iconSize = 15.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Left column: category/subcategory
            Column(
                modifier = Modifier.width(110.dp),
                verticalArrangement = if (hasSubcategory && !isTransfer) Arrangement.Top else Arrangement.Center
            ) {
                Text(
                    text = if (isTransfer) "Transfer" else (transaction.category?.name ?: "Uncategorized"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isTransfer && hasSubcategory) {
                    Text(
                        text = transaction.subcategory!!.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right column: note/account
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = if (hasNote) Arrangement.Top else Arrangement.Center
            ) {
                if (hasNote) {
                    Text(
                        text = transaction.expense.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isTransfer && accountName != null && toAccountName != null) {
                    Text(
                        text = "$accountName -> $toAccountName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (accountName != null) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = when {
                    isTransfer -> formatNumber(transaction.expense.amount, currency)
                    isExpense -> "-${formatNumber(transaction.expense.amount, currency)}"
                    else -> "+${formatNumber(transaction.expense.amount, currency)}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                modifier = Modifier.width(100.dp),
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Tap + to add your first transaction",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
