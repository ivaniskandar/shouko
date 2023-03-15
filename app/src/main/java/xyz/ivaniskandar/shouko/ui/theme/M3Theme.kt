package xyz.ivaniskandar.shouko.ui.theme

import androidx.annotation.IntRange
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.toArgb
import xyz.ivaniskandar.shouko.R
import kotlin.math.ln

private fun MonetCompat.getMonetNeutralColor(
    @IntRange(from = 1, to = 2) type: Int,
    @IntRange(from = 50, to = 900) shade: Int,
): Color {
    val monetColor = when (type) {
        1 -> this.getMonetColors().neutral1[shade]
        else -> this.getMonetColors().neutral2[shade]
    }?.toArgb() ?: throw Exception("Neutral$type shade $shade doesn't exist")
    return Color(monetColor)
}

private fun MonetCompat.getMonetAccentColor(
    @IntRange(from = 1, to = 3) type: Int,
    @IntRange(from = 50, to = 900) shade: Int,
): Color {
    val monetColor = when (type) {
        1 -> this.getMonetColors().accent1[shade]
        2 -> this.getMonetColors().accent2[shade]
        else -> this.getMonetColors().accent3[shade]
    }?.toArgb() ?: throw Exception("Accent$type shade $shade doesn't exist")
    return Color(monetColor)
}

/**
 * Any values that are not set will be chosen to best represent default values given by [dynamicLightColorScheme][androidx.compose.material3.dynamicLightColorScheme]
 * on Android 12+ devices
 */
@Composable
@ReadOnlyComposable
fun MonetCompat.lightMonetCompatScheme(
    primary: Color = getMonetAccentColor(1, 700),
    onPrimary: Color = getMonetNeutralColor(1, 50),
    primaryContainer: Color = getMonetAccentColor(2, 100),
    onPrimaryContainer: Color = getMonetAccentColor(1, 900),
    inversePrimary: Color = getMonetAccentColor(1, 200),
    secondary: Color = getMonetAccentColor(2, 700),
    onSecondary: Color = getMonetNeutralColor(1, 50),
    secondaryContainer: Color = getMonetAccentColor(2, 100),
    onSecondaryContainer: Color = getMonetAccentColor(2, 900),
    tertiary: Color = getMonetAccentColor(3, 600),
    onTertiary: Color = getMonetNeutralColor(1, 50),
    tertiaryContainer: Color = getMonetAccentColor(3, 100),
    onTertiaryContainer: Color = getMonetAccentColor(3, 900),
    background: Color = getMonetNeutralColor(1, 50),
    onBackground: Color = getMonetNeutralColor(1, 900),
    surface: Color = getMonetNeutralColor(1, 50),
    onSurface: Color = getMonetNeutralColor(1, 900),
    surfaceVariant: Color = getMonetNeutralColor(2, 100),
    onSurfaceVariant: Color = getMonetNeutralColor(2, 700),
    inverseSurface: Color = getMonetNeutralColor(1, 800),
    inverseOnSurface: Color = getMonetNeutralColor(2, 50),
    outline: Color = getMonetAccentColor(2, 500),
): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        outline = outline,
    )

/**
 * Any values that are not set will be chosen to best represent default values given by [dynamicDarkColorScheme][androidx.compose.material3.dynamicDarkColorScheme]
 * on Android 12+ devices
 */
@Composable
@ReadOnlyComposable
fun MonetCompat.darkMonetCompatScheme(
    primary: Color = getMonetAccentColor(1, 200),
    onPrimary: Color = getMonetAccentColor(1, 800),
    primaryContainer: Color = getMonetAccentColor(1, 600),
    onPrimaryContainer: Color = getMonetAccentColor(2, 100),
    inversePrimary: Color = getMonetAccentColor(1, 600),
    secondary: Color = getMonetAccentColor(2, 200),
    onSecondary: Color = getMonetAccentColor(2, 800),
    secondaryContainer: Color = getMonetAccentColor(2, 700),
    onSecondaryContainer: Color = getMonetAccentColor(2, 100),
    tertiary: Color = getMonetAccentColor(3, 200),
    onTertiary: Color = getMonetAccentColor(3, 700),
    tertiaryContainer: Color = getMonetAccentColor(3, 700),
    onTertiaryContainer: Color = getMonetAccentColor(3, 100),
    background: Color = getMonetNeutralColor(1, 900),
    onBackground: Color = getMonetNeutralColor(1, 100),
    surface: Color = getMonetNeutralColor(1, 900),
    onSurface: Color = getMonetNeutralColor(1, 100),
    surfaceVariant: Color = getMonetNeutralColor(2, 700),
    onSurfaceVariant: Color = getMonetNeutralColor(2, 200),
    inverseSurface: Color = getMonetNeutralColor(1, 100),
    inverseOnSurface: Color = getMonetNeutralColor(1, 800),
    outline: Color = getMonetNeutralColor(2, 500),
): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        outline = outline,
    )

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val GoogleSansFont = GoogleFont(name = "Google Sans")

val GoogleSansFontFamily = FontFamily(
    Font(googleFont = GoogleSansFont, fontProvider = googleFontProvider),
    Font(googleFont = GoogleSansFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = GoogleSansFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleSansFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleSansFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

val ShoukoTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.0.sp,
        letterSpacing = 0.0.sp,
    ),
)

@Composable
fun ShoukoM3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val monet = remember(LocalConfiguration.current) { MonetCompat.getInstance() }
    val colorScheme = if (darkTheme) monet.darkMonetCompatScheme() else monet.lightMonetCompatScheme()

    val googleSansTypography = MaterialTheme.typography.copy(
        headlineSmall = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.2.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
    )

    MaterialTheme(colorScheme = colorScheme, typography = googleSansTypography, content = content)
}

@Composable
fun ShoukoM3PreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}

fun ColorScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return primary.copy(alpha = alpha).compositeOver(surface)
}
