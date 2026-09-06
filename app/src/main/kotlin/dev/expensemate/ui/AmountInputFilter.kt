package dev.expensemate.ui

import android.text.InputFilter
import android.text.Spanned

class AmountInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val next = dest.toString().replaceRange(dstart, dend, source.subSequence(start, end).toString())
        return if (AMOUNT_INPUT_PATTERN.matches(next)) null else ""
    }

    companion object {
        private val AMOUNT_INPUT_PATTERN = Regex("""\d*(\.\d{0,2})?""")
    }
}
