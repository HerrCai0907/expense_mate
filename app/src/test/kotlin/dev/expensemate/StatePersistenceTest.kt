package dev.expensemate

import android.content.Context
import android.os.Bundle
import dev.expensemate.data.ExpenseDatabase
import dev.expensemate.data.SettingsStore
import dev.expensemate.data.TagStore
import dev.expensemate.domain.AnalysisPeriod
import dev.expensemate.model.AppearanceMode
import dev.expensemate.model.TimePreset
import dev.expensemate.state.AppState
import dev.expensemate.state.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class StatePersistenceTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val preferences get() = context.getSharedPreferences("expense_mate", Context.MODE_PRIVATE)

    @Test fun bundleRoundTripKeepsDraftFiltersScreenAndTagOrder() {
        val tags = TagStore(preferences)
        tags.customTags += listOf("餐饮", "交通")
        tags.markTagsUsed(listOf("交通", "餐饮"))
        val original = AppState().apply {
            entry.draftAmount = "12.30"
            entry.draftDetail = "备注"
            entry.draftCustomTag = "未添加"
            entry.selectedTags += "交通"
            entry.selectedExpenseTimeMillis = 123456L
            analysis.analysisPeriod = AnalysisPeriod.DAYS_30
            analysis.analysisTag = "交通"
            analysis.recordSort = 3
            currentScreen = Screen.RECORDS
        }
        val bundle = Bundle()
        original.save(bundle, tags)
        val restoredTags = TagStore(preferences)
        val restored = AppState().apply { restoreUiState(bundle, restoredTags) }
        assertEquals("12.30", restored.entry.draftAmount)
        assertEquals("备注", restored.entry.draftDetail)
        assertEquals("未添加", restored.entry.draftCustomTag)
        assertEquals(setOf("交通"), restored.entry.selectedTags)
        assertEquals(123456L, restored.entry.selectedExpenseTimeMillis)
        assertEquals(AnalysisPeriod.DAYS_30, restored.analysis.analysisPeriod)
        assertEquals("交通", restored.analysis.analysisTag)
        assertEquals(3, restored.analysis.recordSort)
        assertEquals(Screen.RECORDS, restored.currentScreen)
        assertEquals(listOf("交通", "餐饮"), restoredTags.tagLru.toList())
        assertEquals(tags.customTags, restoredTags.customTags)
        // These are the keys written by the pre-refactor Activity.
        assertEquals("12.30", bundle.getString("amount"))
        assertEquals("RECORDS", bundle.getString("screen"))
    }

    @Test fun legacyBundleAndInvalidEnumsUseExistingFallbacks() {
        val bundle = Bundle().apply {
            putString("amount", "5.00")
            putString("screen", "removed_screen")
            putString("analysis_period", "removed_period")
            putInt("record_sort", 99)
        }
        val restored = AppState().apply { restoreUiState(bundle, TagStore(preferences)) }
        assertEquals("5.00", restored.entry.draftAmount)
        assertEquals(Screen.ENTRY, restored.currentScreen)
        assertEquals(AnalysisPeriod.MONTH, restored.analysis.analysisPeriod)
        assertEquals(3, restored.analysis.recordSort)
        assertNull(restored.entry.selectedExpenseTimeMillis)
    }

    @Test fun legacyPreferencesAndRecentTagsSurviveStoreRecreation() {
        preferences.edit()
            .putString("custom_tags", "[\" 餐饮 \",\"交通\",\"餐饮\",\"\"]")
            .putString("tag_lru", "[\"交通\",\"unknown\"]")
            .apply()
        val tags = TagStore(preferences)
        assertEquals(listOf("交通", "餐饮"), tags.rankedTags())
        tags.markTagsUsed(listOf("餐饮", "购物"))
        assertEquals(listOf("餐饮", "购物", "交通"), TagStore(preferences).rankedTags())
    }

    @Test fun presetsAndAppearanceKeepStoredFormatAndCorruptionFallback() {
        preferences.edit().putString("time_presets", "broken json").putString("appearance_mode", "unknown").apply()
        val settings = SettingsStore(preferences)
        assertEquals(5, settings.timePresets.size)
        assertEquals(AppearanceMode.SYSTEM, settings.appearanceMode())
        settings.timePresets.clear()
        settings.timePresets += TimePreset("早餐", 7, 30)
        settings.saveTimePresets()
        settings.setAppearanceMode(AppearanceMode.NIGHT)
        val restored = SettingsStore(preferences)
        assertEquals(listOf(TimePreset("早餐", 7, 30)), restored.timePresets)
        assertEquals(AppearanceMode.NIGHT, restored.appearanceMode())
    }

    @Test fun databaseCanReadLegacySchemaAndAppendWithoutMigration() {
        // Build the original schema independently of the refactored helper.
        context.openOrCreateDatabase("expense_mate.db", Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL("""
                CREATE TABLE expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    amount_cents INTEGER NOT NULL,
                    tags TEXT NOT NULL,
                    detail TEXT NOT NULL,
                    created_at_millis INTEGER NOT NULL
                )
            """.trimIndent())
            legacy.execSQL("CREATE INDEX idx_expenses_created_at ON expenses(created_at_millis)")
            legacy.execSQL("INSERT INTO expenses VALUES (1, 1234, '餐饮,午餐', '备注', 123456)")
            legacy.version = 1
        }
        val reopened = ExpenseDatabase(context)
        try {
            val records = reopened.expensesSince(0)
            assertEquals(1, records.size)
            assertEquals(1L, records.single().id)
            assertEquals(1234L, records.single().amountCents)
            assertEquals(listOf("餐饮", "午餐"), records.single().tags)
            assertEquals("备注", records.single().detail)
            assertEquals(123456L, records.single().createdAtMillis)
            reopened.insert(BigDecimal("5.00"), listOf("交通"), "", 123457)
            assertEquals(2, reopened.countSince(0))
            assertEquals(1734L, reopened.totalSince(0))
        } finally {
            reopened.close()
        }
    }
}
