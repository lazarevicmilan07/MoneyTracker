# Simple Expense Tracker

A production-ready Android expense tracking app with offline-first architecture, clean UI, and monetization support.

## Features

### Free Features
- **One-tap transaction entry** - Quick and intuitive expense/income logging
- **Monthly overview** - Visual pie chart breakdown of spending categories
- **10 default categories** - Pre-configured expense categories
- **Light & Dark theme** - System-aware theming
- **Offline-first** - All data stored locally with Room database
- **No login required** - Start tracking immediately

### Premium Features (One-time Purchase)
- **Unlimited categories** - Create custom categories beyond the 10 free limit
- **Export to CSV** - Download transactions as spreadsheet
- **Export to PDF** - Generate formatted expense reports
- **Backup & Restore** - Full data backup to JSON file
- **Ad-free experience** - Remove banner ads

## Architecture

```
app/
├── src/main/java/com/expensetracker/app/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/           # Room DAOs
│   │   │   ├── entity/        # Room entities
│   │   │   ├── Converters.kt
│   │   │   └── ExpenseDatabase.kt
│   │   ├── mapper/            # Entity <-> Domain mappers
│   │   ├── preferences/       # DataStore preferences
│   │   └── repository/        # Data repositories
│   ├── di/                    # Hilt dependency injection
│   ├── domain/
│   │   ├── model/             # Domain models
│   │   └── usecase/           # Business logic
│   ├── billing/               # Google Play Billing
│   ├── navigation/            # Jetpack Navigation
│   └── ui/
│       ├── components/        # Reusable UI components
│       ├── dashboard/         # Dashboard screen
│       ├── transaction/       # Add/Edit transaction
│       ├── categories/        # Category management
│       ├── settings/          # Settings screen
│       ├── premium/           # Premium upgrade screen
│       └── theme/             # Material 3 theming
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt
- **Database:** Room
- **Preferences:** DataStore
- **Navigation:** Jetpack Navigation Compose
- **Ads:** Google AdMob
- **Billing:** Google Play Billing Library 6.x
- **PDF:** iText 7
- **CSV:** Apache Commons CSV

## Data Models

### Expense
```kotlin
data class Expense(
    val id: Long,
    val amount: Double,
    val note: String,
    val categoryId: Long?,
    val type: TransactionType,  // EXPENSE or INCOME
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
    val isDefault: Boolean,
    val createdAt: Long
)
```

## Screens

1. **Dashboard** - Monthly summary, pie chart, recent transactions
2. **Add Transaction** - Quick entry with category selection
3. **Edit Transaction** - Modify existing transactions
4. **Categories** - CRUD operations for categories
5. **Settings** - Theme, currency, export, backup
6. **Premium** - Feature comparison and purchase flow

## Navigation Flow

```
Dashboard
├── → Add Transaction → (save) → Dashboard
├── → Edit Transaction → (save/delete) → Dashboard
├── → Categories → (manage) → Categories
│   └── → Premium
└── → Settings
    └── → Premium
```

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Configuration

1. **Clone the repository**

2. **AdMob Setup**
   - Create an AdMob account at https://admob.google.com
   - Create an app and banner ad unit
   - Replace the test IDs in `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "ADMOB_APP_ID", "\"your-app-id\"")
   buildConfigField("String", "ADMOB_BANNER_ID", "\"your-banner-id\"")
   ```

3. **Google Play Billing Setup**
   - Create a Google Play Developer account
   - Create an in-app product with ID `premium_unlock`
   - Set price and description in Play Console

4. **Build the app**
   ```bash
   ./gradlew assembleRelease
   ```

### Signing for Release

Create a keystore and configure in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "your-store-password"
            keyAlias = "your-key-alias"
            keyPassword = "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## Monetization

### Banner Ads
- Displayed at bottom of screen
- Hidden for premium users
- Uses adaptive banner size
- Test ads in debug builds

### In-App Purchase
- One-time purchase: `premium_unlock`
- Unlocks: unlimited categories, export, backup, ad-free
- Purchase verified and acknowledged via BillingManager

## Google Play Policy Compliance

- **Privacy:** No personal data collected, all data local
- **Ads:** Clear ad labeling, appropriate placement
- **Billing:** Standard Play Billing flow
- **Permissions:** Minimal (INTERNET, BILLING only)
- **Content:** Family-friendly financial tool
- **Backup:** User-controlled, encrypted local backup

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test In-App Purchases
Use license testers in Play Console for free testing.

## Build Variants

| Variant | AdMob | Minify | Description |
|---------|-------|--------|-------------|
| debug   | Test IDs | No | Development |
| release | Production IDs | Yes | Store release |

## Release Checklist

- [ ] Update version code/name in build.gradle.kts
- [ ] Replace AdMob test IDs with production IDs
- [ ] Configure release signing
- [ ] Run ProGuard/R8 and test
- [ ] Generate signed APK/AAB
- [ ] Create Play Store listing
- [ ] Set up in-app product pricing
- [ ] Submit for review

## License

MIT License - See LICENSE file for details.
