package com.moneytracker.simplebudget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.ui.theme.ExpenseRed

@Composable
fun CategorySelectionPanel(
    categories: List<Category>,
    allCategories: List<Category>,
    selectedParentCategoryId: Long?,
    subcategories: List<Category>,
    selectedSubcategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onSubcategorySelected: (Long) -> Unit,
    onParentSelected: () -> Unit,
    onClose: (() -> Unit)? = null,
    onEditCategories: (() -> Unit)? = null,
    onOverallSelected: (() -> Unit)? = null,
    isOverallSelected: Boolean = false
) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val typeColor = ExpenseRed

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (onClose != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    IconButton(onClick = { onEditCategories?.invoke() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            color = Color.Transparent
        ) {
            val leftColRows = categories.size + (if (onOverallSelected != null) 1 else 0)
            val maxRows = maxOf(leftColRows, subcategories.size)
            val contentHeight = (maxRows * 49).dp

            Row(modifier = Modifier.heightIn(max = 300.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        if (onOverallSelected != null) {
                            item {
                                OverallCategoryItem(
                                    isSelected = isOverallSelected,
                                    typeColor = typeColor,
                                    onClick = onOverallSelected
                                )
                                HorizontalDivider(thickness = 1.dp, color = borderColor)
                            }
                        }
                        items(categories) { category ->
                            val isSelected = category.id == selectedParentCategoryId
                            val hasSubcategories = allCategories.any { it.parentCategoryId == category.id }

                            CategoryListItem(
                                category = category,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) onParentSelected()
                                    else onCategorySelected(category.id)
                                },
                                showArrow = hasSubcategories,
                                typeColor = typeColor
                            )
                            HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .width(1.dp)
                            .height(contentHeight)
                            .background(borderColor)
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(subcategories) { subcategory ->
                        SubcategoryListItem(
                            subcategory = subcategory,
                            isSelected = subcategory.id == selectedSubcategoryId,
                            onClick = { onSubcategorySelected(subcategory.id) }
                        )
                        HorizontalDivider(thickness = 1.dp, color = borderColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallCategoryItem(
    isSelected: Boolean,
    typeColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) typeColor.copy(alpha = 0.1f) else Color.Transparent,
        label = "overall_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Savings,
            contentDescription = null,
            tint = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(28.dp).padding(4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.budget_all_categories),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    showArrow: Boolean = false,
    typeColor: Color = Color.Transparent
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) typeColor.copy(alpha = 0.1f) else Color.Transparent,
        label = "cat_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            icon = category.icon,
            color = category.color,
            size = 28.dp,
            iconSize = 16.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurface
        )
        if (showArrow) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) typeColor.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SubcategoryListItem(
    subcategory: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        label = "subcat_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            icon = subcategory.icon,
            color = subcategory.color,
            size = 28.dp,
            iconSize = 16.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
