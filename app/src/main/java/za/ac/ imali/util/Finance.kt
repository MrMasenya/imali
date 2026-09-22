package za.ac.imali.util

import za.ac.imali.data.Budget
import za.ac.imali.data.Transaction

object Finance {
    fun balance(transactions: List<Transaction>): Double =
        transactions.sumOf { if (it.type == "income") it.amount else -it.amount }

    fun spentInCategory(transactions: List<Transaction>, category: String, month: String): Double =
        transactions
            .filter { it.type == "expense" && it.category == category && it.date.startsWith(month) }
            .sumOf { it.amount }

    fun budgetProgress(spent: Double, limit: Double): Float =
        if (limit <= 0) 0f else (spent / limit).toFloat().coerceIn(0f, 1f)

    fun budgetColor(progress: Float): BudgetColor = when {
        progress >= 1f -> BudgetColor.RED
        progress >= 0.8f -> BudgetColor.AMBER
        else -> BudgetColor.GREEN
    }

    fun categoryTotals(transactions: List<Transaction>): Map<String, Double> =
        transactions
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

    fun enrichBudgets(budgets: List<Budget>, transactions: List<Transaction>): List<BudgetStatus> =
        budgets.map { budget ->
            val spent = spentInCategory(transactions, budget.category, budget.month)
            val progress = budgetProgress(spent, budget.limit)
            BudgetStatus(budget, spent, progress, budgetColor(progress))
        }
}

enum class BudgetColor { GREEN, AMBER, RED }

data class BudgetStatus(
    val budget: Budget,
    val spent: Double,
    val progress: Float,
    val color: BudgetColor,
)
