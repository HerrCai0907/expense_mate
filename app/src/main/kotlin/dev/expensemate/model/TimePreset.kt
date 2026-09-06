package dev.expensemate.model

import java.util.Locale

data class TimePreset(
    val label: String,
    val hour: Int,
    val minute: Int
) {
    fun displayText(): String = "%s %02d:%02d".format(Locale.CHINA, label, hour, minute)
}
