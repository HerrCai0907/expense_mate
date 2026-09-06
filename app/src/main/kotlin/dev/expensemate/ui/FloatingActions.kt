package dev.expensemate.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import dev.expensemate.R
import dev.expensemate.state.Screen

class FloatingActions(
    private val ui: UiComponents,
    private val navigate: (Screen) -> Unit
) {
    private val context get() = ui.context

    fun create(): View {
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.END).apply {
                topMargin = ui.topSystemInset + ui.dp(16)
                rightMargin = ui.dp(16)
            }
        }
        actions.addView(iconButton(R.drawable.ic_settings, "设置") { navigate(Screen.SETTINGS) })
        actions.addView(iconButton(R.drawable.ic_analytics, "分析") { navigate(Screen.ANALYSIS) })
        return actions
    }

    private fun iconButton(icon: Int, label: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            contentDescription = label
            setImageResource(icon)
            setColorFilter(ui.palette.secondaryButtonText)
            background = ui.rounded(ui.palette.secondaryButton, 12f)
            setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)).apply {
                leftMargin = ui.dp(8)
            }
        }
    }
}
