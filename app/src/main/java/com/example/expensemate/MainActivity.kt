package com.example.expensemate

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray

class MainActivity : Activity() {
    private lateinit var database: ExpenseDatabase
    private lateinit var preferences: SharedPreferences
    private lateinit var root: FrameLayout
    private lateinit var content: FrameLayout
    private lateinit var floatingActions: View
    private lateinit var palette: Palette
    private var topSystemInset = 0
    private val selectedTags = linkedSetOf<String>()
    private val customTags = linkedSetOf<String>()
    private val tagLru = linkedSetOf<String>()
    private val timePresets = mutableListOf<TimePreset>()
    private val tagPriorityRanker = TagPriorityRanker()
    private var currentScreen = Screen.ENTRY
    private var analysisPeriod = AnalysisPeriod.MONTH
    private var analysisTag: String? = null
    private var recordSort = 0
    private var draftAmount = ""
    private var draftDetail = ""
    private var draftCustomTag = ""
    private var selectedExpenseTimeMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = ExpenseDatabase(this)
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        topSystemInset = statusBarHeight()
        palette = currentPalette()
        restoreStoredTags()
        restoreTimePresets()
        restoreUiState(savedInstanceState)

        applySystemBars()

        root = FrameLayout(this).apply {
            setBackgroundColor(palette.pageBackground)
        }

        content = FrameLayout(this)
        root.addView(content, FrameLayout.LayoutParams(match, match))
        floatingActions = createFloatingActions()
        root.addView(floatingActions)

