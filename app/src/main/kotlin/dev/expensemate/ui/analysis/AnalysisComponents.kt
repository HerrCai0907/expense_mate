package dev.expensemate.ui.analysis

import android.app.AlertDialog
import android.graphics.Typeface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import dev.expensemate.domain.AnalysisPeriod
import dev.expensemate.domain.formatExpenseTime
import dev.expensemate.domain.money
import dev.expensemate.model.Expense
import dev.expensemate.state.AnalysisState
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.match
import dev.expensemate.ui.wrap
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class AnalysisComponents(
    private val ui: UiComponents,
    private val state: AnalysisState
) {
    private val context get() = ui.context

    fun analysisText(value: String, size: Float = 14f, bold: Boolean = false): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(if (bold) ui.palette.textPrimary else ui.palette.textSecondary)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, ui.dp(4), 0, ui.dp(4))
    }

    fun analysisMoney(cents: BigDecimal): String =
        NumberFormat.getCurrencyInstance(Locale.CHINA).format(cents.movePointLeft(2))

    fun analysisPanel(title: String): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(18), ui.topSystemInset + ui.dp(78), ui.dp(18), ui.dp(32))
        }
        panel.addView(ui.pageTitle(title))
        return panel
    }

    fun analysisSelector(options: List<String>, selected: Int, onSelected: (Int) -> Unit): Spinner {
        return Spinner(context).apply {
            adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, options) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    analysisText(options[position], 15f, true).apply { setPadding(ui.dp(10), ui.dp(12), ui.dp(10), ui.dp(12)) }
                override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    analysisText(options[position], 15f).apply {
                        setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
                        setBackgroundColor(ui.palette.cardSurface)
                    }
            }
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setSelection(selected)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position != selected) onSelected(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = ui.dp(10) }
        }
    }

    fun addAnalysisFilters(panel: LinearLayout, all: List<Expense>, refresh: () -> Unit) {
        panel.addView(analysisText("时间范围", bold = true))
        panel.addView(analysisSelector(AnalysisPeriod.values().map { it.label }, state.analysisPeriod.ordinal) {
            state.analysisPeriod = AnalysisPeriod.values()[it]
            refresh()
        })
        val tags = all.flatMap { it.tags.ifEmpty { listOf("未分类") } }.distinct().sorted()
        if (state.analysisTag !in tags) state.analysisTag = null
        panel.addView(analysisText("标签", bold = true))
        panel.addView(analysisSelector(listOf("全部标签") + tags, tags.indexOf(state.analysisTag) + 1) {
            state.analysisTag = if (it == 0) null else tags[it - 1]
            refresh()
        })
    }

    fun analysisAction(label: String, action: () -> Unit): Button = Button(context).apply {
        text = label
        isAllCaps = false
        setTextColor(ui.palette.secondaryButtonText)
        background = ui.rounded(ui.palette.secondaryButton, 8f)
        layoutParams = LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = ui.dp(12) }
        setOnClickListener { action() }
    }

    fun expenseRecordRow(expense: Expense): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, ui.dp(10), 0, ui.dp(10))
        addView(analysisText(money(expense.amountCents), 20f, true))
        addView(analysisText(expense.tags.joinToString(" · ").ifEmpty { "未分类" }))
        addView(analysisText(formatExpenseTime(expense.createdAtMillis), 12f))
        if (expense.detail.isNotBlank()) addView(analysisText(expense.detail).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END })
        addView(analysisText("查看详情 ›", 12f))
        background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
        layoutParams = LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = ui.dp(8) }
        setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        isFocusable = true
        setOnClickListener {
            val detail = ScrollView(context).apply {
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(ui.dp(24), ui.dp(16), ui.dp(24), ui.dp(16))
                    addView(analysisText(money(expense.amountCents), 28f, true))
                    addView(analysisText("支出时间\n${formatExpenseTime(expense.createdAtMillis)}"))
                    addView(analysisText("标签\n${expense.tags.joinToString(" · ").ifEmpty { "未分类" }}"))
                    addView(analysisText("备注\n${expense.detail.ifBlank { "无备注" }}").apply { setTextIsSelectable(true) })
                    addView(analysisText("记录编号  #${expense.id}", 12f))
                })
            }
            AlertDialog.Builder(context, ui.dialogTheme()).setTitle("支出详情").setView(detail)
                .setPositiveButton("关闭", null).show()
        }
    }
}
