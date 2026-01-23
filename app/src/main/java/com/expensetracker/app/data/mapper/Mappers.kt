package com.expensetracker.app.data.mapper

import androidx.compose.ui.graphics.Color
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = Color(color.toULong()),
    isDefault = isDefault,
    createdAt = createdAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color.value.toLong(),
    isDefault = isDefault,
    createdAt = createdAt
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amount = amount,
    note = note,
    categoryId = categoryId,
    type = type,
    date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate(),
    createdAt = createdAt
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amount = amount,
    note = note,
    categoryId = categoryId,
    type = type,
    date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    createdAt = createdAt
)
