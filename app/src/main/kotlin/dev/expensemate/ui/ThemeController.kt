package dev.expensemate.ui

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import dev.expensemate.data.SettingsStore
import dev.expensemate.model.AppearanceMode

class ThemeController(private val activity: Activity) {
    fun currentPalette(settings: SettingsStore): Palette {
        val useNight = when (settings.appearanceMode()) {
            AppearanceMode.DAY -> false
            AppearanceMode.NIGHT -> true
            AppearanceMode.SYSTEM -> (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        }
        return if (useNight) Palette.night() else Palette.day()
    }

    fun applySystemBars(palette: Palette) {
        activity.window.statusBarColor = palette.systemBar
        activity.window.navigationBarColor = palette.systemBar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            activity.window.decorView.systemUiVisibility = if (palette.lightStatusBar) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                0
            }
        }
    }

    fun statusBarHeight(): Int {
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) activity.resources.getDimensionPixelSize(resourceId) else 0
    }
}
