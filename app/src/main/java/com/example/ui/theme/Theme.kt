package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun KratomTrackerTheme(
    themeName: String = "emerald",
    customAccentHex: String = "#30D158",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val accentColor = when (themeName) {
        "dynamic" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).primary
            } else {
                ThemeEmerald
            }
        }
        "cyan" -> ThemeCyan
        "purple" -> ThemePurple
        "amber" -> ThemeAmber
        "rose" -> ThemeRose
        "gold" -> ThemeGold
        "custom" -> parseHexColor(customAccentHex, ThemeEmerald)
        else -> ThemeEmerald // "emerald" default
    }

    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color.Black,
        primaryContainer = accentColor.copy(alpha = 0.2f),
        onPrimaryContainer = accentColor,
        secondary = ColorWater,
        onSecondary = Color.Black,
        secondaryContainer = ColorWater.copy(alpha = 0.2f),
        onSecondaryContainer = ColorWater,
        tertiary = ColorNight,
        onTertiary = Color.White,
        background = AmoledBlack,
        onBackground = TextPrimary,
        surface = CardSurface1,
        onSurface = TextPrimary,
        surfaceVariant = CardSurface2,
        onSurfaceVariant = TextSecondary,
        surfaceContainer = CardSurface2,
        surfaceContainerHigh = CardSurface3,
        outline = CardBorder,
        outlineVariant = Color(0x24FFFFFF),
        error = ColorDanger,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
