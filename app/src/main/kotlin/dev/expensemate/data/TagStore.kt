package dev.expensemate.data

import android.content.SharedPreferences
import dev.expensemate.domain.TagPriorityRanker
import org.json.JSONArray

class TagStore(private val preferences: SharedPreferences) {
    val customTags = linkedSetOf<String>()
    val tagLru = linkedSetOf<String>()
    private val ranker = TagPriorityRanker()

    init { restoreStoredTags() }

    fun rankedTags(): List<String> = ranker.rank(customTags, tagLru).map { it.tag }

    fun markTagsUsed(tags: Collection<String>) {
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .asReversed()
            .forEach { tag ->
                customTags += tag
                tagLru.remove(tag)
                tagLru.addFirstCompat(tag)
            }
        tagLru.retainAll(customTags)
        saveStoredTags()
    }

    private fun restoreStoredTags() {
        customTags.clear()
        customTags += preferences.getJsonStringArray(PREF_CUSTOM_TAGS)
        tagLru.clear()
        tagLru += preferences.getJsonStringArray(PREF_TAG_LRU).filter { it in customTags }
    }

    fun saveStoredTags() {
        preferences.edit()
            .putJsonStringArray(PREF_CUSTOM_TAGS, customTags)
            .putJsonStringArray(PREF_TAG_LRU, tagLru.filter { it in customTags })
            .apply()
    }

    private fun SharedPreferences.getJsonStringArray(key: String): List<String> {
        val raw = getString(key, null) ?: return emptyList()
        return runCatching {
            val values = JSONArray(raw)
            List(values.length()) { index -> values.optString(index).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }.getOrDefault(emptyList())
    }

    private fun SharedPreferences.Editor.putJsonStringArray(
        key: String,
        values: Collection<String>
    ): SharedPreferences.Editor {
        val json = JSONArray()
        values.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { json.put(it) }
        return putString(key, json.toString())
    }

    private fun <T> LinkedHashSet<T>.addFirstCompat(value: T) {
        val current = toList()
        clear()
        add(value)
        current.filterNot { it == value }.forEach { add(it) }
    }

    companion object {
        private const val PREF_CUSTOM_TAGS = "custom_tags"
        private const val PREF_TAG_LRU = "tag_lru"
    }
}
