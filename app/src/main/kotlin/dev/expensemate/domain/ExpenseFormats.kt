package dev.expensemate.domain

import dev.expensemate.ui.match
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

fun money(cents: Long): String {
    val value = BigDecimal(cents).movePointLeft(2)
    return NumberFormat.getCurrencyInstance(Locale.CHINA).format(value)
}

fun formatExpenseTime(millis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.CHINA)
        .format(millis)
}

fun parsePresetTime(raw: String): Pair<Int, Int>? {
    val match = PRESET_TIME_PATTERN.matchEntire(raw) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}
private val PRESET_TIME_PATTERN = Regex("""(\d{1,2}):(\d{2})""")
