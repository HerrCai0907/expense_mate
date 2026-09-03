package com.example.expensemate

import android.app.Activity
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
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var database: ExpenseDatabase
    private lateinit var content: FrameLayout
    private val selectedTags = linkedSetOf<String>()
    private val customTags = linkedSetOf<String>()
    private var currentScreen = Screen.ENTRY
    private var draftAmount = ""
    private var draftDetail = ""
    private var draftCustomTag = ""
    private val defaultTags = listOf("餐饮", "交通", "购物", "娱乐", "住房", "医疗")

    private val blue = Color.rgb(21, 101, 192)
    private val blueDark = Color.rgb(13, 71, 161)
    private val orange = Color.rgb(255, 143, 0)
    private val surface = Color.rgb(246, 248, 252)
    private val textPrimary = Color.rgb(28, 35, 45)
    private val textSecondary = Color.rgb(91, 101, 115)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = ExpenseDatabase(this)
        restoreUiState(savedInstanceState)

        window.statusBarColor = blueDark
        window.navigationBarColor = blueDark

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
        }

        root.addView(createHeader())
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(match, 0, 1f))

        setContentView(root)
        when (currentScreen) {
            Screen.ENTRY -> showEntryForm()
            Screen.SETTINGS -> showSettings()
            Screen.ANALYSIS -> showAnalysis()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AMOUNT, draftAmount)
        outState.putString(KEY_DETAIL, draftDetail)
        outState.putString(KEY_CUSTOM_TAG, draftCustomTag)
        outState.putStringArrayList(KEY_SELECTED_TAGS, ArrayList(selectedTags))
        outState.putStringArrayList(KEY_CUSTOM_TAGS, ArrayList(customTags))
        outState.putString(KEY_SCREEN, currentScreen.name)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (currentScreen == Screen.ENTRY) {
            super.onBackPressed()
        } else {
            showEntryForm()
        }
    }

    private fun restoreUiState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }
        draftAmount = savedInstanceState.getString(KEY_AMOUNT).orEmpty()
        draftDetail = savedInstanceState.getString(KEY_DETAIL).orEmpty()
        draftCustomTag = savedInstanceState.getString(KEY_CUSTOM_TAG).orEmpty()
        selectedTags.clear()
        savedInstanceState.getStringArrayList(KEY_SELECTED_TAGS)?.let { selectedTags += it }
        customTags.clear()
        savedInstanceState.getStringArrayList(KEY_CUSTOM_TAGS)?.let { customTags += it }
        currentScreen = savedInstanceState.getString(KEY_SCREEN)
            ?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?: Screen.ENTRY
    }

    private fun createHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(14), dp(16))
            background = rounded(blue, 0f)
        }

        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleBlock.addView(TextView(this).apply {
            text = "记账"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        })
        titleBlock.addView(TextView(this).apply {
            text = "本地账单记录"
            setTextColor(Color.rgb(205, 225, 255))
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
            includeFontPadding = false
        })
        header.addView(titleBlock, LinearLayout.LayoutParams(0, wrap, 1f))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        actions.addView(iconButton(R.drawable.ic_settings, "设置") { showSettings() })
        actions.addView(iconButton(R.drawable.ic_analytics, "分析") { showAnalysis() })
        header.addView(actions, LinearLayout.LayoutParams(wrap, wrap))
        return header
    }

    private fun iconButton(icon: Int, label: String, onClick: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            contentDescription = label
            setImageResource(icon)
            setColorFilter(Color.WHITE)
            background = rounded(orange, 12f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun showEntryForm() {
        currentScreen = Screen.ENTRY
        content.removeAllViews()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        form.addView(TextView(this).apply {
            text = "记录一笔支出"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            includeFontPadding = false
        })

        val amountInput = EditText(this).apply {
            hint = "金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            imeOptions = EditorInfo.IME_ACTION_NEXT
            filters = arrayOf(AmountInputFilter())
            background = rounded(Color.WHITE, 8f, Color.rgb(214, 223, 235))
            setPadding(dp(14), 0, dp(14), 0)
            textSize = 20f
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
        }
        amountInput.setText(draftAmount)
        amountInput.afterTextChanged { draftAmount = it }
        form.addView(labeledField("金额", amountInput, top = 22))

        form.addView(TextView(this).apply {
            text = "标签"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textSecondary)
            setPadding(0, dp(22), 0, dp(10))
        })

        val tagSpinner = Spinner(this).apply {
            background = rounded(Color.WHITE, 8f, Color.rgb(214, 223, 235))
            setPadding(dp(10), 0, dp(10), 0)
        }
        form.addView(tagSpinner, LinearLayout.LayoutParams(match, dp(52)))
        renderTagDropdown(tagSpinner)

        val customRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val customTagInput = EditText(this).apply {
            hint = "手动输入标签"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            background = rounded(Color.WHITE, 8f, Color.rgb(214, 223, 235))
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
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
            setTextColor(Color.WHITE)
            background = rounded(orange, 8f)
            setOnClickListener { addCustomTag() }
        }, LinearLayout.LayoutParams(dp(86), dp(50)).apply { leftMargin = dp(10) })
        form.addView(customRow)

        val detailInput = EditText(this).apply {
            hint = "详细"
            gravity = Gravity.TOP
            minLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            background = rounded(Color.WHITE, 8f, Color.rgb(214, 223, 235))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
        }
        detailInput.setText(draftDetail)
        detailInput.afterTextChanged { draftDetail = it }
        form.addView(labeledField("详细", detailInput, top = 22, height = dp(140)))

        form.addView(Button(this).apply {
            text = "保存账单"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(orange, 8f)
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

                database.insert(amount, selectedTags.toList(), detailInput.text.toString().trim())
                draftAmount = ""
                draftDetail = ""
                draftCustomTag = ""
                amountInput.text.clear()
                detailInput.text.clear()
                selectedTags.clear()
                renderTagDropdown(tagSpinner)
                toast("已保存")
            }
        }, LinearLayout.LayoutParams(match, dp(54)).apply { topMargin = dp(28) })

        scroll.addView(form)
        content.addView(scroll)
    }

    private fun showSettings() {
        currentScreen = Screen.SETTINGS
        content.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(28), dp(18), dp(18))
        }
        panel.addView(pageTitle("设置"))
        panel.addView(card().apply {
            addView(TextView(context).apply {
                text = "设置功能占位"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textPrimary)
            })
            addView(TextView(context).apply {
                text = "后续可以在这里加入默认标签、预算、导出等选项。"
                textSize = 14f
                setTextColor(textSecondary)
                setPadding(0, dp(10), 0, 0)
            })
        })
        panel.addView(backButton())
        content.addView(panel)
    }

    private fun showAnalysis() {
        currentScreen = Screen.ANALYSIS
        content.removeAllViews()
        val weekStart = startOfCurrentWeekMillis()
        val monthStart = startOfCurrentMonthMillis()
        val weekTotal = database.totalSince(weekStart)
        val monthTotal = database.totalSince(monthStart)
        val weekCount = database.countSince(weekStart)
        val monthCount = database.countSince(monthStart)
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

        val scroll = ScrollView(this).apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        panel.addView(pageTitle("支出分析"))
        panel.addView(metricCard("本周支出", money(weekTotal), "本周共 $weekCount 笔"))
        panel.addView(metricCard("本月支出", money(monthTotal), "本月共 $monthCount 笔"))
        panel.addView(metricCard("本月日均", money(monthTotal / dayOfMonth), "按当前日期自然日均摊"))
        panel.addView(backButton())
        scroll.addView(panel)
        content.addView(scroll)
    }

    private fun renderTagDropdown(spinner: Spinner) {
        val tags = defaultTags + customTags.filterNot { it in defaultTags }
        val selectedTag = selectedTags.firstOrNull()?.takeIf { it in tags } ?: tags.first()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tags).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter
        spinner.setSelection(tags.indexOf(selectedTag), false)
        selectedTags.clear()
        selectedTags += selectedTag
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTags.clear()
                selectedTags += tags[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun labeledField(label: String, field: View, top: Int, height: Int = dp(52)): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(top), 0, 0)
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textSecondary)
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
            setTextColor(textPrimary)
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
                setTextColor(textSecondary)
            })
            addView(TextView(context).apply {
                text = value
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(blue)
                includeFontPadding = false
                setPadding(0, dp(8), 0, dp(6))
            })
            addView(TextView(context).apply {
                text = subtitle
                textSize = 13f
                setTextColor(textSecondary)
            })
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 8f, Color.rgb(226, 232, 240))
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply {
                bottomMargin = dp(12)
            }
        }
    }

    private fun backButton(): Button {
        return Button(this).apply {
            text = "返回录入"
            setTextColor(Color.WHITE)
            background = rounded(blue, 8f)
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

    companion object {
        private const val match = LinearLayout.LayoutParams.MATCH_PARENT
        private const val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        private const val KEY_AMOUNT = "amount"
        private const val KEY_DETAIL = "detail"
        private const val KEY_CUSTOM_TAG = "custom_tag"
        private const val KEY_SELECTED_TAGS = "selected_tags"
        private const val KEY_CUSTOM_TAGS = "custom_tags"
        private const val KEY_SCREEN = "screen"
        private const val AMOUNT_DECIMAL_PLACES = 2
        private val AMOUNT_INPUT_PATTERN = Regex("""\d*(\.\d{0,2})?""")
        private val MAX_AMOUNT_CENTS = BigDecimal.valueOf(Long.MAX_VALUE)
    }

    private enum class Screen {
        ENTRY,
        SETTINGS,
        ANALYSIS
    }
}
