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

val ShoukoAccent = Color(0xFFF48FB1)
private val ShoukoShapes = Shapes(medium = RoundedCornerShape(8.dp))

@Composable
fun ShoukoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColors(
            primary = ShoukoAccent,
            primaryVariant = ShoukoAccent,
            secondary = ShoukoAccent,
            secondaryVariant = ShoukoAccent
        )
    } else {
        lightColors(
            primary = ShoukoAccent,
            primaryVariant = ShoukoAccent,
            secondary = ShoukoAccent,
            secondaryVariant = ShoukoAccent
        )
    }
    MaterialTheme(colors = colors, shapes = ShoukoShapes, content = content)
}
