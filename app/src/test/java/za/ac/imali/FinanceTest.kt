package za.ac.imali

import org.junit.Assert.assertEquals
import org.junit.Test
import za.ac.imali.data.Budget
import za.ac.imali.data.Transaction
import za.ac.imali.util.BudgetColor
import za.ac.imali.util.Finance

class FinanceTest {
    @Test
    fun balanceCalculation() {
        val txs = listOf(
            Transaction(id = "1", amount = 100.0, type = "income", category = "Salary", date = "2026-11-01"),
            Transaction(id = "2", amount = 40.0, type = "expense", category = "Food", date = "2026-11-02"),
        )
        assertEquals(60.0, Finance.balance(txs), 0.001)
    }

    @Test
    fun budgetProgressColors() {
        assertEquals(BudgetColor.GREEN, Finance.budgetColor(0.5f))
        assertEquals(BudgetColor.AMBER, Finance.budgetColor(0.85f))
        assertEquals(BudgetColor.RED, Finance.budgetColor(1.1f))
    }

    @Test
    fun enrichBudgets() {
        val budgets = listOf(Budget(id = "b1", category = "Food", month = "2026-11", limit = 100.0))
        val txs = listOf(
            Transaction(id = "t1", amount = 80.0, type = "expense", category = "Food", date = "2026-11-05"),
        )
        val result = Finance.enrichBudgets(budgets, txs)
        assertEquals(0.8f, result.first().progress, 0.01f)
    }
}
