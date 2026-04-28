package com.moneytracker.simplebudget.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val subcategoryId: Long? = null,
    val amount: Double,
    val period: String = "MONTHLY",
    val year: Int,
    val month: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val groupId: String? = null
)
