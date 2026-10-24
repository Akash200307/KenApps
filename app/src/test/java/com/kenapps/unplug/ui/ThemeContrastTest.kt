package com.kenapps.unplug.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kenapps.unplug.data.Palette
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Float =
        (max(a.luminance(), b.luminance()) + .05f) / (min(a.luminance(), b.luminance()) + .05f)

    private fun verify(label: String, pair: (androidx.compose.material3.ColorScheme) -> Pair<Color, Color>) {
        Palette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val (foreground, background) = pair(kenAppsColorScheme(palette, dark))
                val ratio = contrast(foreground, background)
                assertTrue("$palette dark=$dark $label contrast=$ratio", ratio >= 4.5f)
            }
        }
    }

    @Test fun bodyTextMeetsContrast() = verify("body") { it.onSurface to it.surface }
    @Test fun secondaryTextMeetsContrast() = verify("secondary") { it.onSurfaceVariant to it.surface }
    @Test fun secondaryTextOnCardsMeetsContrast() = verify("secondary card") { it.onSurfaceVariant to it.surfaceContainer }
    @Test fun buttonLabelsMeetContrast() = verify("button") { it.onPrimary to it.primary }
    @Test fun statusLabelsMeetContrast() = verify("status") { it.onPrimaryContainer to it.primaryContainer }
}
