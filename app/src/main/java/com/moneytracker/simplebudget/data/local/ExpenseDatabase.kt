package com.moneytracker.simplebudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneytracker.simplebudget.data.local.dao.AccountDao
import com.moneytracker.simplebudget.data.local.dao.BudgetDao
import com.moneytracker.simplebudget.data.local.dao.CategoryDao
import com.moneytracker.simplebudget.data.local.dao.ExpenseDao
import com.moneytracker.simplebudget.data.local.entity.AccountEntity
import com.moneytracker.simplebudget.data.local.entity.BudgetEntity
import com.moneytracker.simplebudget.data.local.entity.CategoryEntity
import com.moneytracker.simplebudget.data.local.entity.ExpenseEntity

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        BudgetEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create accounts table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        initialBalance REAL NOT NULL DEFAULT 0.0,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)

                // Insert default Cash account
                database.execSQL("""
                    INSERT INTO accounts (name, type, icon, color, initialBalance, isDefault, createdAt)
                    VALUES ('Cash', 'CASH', 'wallet', 4283215696, 0.0, 1, ${System.currentTimeMillis()})
                """)

                // Add accountId column to expenses table
                database.execSQL("ALTER TABLE expenses ADD COLUMN accountId INTEGER DEFAULT 1")

                // Create index for accountId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_accountId ON expenses(accountId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add parentCategoryId column to categories table for subcategories support
                database.execSQL("ALTER TABLE categories ADD COLUMN parentCategoryId INTEGER DEFAULT NULL")

                // Create index for parentCategoryId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parentCategoryId ON categories(parentCategoryId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN subcategoryId INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN toAccountId INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
                // Assign sequential displayOrder preserving current ordering (default first, then alphabetical)
                database.execSQL("""
                    UPDATE categories SET displayOrder = (
                        SELECT COUNT(*) FROM categories AS c2
                        WHERE (c2.parentCategoryId IS NULL AND categories.parentCategoryId IS NULL
                               OR c2.parentCategoryId = categories.parentCategoryId)
                          AND (c2.isDefault > categories.isDefault
                               OR (c2.isDefault = categories.isDefault AND c2.name < categories.name)
                               OR (c2.isDefault = categories.isDefault AND c2.name = categories.name AND c2.id < categories.id))
                    )
                """)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'EXPENSE'")
                // Tag root income categories
                database.execSQL("""
                    UPDATE categories SET categoryType = 'INCOME'
                    WHERE name IN ('Salary','Investment') AND parentCategoryId IS NULL
                """)
                // Tag their subcategories
                database.execSQL("""
                    UPDATE categories SET categoryType = 'INCOME'
                    WHERE parentCategoryId IN (
                        SELECT id FROM categories WHERE name IN ('Salary','Investment') AND parentCategoryId IS NULL
                    )
                """)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER,
                        subcategoryId INTEGER DEFAULT NULL,
                        amount REAL NOT NULL,
                        period TEXT NOT NULL DEFAULT 'MONTHLY',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_subcategoryId ON budgets(subcategoryId)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val currentYear = java.time.LocalDate.now().year
                val currentMonth = java.time.LocalDate.now().monthValue
                database.execSQL("ALTER TABLE budgets ADD COLUMN year INTEGER NOT NULL DEFAULT $currentYear")
                database.execSQL("ALTER TABLE budgets ADD COLUMN month INTEGER DEFAULT NULL")
                database.execSQL("UPDATE budgets SET month = $currentMonth WHERE period = 'MONTHLY'")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE budgets ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE budgets ADD COLUMN groupId TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER,
                        `subcategoryId` INTEGER,
                        `amount` REAL NOT NULL,
                        `period` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO `budgets_new` (id, categoryId, subcategoryId, amount, period, year, month, isActive, createdAt)
                    SELECT id, categoryId, subcategoryId, amount, period, year, month, isActive, createdAt FROM budgets
                """.trimIndent())
                database.execSQL("DROP TABLE budgets")
                database.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_subcategoryId` ON `budgets` (`subcategoryId`)")
            }
        }
    }
}