        setContentView(root)
        when (currentScreen) {
            Screen.ENTRY -> showEntryForm()
            Screen.SETTINGS -> showSettings()
            Screen.ANALYSIS -> showAnalysis()
            Screen.RECORDS -> showRecords()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AMOUNT, draftAmount)
        outState.putString(KEY_DETAIL, draftDetail)
        outState.putString(KEY_CUSTOM_TAG, draftCustomTag)
        outState.putStringArrayList(KEY_SELECTED_TAGS, ArrayList(selectedTags))
        outState.putStringArrayList(KEY_CUSTOM_TAGS, ArrayList(customTags))
        outState.putStringArrayList(KEY_TAG_LRU, ArrayList(tagLru))
        outState.putString(KEY_SCREEN, currentScreen.name)
        outState.putString("analysis_period", analysisPeriod.name)
        outState.putString("analysis_tag", analysisTag)
        outState.putInt("record_sort", recordSort)
        selectedExpenseTimeMillis?.let { outState.putLong(KEY_EXPENSE_TIME, it) }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (currentScreen == Screen.RECORDS) {
            showAnalysis()
        } else if (currentScreen == Screen.ENTRY) {
            super.onBackPressed()
        } else {
            showEntryForm()
        }
    }

    private fun restoreUiState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }
        analysisPeriod = runCatching { AnalysisPeriod.valueOf(savedInstanceState.getString("analysis_period").orEmpty()) }.getOrDefault(AnalysisPeriod.MONTH)
        analysisTag = savedInstanceState.getString("analysis_tag")
        recordSort = savedInstanceState.getInt("record_sort").coerceIn(0, 3)
        draftAmount = savedInstanceState.getString(KEY_AMOUNT).orEmpty()
        draftDetail = savedInstanceState.getString(KEY_DETAIL).orEmpty()
        draftCustomTag = savedInstanceState.getString(KEY_CUSTOM_TAG).orEmpty()
        selectedTags.clear()
        savedInstanceState.getStringArrayList(KEY_SELECTED_TAGS)?.let { selectedTags += it }
        customTags.clear()
        savedInstanceState.getStringArrayList(KEY_CUSTOM_TAGS)?.let { customTags += it }
        tagLru.clear()
        savedInstanceState.getStringArrayList(KEY_TAG_LRU)?.let { tagLru += it }
        selectedExpenseTimeMillis = if (savedInstanceState.containsKey(KEY_EXPENSE_TIME)) {
            savedInstanceState.getLong(KEY_EXPENSE_TIME)
        } else {
            null
        }
        currentScreen = savedInstanceState.getString(KEY_SCREEN)
            ?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?: Screen.ENTRY
    }

    private fun createFloatingActions(): View {
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.END).apply {
                topMargin = topSystemInset + dp(16)
                rightMargin = dp(16)
            }
        }
        actions.addView(iconButton(R.drawable.ic_settings, "设置") { showSettings() })
        actions.addView(iconButton(R.drawable.ic_analytics, "分析") { showAnalysis() })
        return actions
    }

    private fun iconButton(icon: Int, label: String, onClick: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            contentDescription = label
            setImageResource(icon)
            setColorFilter(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 12f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                leftMargin = dp(8)
            }
        }
    }

    private fun showEntryForm() {
        currentScreen = Screen.ENTRY
        content.removeAllViews()
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), topSystemInset + dp(78), dp(18), dp(18))
        }

        val amountInput = EditText(this).apply {
            hint = "金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            imeOptions = EditorInfo.IME_ACTION_NEXT
            filters = arrayOf(AmountInputFilter())
            background = rounded(palette.inputSurface, 8f, palette.border)
            setPadding(dp(14), 0, dp(14), 0)
            textSize = 20f
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
        }
        amountInput.setText(draftAmount)
        amountInput.afterTextChanged { draftAmount = it }
        form.addView(labeledField("金额", amountInput, top = 0))

        form.addView(TextView(this).apply {
            text = "标签"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textSecondary)
            setPadding(0, dp(12), 0, dp(8))
        })

        val tagSpinner = Spinner(this).apply {
            background = rounded(palette.inputSurface, 8f, palette.border)
            setPadding(dp(10), 0, dp(10), 0)
        }
        form.addView(tagSpinner, LinearLayout.LayoutParams(match, dp(52)))
        renderTagDropdown(tagSpinner)

        val customRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val customTagInput = EditText(this).apply {
            hint = "手动输入标签"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            background = rounded(palette.inputSurface, 8f, palette.border)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
        }
        customTagInput.setText(draftCustomTag)
        customTagInput.afterTextChanged { draftCustomTag = it }
        fun addCustomTag(showEmptyMessage: Boolean = true): Boolean {
            val tag = customTagInput.text.toString().trim()
            if (tag.isEmpty()) {
                if (showEmptyMessage) {
                    toast("请输入标签")
                }
                return false
            }
            customTags += tag
            saveStoredTags()
            selectedTags.clear()
            selectedTags += tag
            customTagInput.text.clear()
            renderTagDropdown(tagSpinner)
            return true
        }
        customTagInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCustomTag()
                true
            } else {
                false
            }
        }
        customRow.addView(customTagInput, LinearLayout.LayoutParams(0, dp(50), 1f))
        customRow.addView(Button(this).apply {
            text = "添加"
            setTextColor(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 8f)
            setOnClickListener { addCustomTag() }
        }, LinearLayout.LayoutParams(dp(86), dp(50)).apply { leftMargin = dp(10) })
        form.addView(customRow)

        form.addView(TextView(this).apply {
            text = "记账时间"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textSecondary)
            setPadding(0, dp(12), 0, dp(8))
        })
        val expenseTimeLabel = TextView(this).apply {
            textSize = 15f
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER_VERTICAL
        }
        fun refreshExpenseTimeLabel() {
            expenseTimeLabel.text = selectedExpenseTimeMillis?.let { formatExpenseTime(it) } ?: "未选择，保存时使用当前时间"
        }
        refreshExpenseTimeLabel()
        val expenseTimeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        expenseTimeRow.addView(expenseTimeLabel, LinearLayout.LayoutParams(0, dp(46), 1f))
        expenseTimeRow.addView(Button(this).apply {
            text = "选择"
            setTextColor(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 8f)
            setOnClickListener {
                showExpenseTimePicker {
                    refreshExpenseTimeLabel()
                }
            }
        }, LinearLayout.LayoutParams(dp(78), dp(46)).apply { leftMargin = dp(8) })
        expenseTimeRow.addView(Button(this).apply {
            text = "清除"
            setTextColor(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 8f)
            setOnClickListener {
                selectedExpenseTimeMillis = null
                refreshExpenseTimeLabel()
            }
        }, LinearLayout.LayoutParams(dp(78), dp(46)).apply { leftMargin = dp(8) })
        form.addView(expenseTimeRow)

        form.addView(View(this), LinearLayout.LayoutParams(match, 0, 1f))

        val detailButton = Button(this).apply {
            text = detailButtonText()
            setTextColor(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 8f)
            setOnClickListener {
                showDetailDialog {
                    text = detailButtonText()
                }
            }
        }
        form.addView(detailButton, LinearLayout.LayoutParams(match, dp(48)).apply { topMargin = dp(12) })

        form.addView(Button(this).apply {
            text = "保存账单"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.primaryButtonText)
            background = rounded(palette.primaryButton, 8f)
            setOnClickListener {
                val amount = parseAmount(amountInput.text.toString())
                if (amount == null) {
                    toast("请输入有效金额")
                    return@setOnClickListener
                }
                if (!canStoreAmount(amount)) {
                    toast("金额过大，无法保存")
                    return@setOnClickListener
                }
                addCustomTag(showEmptyMessage = false)
                if (selectedTags.isEmpty()) {
                    toast("请选择或添加至少一个标签")
                    return@setOnClickListener
                }

                database.insert(
                    amount,
                    selectedTags.toList(),
                    draftDetail.trim(),
                    selectedExpenseTimeMillis ?: System.currentTimeMillis()
                )
                markTagsUsed(selectedTags)
                draftAmount = ""
                draftDetail = ""
                draftCustomTag = ""
                selectedExpenseTimeMillis = null
                amountInput.text.clear()
                selectedTags.clear()
                renderTagDropdown(tagSpinner)
                refreshExpenseTimeLabel()
                detailButton.text = detailButtonText()
                toast("已保存")
            }
        }, LinearLayout.LayoutParams(match, dp(52)).apply { topMargin = dp(14) })

        content.addView(form, FrameLayout.LayoutParams(match, match))
    }

    private fun showSettings() {
        currentScreen = Screen.SETTINGS
        content.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), topSystemInset + dp(78), dp(18), dp(18))
        }
        panel.addView(pageTitle("设置"))
        panel.addView(card().apply {
            addView(TextView(context).apply {
                text = "外观"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.textPrimary)
            })
            addView(TextView(context).apply {
                text = "日间 / 夜间 / 跟随系统"
                textSize = 14f
                setTextColor(palette.textSecondary)
                setPadding(0, dp(10), 0, 0)
            })
            addView(appearanceModeRow())
        })
        panel.addView(card().apply {
            addView(TextView(context).apply {
                text = "时间预设"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.textPrimary)
            })
            addView(TextView(context).apply {
                text = timePresets.joinToString(" / ") { it.displayText() }
                textSize = 14f
                setTextColor(palette.textSecondary)
                setPadding(0, dp(10), 0, 0)
            })
            addView(Button(context).apply {
                text = "管理时间预设"
                setTextColor(palette.secondaryButtonText)
                background = rounded(palette.secondaryButton, 8f, palette.border)
                setOnClickListener { showTimePresetManager() }
            }, LinearLayout.LayoutParams(match, dp(48)).apply { topMargin = dp(16) })
        })
        panel.addView(backButton())
        content.addView(panel, FrameLayout.LayoutParams(match, match))
    }

    private fun appearanceModeRow(): View {
        val currentMode = appearanceMode()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
            AppearanceMode.values().forEachIndexed { index, mode ->
                addView(Button(context).apply {
                    text = mode.label
                    val selected = mode == currentMode
                    setTextColor(if (selected) palette.primaryButtonText else palette.secondaryButtonText)
                    background = rounded(
                        if (selected) palette.primaryButton else palette.secondaryButton,
                        8f,
                        palette.border
                    )
                    setOnClickListener {
                        preferences.edit().putString(PREF_APPEARANCE_MODE, mode.name).apply()
                        palette = currentPalette()
                        applySystemBars()
                        root.setBackgroundColor(palette.pageBackground)
                        root.removeView(floatingActions)
                        floatingActions = createFloatingActions()
                        root.addView(floatingActions)
                        showSettings()
                    }
                }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    if (index > 0) {
                        leftMargin = dp(8)
                    }
                })
            }
        }
    }

    private fun analysisText(value: String, size: Float = 14f, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(if (bold) palette.textPrimary else palette.textSecondary)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun analysisMoney(cents: BigDecimal): String =
        NumberFormat.getCurrencyInstance(Locale.CHINA).format(cents.movePointLeft(2))

    private fun analysisPanel(title: String): LinearLayout {
        content.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), topSystemInset + dp(78), dp(18), dp(32))
        }
        panel.addView(pageTitle(title))
        content.addView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(24))
            addView(panel)
        }, FrameLayout.LayoutParams(match, match))
        return panel
    }

    private fun analysisSelector(options: List<String>, selected: Int, onSelected: (Int) -> Unit): Spinner {
        return Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, options) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    analysisText(options[position], 15f, true).apply { setPadding(dp(10), dp(12), dp(10), dp(12)) }
                override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    analysisText(options[position], 15f).apply {
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        setBackgroundColor(palette.cardSurface)
                    }
            }
            background = rounded(palette.inputSurface, 8f, palette.border)
            setSelection(selected)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position != selected) onSelected(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = dp(10) }
        }
    }

    private fun addAnalysisFilters(panel: LinearLayout, all: List<Expense>, refresh: () -> Unit) {
        panel.addView(analysisText("时间范围", bold = true))
        panel.addView(analysisSelector(AnalysisPeriod.values().map { it.label }, analysisPeriod.ordinal) {
            analysisPeriod = AnalysisPeriod.values()[it]
            refresh()
        })
        val tags = all.flatMap { it.tags.ifEmpty { listOf("未分类") } }.distinct().sorted()
        if (analysisTag !in tags) analysisTag = null
        panel.addView(analysisText("标签", bold = true))
        panel.addView(analysisSelector(listOf("全部标签") + tags, tags.indexOf(analysisTag) + 1) {
            analysisTag = if (it == 0) null else tags[it - 1]
            refresh()
        })
    }

    private fun analysisAction(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(palette.secondaryButtonText)
        background = rounded(palette.secondaryButton, 8f)
        layoutParams = LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = dp(12) }
        setOnClickListener { action() }
    }

    private fun showAnalysis() {
        currentScreen = Screen.ANALYSIS
        val panel = analysisPanel("支出分析")
        val all = database.expensesSince(0)
        addAnalysisFilters(panel, all) { showAnalysis() }
        val report = ExpenseAnalysis(all, analysisPeriod, analysisTag)
        val date = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
        panel.addView(analysisText("${date.format(report.start)} — ${date.format(report.now)} · 截至当前"))
        val comparison = when {
            analysisPeriod == AnalysisPeriod.ALL -> "全部历史支出"
            report.previousTotal.signum() == 0 && report.total.signum() == 0 -> "前一等长时段与当前均无支出"
            report.previousTotal.signum() == 0 -> "前一等长时段无支出"
            else -> {
                val change = (report.total - report.previousTotal).multiply(BigDecimal(100))
                    .divide(report.previousTotal, 1, RoundingMode.HALF_UP)
                "较前一等长时段${if (change.signum() >= 0) "增加" else "减少"} ${change.abs()}%"
            }
        }
        panel.addView(metricCard("${analysisPeriod.label}支出", analysisMoney(report.total), comparison))
        if (analysisPeriod != AnalysisPeriod.ALL) {
            panel.addView(analysisText("对比前 ${report.days} 个自然日的相同时刻截止数据，使用相同标签筛选。"))
        }
        panel.addView(card().apply {
            addView(analysisText("消费概览", 17f, true))
            addView(analysisText("共 ${report.expenses.size} 笔 · 有支出 ${report.activeDays} 天 / ${report.days} 天"))
            addView(analysisText("日均支出  ${analysisMoney(report.total.divide(BigDecimal(report.days), 0, RoundingMode.HALF_UP))}", 16f, true))
            addView(analysisText("平均每笔  ${analysisMoney(report.total.divide(BigDecimal(report.expenses.size.coerceAtLeast(1)), 0, RoundingMode.HALF_UP))}", 16f, true))
            addView(analysisText("日均按所选范围内的自然日计算，包含无支出日期。"))
        })
        panel.addView(analysisAction("查看全部 ${report.expenses.size} 笔记录 →") { showRecords() })
        if (report.expenses.isEmpty()) {
            panel.addView(metricCard("暂无支出", "还没有记录", "可切换时间范围、标签，或返回录入添加支出。"))
        } else {
            addDistribution(panel, "支出趋势", report.trend, "按${if (report.trend.first().label.length == 7) "月" else "日"}汇总 · 无支出日期记为 0", false)
            addDistribution(panel, "标签排行", report.tags, "每个标签计入该笔全额，多标签占比之和可能超过 100%。", true, report.total)
            addDistribution(panel, "星期分布", report.weekdays, "按支出日期汇总，展示各星期的累计金额。", true, report.total)
            addDistribution(panel, "时段分布", report.hours, "按支出发生时间汇总。", true, report.total)
            panel.addView(card().apply {
                addView(analysisText("大额支出 TOP 5", 17f, true))
                report.expenses.sortedWith(compareByDescending<Expense> { it.amountCents }.thenByDescending { it.createdAtMillis })
                    .take(5).forEach { addView(expenseRecordRow(it)) }
            })
        }
        panel.addView(backButton())
    }

    private fun addDistribution(panel: LinearLayout, title: String, buckets: List<AnalysisBucket>, note: String,
                                showShare: Boolean, total: BigDecimal = BigDecimal.ZERO) {
        panel.addView(card().apply {
            addView(analysisText(title, 17f, true))
            addView(analysisText(note, 12f))
            val peak = buckets.maxOfOrNull { it.total } ?: BigDecimal.ZERO
            val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            fun append(items: List<AnalysisBucket>) {
                items.forEach { bucket ->
                    val share = if (showShare && total.signum() > 0) " · ${bucket.total.multiply(BigDecimal(100)).divide(total, 1, RoundingMode.HALF_UP)}%" else ""
                    rows.addView(analysisText("${bucket.label}   ${analysisMoney(bucket.total)}$share", 14f, true))
                    rows.addView(android.widget.ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                        max = 1000
                        progress = if (peak.signum() > 0) bucket.total.multiply(BigDecimal(1000)).divide(peak, 0, RoundingMode.HALF_UP).toInt() else 0
                        progressTintList = android.content.res.ColorStateList.valueOf(palette.primaryButton)
                        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(palette.border)
                        contentDescription = "${bucket.label}：${analysisMoney(bucket.total)}"
                        layoutParams = LinearLayout.LayoutParams(match, dp(8)).apply { bottomMargin = dp(8) }
                    })
                }
            }
            addView(rows)
            val limit = 7
            append(buckets.takeLast(if (title == "支出趋势") limit else buckets.size).take(limit))
            if (buckets.size > limit) {
                addView(analysisAction("展开全部 ${buckets.size} 项") {}.apply {
                    setOnClickListener { rows.removeAllViews(); append(buckets); visibility = View.GONE }
                })
            }
        })
    }

    private fun expenseRecordRow(expense: Expense): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(10), 0, dp(10))
        addView(analysisText(money(expense.amountCents), 20f, true))
        addView(analysisText(expense.tags.joinToString(" · ").ifEmpty { "未分类" }))
        addView(analysisText(formatExpenseTime(expense.createdAtMillis), 12f))
        if (expense.detail.isNotBlank()) addView(analysisText(expense.detail).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END })
        addView(analysisText("查看详情 ›", 12f))
        background = rounded(palette.inputSurface, 8f, palette.border)
        layoutParams = LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = dp(8) }
        setPadding(dp(12), dp(10), dp(12), dp(10))
        isFocusable = true
        setOnClickListener {
            val detail = ScrollView(this@MainActivity).apply {
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(24), dp(16), dp(24), dp(16))
                    addView(analysisText(money(expense.amountCents), 28f, true))
                    addView(analysisText("支出时间\n${formatExpenseTime(expense.createdAtMillis)}"))
                    addView(analysisText("标签\n${expense.tags.joinToString(" · ").ifEmpty { "未分类" }}"))
                    addView(analysisText("备注\n${expense.detail.ifBlank { "无备注" }}").apply { setTextIsSelectable(true) })
                    addView(analysisText("记录编号  #${expense.id}", 12f))
                })
            }
            AlertDialog.Builder(this@MainActivity, dialogTheme()).setTitle("支出详情").setView(detail)
                .setPositiveButton("关闭", null).show()
        }
    }

    private fun showRecords() {
        currentScreen = Screen.RECORDS
        val panel = analysisPanel("支出记录")
        val all = database.expensesSince(0)
        addAnalysisFilters(panel, all) { showRecords() }
        panel.addView(analysisText("排序方式", bold = true))
        panel.addView(analysisSelector(listOf("时间：从新到旧", "时间：从旧到新", "金额：从高到低", "金额：从低到高"), recordSort) {
            recordSort = it
            showRecords()
        })
        val report = ExpenseAnalysis(all, analysisPeriod, analysisTag)
        val comparator = when (recordSort) {
            1 -> compareBy<Expense> { it.createdAtMillis }
            2 -> compareByDescending<Expense> { it.amountCents }.thenByDescending { it.createdAtMillis }
            3 -> compareBy<Expense> { it.amountCents }.thenByDescending { it.createdAtMillis }
            else -> compareByDescending<Expense> { it.createdAtMillis }
        }.thenByDescending { it.id }
        val records = report.expenses.sortedWith(comparator)
        panel.addView(analysisText("共 ${records.size} 笔 · 合计 ${analysisMoney(report.total)}", 16f, true))
        panel.addView(analysisAction("返回支出分析") { showAnalysis() })
        if (records.isEmpty()) panel.addView(analysisText("暂无符合条件的记录，请调整时间范围或标签。"))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        panel.addView(list)
        var shown = 0
        val more = analysisAction("加载更多") {}
        fun loadMore() {
            val next = (shown + 50).coerceAtMost(records.size)
            records.subList(shown, next).forEach { list.addView(expenseRecordRow(it)) }
            shown = next
            more.text = "加载更多（已显示 $shown / ${records.size}）"
            more.visibility = if (shown < records.size) View.VISIBLE else View.GONE
        }
        more.setOnClickListener { loadMore() }
        panel.addView(more)
        loadMore()
    }

    private fun renderTagDropdown(spinner: Spinner) {
        val rankedTags = tagPriorityRanker.rank(customTags, tagLru)
        val tags = rankedTags.map { it.tag }
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, tags) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                return tagOptionView(getItem(position).orEmpty())
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                return tagOptionView(getItem(position).orEmpty())
            }
        }
        spinner.adapter = adapter
        if (tags.isEmpty()) {
            spinner.isEnabled = false
            selectedTags.clear()
            spinner.onItemSelectedListener = null
            return
        }

        spinner.isEnabled = true
        val selectedTag = selectedTags.firstOrNull()?.takeIf { it in tags } ?: tags.first()
        selectedTags.clear()
        selectedTags += selectedTag
        spinner.setSelection(tags.indexOf(selectedTag), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTags.clear()
                selectedTags += tags[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun tagOptionView(label: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(48)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(palette.textPrimary)
            setBackgroundColor(palette.inputSurface)
        }
    }

    private fun markTagsUsed(tags: Collection<String>) {
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .asReversed()
            .forEach { tag ->
                customTags += tag
                tagLru.remove(tag)
                tagLru.addFirstCompat(tag)
            }
        tagLru.retainAll(customTags)
        saveStoredTags()
    }

    private fun restoreStoredTags() {
        customTags.clear()
        customTags += preferences.getJsonStringArray(PREF_CUSTOM_TAGS)
        tagLru.clear()
        tagLru += preferences.getJsonStringArray(PREF_TAG_LRU).filter { it in customTags }
    }

    private fun saveStoredTags() {
        preferences.edit()
            .putJsonStringArray(PREF_CUSTOM_TAGS, customTags)
            .putJsonStringArray(PREF_TAG_LRU, tagLru.filter { it in customTags })
            .apply()
    }

    private fun restoreTimePresets() {
        timePresets.clear()
        val raw = preferences.getString(PREF_TIME_PRESETS, null)
        val storedPresets = raw?.let { parseTimePresets(it) }.orEmpty()
        timePresets += storedPresets.ifEmpty { defaultTimePresets() }
    }

    private fun saveTimePresets() {
        val json = JSONArray()
        timePresets.forEach { preset ->
            json.put(org.json.JSONObject().apply {
                put("label", preset.label)
                put("hour", preset.hour)
                put("minute", preset.minute)
            })
        }
        preferences.edit().putString(PREF_TIME_PRESETS, json.toString()).apply()
    }

    private fun parseTimePresets(raw: String): List<TimePreset> {
        return runCatching {
            val values = JSONArray(raw)
            buildList {
                for (index in 0 until values.length()) {
                    val item = values.optJSONObject(index) ?: continue
                    val label = item.optString("label").trim()
                    val hour = item.optInt("hour", -1)
                    val minute = item.optInt("minute", -1)
                    if (label.isNotEmpty() && hour in 0..23 && minute in 0..59) {
                        add(TimePreset(label, hour, minute))
                    }
                }
            }.distinctBy { it.label }
        }.getOrDefault(emptyList())
    }

    private fun showExpenseTimePicker(onTimeSelected: () -> Unit) {
        val initial = Calendar.getInstance().apply {
            selectedExpenseTimeMillis?.let { timeInMillis = it }
        }
        DatePickerDialog(
            this,
            dialogTheme(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    timeInMillis = initial.timeInMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                showTimePresetPicker(selectedDate, onTimeSelected)
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePresetPicker(selectedDate: Calendar, onTimeSelected: () -> Unit) {
        val options = timePresets.map { it.displayText() } + "自定义时间"
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("选择时间")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < timePresets.size) {
                    val preset = timePresets[which]
                    selectedDate.set(Calendar.HOUR_OF_DAY, preset.hour)
                    selectedDate.set(Calendar.MINUTE, preset.minute)
                    selectedDate.set(Calendar.SECOND, 0)
                    selectedDate.set(Calendar.MILLISECOND, 0)
                    selectedExpenseTimeMillis = selectedDate.timeInMillis
                    onTimeSelected()
                } else {
                    showCustomTimePicker(selectedDate, onTimeSelected)
                }
            }
            .show()
    }

    private fun showCustomTimePicker(selectedDate: Calendar, onTimeSelected: () -> Unit) {
        TimePickerDialog(
            this,
            dialogTheme(),
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                selectedDate.set(Calendar.SECOND, 0)
                selectedDate.set(Calendar.MILLISECOND, 0)
                selectedExpenseTimeMillis = selectedDate.timeInMillis
                onTimeSelected()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun showTimePresetManager() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun renderPresets() {
            panel.removeAllViews()
            timePresets.forEach { preset ->
                panel.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(8))
                    addView(TextView(context).apply {
                        text = preset.displayText()
                        textSize = 16f
                        setTextColor(palette.textPrimary)
                    }, LinearLayout.LayoutParams(0, dp(44), 1f))
                    addView(Button(context).apply {
                        text = "删除"
                        setTextColor(palette.secondaryButtonText)
                        background = rounded(palette.secondaryButton, 8f, palette.border)
                        setOnClickListener {
                            timePresets.remove(preset)
                            saveTimePresets()
                            renderPresets()
                        }
                    }, LinearLayout.LayoutParams(dp(78), dp(44)).apply { leftMargin = dp(8) })
                })
            }
            panel.addView(Button(this).apply {
                text = "新增预设"
                setTextColor(palette.primaryButtonText)
                background = rounded(palette.primaryButton, 8f)
                setOnClickListener { showAddTimePresetDialog { renderPresets() } }
            }, LinearLayout.LayoutParams(match, dp(48)).apply { topMargin = dp(8) })
        }
        renderPresets()
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("时间预设")
            .setView(panel)
            .setPositiveButton("完成") { _, _ -> showSettings() }
            .show()
    }

    private fun showAddTimePresetDialog(onPresetAdded: () -> Unit) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val nameInput = EditText(this).apply {
            hint = "名称"
            setSingleLine(true)
            background = rounded(palette.inputSurface, 8f, palette.border)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
        }
        val timeInput = EditText(this).apply {
            hint = "时间，例如 08:30"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
            background = rounded(palette.inputSurface, 8f, palette.border)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
        }
        form.addView(nameInput, LinearLayout.LayoutParams(match, dp(50)))
        form.addView(timeInput, LinearLayout.LayoutParams(match, dp(50)).apply { topMargin = dp(12) })

        AlertDialog.Builder(this, dialogTheme())
            .setTitle("新增时间预设")
            .setView(form)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val label = nameInput.text.toString().trim()
                        val time = parsePresetTime(timeInput.text.toString().trim())
                        when {
                            label.isEmpty() -> toast("请输入名称")
                            time == null -> toast("请输入正确时间")
                            timePresets.any { it.label == label } -> toast("预设名称已存在")
                            else -> {
                                timePresets += TimePreset(label, time.first, time.second)
                                saveTimePresets()
                                dismiss()
                                onPresetAdded()
                            }
                        }
                    }
                }
            }
            .show()
    }

    private fun showDetailDialog(onDetailChanged: () -> Unit) {
        val input = EditText(this).apply {
            setText(draftDetail)
            hint = "详细"
            gravity = Gravity.TOP
            minLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            background = rounded(palette.inputSurface, 8f, palette.border)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
        }
        val container = FrameLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, FrameLayout.LayoutParams(match, dp(160)))
        }
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("详细")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                draftDetail = input.text.toString()
                onDetailChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun SharedPreferences.getJsonStringArray(key: String): List<String> {
        val raw = getString(key, null) ?: return emptyList()
        return runCatching {
            val values = JSONArray(raw)
            List(values.length()) { index -> values.optString(index).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }.getOrDefault(emptyList())
    }

    private fun SharedPreferences.Editor.putJsonStringArray(
        key: String,
        values: Collection<String>
    ): SharedPreferences.Editor {
        val json = JSONArray()
        values.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { json.put(it) }
        return putString(key, json.toString())
    }

    private fun <T> LinkedHashSet<T>.addFirstCompat(value: T) {
        val current = toList()
        clear()
        add(value)
        current.filterNot { it == value }.forEach { add(it) }
    }

    private fun labeledField(label: String, field: View, top: Int, height: Int = dp(52)): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(top), 0, 0)
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.textSecondary)
                setPadding(0, 0, 0, dp(8))
            })
            addView(field, LinearLayout.LayoutParams(match, height))
        }
    }

    private fun pageTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
            includeFontPadding = false
            setPadding(0, 0, 0, dp(18))
        }
    }

    private fun metricCard(title: String, value: String, subtitle: String): View {
        return card().apply {
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.textSecondary)
            })
            addView(TextView(context).apply {
                text = value
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.textPrimary)
                includeFontPadding = false
                setPadding(0, dp(8), 0, dp(6))
            })
            addView(TextView(context).apply {
                text = subtitle
                textSize = 13f
                setTextColor(palette.textSecondary)
            })
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(palette.cardSurface, 8f, palette.border)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply {
                bottomMargin = dp(12)
            }
        }
    }

    private fun backButton(): Button {
        return Button(this).apply {
            text = "返回录入"
            setTextColor(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 8f)
            setOnClickListener { showEntryForm() }
            layoutParams = LinearLayout.LayoutParams(match, dp(52)).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun parseAmount(raw: String): BigDecimal? {
        return raw.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { BigDecimal(it) }.getOrNull()
        }?.takeIf { it > BigDecimal.ZERO && it.scale() <= AMOUNT_DECIMAL_PLACES }
            ?.setScale(AMOUNT_DECIMAL_PLACES, RoundingMode.UNNECESSARY)
    }

    private inner class AmountInputFilter : InputFilter {
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
    }

    private fun canStoreAmount(amount: BigDecimal): Boolean {
        return amount.movePointRight(2) <= MAX_AMOUNT_CENTS
    }

    private fun money(cents: Long): String {
        val value = BigDecimal(cents).movePointLeft(2)
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format(value)
    }

    private fun formatExpenseTime(millis: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.CHINA)
            .format(millis)
    }

    private fun parsePresetTime(raw: String): Pair<Int, Int>? {
        val match = PRESET_TIME_PATTERN.matchEntire(raw) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour to minute else null
    }

    private fun detailButtonText(): String {
        return if (draftDetail.isBlank()) "详细" else "详细（已填写）"
    }

    private fun appearanceMode(): AppearanceMode {
        val stored = preferences.getString(PREF_APPEARANCE_MODE, AppearanceMode.SYSTEM.name)
        return stored?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM
    }

    private fun currentPalette(): Palette {
        val useNight = when (appearanceMode()) {
            AppearanceMode.DAY -> false
            AppearanceMode.NIGHT -> true
            AppearanceMode.SYSTEM -> isSystemNightMode()
        }
        return if (useNight) Palette.night() else Palette.day()
    }

    private fun dialogTheme(): Int {
        return if (palette.isNight) R.style.AppDialogDark else R.style.AppDialogLight
    }

    private fun isSystemNightMode(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applySystemBars() {
        window.statusBarColor = palette.systemBar
        window.navigationBarColor = palette.systemBar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (palette.lightStatusBar) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                0
            }
        }
    }

    private fun startOfCurrentWeekMillis(): Long {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            setStartOfDay()
        }.timeInMillis
    }

    private fun startOfCurrentMonthMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            setStartOfDay()
        }.timeInMillis
    }

    private fun Calendar.setStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun rounded(color: Int, radiusDp: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            strokeColor?.let { setStroke(dp(1), it) }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun EditText.afterTextChanged(onChanged: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString().orEmpty())
            }
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun statusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    companion object {
        private const val match = LinearLayout.LayoutParams.MATCH_PARENT
        private const val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        private const val KEY_AMOUNT = "amount"
        private const val KEY_DETAIL = "detail"
        private const val KEY_CUSTOM_TAG = "custom_tag"
        private const val KEY_SELECTED_TAGS = "selected_tags"
        private const val KEY_CUSTOM_TAGS = "custom_tags"
        private const val KEY_TAG_LRU = "tag_lru"
        private const val KEY_SCREEN = "screen"
        private const val KEY_EXPENSE_TIME = "expense_time"
        private const val PREFERENCES_NAME = "expense_mate"
        private const val PREF_CUSTOM_TAGS = "custom_tags"
        private const val PREF_TAG_LRU = "tag_lru"
        private const val PREF_APPEARANCE_MODE = "appearance_mode"
        private const val PREF_TIME_PRESETS = "time_presets"
        private const val AMOUNT_DECIMAL_PLACES = 2
        private val AMOUNT_INPUT_PATTERN = Regex("""\d*(\.\d{0,2})?""")
        private val PRESET_TIME_PATTERN = Regex("""(\d{1,2}):(\d{2})""")
        private val MAX_AMOUNT_CENTS = BigDecimal.valueOf(Long.MAX_VALUE)

        private fun defaultTimePresets() = listOf(
            TimePreset("早上", 8, 0),
            TimePreset("中午", 12, 0),
            TimePreset("下午", 15, 0),
            TimePreset("傍晚", 18, 0),
            TimePreset("夜间", 21, 0)
        )
    }

    private enum class AppearanceMode(val label: String) {
        DAY("日间"),
        NIGHT("夜间"),
        SYSTEM("跟随系统")
    }

    private data class TimePreset(
        val label: String,
        val hour: Int,
        val minute: Int
    ) {
        fun displayText(): String = "%s %02d:%02d".format(Locale.CHINA, label, hour, minute)
    }

    private data class Palette(
        val pageBackground: Int,
        val inputSurface: Int,
        val cardSurface: Int,
        val border: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val primaryButton: Int,
        val primaryButtonText: Int,
        val secondaryButton: Int,
        val secondaryButtonText: Int,
        val systemBar: Int,
        val lightStatusBar: Boolean,
        val isNight: Boolean
    ) {
        companion object {
            fun day() = Palette(
                pageBackground = Color.rgb(245, 245, 245),
                inputSurface = Color.WHITE,
                cardSurface = Color.WHITE,
                border = Color.rgb(210, 210, 210),
                textPrimary = Color.rgb(18, 18, 18),
                textSecondary = Color.rgb(96, 96, 96),
                primaryButton = Color.rgb(18, 18, 18),
                primaryButtonText = Color.WHITE,
                secondaryButton = Color.rgb(224, 224, 224),
                secondaryButtonText = Color.rgb(24, 24, 24),
                systemBar = Color.rgb(245, 245, 245),
                lightStatusBar = true,
                isNight = false
            )

            fun night() = Palette(
                pageBackground = Color.rgb(10, 10, 10),
                inputSurface = Color.rgb(31, 31, 31),
                cardSurface = Color.rgb(24, 24, 24),
                border = Color.rgb(78, 78, 78),
                textPrimary = Color.rgb(245, 245, 245),
                textSecondary = Color.rgb(170, 170, 170),
                primaryButton = Color.WHITE,
                primaryButtonText = Color.rgb(12, 12, 12),
                secondaryButton = Color.rgb(47, 47, 47),
                secondaryButtonText = Color.rgb(238, 238, 238),
                systemBar = Color.rgb(0, 0, 0),
                lightStatusBar = false,
                isNight = true
            )
        }
    }

    private enum class Screen {
        ENTRY,
        SETTINGS,
        ANALYSIS,
        RECORDS
    }
}
