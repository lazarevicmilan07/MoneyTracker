package com.expensetracker.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val color: Long,
    val initialBalance: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AccountWithBalanceEntity(
    @Embedded val account: AccountEntity,
    val currentBalance: Double
)
