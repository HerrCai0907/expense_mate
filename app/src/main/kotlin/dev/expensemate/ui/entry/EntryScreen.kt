package dev.expensemate.ui.entry

import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import dev.expensemate.data.ExpenseDatabase
import dev.expensemate.data.SettingsStore
import dev.expensemate.data.TagStore
import dev.expensemate.domain.Amounts
import dev.expensemate.domain.formatExpenseTime
import dev.expensemate.state.EntryState
import dev.expensemate.ui.AmountInputFilter
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.match

class EntryScreen(
    private val ui: UiComponents,
    private val state: EntryState,
    private val database: ExpenseDatabase,
    private val tags: TagStore,
    settings: SettingsStore
) {
    private val context get() = ui.context

    private val dialogs = EntryDialogs(ui, state, settings)
    private val tagDropdown = TagDropdown(ui, state, tags)

    fun render(): View {
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(18), ui.topSystemInset + ui.dp(78), ui.dp(18), ui.dp(18))
        }

        val amountInput = EditText(context).apply {
            hint = "金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            imeOptions = EditorInfo.IME_ACTION_NEXT
            filters = arrayOf(AmountInputFilter())
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setPadding(ui.dp(14), 0, ui.dp(14), 0)
            textSize = 20f
            setTextColor(ui.palette.textPrimary)
            setHintTextColor(ui.palette.textSecondary)
        }
        amountInput.setText(state.draftAmount)
        with(ui) { amountInput.afterTextChanged { state.draftAmount = it } }
        form.addView(ui.labeledField("金额", amountInput, top = 0))

        form.addView(TextView(context).apply {
            text = "标签"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ui.palette.textSecondary)
            setPadding(0, ui.dp(12), 0, ui.dp(8))
        })

        val tagSpinner = Spinner(context).apply {
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setPadding(ui.dp(10), 0, ui.dp(10), 0)
        }
        form.addView(tagSpinner, LinearLayout.LayoutParams(match, ui.dp(52)))
        tagDropdown.render(tagSpinner)

        val customRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ui.dp(8), 0, 0)
        }
        val customTagInput = EditText(context).apply {
            hint = "手动输入标签"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setPadding(ui.dp(14), 0, ui.dp(14), 0)
            setTextColor(ui.palette.textPrimary)
            setHintTextColor(ui.palette.textSecondary)
        }
        customTagInput.setText(state.draftCustomTag)
        with(ui) { customTagInput.afterTextChanged { state.draftCustomTag = it } }
        fun addCustomTag(showEmptyMessage: Boolean = true): Boolean {
            val tag = customTagInput.text.toString().trim()
            if (tag.isEmpty()) {
                if (showEmptyMessage) {
                    ui.toast("请输入标签")
                }
                return false
            }
            tags.customTags += tag
            tags.saveStoredTags()
            state.selectedTags.clear()
            state.selectedTags += tag
            customTagInput.text.clear()
            tagDropdown.render(tagSpinner)
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
        customRow.addView(customTagInput, LinearLayout.LayoutParams(0, ui.dp(50), 1f))
        customRow.addView(Button(context).apply {
            text = "添加"
            setTextColor(ui.palette.secondaryButtonText)
            background = ui.rounded(ui.palette.secondaryButton, 8f)
            setOnClickListener { addCustomTag() }
        }, LinearLayout.LayoutParams(ui.dp(86), ui.dp(50)).apply { leftMargin = ui.dp(10) })
        form.addView(customRow)

        form.addView(TextView(context).apply {
            text = "记账时间"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ui.palette.textSecondary)
            setPadding(0, ui.dp(12), 0, ui.dp(8))
        })
        val expenseTimeLabel = TextView(context).apply {
            textSize = 15f
            setTextColor(ui.palette.textSecondary)
            gravity = Gravity.CENTER_VERTICAL
        }
        fun refreshExpenseTimeLabel() {
            expenseTimeLabel.text = state.selectedExpenseTimeMillis?.let { formatExpenseTime(it) } ?: "未选择，保存时使用当前时间"
        }
        refreshExpenseTimeLabel()
        val expenseTimeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        expenseTimeRow.addView(expenseTimeLabel, LinearLayout.LayoutParams(0, ui.dp(46), 1f))
        expenseTimeRow.addView(Button(context).apply {
            text = "选择"
            setTextColor(ui.palette.secondaryButtonText)
            background = ui.rounded(ui.palette.secondaryButton, 8f)
            setOnClickListener {
                dialogs.showExpenseTimePicker {
                    refreshExpenseTimeLabel()
                }
            }
        }, LinearLayout.LayoutParams(ui.dp(78), ui.dp(46)).apply { leftMargin = ui.dp(8) })
        expenseTimeRow.addView(Button(context).apply {
            text = "清除"
            setTextColor(ui.palette.secondaryButtonText)
            background = ui.rounded(ui.palette.secondaryButton, 8f)
            setOnClickListener {
                state.selectedExpenseTimeMillis = null
                refreshExpenseTimeLabel()
            }
        }, LinearLayout.LayoutParams(ui.dp(78), ui.dp(46)).apply { leftMargin = ui.dp(8) })
        form.addView(expenseTimeRow)

        form.addView(View(context), LinearLayout.LayoutParams(match, 0, 1f))

        val detailButton = Button(context).apply {
            text = detailButtonText()
            setTextColor(ui.palette.secondaryButtonText)
            background = ui.rounded(ui.palette.secondaryButton, 8f)
            setOnClickListener {
                dialogs.showDetailDialog {
                    text = detailButtonText()
                }
            }
        }
        form.addView(detailButton, LinearLayout.LayoutParams(match, ui.dp(48)).apply { topMargin = ui.dp(12) })

        form.addView(Button(context).apply {
            text = "保存账单"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ui.palette.primaryButtonText)
            background = ui.rounded(ui.palette.primaryButton, 8f)
            setOnClickListener {
                val amount = Amounts.parseAmount(amountInput.text.toString())
                if (amount == null) {
                    ui.toast("请输入有效金额")
                    return@setOnClickListener
                }
                if (!Amounts.canStoreAmount(amount)) {
                    ui.toast("金额过大，无法保存")
                    return@setOnClickListener
                }
                addCustomTag(showEmptyMessage = false)
                if (state.selectedTags.isEmpty()) {
                    ui.toast("请选择或添加至少一个标签")
                    return@setOnClickListener
                }

                database.insert(
                    amount,
                    state.selectedTags.toList(),
                    state.draftDetail.trim(),
                    state.selectedExpenseTimeMillis ?: System.currentTimeMillis()
                )
                tags.markTagsUsed(state.selectedTags)
                state.draftAmount = ""
                state.draftDetail = ""
                state.draftCustomTag = ""
                state.selectedExpenseTimeMillis = null
                amountInput.text.clear()
                state.selectedTags.clear()
                tagDropdown.render(tagSpinner)
                refreshExpenseTimeLabel()
                detailButton.text = detailButtonText()
                ui.toast("已保存")
            }
        }, LinearLayout.LayoutParams(match, ui.dp(52)).apply { topMargin = ui.dp(14) })

        return form
    }

    private fun detailButtonText(): String {
        return if (state.draftDetail.isBlank()) "详细" else "详细（已填写）"
    }
}
