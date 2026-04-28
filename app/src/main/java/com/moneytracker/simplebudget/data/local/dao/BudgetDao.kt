package com.moneytracker.simplebudget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.simplebudget.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE isActive = 1 AND period = 'MONTHLY' AND year = :year AND month = :month ORDER BY createdAt ASC")
    fun getActiveMonthlyBudgets(year: Int, month: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isActive = 1 AND period = 'YEARLY' AND year = :year ORDER BY createdAt ASC")
    fun getActiveYearlyBudgets(year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1")
    suspend fun getActiveBudgetCount(): Int

    @Query("""
        SELECT COUNT(*) FROM budgets
        WHERE isActive = 1
        AND categoryId IS :categoryId
        AND subcategoryId IS :subcategoryId
        AND period = :period
        AND year = :year
        AND month IS :month
        AND id != :excludeId
    """)
    suspend fun countDuplicates(
        categoryId: Long?,
        subcategoryId: Long?,
        period: String,
        year: Int,
        month: Int?,
        excludeId: Long
    ): Int

    @Query("""
        SELECT * FROM budgets
        WHERE isActive = 1
        AND categoryId IS :categoryId
        AND subcategoryId IS :subcategoryId
        AND period = :period
        AND year = :year
        AND month IS :month
        LIMIT 1
    """)
    suspend fun findBudgetForPeriod(
        categoryId: Long?,
        subcategoryId: Long?,
        period: String,
        year: Int,
        month: Int?
    ): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}
