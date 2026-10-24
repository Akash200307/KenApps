package com.kenapps.unplug.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kenapps.unplug.data.AppSettings
import com.kenapps.unplug.data.Palette
import com.kenapps.unplug.data.ThemeMode

val Palette.label: String get() = when (this) {
    Palette.MIST -> "Mist"
    Palette.LAVENDER -> "Lavender"
    Palette.SAND -> "Sand"
}
val Palette.description: String get() = when (this) {
    Palette.MIST -> "Fresh teal. A softer kind of focus."
    Palette.LAVENDER -> "Quiet plum. A little room to unwind."
    Palette.SAND -> "Warm bronze. A slower rhythm."
}
val Palette.accent: Color get() = when (this) {
    Palette.MIST -> Color(0xFF176B57)
    Palette.LAVENDER -> Color(0xFF68518C)
    Palette.SAND -> Color(0xFF825724)
}
val ThemeMode.label: String get() = when (this) {
    ThemeMode.SYSTEM -> "Follow system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
fun KenAppsTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = kenAppsColorScheme(settings.palette, dark), content = content)
}

internal fun kenAppsColorScheme(palette: Palette, dark: Boolean): ColorScheme {
    val (background, surface, container, primary, foreground) = when (palette) {
        Palette.MIST -> if (dark) listOf(0xFF111A18, 0xFF192420, 0xFF24372F, 0xFF8AD5C1, 0xFFE0EAE4)
            else listOf(0xFFF5F6F0, 0xFFFCFDF8, 0xFFE4EEE6, 0xFF176B57, 0xFF172E26)
        Palette.LAVENDER -> if (dark) listOf(0xFF1A1620, 0xFF231D2C, 0xFF352940, 0xFFD4B8F3, 0xFFEDE2F4)
            else listOf(0xFFF8F5FA, 0xFFFFFBFF, 0xFFEFE5F6, 0xFF68518C, 0xFF30263A)
        Palette.SAND -> if (dark) listOf(0xFF201A13, 0xFF2A2319, 0xFF3D3022, 0xFFE9BF86, 0xFFF3E6D3)
            else listOf(0xFFFAF6EE, 0xFFFFFCF5, 0xFFF1E6D5, 0xFF825724, 0xFF372A1B)
    }.map { Color(it) }
    return if (dark) darkColorScheme(
        primary = primary, onPrimary = background, primaryContainer = container,
        onPrimaryContainer = foreground, secondary = primary, onSecondary = background,
        secondaryContainer = container, onSecondaryContainer = foreground,
        background = background, onBackground = foreground,
        surface = surface, onSurface = foreground, surfaceVariant = container,
        onSurfaceVariant = Color(0xFFC1C5C0), surfaceContainer = container,
        outline = Color(0xFF8E968F), outlineVariant = Color(0xFF4B554D),
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White, primaryContainer = container,
        onPrimaryContainer = foreground, secondary = primary, onSecondary = Color.White,
        secondaryContainer = container, onSecondaryContainer = foreground,
        background = background, onBackground = foreground,
        surface = surface, onSurface = foreground, surfaceVariant = container,
        onSurfaceVariant = Color(0xFF4F5B53), surfaceContainer = container,
        outline = Color(0xFF68766C), outlineVariant = Color(0xFFB9C7BC),
    )
}
