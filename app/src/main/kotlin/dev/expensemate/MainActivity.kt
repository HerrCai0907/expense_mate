package dev.expensemate

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import dev.expensemate.data.ExpenseDatabase
import dev.expensemate.data.SettingsStore
import dev.expensemate.data.TagStore
import dev.expensemate.state.AppState
import dev.expensemate.state.Screen
import dev.expensemate.ui.FloatingActions
import dev.expensemate.ui.ThemeController
import dev.expensemate.ui.UiComponents
import dev.expensemate.ui.analysis.AnalysisScreen
import dev.expensemate.ui.analysis.RecordsScreen
import dev.expensemate.ui.entry.EntryScreen
import dev.expensemate.ui.match
import dev.expensemate.ui.settings.SettingsScreen

class MainActivity : Activity() {
    private lateinit var database: ExpenseDatabase
    private lateinit var tags: TagStore
    private lateinit var settings: SettingsStore
    private lateinit var theme: ThemeController
    private lateinit var root: FrameLayout
    private lateinit var content: FrameLayout
    private lateinit var floatingActions: View
    private lateinit var ui: UiComponents
    private val state = AppState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = ExpenseDatabase(this)
        val preferences = getSharedPreferences("expense_mate", MODE_PRIVATE)
        tags = TagStore(preferences)
        settings = SettingsStore(preferences)
        state.restoreUiState(savedInstanceState, tags)
        theme = ThemeController(this)
        root = FrameLayout(this)
        content = FrameLayout(this)
        root.addView(content, FrameLayout.LayoutParams(match, match))
        refreshAppearance()
        setContentView(root)
        navigate(state.currentScreen)
    }

    private fun refreshAppearance() {
        val palette = theme.currentPalette(settings)
        theme.applySystemBars(palette)
        ui = UiComponents(this, palette, theme.statusBarHeight(), ::navigate)
        root.setBackgroundColor(palette.pageBackground)
        if (::floatingActions.isInitialized) root.removeView(floatingActions)
        floatingActions = FloatingActions(ui, ::navigate).create()
        root.addView(floatingActions)
    }

    private fun navigate(screen: Screen) {
        state.currentScreen = screen
        content.removeAllViews()
        val page = when (screen) {
            Screen.ENTRY -> EntryScreen(ui, state.entry, database, tags, settings).render()
            Screen.SETTINGS -> SettingsScreen(ui, settings, ::navigate) {
                refreshAppearance()
                navigate(Screen.SETTINGS)
            }.render()
            Screen.ANALYSIS -> AnalysisScreen(ui, state.analysis, database, ::navigate).render()
            Screen.RECORDS -> RecordsScreen(ui, state.analysis, database, ::navigate).render()
        }
        content.addView(page, FrameLayout.LayoutParams(match, match))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        state.save(outState, tags)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when (state.currentScreen) {
            Screen.RECORDS -> navigate(Screen.ANALYSIS)
            Screen.ENTRY -> super.onBackPressed()
            else -> navigate(Screen.ENTRY)
        }
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }
}
