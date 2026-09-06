package dev.expensemate.model



data class Expense(
    val id: Long,
    val amountCents: Long,
    val tags: List<String>,
    val detail: String,
    val createdAtMillis: Long
)
