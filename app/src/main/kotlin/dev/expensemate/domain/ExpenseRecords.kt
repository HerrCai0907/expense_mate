package dev.expensemate.domain

import dev.expensemate.model.Expense

object ExpenseRecords {
    fun sorted(expenses: List<Expense>, recordSort: Int): List<Expense> {
        val comparator = when (recordSort) {
            1 -> compareBy<Expense> { it.createdAtMillis }
            2 -> compareByDescending<Expense> { it.amountCents }.thenByDescending { it.createdAtMillis }
            3 -> compareBy<Expense> { it.amountCents }.thenByDescending { it.createdAtMillis }
            else -> compareByDescending<Expense> { it.createdAtMillis }
        }.thenByDescending { it.id }
        return expenses.sortedWith(comparator)
    }
}
