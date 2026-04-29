package com.moneytracker.simplebudget.data.repository

import com.moneytracker.simplebudget.data.local.dao.BudgetDao
import com.moneytracker.simplebudget.data.local.dao.ExpenseDao
import com.moneytracker.simplebudget.data.mapper.toDomain
import com.moneytracker.simplebudget.data.mapper.toEntity
import com.moneytracker.simplebudget.domain.model.Budget
import com.moneytracker.simplebudget.domain.model.BudgetPeriod
import com.moneytracker.simplebudget.domain.model.BudgetWithProgress
import com.moneytracker.simplebudget.domain.model.BudgetScope
import com.moneytracker.simplebudget.domain.model.TransactionType
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
    private val categoryRepository: CategoryRepository
) {

    fun getBudgetsWithProgress(year: Int, month: Int, period: BudgetPeriod): Flow<List<BudgetWithProgress>> {
        val monthStart = LocalDate.of(year, month, 1).toEpochMilli()
        val monthEnd = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).toEpochMilli()
        val yearStart = LocalDate.of(year, 1, 1).toEpochMilli()
        val yearEnd = LocalDate.of(year, 12, 31).toEpochMilli()

        val budgetsFlow = when (period) {
            BudgetPeriod.MONTHLY -> budgetDao.getActiveMonthlyBudgets(year, month)
            BudgetPeriod.YEARLY -> budgetDao.getActiveYearlyBudgets(year)
        }

        return categoryRepository.getAllCategories().flatMapLatest { categories ->
            combine(
                budgetsFlow,
                expenseDao.getCategoryTotalsFlow(TransactionType.EXPENSE, monthStart, monthEnd),
                expenseDao.getCategoryTotalsFlow(TransactionType.EXPENSE, yearStart, yearEnd),
                expenseDao.getSubcategoryTotalsFlow(TransactionType.EXPENSE, monthStart, monthEnd),
                expenseDao.getSubcategoryTotalsFlow(TransactionType.EXPENSE, yearStart, yearEnd)
            ) { budgets, monthlyCat, yearlyCat, monthlySub, yearlySub ->
                val monthlyCatMap = monthlyCat.associate { it.categoryId to it.total }
                val yearlyCatMap = yearlyCat.associate { it.categoryId to it.total }
                val monthlySubMap = monthlySub.associate { it.categoryId to it.total }
                val yearlySubMap = yearlySub.associate { it.categoryId to it.total }

                budgets.map { entity ->
                    val budget = entity.toDomain()
                    val category = budget.categoryId?.let { id -> categories.find { it.id == id } }
                    val subcategory = budget.subcategoryId?.let { id -> categories.find { it.id == id } }

                    val spent = computeSpent(budget, monthlyCatMap, yearlyCatMap, monthlySubMap, yearlySubMap)
                    val remaining = budget.amount - spent
                    val percentage = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f

                    BudgetWithProgress(budget, category, subcategory, spent, remaining, percentage)
                }.sortedByDescending { it.percentage }
            }
        }
    }

    fun getBudgetProgressForCategory(
        categoryId: Long?,
        subcategoryId: Long?,
        year: Int,
        month: Int
    ): Flow<BudgetWithProgress?> {
        if (categoryId == null && subcategoryId == null) return flowOf(null)

        val monthStart = LocalDate.of(year, month, 1).toEpochMilli()
        val monthEnd = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).toEpochMilli()
        val yearStart = LocalDate.of(year, 1, 1).toEpochMilli()
        val yearEnd = LocalDate.of(year, 12, 31).toEpochMilli()

        val isSubcategoryBudget = subcategoryId != null && subcategoryId != categoryId

        // Check monthly budgets for this category first, then yearly
        return budgetDao.getActiveMonthlyBudgets(year, month).flatMapLatest { monthlyBudgets ->
            val monthlyEntity = if (isSubcategoryBudget) {
                monthlyBudgets.find { it.subcategoryId == subcategoryId }
            } else {
                monthlyBudgets.find { it.categoryId == categoryId && it.subcategoryId == null }
            }

            if (monthlyEntity != null) {
                if (isSubcategoryBudget) {
                    expenseDao.getSubcategoryTotalsFlow(TransactionType.EXPENSE, monthStart, monthEnd).map { totals ->
                        val spent = totals.find { it.categoryId == subcategoryId }?.total ?: 0.0
                        buildProgress(monthlyEntity, spent)
                    }
                } else {
                    expenseDao.getCategoryTotalsFlow(TransactionType.EXPENSE, monthStart, monthEnd).map { totals ->
                        val spent = totals.find { it.categoryId == categoryId }?.total ?: 0.0
                        buildProgress(monthlyEntity, spent)
                    }
                }
            } else {
                // Fall back to yearly budget
                budgetDao.getActiveYearlyBudgets(year).flatMapLatest { yearlyBudgets ->
                    val yearlyEntity = if (isSubcategoryBudget) {
                        yearlyBudgets.find { it.subcategoryId == subcategoryId }
                    } else {
                        yearlyBudgets.find { it.categoryId == categoryId && it.subcategoryId == null }
                    }

                    if (yearlyEntity == null) {
                        flowOf(null)
                    } else if (isSubcategoryBudget) {
                        expenseDao.getSubcategoryTotalsFlow(TransactionType.EXPENSE, yearStart, yearEnd).map { totals ->
                            val spent = totals.find { it.categoryId == subcategoryId }?.total ?: 0.0
                            buildProgress(yearlyEntity, spent)
                        }
                    } else {
                        expenseDao.getCategoryTotalsFlow(TransactionType.EXPENSE, yearStart, yearEnd).map { totals ->
                            val spent = totals.find { it.categoryId == categoryId }?.total ?: 0.0
                            buildProgress(yearlyEntity, spent)
                        }
                    }
                }
            }
        }
    }

    suspend fun getAllBudgetsSync(): List<Budget> = budgetDao.getAllBudgets().map { it.toDomain() }

    suspend fun deleteAllBudgets() = budgetDao.deleteAll()

    suspend fun getBudgetById(id: Long): Budget? = budgetDao.getBudgetById(id)?.toDomain()


    suspend fun insertBudget(budget: Budget): Long = budgetDao.insert(budget.toEntity())

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget.toEntity())

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget.toEntity())

    suspend fun getActiveBudgetCount(): Int = budgetDao.getActiveBudgetCount()

    suspend fun saveBudgetForScope(budget: Budget, scope: BudgetScope) {
        val groupId = if (scope != BudgetScope.THIS_PERIOD_ONLY) {
            budget.groupId ?: UUID.randomUUID().toString()
        } else budget.groupId

        val periods = generatePeriodsForScope(scope, budget.period, budget.year, budget.month)

        for ((periodYear, periodMonth) in periods) {
            val isPrimary = periodYear == budget.year && periodMonth == budget.month
            val existing = budgetDao.findBudgetForPeriod(
                budget.categoryId, budget.subcategoryId, budget.period.name, periodYear, periodMonth
            )

            if (isPrimary) {
                val toSave = budget.copy(year = periodYear, month = periodMonth, groupId = groupId)
                if (existing != null) {
                    budgetDao.update(toSave.copy(id = existing.id).toEntity())
                } else {
                    budgetDao.insert(toSave.copy(id = 0L).toEntity())
                }
            } else {
                if (existing != null) {
                    budgetDao.update(existing.copy(amount = budget.amount, groupId = groupId))
                } else {
                    budgetDao.insert(
                        budget.copy(
                            id = 0L,
                            groupId = groupId,
                            year = periodYear,
                            month = periodMonth
                        ).toEntity()
                    )
                }
            }
        }
    }

    private fun generatePeriodsForScope(
        scope: BudgetScope,
        period: BudgetPeriod,
        year: Int,
        month: Int?
    ): List<Pair<Int, Int?>> {
        return when (period) {
            BudgetPeriod.MONTHLY -> {
                val anchor = YearMonth.of(year, month!!)
                when (scope) {
                    BudgetScope.THIS_PERIOD_ONLY -> listOf(year to month)
                    BudgetScope.THIS_AND_3_BEFORE ->
                        (0..3).map { anchor.minusMonths(it.toLong()) }.map { it.year to it.monthValue }
                    BudgetScope.THIS_AND_6_BEFORE ->
                        (0..6).map { anchor.minusMonths(it.toLong()) }.map { it.year to it.monthValue }
                    BudgetScope.THIS_AND_ALL_BEFORE ->
                        (0..12).map { anchor.minusMonths(it.toLong()) }.map { it.year to it.monthValue }
                    BudgetScope.ALL_PERIODS ->
                        (-12..12).map { anchor.plusMonths(it.toLong()) }.map { it.year to it.monthValue }
                    BudgetScope.THIS_AND_3_FUTURE ->
                        (0..3).map { anchor.plusMonths(it.toLong()) }.map { it.year to it.monthValue }
                    BudgetScope.THIS_AND_6_FUTURE ->
                        (0..6).map { anchor.plusMonths(it.toLong()) }.map { it.year to it.monthValue }
                    BudgetScope.THIS_AND_FUTURE ->
                        (0..12).map { anchor.plusMonths(it.toLong()) }.map { it.year to it.monthValue }
                }
            }
            BudgetPeriod.YEARLY -> {
                when (scope) {
                    BudgetScope.THIS_PERIOD_ONLY -> listOf(year to null)
                    BudgetScope.THIS_AND_3_BEFORE -> (0..3).map { (year - it) to null }
                    BudgetScope.THIS_AND_6_BEFORE -> (0..5).map { (year - it) to null }
                    BudgetScope.THIS_AND_ALL_BEFORE -> (0..5).map { (year - it) to null }
                    BudgetScope.ALL_PERIODS -> (-5..5).map { (year + it) to null }
                    BudgetScope.THIS_AND_3_FUTURE -> (0..3).map { (year + it) to null }
                    BudgetScope.THIS_AND_6_FUTURE -> (0..5).map { (year + it) to null }
                    BudgetScope.THIS_AND_FUTURE -> (0..5).map { (year + it) to null }
                }
            }
        }
    }

    suspend fun findConflictsForScope(
        budget: Budget,
        scope: BudgetScope,
        excludeId: Long = 0L
    ): List<Budget> {
        val periods = generatePeriodsForScope(scope, budget.period, budget.year, budget.month)
        return periods.mapNotNull { (periodYear, periodMonth) ->
            val entity = budgetDao.findBudgetForPeriod(
                budget.categoryId, budget.subcategoryId,
                budget.period.name, periodYear, periodMonth
            )
            if (entity != null && entity.id != excludeId) entity.toDomain() else null
        }
    }

    suspend fun isDuplicateBudget(
        categoryId: Long?,
        subcategoryId: Long?,
        period: BudgetPeriod,
        year: Int,
        month: Int?,
        excludeId: Long = 0L
    ): Boolean = budgetDao.countDuplicates(
        categoryId = categoryId,
        subcategoryId = subcategoryId,
        period = period.name,
        year = year,
        month = month,
        excludeId = excludeId
    ) > 0

    private fun computeSpent(
        budget: Budget,
        monthlyCat: Map<Long?, Double>,
        yearlyCat: Map<Long?, Double>,
        monthlySub: Map<Long?, Double>,
        yearlySub: Map<Long?, Double>
    ): Double {
        val isYearly = budget.period == BudgetPeriod.YEARLY
        return when {
            budget.subcategoryId != null -> {
                if (isYearly) yearlySub[budget.subcategoryId] ?: 0.0
                else monthlySub[budget.subcategoryId] ?: 0.0
            }
            budget.categoryId != null -> {
                if (isYearly) yearlyCat[budget.categoryId] ?: 0.0
                else monthlyCat[budget.categoryId] ?: 0.0
            }
            else -> {
                if (isYearly) yearlyCat.values.sum() else monthlyCat.values.sum()
            }
        }
    }

    private fun buildProgress(entity: com.moneytracker.simplebudget.data.local.entity.BudgetEntity, spent: Double): BudgetWithProgress {
        val budget = entity.toDomain()
        val remaining = budget.amount - spent
        val percentage = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
        return BudgetWithProgress(budget, null, null, spent, remaining, percentage)
    }

    private fun LocalDate.toEpochMilli(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
