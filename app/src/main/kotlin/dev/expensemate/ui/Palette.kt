package dev.expensemate.ui

import android.graphics.Color

data class Palette(
    val pageBackground: Int,
    val inputSurface: Int,
    val cardSurface: Int,
    val border: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val primaryButton: Int,
    val primaryButtonText: Int,
    val secondaryButton: Int,
    val secondaryButtonText: Int,
    val systemBar: Int,
    val lightStatusBar: Boolean,
    val isNight: Boolean
) {
    companion object {
        fun day() = Palette(
            pageBackground = Color.rgb(245, 245, 245),
            inputSurface = Color.WHITE,
            cardSurface = Color.WHITE,
            border = Color.rgb(210, 210, 210),
            textPrimary = Color.rgb(18, 18, 18),
            textSecondary = Color.rgb(96, 96, 96),
            primaryButton = Color.rgb(18, 18, 18),
            primaryButtonText = Color.WHITE,
            secondaryButton = Color.rgb(224, 224, 224),
            secondaryButtonText = Color.rgb(24, 24, 24),
            systemBar = Color.rgb(245, 245, 245),
            lightStatusBar = true,
            isNight = false
        )

        fun night() = Palette(
            pageBackground = Color.rgb(10, 10, 10),
            inputSurface = Color.rgb(31, 31, 31),
            cardSurface = Color.rgb(24, 24, 24),
            border = Color.rgb(78, 78, 78),
            textPrimary = Color.rgb(245, 245, 245),
            textSecondary = Color.rgb(170, 170, 170),
            primaryButton = Color.WHITE,
            primaryButtonText = Color.rgb(12, 12, 12),
            secondaryButton = Color.rgb(47, 47, 47),
            secondaryButtonText = Color.rgb(238, 238, 238),
            systemBar = Color.rgb(0, 0, 0),
            lightStatusBar = false,
            isNight = true
        )
    }
}
