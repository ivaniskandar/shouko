package xyz.ivaniskandar.shouko.ui.theme

import android.app.WallpaperColors
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import xyz.ivaniskandar.shouko.ShoukoApplication.Companion.wallpaperColors

private val ShoukoOriginalAccent = Color(0xFFF48FB1)

var ShoukoAccent = ShoukoOriginalAccent
    private set

private val ShoukoShapes = Shapes(medium = RoundedCornerShape(8.dp))

@Composable
fun ShoukoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    ShoukoAccent = getAccent(wallpaperColors, darkTheme)
    val bg = getAccentedBackground(ShoukoAccent, darkTheme)
    val colors = if (darkTheme) {
        darkColors(
            primary = ShoukoAccent,
            primaryVariant = ShoukoAccent,
            secondary = ShoukoAccent,
            secondaryVariant = ShoukoAccent,
            background = bg,
            surface = bg
        )
    } else {
        lightColors(
            primary = ShoukoAccent,
            primaryVariant = ShoukoAccent,
            secondary = ShoukoAccent,
            secondaryVariant = ShoukoAccent,
            background = bg,
            surface = bg
        )
    }
    MaterialTheme(colors = colors, shapes = ShoukoShapes, content = content)
}

/**
 * Creates accent color from WallpaperColors.
 * Will automatically adjust the color if it's not contrast
 * enough for the background color.
 *
 * @param darkTheme true if getting color for dark theme
 * @return accent selected from WallpaperColors, [ShoukoOriginalAccent] if WallpaperColors is not available
 */
private fun getAccent(wallpaperColors: WallpaperColors?, darkTheme: Boolean): Color {
    val tempAccent = wallpaperColors?.primaryColor?.toArgb() ?: return ShoukoOriginalAccent

    // Make sure it's contrast enough
    val backgroundColor = (if (darkTheme) Color(0xFF121212) else Color.White).toArgb()
    var contrast = ColorUtils.calculateContrast(tempAccent, backgroundColor)
    val compositeColor = (if (darkTheme) Color.White else Color.Black).toArgb()
    var compositeAlphaPercentage = 1
    var compositedAccent = tempAccent
    while (contrast < 4.5 && compositeAlphaPercentage <= 100) {
        compositedAccent = ColorUtils.compositeColors(
            ColorUtils.setAlphaComponent(compositeColor, compositeAlphaPercentage * 255 / 100),
            tempAccent
        )
        compositeAlphaPercentage += 1
        contrast = ColorUtils.calculateContrast(compositedAccent, backgroundColor)
    }

    return Color(compositedAccent)
}

/**
 * Creates accented background/surface color
 *
 * @param accent the accent color to use
 * @param darkTheme true if getting color for dark theme
 */
private fun getAccentedBackground(accent: Color, darkTheme: Boolean): Color {
    val base = (if (darkTheme) Color(0xFF121212) else Color.White).toArgb()
    val accentAlpha = if (darkTheme) 0x14 else 0x33 // 8% dark, 20% light
    val color = ColorUtils.compositeColors(
        ColorUtils.setAlphaComponent(accent.toArgb(), accentAlpha),
        base
    )
    return Color(color)
}
