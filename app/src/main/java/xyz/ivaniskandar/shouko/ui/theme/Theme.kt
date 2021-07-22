package xyz.ivaniskandar.shouko.ui.theme

import android.app.WallpaperColors
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import xyz.ivaniskandar.shouko.ShoukoApplication.Companion.wallpaperColors

private val ShoukoOriginalAccent = Color(0xFFF48FB1)

var ShoukoAccent = ShoukoOriginalAccent
    private set

private val ShoukoShapes = Shapes(medium = RoundedCornerShape(8.dp))

@Composable
fun ShoukoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    ShoukoAccent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getMonetAccent(LocalContext.current, darkTheme)
    } else {
        getAccent(wallpaperColors, darkTheme)
    }
    val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getMonetBackground(LocalContext.current, darkTheme)
    } else {
        getAccentedBackground(ShoukoAccent, darkTheme)
    }
    val onBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            Color(LocalContext.current.getColor(android.R.color.system_neutral1_50))
        } else {
            Color(LocalContext.current.getColor(android.R.color.system_neutral1_900))
        }
    } else {
        if (darkTheme) {
            Color.White
        } else {
            Color.Black
        }
    }
    val colors = if (darkTheme) {
        darkColors(
            primary = ShoukoAccent,
            primaryVariant = ShoukoAccent,
            secondary = ShoukoAccent,
            secondaryVariant = ShoukoAccent,
            background = background,
            surface = background,
            onBackground = onBackground,
            onSurface = onBackground
        )
    } else {
        lightColors(
            primary = ShoukoAccent,
            primaryVariant = ShoukoAccent,
            secondary = ShoukoAccent,
            secondaryVariant = ShoukoAccent,
            background = background,
            surface = background,
            onBackground = onBackground,
            onSurface = onBackground
        )
    }
    MaterialTheme(colors = colors, shapes = ShoukoShapes, content = content)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun getMonetAccent(context: Context, darkTheme: Boolean): Color {
    val colorInt = if (darkTheme) {
        context.getColor(android.R.color.system_accent1_200)
    } else {
        context.getColor(android.R.color.system_accent1_600)
    }
    return Color(colorInt)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun getMonetBackground(context: Context, darkTheme: Boolean): Color {
    val colorInt = if (darkTheme) {
        context.getColor(android.R.color.system_neutral1_900)
    } else {
        context.getColor(android.R.color.system_neutral1_50)
    }
    return Color(colorInt)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun getMonetSurface(context: Context, darkTheme: Boolean): Color {
    val colorInt = if (darkTheme) {
        context.getColor(android.R.color.system_neutral1_800)
    } else {
        ColorStateList.valueOf(context.getColor(android.R.color.system_neutral1_500))
            .withLStar(98F)
            .defaultColor
    }
    return Color(colorInt)
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
