package dev.expensemate.domain

import dev.expensemate.model.Expense
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AnalysisPeriod(val label: String) {
    WEEK("本周"), MONTH("本月"), DAYS_30("近30天"), YEAR("今年"), ALL("全部")
}

data class AnalysisBucket(val label: String, val expenses: List<Expense>) {
    val total: BigDecimal get() = expenses.fold(BigDecimal.ZERO) { sum, item -> sum + item.amountCents.toBigDecimal() }
}

class ExpenseAnalysis(
    allExpenses: List<Expense>,
    val period: AnalysisPeriod,
    val tag: String? = null,
    val now: Long = System.currentTimeMillis()
) {
    private fun calendar(time: Long) = Calendar.getInstance().apply { timeInMillis = time }
    private fun midnight(time: Long) = calendar(time).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start: Long = midnight(now).apply {
        when (period) {
            AnalysisPeriod.WEEK -> add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
            AnalysisPeriod.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
            AnalysisPeriod.DAYS_30 -> add(Calendar.DAY_OF_MONTH, -29)
            AnalysisPeriod.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
            AnalysisPeriod.ALL -> timeInMillis = midnight(allExpenses.filter { it.createdAtMillis <= now }
                .minOfOrNull { it.createdAtMillis } ?: now).timeInMillis
        }
    }.timeInMillis
    // Calendar-day arithmetic keeps the denominator correct across daylight-saving changes.
    val days: Int = run {
        val cursor = midnight(start)
        var count = 0
        while (cursor.timeInMillis <= now) {
            count++
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        count.coerceAtLeast(1)
    }
    private val previousStart = calendar(start).apply { add(Calendar.DAY_OF_MONTH, -days) }.timeInMillis
    private val previousEnd = calendar(now).apply { add(Calendar.DAY_OF_MONTH, -days) }.timeInMillis
    private val matching = allExpenses.filter { tag == null || tag in it.tags || (tag == "未分类" && it.tags.isEmpty()) }
    val expenses = matching.filter { it.createdAtMillis in start..now }
    val total = AnalysisBucket("", expenses).total
    val previousTotal = AnalysisBucket("", matching.filter {
        it.createdAtMillis >= previousStart && it.createdAtMillis <= previousEnd && it.createdAtMillis < start
    }).total
    val activeDays = expenses.map { midnight(it.createdAtMillis).timeInMillis }.distinct().size
    val tags = expenses.flatMap { expense -> expense.tags.distinct().ifEmpty { listOf("未分类") }.map { it to expense } }
        .groupBy({ it.first }, { it.second }).map { AnalysisBucket(it.key, it.value) }.sortedByDescending { it.total }
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").mapIndexed { index, label ->
        AnalysisBucket(label, expenses.filter { (calendar(it.createdAtMillis).get(Calendar.DAY_OF_WEEK) + 5) % 7 == index })
    }
    val hours = listOf("凌晨 00–06", "上午 06–12", "下午 12–18", "晚间 18–24").mapIndexed { index, label ->
        AnalysisBucket(label, expenses.filter { calendar(it.createdAtMillis).get(Calendar.HOUR_OF_DAY) / 6 == index })
    }
    val trend: List<AnalysisBucket> = run {
        val monthly = period == AnalysisPeriod.YEAR || (period == AnalysisPeriod.ALL && days > 60)
        val format = SimpleDateFormat(if (monthly) "yyyy-MM" else "MM-dd", Locale.CHINA)
        val grouped = expenses.groupBy { format.format(it.createdAtMillis) }
        val cursor = midnight(start)
        if (monthly) cursor.set(Calendar.DAY_OF_MONTH, 1)
        buildList {
            while (cursor.timeInMillis <= now) {
                val label = format.format(cursor.time)
                add(AnalysisBucket(label, grouped[label].orEmpty()))
                cursor.add(if (monthly) Calendar.MONTH else Calendar.DAY_OF_MONTH, 1)
            }
        }
    }
}
