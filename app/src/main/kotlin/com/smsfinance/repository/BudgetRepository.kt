package com.smsfinance.repository

import com.smsfinance.data.dao.BudgetDao
import com.smsfinance.data.entity.BudgetEntity
import com.smsfinance.domain.model.Budget
import com.smsfinance.domain.model.BudgetProgress
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    private val transactionRepository: TransactionRepository
) {
    fun getBudgetsForUser(userId: Long): Flow<List<Budget>> =
        dao.getBudgetsForUser(userId).map { it.map { e -> e.toDomain() } }

    /**
     * Get budgets with real-time spending progress for the current month.
     */
    fun getBudgetProgressForCurrentMonth(userId: Long): Flow<List<BudgetProgress>> {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        // Date range = start of this month to now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.timeInMillis
        val now = System.currentTimeMillis()

        return combine(
            dao.getBudgetsForMonth(userId, month, year),
            transactionRepository.getTransactionsByDateRange(startOfMonth, now)
        ) { budgets, transactions ->
            budgets.map { entity ->
                val budget = entity.toDomain()
                val spent = calculateSpentForBudget(budget, transactions)
                val remaining = maxOf(0.0, budget.amount - spent)
                val percent = if (budget.amount > 0) (spent / budget.amount) * 100 else 0.0
                BudgetProgress(
                    budget = budget,
                    spent = spent,
                    remaining = remaining,
                    percentUsed = percent,
                    isOverBudget = spent > budget.amount
                )
            }
        }
    }

    /**
     * Match transactions to a budget by keywords in source/description.
     */
    private fun calculateSpentForBudget(budget: Budget, transactions: List<Transaction>): Double {
        val keywords = budget.keywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return transactions
            .filter { it.type == TransactionType.WITHDRAWAL }
            .filter { tx ->
                if (keywords.isEmpty()) return@filter false
                val searchable = "${tx.source} ${tx.description}".lowercase()
                keywords.any { kw -> searchable.contains(kw) }
            }
            .sumOf { it.amount }
    }

    suspend fun insertBudget(budget: Budget): Long = dao.insert(budget.toEntity())
    suspend fun updateBudget(budget: Budget) = dao.update(budget.toEntity())
    suspend fun deleteBudget(budget: Budget) = dao.delete(budget.toEntity())

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun BudgetEntity.toDomain() = Budget(id, userId, category, icon, color, amount, keywords, month, year, createdAt)
    private fun Budget.toEntity() = BudgetEntity(id, userId, category, icon, color, amount, keywords, month, year, createdAt)
}
