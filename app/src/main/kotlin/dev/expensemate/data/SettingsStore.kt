package dev.expensemate.data

import android.content.SharedPreferences
import dev.expensemate.model.AppearanceMode
import dev.expensemate.model.TimePreset
import org.json.JSONArray

class SettingsStore(private val preferences: SharedPreferences) {
    val timePresets = mutableListOf<TimePreset>()

    init { restoreTimePresets() }

    fun setAppearanceMode(mode: AppearanceMode) {
        preferences.edit().putString(PREF_APPEARANCE_MODE, mode.name).apply()
    }

    private fun restoreTimePresets() {
        timePresets.clear()
        val raw = preferences.getString(PREF_TIME_PRESETS, null)
        val storedPresets = raw?.let { parseTimePresets(it) }.orEmpty()
        timePresets += storedPresets.ifEmpty { defaultTimePresets() }
    }

    fun saveTimePresets() {
        val json = JSONArray()
        timePresets.forEach { preset ->
            json.put(org.json.JSONObject().apply {
                put("label", preset.label)
                put("hour", preset.hour)
                put("minute", preset.minute)
            })
        }
        preferences.edit().putString(PREF_TIME_PRESETS, json.toString()).apply()
    }

    private fun parseTimePresets(raw: String): List<TimePreset> {
        return runCatching {
            val values = JSONArray(raw)
            buildList {
                for (index in 0 until values.length()) {
                    val item = values.optJSONObject(index) ?: continue
                    val label = item.optString("label").trim()
                    val hour = item.optInt("hour", -1)
                    val minute = item.optInt("minute", -1)
                    if (label.isNotEmpty() && hour in 0..23 && minute in 0..59) {
                        add(TimePreset(label, hour, minute))
                    }
                }
            }.distinctBy { it.label }
        }.getOrDefault(emptyList())
    }

    fun appearanceMode(): AppearanceMode {
        val stored = preferences.getString(PREF_APPEARANCE_MODE, AppearanceMode.SYSTEM.name)
        return stored?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM
    }

    companion object {
        private const val PREF_TIME_PRESETS = "time_presets"
        private const val PREF_APPEARANCE_MODE = "appearance_mode"

        private fun defaultTimePresets() = listOf(
            TimePreset("早上", 8, 0),
            TimePreset("中午", 12, 0),
            TimePreset("下午", 15, 0),
            TimePreset("傍晚", 18, 0),
            TimePreset("夜间", 21, 0)
        )
    }
}
