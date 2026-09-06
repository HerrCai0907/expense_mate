package dev.expensemate.state

import android.os.Bundle
import dev.expensemate.data.TagStore
import dev.expensemate.domain.AnalysisPeriod

class AppState {
    val entry = EntryState()
    val analysis = AnalysisState()
    var currentScreen = Screen.ENTRY

    fun save(outState: Bundle, tags: TagStore) {
        outState.putString(KEY_AMOUNT, entry.draftAmount)
        outState.putString(KEY_DETAIL, entry.draftDetail)
        outState.putString(KEY_CUSTOM_TAG, entry.draftCustomTag)
        outState.putStringArrayList(KEY_SELECTED_TAGS, ArrayList(entry.selectedTags))
        outState.putStringArrayList(KEY_CUSTOM_TAGS, ArrayList(tags.customTags))
        outState.putStringArrayList(KEY_TAG_LRU, ArrayList(tags.tagLru))
        outState.putString(KEY_SCREEN, currentScreen.name)
        outState.putString("analysis_period", analysis.analysisPeriod.name)
        outState.putString("analysis_tag", analysis.analysisTag)
        outState.putInt("record_sort", analysis.recordSort)
        entry.selectedExpenseTimeMillis?.let { outState.putLong(KEY_EXPENSE_TIME, it) }
    }

    fun restoreUiState(savedInstanceState: Bundle?, tags: TagStore) {
        if (savedInstanceState == null) {
            return
        }
        analysis.analysisPeriod = runCatching { AnalysisPeriod.valueOf(savedInstanceState.getString("analysis_period").orEmpty()) }.getOrDefault(AnalysisPeriod.MONTH)
        analysis.analysisTag = savedInstanceState.getString("analysis_tag")
        analysis.recordSort = savedInstanceState.getInt("record_sort").coerceIn(0, 3)
        entry.draftAmount = savedInstanceState.getString(KEY_AMOUNT).orEmpty()
        entry.draftDetail = savedInstanceState.getString(KEY_DETAIL).orEmpty()
        entry.draftCustomTag = savedInstanceState.getString(KEY_CUSTOM_TAG).orEmpty()
        entry.selectedTags.clear()
        savedInstanceState.getStringArrayList(KEY_SELECTED_TAGS)?.let { entry.selectedTags += it }
        tags.customTags.clear()
        savedInstanceState.getStringArrayList(KEY_CUSTOM_TAGS)?.let { tags.customTags += it }
        tags.tagLru.clear()
        savedInstanceState.getStringArrayList(KEY_TAG_LRU)?.let { tags.tagLru += it }
        entry.selectedExpenseTimeMillis = if (savedInstanceState.containsKey(KEY_EXPENSE_TIME)) {
            savedInstanceState.getLong(KEY_EXPENSE_TIME)
        } else {
            null
        }
        currentScreen = savedInstanceState.getString(KEY_SCREEN)
            ?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?: Screen.ENTRY
    }

    companion object {
        private const val KEY_AMOUNT = "amount"
        private const val KEY_DETAIL = "detail"
        private const val KEY_CUSTOM_TAG = "custom_tag"
        private const val KEY_SELECTED_TAGS = "selected_tags"
        private const val KEY_CUSTOM_TAGS = "custom_tags"
        private const val KEY_TAG_LRU = "tag_lru"
        private const val KEY_SCREEN = "screen"
        private const val KEY_EXPENSE_TIME = "expense_time"
    }
}
