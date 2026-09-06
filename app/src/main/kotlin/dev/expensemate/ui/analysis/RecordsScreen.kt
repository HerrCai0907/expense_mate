package dev.expensemate.ui.analysis

import android.view.View
import android.widget.LinearLayout
import dev.expensemate.data.ExpenseDatabase
import dev.expensemate.domain.ExpenseAnalysis
import dev.expensemate.domain.ExpenseRecords
import dev.expensemate.state.AnalysisState
import dev.expensemate.state.Screen
import dev.expensemate.ui.UiComponents

class RecordsScreen(
    private val ui: UiComponents,
    private val state: AnalysisState,
    private val database: ExpenseDatabase,
    private val navigate: (Screen) -> Unit
) {
    private val context get() = ui.context

    private val components = AnalysisComponents(ui, state)

    fun render(): View {
        val panel = components.analysisPanel("支出记录")
        val all = database.expensesSince(0)
        components.addAnalysisFilters(panel, all) { navigate(Screen.RECORDS) }
        panel.addView(components.analysisText("排序方式", bold = true))
        panel.addView(components.analysisSelector(listOf("时间：从新到旧", "时间：从旧到新", "金额：从高到低", "金额：从低到高"), state.recordSort) {
            state.recordSort = it
            navigate(Screen.RECORDS)
        })
        val report = ExpenseAnalysis(all, state.analysisPeriod, state.analysisTag)
        val records = ExpenseRecords.sorted(report.expenses, state.recordSort)
        panel.addView(components.analysisText("共 ${records.size} 笔 · 合计 ${components.analysisMoney(report.total)}", 16f, true))
        panel.addView(components.analysisAction("返回支出分析") { navigate(Screen.ANALYSIS) })
        if (records.isEmpty()) panel.addView(components.analysisText("暂无符合条件的记录，请调整时间范围或标签。"))
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        panel.addView(list)
        var shown = 0
        val more = components.analysisAction("加载更多") {}
        fun loadMore() {
            val next = (shown + 50).coerceAtMost(records.size)
            records.subList(shown, next).forEach { list.addView(components.expenseRecordRow(it)) }
            shown = next
            more.text = "加载更多（已显示 $shown / ${records.size}）"
            more.visibility = if (shown < records.size) View.VISIBLE else View.GONE
        }
        more.setOnClickListener { loadMore() }
        panel.addView(more)
        loadMore()
        return ui.scroll(panel)
    }
}
