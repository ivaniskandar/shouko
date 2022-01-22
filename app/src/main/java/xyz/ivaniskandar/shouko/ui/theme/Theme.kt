package xyz.ivaniskandar.shouko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kieronquinn.monetcompat.extensions.toArgb
import xyz.ivaniskandar.shouko.ShoukoApplication
import dev.kdrag0n.monet.colors.Color as KDrag0nColor

private val ShoukoOriginalAccent = Color(0xFFF48FB1)

var ShoukoAccent = ShoukoOriginalAccent
    private set

private val ShoukoShapes = Shapes(medium = RoundedCornerShape(8.dp))

@Composable
fun ShoukoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    ShoukoAccent = getMonetAccent(darkTheme)
    val background = getMonetBackground(darkTheme)
    val onBackground = getMonetOnBackground(darkTheme)
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

private fun getMonetAccent(darkTheme: Boolean): Color {
    val monetColors = ShoukoApplication.monetColors
    var color = if (darkTheme) {
        monetColors?.accent1?.get(200)
    } else {
        monetColors?.accent1?.get(600)
    }?.toComposeColor()

    // Default color when monet color is not ready
    if (color == null) {
        color = ShoukoOriginalAccent
    }
    return color
}

private fun getMonetBackground(darkTheme: Boolean): Color {
    val monetColors = ShoukoApplication.monetColors
    var color = if (darkTheme) {
        monetColors?.neutral1?.get(900)
    } else {
        monetColors?.neutral1?.get(50)
    }?.toComposeColor()

    // Default color when monet color is not ready
    if (color == null) {
        color = if (darkTheme) {
            Color(0xFF121212)
        } else {
            Color.White
        }
    }
    return color
}

private fun getMonetOnBackground(darkTheme: Boolean): Color {
    val monetColors = ShoukoApplication.monetColors
    var color = if (darkTheme) {
        monetColors?.neutral1?.get(50)
    } else {
        monetColors?.neutral1?.get(900)
    }?.toComposeColor()

    // Default color when monet color is not ready
    if (color == null) {
        color = if (darkTheme) {
            Color.White
        } else {
            Color.Black
        }
    }
    return color
}

private fun KDrag0nColor?.toComposeColor(): Color? {
    return this?.toArgb()?.let { Color(it) }
}
