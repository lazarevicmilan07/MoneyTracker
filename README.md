# Money Tracker – Simple Budget

A production-ready Android expense tracking app with offline-first architecture, clean Material 3 UI, and monetization support.

## Features

### Transaction Management
- **Expenses, Income & Transfers** - Track all transaction types with dedicated color coding
- **Multiple Accounts** - Cash, Bank, Credit Card, Savings, Investment, and custom accounts
- **Categories & Subcategories** - 10 default categories with subcategories, plus unlimited custom ones
- **Quick Entry** - Intuitive bottom panel with calculator-style amount input
- **Transaction Details** - View, edit, copy, or delete any transaction

### Accounts
- 4 default accounts seeded on first launch (Cash, Bank Account, Credit Card, Savings)
- 6 account types: Cash, Bank, Credit Card, Savings, Investment, Other
- Per-account balance tracking with transfer support
- Set a default account, customize icons and colors

### Reports & Insights
- **Monthly Reports** - Pie chart breakdown by category with tap-for-details
- **Yearly Reports** - Bar chart visualization of monthly totals
- Swipe navigation between periods
- Income, expense, and balance summaries

### Export
- **Excel (.xlsx)** - Full transaction export with category, subcategory, account, and notes
- **PDF** - Formatted report with summary totals
- Export by month or full year

### Settings & Preferences
- Dark and light theme (system-aware)
- 60+ currency options
- No login or account required

### Premium (One-time Purchase)
- **Backup & Restore** - Full data backup/restore via JSON file
- **Remove Ads** - Ad-free experience

## Architecture

Clean MVVM with Jetpack Compose and Hilt DI across three layers:

```
app/src/main/java/com/expensetracker/app/
├── billing/               # Google Play Billing
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs (Account, Category, Expense)
│   │   ├── entity/        # Room entities
│   │   ├── Converters.kt  # LocalDate type converter
│   │   └── ExpenseDatabase.kt  # Room DB v3 with migrations
│   ├── mapper/            # Entity ↔ Domain mappers
│   ├── preferences/       # DataStore preferences
│   └── repository/        # Repositories
├── di/                    # Hilt AppModule
├── domain/
│   ├── model/             # Domain models (Expense, Category, Account)
│   └── usecase/           # ExportUseCase, BackupRestoreUseCase
├── navigation/            # NavGraph with sealed Screen routes
├── ui/
│   ├── accounts/          # Account management
│   ├── categories/        # Category/subcategory management
│   ├── components/        # Shared Compose components
│   ├── dashboard/         # Main transactions screen
│   ├── premium/           # Premium purchase screen
│   ├── reports/           # Monthly & Yearly reports
│   ├── settings/          # Settings, export, backup/restore
│   ├── theme/             # Material 3 theming
│   └── transaction/       # Add/Edit/Copy/Detail screens
└── MainActivity.kt
```

**Data flow:** Compose screen observes `ViewModel.uiState: StateFlow` → ViewModel collects from `Repository.getX(): Flow` → Repository delegates to `Dao` returning `Flow<List<Entity>>` and maps to domain models.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (BOM 2024.02.00)
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt (KSP)
- **Database:** Room (v3, KSP)
- **Preferences:** DataStore
- **Navigation:** Jetpack Navigation Compose
- **Ads:** Google AdMob
- **Billing:** Google Play Billing Library 6.x
- **PDF:** iText 7
- **Excel:** FastExcel
- **Min SDK:** 26 (Android 8.0), Target/Compile SDK 34

## Data Models

### Expense
```kotlin
data class Expense(
    val id: Long,
    val amount: Double,
    val note: String,
    val categoryId: Long?,
    val subcategoryId: Long?,
    val accountId: Long?,
    val toAccountId: Long?,         // For transfers
    val type: TransactionType,      // EXPENSE, INCOME, TRANSFER
    val date: LocalDate,
    val createdAt: Long
)
```

### Category
```kotlin
data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Color,
    val parentCategoryId: Long?,    // Null for root categories
    val isDefault: Boolean,
    val createdAt: Long
)
```

### Account
```kotlin
data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,          // CASH, BANK, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER
    val icon: String,
    val color: Color,
    val initialBalance: Double,
    val isDefault: Boolean,
    val createdAt: Long
)
```

## Navigation

Bottom navigation with 5 tabs: Transactions, Reports, Accounts, Categories, Settings.

```
Dashboard (Transactions)
├── → Add Transaction
├── → Transaction Detail → Edit / Copy
├── → Monthly Reports
└── → Yearly Reports

Accounts
├── → Add/Edit Account

Categories
├── → Add/Edit Category/Subcategory

Settings
├── → Export (Excel/PDF)
├── → Backup & Restore (Premium)
└── → Premium
```

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build
```bash
./gradlew assembleDebug       # Debug build
./gradlew assembleRelease     # Release build
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumented tests
```

### Configuration

1. **AdMob** - Replace test IDs in `app/build.gradle.kts` with production IDs for release
2. **Google Play Billing** - Create in-app product with ID `premium_unlock` in Play Console
3. **Signing** - Configure release signing in `app/build.gradle.kts`

## Build Variants

| Variant | AdMob IDs    | Minify | Description  |
|---------|--------------|--------|--------------|
| debug   | Test         | No     | Development  |
| release | Production   | Yes    | Store release|

## Monetization

- **Banner Ads** - Adaptive AdMob banner, hidden for premium users
- **In-App Purchase** - One-time `premium_unlock` for backup/restore and ad removal

## License

MIT License - See LICENSE file for details.
