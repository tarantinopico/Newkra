package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Pure AMOLED Dark Mode Base
val AmoledBlack = Color(0xFF000000)
val CardSurface1 = Color(0xFF121214)
val CardSurface2 = Color(0xFF1C1C1E)
val CardSurface3 = Color(0xFF262629)
val CardBorder = Color(0x14FFFFFF)

// Typography colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E8E93)
val TextMuted = Color(0xFF636366)

// Sémantické barvy
val ColorSuccess = Color(0xFF30D158)
val ColorWarning = Color(0xFFFF9F0A)
val ColorDanger = Color(0xFFFF453A)
val ColorWater = Color(0xFF64D2FF)
val ColorWaterDark = Color(0xFF0A84FF)
val ColorNight = Color(0xFFBF5AF2)

// Theme Presets
val ThemeEmerald = Color(0xFF30D158)
val ThemeCyan = Color(0xFF64D2FF)
val ThemePurple = Color(0xFFBF5AF2)
val ThemeAmber = Color(0xFFFF9F0A)
val ThemeRose = Color(0xFFFF375F)
val ThemeGold = Color(0xFFFFD60A)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF30D158)): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
