package dev.expensemate.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.expensemate.R
import dev.expensemate.state.Screen

class UiComponents(
    val context: Context,
    val palette: Palette,
    val topSystemInset: Int,
    private val navigate: (Screen) -> Unit
) {
    fun labeledField(label: String, field: View, top: Int, height: Int = dp(52)): View {
        return LinearLayout(context).apply {
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

    fun pageTitle(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
            includeFontPadding = false
            setPadding(0, 0, 0, dp(18))
        }
    }

    fun metricCard(title: String, value: String, subtitle: String): View {
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

    fun card(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(palette.cardSurface, 8f, palette.border)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply {
                bottomMargin = dp(12)
            }
        }
    }

    fun backButton(): Button {
        return Button(context).apply {
            text = "返回录入"
            setTextColor(palette.secondaryButtonText)
            background = rounded(palette.secondaryButton, 8f)
            setOnClickListener { navigate(Screen.ENTRY) }
            layoutParams = LinearLayout.LayoutParams(match, dp(52)).apply {
                topMargin = dp(10)
            }
        }
    }

    fun dialogTheme(): Int {
        return if (palette.isNight) R.style.AppDialogDark else R.style.AppDialogLight
    }

    fun rounded(color: Int, radiusDp: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            strokeColor?.let { setStroke(dp(1), it) }
        }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun EditText.afterTextChanged(onChanged: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString().orEmpty())
            }
        })
    }

    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    fun dp(value: Float): Int = (value * context.resources.displayMetrics.density).toInt()
    fun scroll(panel: View): ScrollView = ScrollView(context).apply {
        isFillViewport = true
        clipToPadding = false
        setPadding(0, 0, 0, dp(24))
        addView(panel)
    }
}

internal const val match = ViewGroup.LayoutParams.MATCH_PARENT
internal const val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
