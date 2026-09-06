package dev.expensemate.ui.entry

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import dev.expensemate.data.SettingsStore
import dev.expensemate.state.EntryState
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.match
import java.util.Calendar

class EntryDialogs(
    private val ui: UiComponents,
    private val state: EntryState,
    private val settings: SettingsStore
) {
    private val context get() = ui.context

    fun showExpenseTimePicker(onTimeSelected: () -> Unit) {
        val initial = Calendar.getInstance().apply {
            state.selectedExpenseTimeMillis?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            ui.dialogTheme(),
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
        val options = settings.timePresets.map { it.displayText() } + "自定义时间"
        AlertDialog.Builder(context, ui.dialogTheme())
            .setTitle("选择时间")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < settings.timePresets.size) {
                    val preset = settings.timePresets[which]
                    selectedDate.set(Calendar.HOUR_OF_DAY, preset.hour)
                    selectedDate.set(Calendar.MINUTE, preset.minute)
                    selectedDate.set(Calendar.SECOND, 0)
                    selectedDate.set(Calendar.MILLISECOND, 0)
                    state.selectedExpenseTimeMillis = selectedDate.timeInMillis
                    onTimeSelected()
                } else {
                    showCustomTimePicker(selectedDate, onTimeSelected)
                }
            }
            .show()
    }

    private fun showCustomTimePicker(selectedDate: Calendar, onTimeSelected: () -> Unit) {
        TimePickerDialog(
            context,
            ui.dialogTheme(),
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                selectedDate.set(Calendar.SECOND, 0)
                selectedDate.set(Calendar.MILLISECOND, 0)
                state.selectedExpenseTimeMillis = selectedDate.timeInMillis
                onTimeSelected()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun showDetailDialog(onDetailChanged: () -> Unit) {
        val input = EditText(context).apply {
            setText(state.draftDetail)
            hint = "详细"
            gravity = Gravity.TOP
            minLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            background = ui.rounded(ui.palette.inputSurface, 8f, ui.palette.border)
            setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
            setTextColor(ui.palette.textPrimary)
            setHintTextColor(ui.palette.textSecondary)
        }
        val container = FrameLayout(context).apply {
            setPadding(ui.dp(20), ui.dp(8), ui.dp(20), 0)
            addView(input, FrameLayout.LayoutParams(match, ui.dp(160)))
        }
        AlertDialog.Builder(context, ui.dialogTheme())
            .setTitle("详细")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                state.draftDetail = input.text.toString()
                onDetailChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
