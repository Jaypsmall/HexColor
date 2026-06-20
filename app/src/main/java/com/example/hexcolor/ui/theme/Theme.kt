package com.example.hexcolor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.hexcolor.ColorUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

private fun ColorScheme.withColorBlindness(type: String): ColorScheme {
    if (type == "None") return this
    return this.copy(
        primary = ColorUtils.simulateColorBlindness(primary, type),
        onPrimary = ColorUtils.simulateColorBlindness(onPrimary, type),
        primaryContainer = ColorUtils.simulateColorBlindness(primaryContainer, type),
        onPrimaryContainer = ColorUtils.simulateColorBlindness(onPrimaryContainer, type),
        secondary = ColorUtils.simulateColorBlindness(secondary, type),
        onSecondary = ColorUtils.simulateColorBlindness(onSecondary, type),
        secondaryContainer = ColorUtils.simulateColorBlindness(secondaryContainer, type),
        onSecondaryContainer = ColorUtils.simulateColorBlindness(onSecondaryContainer, type),
        tertiary = ColorUtils.simulateColorBlindness(tertiary, type),
        onTertiary = ColorUtils.simulateColorBlindness(onTertiary, type),
        tertiaryContainer = ColorUtils.simulateColorBlindness(tertiaryContainer, type),
        onTertiaryContainer = ColorUtils.simulateColorBlindness(onTertiaryContainer, type),
        error = ColorUtils.simulateColorBlindness(error, type),
        onError = ColorUtils.simulateColorBlindness(onError, type),
        errorContainer = ColorUtils.simulateColorBlindness(errorContainer, type),
        onErrorContainer = ColorUtils.simulateColorBlindness(onErrorContainer, type),
        background = ColorUtils.simulateColorBlindness(background, type),
        onBackground = ColorUtils.simulateColorBlindness(onBackground, type),
        surface = ColorUtils.simulateColorBlindness(surface, type),
        onSurface = ColorUtils.simulateColorBlindness(onSurface, type),
        surfaceVariant = ColorUtils.simulateColorBlindness(surfaceVariant, type),
        onSurfaceVariant = ColorUtils.simulateColorBlindness(onSurfaceVariant, type),
        outline = ColorUtils.simulateColorBlindness(outline, type),
        outlineVariant = ColorUtils.simulateColorBlindness(outlineVariant, type),
        scrim = ColorUtils.simulateColorBlindness(scrim, type),
        inverseSurface = ColorUtils.simulateColorBlindness(inverseSurface, type),
        inverseOnSurface = ColorUtils.simulateColorBlindness(inverseOnSurface, type),
        inversePrimary = ColorUtils.simulateColorBlindness(inversePrimary, type),
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun HexColorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    colorBlindnessMode: String = "None",
    content: @Composable () -> Unit
) {
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (colorBlindnessMode != "None") {
        colorScheme = colorScheme.withColorBlindness(colorBlindnessMode)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
