package com.moneytracker.simplebudget.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.domain.model.CategoryBreakdown
import com.moneytracker.simplebudget.ui.components.CollapsibleSummaryCard
import com.moneytracker.simplebudget.ui.components.CurrencyAmountText
import com.moneytracker.simplebudget.ui.components.MonthSelector
import com.moneytracker.simplebudget.ui.components.MonthYearPickerDialog
import com.moneytracker.simplebudget.ui.components.ScrollToTopButton
import com.moneytracker.simplebudget.ui.components.rememberCollapseProgress
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    currency: String,
    symbolAfter: Boolean = true,
    viewModel: MonthlyReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }

    val swipeThreshold = 100f
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val collapseProgress = rememberCollapseProgress(listState)
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = selectedMonth,
            onMonthSelected = { yearMonth ->
                if (yearMonth != selectedMonth) {
                    val goingBack = yearMonth < selectedMonth
                    coroutineScope.launch {
                        dragOffset.animateTo(
                            if (goingBack) screenWidthPx else -screenWidthPx,
                            tween(150)
                        )
                        viewModel.selectMonth(yearMonth)
                        dragOffset.snapTo(
                            if (goingBack) -screenWidthPx else screenWidthPx
                        )
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    // Scroll to top when month changes
    LaunchedEffect(selectedMonth) {
        listState.scrollToItem(0)
    }

    val animateToPrevious: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousMonth()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }
    val animateToNext: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(-screenWidthPx, tween(150))
            viewModel.nextMonth()
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.monthly_reports_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                listState = listState,
                onClick = {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier.padding(bottom = 60.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned month selector
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MonthSelector(
                    selectedMonth = selectedMonth,
                    onPreviousMonth = animateToPrevious,
                    onNextMonth = animateToNext,
                    onMonthClick = { showMonthPicker = true }
                )
            }

            // Pinned collapsible hero — swipes with content
            CollapsibleSummaryCard(
                income = uiState.totalIncome,
                expense = uiState.totalExpense,
                balance = uiState.balance,
                currency = currency,
                collapseProgress = collapseProgress,
                modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                symbolAfter = symbolAfter
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
                                        dragOffset.animateTo(screenWidthPx, tween(150))
                                        viewModel.previousMonth()
                                        dragOffset.snapTo(-screenWidthPx)
                                        dragOffset.animateTo(0f, tween(200))
                                    } else if (currentOffset < -swipeThreshold) {
                                        dragOffset.animateTo(-screenWidthPx, tween(150))
                                        viewModel.nextMonth()
                                        dragOffset.snapTo(screenWidthPx)
                                        dragOffset.animateTo(0f, tween(200))
                                    } else {
                                        dragOffset.animateTo(0f, tween(200))
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch { dragOffset.animateTo(0f, tween(200)) }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                coroutineScope.launch {
                                    dragOffset.snapTo(dragOffset.value + dragAmount)
                                }
                            }
                        )
                    },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Content with interactive drag offset
                item {
                    Column(
                        modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Expense Pie Chart
                        if (uiState.expenseBreakdown.isNotEmpty()) {
                            BreakdownCard(
                                title = stringResource(R.string.reports_expenses_by_category),
                                breakdown = uiState.expenseBreakdown,
                                currency = currency,
                                color = ExpenseRed,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Income Pie Chart
                        if (uiState.incomeBreakdown.isNotEmpty()) {
                            BreakdownCard(
                                title = stringResource(R.string.reports_income_by_category),
                                breakdown = uiState.incomeBreakdown,
                                currency = currency,
                                color = IncomeGreen,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Empty state
                        if (uiState.expenseBreakdown.isEmpty() && uiState.incomeBreakdown.isEmpty() && !uiState.isLoading) {
                            EmptyReportsState()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownCard(
    title: String,
    breakdown: List<CategoryBreakdown>,
    currency: String,
    color: Color,
    symbolAfter: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pie Chart
            PieChart(
                breakdown = breakdown,
                currency = currency,
                accentColor = color,
                symbolAfter = symbolAfter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            val intPercentages = breakdown.map { it.percentage }.toRoundedPercentages()
            breakdown.forEachIndexed { index, item ->
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
                        text = item.category?.name ?: stringResource(R.string.transaction_uncategorized),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${intPercentages[index]}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    CurrencyAmountText(
                        amount = item.amount,
                        currencyCode = currency,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        symbolAfter = symbolAfter
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(
    breakdown: List<CategoryBreakdown>,
    currency: String,
    accentColor: Color = Color.Unspecified,
    symbolAfter: Boolean = true,
    onSliceSelected: ((Int) -> Unit)? = null,
    showTooltip: Boolean = true,
    resetKey: Any = Unit,
    // When provided, overrides internal single-selection with external multi-selection (list mode)
    selectedIndices: Set<Int>? = null,
    modifier: Modifier = Modifier
) {
    val total = breakdown.sumOf { it.amount }.toFloat()
    // Internal single-selection state — only used when selectedIndices is null
    var selectedIndex by remember(resetKey) { mutableIntStateOf(-1) }

    val sliceAngles = remember(breakdown, total) {
        var cumulative = -90f
        breakdown.map { item ->
            val sweep = if (total > 0f) (item.amount.toFloat() / total * 360f) else 0f
            val start = cumulative
            cumulative += sweep
            start to sweep
        }
    }

    // Per-slice animated scale — driven by external set or internal index
    val scales = breakdown.indices.map { i ->
        val isThisSelected = if (selectedIndices != null) i in selectedIndices else i == selectedIndex
        animateFloatAsState(
            targetValue = if (isThisSelected) 1.15f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "slice_scale_$i"
        ).value
    }

    // Entrance sweep animation — restarts when breakdown changes (e.g. month switch)
    val animProgress = remember(breakdown) { Animatable(0f) }
    LaunchedEffect(breakdown) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = modifier.pointerInput(breakdown, resetKey) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        val outerR = minOf(size.width, size.height) / 2f * 0.85f
                        val innerR = outerR * 0.60f
                        if (dist in innerR..outerR) {
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < -90f) angle += 360f
                            val tapped = sliceAngles.indexOfFirst { (start, sweep) ->
                                angle >= start && angle < start + sweep
                            }
                            if (selectedIndices != null) {
                                // External mode: just fire callback, external state owns selection
                                if (tapped != -1) onSliceSelected?.invoke(tapped)
                            } else {
                                selectedIndex = if (tapped == selectedIndex) -1 else tapped
                                if (tapped != -1) onSliceSelected?.invoke(tapped)
                            }
                        } else {
                            if (selectedIndices != null) {
                                // Center-hole tap in external mode → clear all
                                if (dist < innerR) onSliceSelected?.invoke(-1)
                            } else {
                                selectedIndex = -1
                            }
                        }
                    }
                }
            ) {
                val canvasSize = size.minDimension
                val outerRadius = canvasSize / 2f * 0.85f
                val strokeWidth = outerRadius * 0.37f
                val arcRadius = outerRadius * 0.85f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val gapDegrees = if (breakdown.size > 1) 1.5f else 0f

                val totalFilled = 360f * animProgress.value

                sliceAngles.forEachIndexed { index, (start, sweep) ->
                    val sliceColor = breakdown[index].category?.color ?: Color.Gray
                    val isThisSelected = if (selectedIndices != null) index in selectedIndices else index == selectedIndex
                    val isAnySelected = if (selectedIndices != null) selectedIndices.isNotEmpty() else selectedIndex != -1
                    val drawStroke = strokeWidth * scales[index]
                    val adjustedSweep = (sweep - gapDegrees).coerceAtLeast(0.5f)
                    val adjustedStart = start + gapDegrees / 2f

                    // Clip to entrance animation progress (sweep in from 12 o'clock)
                    val relativeStart = adjustedStart + 90f
                    val visibleSweep = (totalFilled - relativeStart).coerceIn(0f, adjustedSweep)
                    if (visibleSweep <= 0f) return@forEachIndexed

                    drawArc(
                        color = if (isAnySelected && !isThisSelected) sliceColor.copy(alpha = 0.4f) else sliceColor,
                        startAngle = adjustedStart,
                        sweepAngle = visibleSweep,
                        useCenter = false,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = drawStroke,
                            cap = StrokeCap.Butt
                        )
                    )
                }
            }

            // Center label: 100% at rest; selected slice % (single); sum % (multi)
            if (showTooltip) {
                val intPercentages = breakdown.map { it.percentage }.toRoundedPercentages()
                val labelText = when {
                    selectedIndices != null && selectedIndices.isNotEmpty() ->
                        "${breakdown.filterIndexed { i, _ -> i in selectedIndices }.sumOf { it.percentage.toDouble() }.roundToInt()}%"
                    selectedIndices != null -> "100%"
                    selectedIndex in breakdown.indices -> "${intPercentages[selectedIndex]}%"
                    else -> "100%"
                }
                AnimatedContent(
                    targetState = labelText,
                    transitionSpec = {
                        (fadeIn(tween(160)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(100)) + slideOutVertically { -it / 2 })
                    },
                    label = "center_label"
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyReportsState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.reports_no_data_month),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reports_no_data_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Distributes integer percentages so they always sum to exactly 100,
 * using the largest-remainder method (Hamilton method).
 */
internal fun List<Float>.toRoundedPercentages(): List<Int> {
    if (isEmpty()) return emptyList()
    val floors = map { it.toInt() }
    val remainder = (100 - floors.sum()).coerceAtLeast(0)
    val sortedByFraction = indices.sortedByDescending { this[it] - floors[it] }
    return floors.toMutableList().also { result ->
        sortedByFraction.take(remainder).forEach { i -> result[i]++ }
    }
}
