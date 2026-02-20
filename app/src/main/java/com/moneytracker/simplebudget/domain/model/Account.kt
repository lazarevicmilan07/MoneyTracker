package com.moneytracker.simplebudget.domain.model

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
    icon = "attach_money",
    color = Color(0xFF4CAF50),
    initialBalance = 0.0,
    isDefault = true
)

val DefaultAccounts = listOf(
    DefaultAccount,
    Account(
        name = "Bank Account",
        type = AccountType.BANK,
        icon = "account_balance",
        color = Color(0xFF2196F3),
        initialBalance = 0.0,
        isDefault = false
    ),
    Account(
        name = "Credit Card",
        type = AccountType.CREDIT_CARD,
        icon = "payments",
        color = Color(0xFFFF9800),
        initialBalance = 0.0,
        isDefault = false
    ),
    Account(
        name = "Savings",
        type = AccountType.SAVINGS,
        icon = "savings",
        color = Color(0xFF9C27B0),
        initialBalance = 0.0,
        isDefault = false
    )
)

val AccountTypeIcons = mapOf(
    AccountType.CASH to "attach_money",
    AccountType.BANK to "account_balance",
    AccountType.CREDIT_CARD to "payments",
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
