package dev.expensemate.ui.analysis

import android.view.View
import dev.expensemate.data.ExpenseDatabase
import dev.expensemate.domain.AnalysisPeriod
import dev.expensemate.domain.ExpenseAnalysis
import dev.expensemate.model.Expense
import dev.expensemate.state.AnalysisState
import dev.expensemate.state.Screen
import dev.expensemate.ui.UiComponents
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class AnalysisScreen(
    private val ui: UiComponents,
    private val state: AnalysisState,
    private val database: ExpenseDatabase,
    private val navigate: (Screen) -> Unit
) {
    private val components = AnalysisComponents(ui, state)
    private val distribution = DistributionChart(ui, components)

    fun render(): View {
        val panel = components.analysisPanel("支出分析")
        val all = database.expensesSince(0)
        components.addAnalysisFilters(panel, all) { navigate(Screen.ANALYSIS) }
        val report = ExpenseAnalysis(all, state.analysisPeriod, state.analysisTag)
        val date = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
        panel.addView(components.analysisText("${date.format(report.start)} — ${date.format(report.now)} · 截至当前"))
        val comparison = when {
            state.analysisPeriod == AnalysisPeriod.ALL -> "全部历史支出"
            report.previousTotal.signum() == 0 && report.total.signum() == 0 -> "前一等长时段与当前均无支出"
            report.previousTotal.signum() == 0 -> "前一等长时段无支出"
            else -> {
                val change = (report.total - report.previousTotal).multiply(BigDecimal(100))
                    .divide(report.previousTotal, 1, RoundingMode.HALF_UP)
                "较前一等长时段${if (change.signum() >= 0) "增加" else "减少"} ${change.abs()}%"
            }
        }
        panel.addView(ui.metricCard("${state.analysisPeriod.label}支出", components.analysisMoney(report.total), comparison))
        if (state.analysisPeriod != AnalysisPeriod.ALL) {
            panel.addView(components.analysisText("对比前 ${report.days} 个自然日的相同时刻截止数据，使用相同标签筛选。"))
        }
        panel.addView(ui.card().apply {
            addView(components.analysisText("消费概览", 17f, true))
            addView(components.analysisText("共 ${report.expenses.size} 笔 · 有支出 ${report.activeDays} 天 / ${report.days} 天"))
            addView(components.analysisText("日均支出  ${components.analysisMoney(report.total.divide(BigDecimal(report.days), 0, RoundingMode.HALF_UP))}", 16f, true))
            addView(components.analysisText("平均每笔  ${components.analysisMoney(report.total.divide(BigDecimal(report.expenses.size.coerceAtLeast(1)), 0, RoundingMode.HALF_UP))}", 16f, true))
            addView(components.analysisText("日均按所选范围内的自然日计算，包含无支出日期。"))
        })
        panel.addView(components.analysisAction("查看全部 ${report.expenses.size} 笔记录 →") { navigate(Screen.RECORDS) })
        if (report.expenses.isEmpty()) {
            panel.addView(ui.metricCard("暂无支出", "还没有记录", "可切换时间范围、标签，或返回录入添加支出。"))
        } else {
            distribution.addDistribution(panel, "支出趋势", report.trend, "按${if (report.trend.first().label.length == 7) "月" else "日"}汇总 · 无支出日期记为 0", false)
            distribution.addDistribution(panel, "标签排行", report.tags, "每个标签计入该笔全额，多标签占比之和可能超过 100%。", true, report.total)
            distribution.addDistribution(panel, "星期分布", report.weekdays, "按支出日期汇总，展示各星期的累计金额。", true, report.total)
            distribution.addDistribution(panel, "时段分布", report.hours, "按支出发生时间汇总。", true, report.total)
            panel.addView(ui.card().apply {
                addView(components.analysisText("大额支出 TOP 5", 17f, true))
                report.expenses.sortedWith(compareByDescending<Expense> { it.amountCents }.thenByDescending { it.createdAtMillis })
                    .take(5).forEach { addView(components.expenseRecordRow(it)) }
            })
        }
        panel.addView(ui.backButton())
        return ui.scroll(panel)
    }
}
