package dev.expensemate.ui.analysis

import android.view.View
import android.widget.LinearLayout
import dev.expensemate.domain.AnalysisBucket
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.match
import java.math.BigDecimal
import java.math.RoundingMode

class DistributionChart(
    private val ui: UiComponents,
    private val components: AnalysisComponents
) {
    private val context get() = ui.context

    fun addDistribution(panel: LinearLayout, title: String, buckets: List<AnalysisBucket>, note: String,
                                showShare: Boolean, total: BigDecimal = BigDecimal.ZERO) {
        panel.addView(ui.card().apply {
            addView(components.analysisText(title, 17f, true))
            addView(components.analysisText(note, 12f))
            val peak = buckets.maxOfOrNull { it.total } ?: BigDecimal.ZERO
            val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            fun append(items: List<AnalysisBucket>) {
                items.forEach { bucket ->
                    val share = if (showShare && total.signum() > 0) " · ${bucket.total.multiply(BigDecimal(100)).divide(total, 1, RoundingMode.HALF_UP)}%" else ""
                    rows.addView(components.analysisText("${bucket.label}   ${components.analysisMoney(bucket.total)}$share", 14f, true))
                    rows.addView(android.widget.ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                        max = 1000
                        progress = if (peak.signum() > 0) bucket.total.multiply(BigDecimal(1000)).divide(peak, 0, RoundingMode.HALF_UP).toInt() else 0
                        progressTintList = android.content.res.ColorStateList.valueOf(ui.palette.primaryButton)
                        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ui.palette.border)
                        contentDescription = "${bucket.label}：${components.analysisMoney(bucket.total)}"
                        layoutParams = LinearLayout.LayoutParams(match, ui.dp(8)).apply { bottomMargin = ui.dp(8) }
                    })
                }
            }
            addView(rows)
            val limit = 7
            append(buckets.takeLast(if (title == "支出趋势") limit else buckets.size).take(limit))
            if (buckets.size > limit) {
                addView(components.analysisAction("展开全部 ${buckets.size} 项") {}.apply {
                    setOnClickListener { rows.removeAllViews(); append(buckets); visibility = View.GONE }
                })
            }
        })
    }
}
