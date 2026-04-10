@file:OptIn(ExperimentalMaterial3Api::class)

package com.moneytracker.simplebudget.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.domain.model.CategoryBreakdown
import com.moneytracker.simplebudget.ui.components.CurrencyAmountText

// ─── Mode switcher ────────────────────────────────────────────────────────────

@Composable
fun SubcategoryModeSwitcher(
    currentMode: SubcategoryDisplayMode,
    onModeChange: (SubcategoryDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = SubcategoryDisplayMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = currentMode == mode) {
                        Icon(
                            imageVector = when (mode) {
                                SubcategoryDisplayMode.BOTTOM_SHEET -> Icons.Default.Layers
                                SubcategoryDisplayMode.DRILL_DOWN -> Icons.Default.ZoomIn
                                SubcategoryDisplayMode.EXPANDABLE_LIST -> Icons.Default.UnfoldMore
                            },
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(
                        text = when (mode) {
                            SubcategoryDisplayMode.BOTTOM_SHEET -> stringResource(R.string.stats_mode_sheet)
                            SubcategoryDisplayMode.DRILL_DOWN -> stringResource(R.string.stats_mode_drill)
                            SubcategoryDisplayMode.EXPANDABLE_LIST -> stringResource(R.string.stats_mode_list)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

// ─── Main card ────────────────────────────────────────────────────────────────

@Composable
fun SubcategoryAwareBreakdownCard(
    title: String,
    breakdown: List<CategoryBreakdown>,
    subcategoryBreakdowns: Map<Long, List<CategoryBreakdown>>,
    currency: String,
    accentColor: Color,
    symbolAfter: Boolean,
    displayMode: SubcategoryDisplayMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (displayMode) {
                SubcategoryDisplayMode.BOTTOM_SHEET ->
                    BottomSheetModeContent(breakdown, subcategoryBreakdowns, currency, accentColor, symbolAfter)
                SubcategoryDisplayMode.DRILL_DOWN ->
                    DrillDownModeContent(breakdown, subcategoryBreakdowns, currency, accentColor, symbolAfter)
                SubcategoryDisplayMode.EXPANDABLE_LIST ->
                    ExpandableModeContent(breakdown, subcategoryBreakdowns, currency, accentColor, symbolAfter)
            }
        }
    }
}

// ─── Mode 1: Bottom Sheet ─────────────────────────────────────────────────────

@Composable
private fun BottomSheetModeContent(
    breakdown: List<CategoryBreakdown>,
    subcategoryBreakdowns: Map<Long, List<CategoryBreakdown>>,
    currency: String,
    accentColor: Color,
    symbolAfter: Boolean
) {
    var selectedCategory by remember { mutableStateOf<CategoryBreakdown?>(null) }
    var pieResetKey by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PieChart(
        breakdown = breakdown,
        currency = currency,
        accentColor = accentColor,
        symbolAfter = symbolAfter,
        resetKey = pieResetKey,
        onSliceSelected = { index -> selectedCategory = breakdown.getOrNull(index) },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    val intPercentages = breakdown.map { it.percentage }.toRoundedPercentages()
    breakdown.forEachIndexed { index, item ->
        val hasSubs = subcategoryBreakdowns.containsKey(item.category?.id)
        BreakdownItemRow(
            name = item.category?.name ?: stringResource(R.string.transaction_uncategorized),
            color = item.category?.color ?: Color.Gray,
            percentage = "${intPercentages[index]}%",
            amount = item.amount,
            currency = currency,
            symbolAfter = symbolAfter,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(if (hasSubs) Modifier.clickable { selectedCategory = item } else Modifier)
                .padding(vertical = 5.dp)
        )
    }

    if (selectedCategory != null) {
        val category = selectedCategory!!
        val subBreakdown = subcategoryBreakdowns[category.category?.id] ?: emptyList()
        ModalBottomSheet(
            onDismissRequest = {
                selectedCategory = null
                pieResetKey++
            },
            sheetState = sheetState
        ) {
            SubcategoryDetailSheet(
                category = category,
                subBreakdown = subBreakdown,
                currency = currency,
                symbolAfter = symbolAfter
            )
        }
    }
}

@Composable
private fun SubcategoryDetailSheet(
    category: CategoryBreakdown,
    subBreakdown: List<CategoryBreakdown>,
    currency: String,
    symbolAfter: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(category.category?.color ?: Color.Gray)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category.category?.name ?: stringResource(R.string.transaction_uncategorized),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            CurrencyAmountText(
                amount = category.amount,
                currencyCode = currency,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                symbolAfter = symbolAfter
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (subBreakdown.isEmpty()) {
            Text(
                text = "No subcategory breakdown available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            PieChart(
                breakdown = subBreakdown,
                currency = currency,
                accentColor = category.category?.color ?: Color.Gray,
                symbolAfter = symbolAfter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            val subIntPercentages = subBreakdown.map { it.percentage }.toRoundedPercentages()
            subBreakdown.forEachIndexed { index, sub ->
                BreakdownItemRow(
                    name = sub.category?.name ?: stringResource(R.string.transaction_uncategorized),
                    color = sub.category?.color ?: Color.Gray,
                    percentage = "${subIntPercentages[index]}%",
                    amount = sub.amount,
                    currency = currency,
                    symbolAfter = symbolAfter,
                    swatchHeight = 16.dp
                )
            }
        }
    }
}

// ─── Mode 2: Drill Down ───────────────────────────────────────────────────────

@Composable
private fun DrillDownModeContent(
    breakdown: List<CategoryBreakdown>,
    subcategoryBreakdowns: Map<Long, List<CategoryBreakdown>>,
    currency: String,
    accentColor: Color,
    symbolAfter: Boolean
) {
    var drillCategory by remember { mutableStateOf<CategoryBreakdown?>(null) }

    AnimatedContent(
        targetState = drillCategory,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)) togetherWith
                        slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280))
            } else {
                slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280)) togetherWith
                        slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280))
            }
        },
        label = "DrillDownContent"
    ) { currentDrill ->
        if (currentDrill == null) {
            Column {
                PieChart(
                    breakdown = breakdown,
                    currency = currency,
                    accentColor = accentColor,
                    symbolAfter = symbolAfter,
                    onSliceSelected = { index ->
                        val item = breakdown.getOrNull(index) ?: return@PieChart
                        if (subcategoryBreakdowns.containsKey(item.category?.id)) {
                            drillCategory = item
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap a slice to drill into subcategories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val intPercentages = breakdown.map { it.percentage }.toRoundedPercentages()
                breakdown.forEachIndexed { index, item ->
                    val hasSubs = subcategoryBreakdowns.containsKey(item.category?.id)
                    BreakdownItemRow(
                        name = item.category?.name ?: stringResource(R.string.transaction_uncategorized),
                        color = item.category?.color ?: Color.Gray,
                        percentage = "${intPercentages[index]}%",
                        amount = item.amount,
                        currency = currency,
                        symbolAfter = symbolAfter,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (hasSubs) Modifier.clickable { drillCategory = item } else Modifier)
                            .padding(vertical = 5.dp)
                    )
                }
            }
        } else {
            val subBreakdown = subcategoryBreakdowns[currentDrill.category?.id] ?: emptyList()
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { drillCategory = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(currentDrill.category?.color ?: Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentDrill.category?.name ?: stringResource(R.string.transaction_uncategorized),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    CurrencyAmountText(
                        amount = currentDrill.amount,
                        currencyCode = currency,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        symbolAfter = symbolAfter
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (subBreakdown.isEmpty()) {
                    Text(
                        text = "No subcategory breakdown available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    PieChart(
                        breakdown = subBreakdown,
                        currency = currency,
                        accentColor = currentDrill.category?.color ?: accentColor,
                        symbolAfter = symbolAfter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val subIntPercentages = subBreakdown.map { it.percentage }.toRoundedPercentages()
                    subBreakdown.forEachIndexed { index, sub ->
                        BreakdownItemRow(
                            name = sub.category?.name ?: stringResource(R.string.transaction_uncategorized),
                            color = sub.category?.color ?: Color.Gray,
                            percentage = "${subIntPercentages[index]}%",
                            amount = sub.amount,
                            currency = currency,
                            symbolAfter = symbolAfter
                        )
                    }
                }
            }
        }
    }
}

// ─── Shared row ───────────────────────────────────────────────────────────────

@Composable
private fun BreakdownItemRow(
    name: String,
    color: Color,
    percentage: String,
    amount: Double,
    currency: String,
    symbolAfter: Boolean,
    modifier: Modifier = Modifier.padding(vertical = 5.dp),
    swatchWidth: Dp = 4.dp,
    swatchHeight: Dp = 20.dp,
    spacerWidth: Dp = 10.dp,
    nameStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    nameFontWeight: FontWeight = FontWeight.Normal,
    amountStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    amountFontWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(swatchWidth)
                .height(swatchHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(spacerWidth))
        Text(
            text = name,
            style = nameStyle,
            fontWeight = nameFontWeight,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = percentage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(spacerWidth))
        CurrencyAmountText(
            amount = amount,
            currencyCode = currency,
            style = amountStyle,
            fontWeight = amountFontWeight,
            symbolAfter = symbolAfter
        )
    }
}

// ─── Mode 3: Expandable List ──────────────────────────────────────────────────

@Composable
private fun ExpandableModeContent(
    breakdown: List<CategoryBreakdown>,
    subcategoryBreakdowns: Map<Long, List<CategoryBreakdown>>,
    currency: String,
    accentColor: Color,
    symbolAfter: Boolean
) {
    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }
    val intPercentages = breakdown.map { it.percentage }.toRoundedPercentages()

    // Derive selected indices from expanded rows — keeps donut and list in sync
    val selectedIndices = breakdown.indices
        .filter { i -> expandedStates[breakdown[i].category?.id] == true }
        .toSet()

    PieChart(
        breakdown = breakdown,
        currency = currency,
        accentColor = accentColor,
        symbolAfter = symbolAfter,
        selectedIndices = selectedIndices,
        onSliceSelected = { index ->
            if (index == -1) {
                // Center-hole tap: deselect and collapse everything
                expandedStates.keys.toList().forEach { expandedStates[it] = false }
            } else {
                val id = breakdown.getOrNull(index)?.category?.id ?: return@PieChart
                expandedStates[id] = !(expandedStates[id] ?: false)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))

    breakdown.forEachIndexed { itemIndex, item ->
        val parentId = item.category?.id
        val subBreakdown = if (parentId != null) subcategoryBreakdowns[parentId] ?: emptyList() else emptyList()
        val hasSubs = subBreakdown.isNotEmpty()
        val isExpanded = expandedStates[parentId] ?: false

        Column {
            BreakdownItemRow(
                name = item.category?.name ?: stringResource(R.string.transaction_uncategorized),
                color = item.category?.color ?: Color.Gray,
                percentage = "${intPercentages[itemIndex]}%",
                amount = item.amount,
                currency = currency,
                symbolAfter = symbolAfter,
                nameFontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expandedStates[parentId ?: 0L] = !isExpanded }
                    .padding(vertical = 5.dp)
            )

            AnimatedVisibility(
                visible = isExpanded && hasSubs,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, bottom = 4.dp)
                ) {
                    val subIntPercentages = subBreakdown.map { it.percentage }.toRoundedPercentages()
                    subBreakdown.forEachIndexed { subIndex, sub ->
                        BreakdownItemRow(
                            name = sub.category?.name ?: stringResource(R.string.transaction_uncategorized),
                            color = sub.category?.color ?: Color.Gray,
                            percentage = "${subIntPercentages[subIndex]}%",
                            amount = sub.amount,
                            currency = currency,
                            symbolAfter = symbolAfter,
                            modifier = Modifier.padding(vertical = 4.dp),
                            swatchWidth = 3.dp,
                            swatchHeight = 14.dp,
                            spacerWidth = 8.dp,
                            nameStyle = MaterialTheme.typography.bodySmall,
                            amountStyle = MaterialTheme.typography.bodySmall,
                            amountFontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
