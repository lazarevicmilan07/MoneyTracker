package com.expensetracker.app.domain.model

import androidx.compose.ui.graphics.Color

enum class AccountType {
    CASH,
    BANK,
    CREDIT_CARD,
    SAVINGS,
    INVESTMENT,
    OTHER
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val color: Color,
    val initialBalance: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AccountWithBalance(
    val account: Account,
    val currentBalance: Double
)

val DefaultAccount = Account(
    name = "Cash",
    type = AccountType.CASH,
    icon = "wallet",
    color = Color(0xFF4CAF50),
    initialBalance = 0.0,
    isDefault = true
)

val AccountTypeIcons = mapOf(
    AccountType.CASH to "wallet",
    AccountType.BANK to "account_balance",
    AccountType.CREDIT_CARD to "credit_card",
    AccountType.SAVINGS to "savings",
    AccountType.INVESTMENT to "trending_up",
    AccountType.OTHER to "account_balance_wallet"
)

val AccountTypeNames = mapOf(
    AccountType.CASH to "Cash",
    AccountType.BANK to "Bank Account",
    AccountType.CREDIT_CARD to "Credit Card",
    AccountType.SAVINGS to "Savings",
    AccountType.INVESTMENT to "Investment",
    AccountType.OTHER to "Other"
)
