package dev.expensemate.ui.settings

import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import dev.expensemate.data.SettingsStore
import dev.expensemate.domain.parsePresetTime
import dev.expensemate.model.TimePreset
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.match

class TimePresetDialogs(
    private val ui: UiComponents,
    private val settings: SettingsStore,
    private val onFinished: () -> Unit
) {
    private val context get() = ui.context

    fun showTimePresetManager() {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(20), ui.dp(8), ui.dp(20), 0)
        }
        fun renderPresets() {
            panel.removeAllViews()
            settings.timePresets.forEach { preset ->
                panel.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, ui.dp(8))
                    addView(TextView(context).apply {
                        text = preset.displayText()
                        textSize = 16f
                        setTextColor(ui.palette.textPrimary)
                    }, LinearLayout.LayoutParams(0, ui.dp(44), 1f))
                    addView(Button(context).apply {
                        text = "删除"
                        setTextColor(ui.palette.secondaryButtonText)
                        background = ui.rounded(ui.palette.secondaryButton, 8f, ui.palette.border)
                        setOnClickListener {
                            settings.timePresets.remove(preset)
                            settings.saveTimePresets()
                            renderPresets()
                        }
                    }, LinearLayout.LayoutParams(ui.dp(78), ui.dp(44)).apply { leftMargin = ui.dp(8) })
                })
            }
            panel.addView(Button(context).apply {
                text = "新增预设"
                setTextColor(ui.palette.primaryButtonText)
                background = ui.rounded(ui.palette.primaryButton, 8f)
                setOnClickListener { showAddTimePresetDialog { renderPresets() } }
            }, LinearLayout.LayoutParams(match, ui.dp(48)).apply { topMargin = ui.dp(8) })
        }
        renderPresets()
        AlertDialog.Builder(context, ui.dialogTheme())
            .setTitle("时间预设")
            .setView(panel)
            .setPositiveButton("完成") { _, _ -> onFinished() }
            .show()
    }

    private fun showAddTimePresetDialog(onPresetAdded: () -> Unit) {
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(20), ui.dp(8), ui.dp(20), 0)
        }
        val nameInput = EditText(context).apply {
            hint = "名称"
            setSingleLine(true)
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setPadding(ui.dp(14), 0, ui.dp(14), 0)
            setTextColor(ui.palette.textPrimary)
            setHintTextColor(ui.palette.textSecondary)
        }
        val timeInput = EditText(context).apply {
            hint = "时间，例如 08:30"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setPadding(ui.dp(14), 0, ui.dp(14), 0)
            setTextColor(ui.palette.textPrimary)
            setHintTextColor(ui.palette.textSecondary)
        }
        form.addView(nameInput, LinearLayout.LayoutParams(match, ui.dp(50)))
        form.addView(timeInput, LinearLayout.LayoutParams(match, ui.dp(50)).apply { topMargin = ui.dp(12) })

        AlertDialog.Builder(context, ui.dialogTheme())
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
                            label.isEmpty() -> ui.toast("请输入名称")
                            time == null -> ui.toast("请输入正确时间")
                            settings.timePresets.any { it.label == label } -> ui.toast("预设名称已存在")
                            else -> {
                                settings.timePresets += TimePreset(label, time.first, time.second)
                                settings.saveTimePresets()
                                dismiss()
                                onPresetAdded()
                            }
                        }
                    }
                }
            }
            .show()
    }
}
