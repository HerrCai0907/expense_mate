package dev.expensemate.ui.settings

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dev.expensemate.data.SettingsStore
import dev.expensemate.model.AppearanceMode
import dev.expensemate.state.Screen
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.match

class SettingsScreen(
    private val ui: UiComponents,
    private val settings: SettingsStore,
    private val navigate: (Screen) -> Unit,
    private val onAppearanceChanged: () -> Unit
) {
    private val context get() = ui.context

    private val presetDialogs = TimePresetDialogs(ui, settings) { navigate(Screen.SETTINGS) }

    fun render(): View {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(ui.dp(18), ui.topSystemInset + ui.dp(78), ui.dp(18), ui.dp(18))
        }
        panel.addView(ui.pageTitle("设置"))
        panel.addView(ui.card().apply {
            addView(TextView(context).apply {
                text = "外观"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ui.palette.textPrimary)
            })
            addView(TextView(context).apply {
                text = "日间 / 夜间 / 跟随系统"
                textSize = 14f
                setTextColor(ui.palette.textSecondary)
                setPadding(0, ui.dp(10), 0, 0)
            })
            addView(appearanceModeRow())
        })
        panel.addView(ui.card().apply {
            addView(TextView(context).apply {
                text = "时间预设"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ui.palette.textPrimary)
            })
            addView(TextView(context).apply {
                text = settings.timePresets.joinToString(" / ") { it.displayText() }
                textSize = 14f
                setTextColor(ui.palette.textSecondary)
                setPadding(0, ui.dp(10), 0, 0)
            })
            addView(Button(context).apply {
                text = "管理时间预设"
                setTextColor(ui.palette.secondaryButtonText)
                background = ui.rounded(ui.palette.secondaryButton, 8f, ui.palette.border)
                setOnClickListener { presetDialogs.showTimePresetManager() }
            }, LinearLayout.LayoutParams(match, ui.dp(48)).apply { topMargin = ui.dp(16) })
        })
        panel.addView(ui.backButton())
        return panel
    }

    private fun appearanceModeRow(): View {
        val currentMode = settings.appearanceMode()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ui.dp(16), 0, 0)
            AppearanceMode.values().forEachIndexed { index, mode ->
                addView(Button(context).apply {
                    text = mode.label
                    val selected = mode == currentMode
                    setTextColor(if (selected) ui.palette.primaryButtonText else ui.palette.secondaryButtonText)
                    background = ui.rounded(
                        if (selected) ui.palette.primaryButton else ui.palette.secondaryButton,
                        8f,
                        ui.palette.border
                    )
                    setOnClickListener {
                        settings.setAppearanceMode(mode)
                        onAppearanceChanged()
                    }
                }, LinearLayout.LayoutParams(0, ui.dp(48), 1f).apply {
                    if (index > 0) {
                        leftMargin = ui.dp(8)
                    }
                })
            }
        }
    }
}
