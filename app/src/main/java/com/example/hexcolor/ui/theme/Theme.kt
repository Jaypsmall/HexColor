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
import com.example.hexcolor.ColorManager
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

private fun ColorScheme.withColorBlindness(type: String): ColorScheme {
    if (type == "None") return this
    return this.copy(
        primary = ColorManager.simulateColorBlindness(primary, type),
        onPrimary = ColorManager.simulateColorBlindness(onPrimary, type),
        primaryContainer = ColorManager.simulateColorBlindness(primaryContainer, type),
        onPrimaryContainer = ColorManager.simulateColorBlindness(onPrimaryContainer, type),
        secondary = ColorManager.simulateColorBlindness(secondary, type),
        onSecondary = ColorManager.simulateColorBlindness(onSecondary, type),
        secondaryContainer = ColorManager.simulateColorBlindness(secondaryContainer, type),
        onSecondaryContainer = ColorManager.simulateColorBlindness(onSecondaryContainer, type),
        tertiary = ColorManager.simulateColorBlindness(tertiary, type),
        onTertiary = ColorManager.simulateColorBlindness(onTertiary, type),
        tertiaryContainer = ColorManager.simulateColorBlindness(tertiaryContainer, type),
        onTertiaryContainer = ColorManager.simulateColorBlindness(onTertiaryContainer, type),
        error = ColorManager.simulateColorBlindness(error, type),
        onError = ColorManager.simulateColorBlindness(onError, type),
        errorContainer = ColorManager.simulateColorBlindness(errorContainer, type),
        onErrorContainer = ColorManager.simulateColorBlindness(onErrorContainer, type),
        background = ColorManager.simulateColorBlindness(background, type),
        onBackground = ColorManager.simulateColorBlindness(onBackground, type),
        surface = ColorManager.simulateColorBlindness(surface, type),
        onSurface = ColorManager.simulateColorBlindness(onSurface, type),
        surfaceVariant = ColorManager.simulateColorBlindness(surfaceVariant, type),
        onSurfaceVariant = ColorManager.simulateColorBlindness(onSurfaceVariant, type),
        outline = ColorManager.simulateColorBlindness(outline, type),
        outlineVariant = ColorManager.simulateColorBlindness(outlineVariant, type),
        scrim = ColorManager.simulateColorBlindness(scrim, type),
        inverseSurface = ColorManager.simulateColorBlindness(inverseSurface, type),
        inverseOnSurface = ColorManager.simulateColorBlindness(inverseOnSurface, type),
        inversePrimary = ColorManager.simulateColorBlindness(inversePrimary, type),
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
