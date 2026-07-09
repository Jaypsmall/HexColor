package com.example.hexcolor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorManager {
    fun hexToColor(hex: String): Color? {
        return try {
            val h = if (hex.startsWith("#")) hex else "#$hex"
            Color(h.toColorInt())
        } catch (_: Exception) {
            null
        }
    }

    fun colorToHex(color: Color): String {
        // 1. Extraemos directamente los floats de sRGB y los redondeamos a enteros (0-255)
        var r = (color.red * 255f).roundToInt()
        var g = (color.green * 255f).roundToInt()
        var b = (color.blue * 255f).roundToInt()

        // 2. Limpieza de ruido: si los tres canales caen en el umbral residual, es negro puro
        if (r < 8 && g < 8 && b < 8) {
            r = 0; g = 0; b = 0
        }
        // Idem para luces altas (blanco puro)
        if (r > 248 && g > 248 && b > 248) {
            r = 255; g = 255; b = 255
        }

        // 3. Formateamos directamente construyendo el entero RGB manual
        val rgbInt = (r shl 16) or (g shl 8) or b
        return String.format(Locale.US, "#%06X", rgbInt)
    }

    fun getComplementary(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[0] = (hsv[0] + 180) % 360
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    fun getAnalogous(color: Color, count: Int = 7): List<Color> {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        val startAngle = -90f
        val endAngle = 90f
        val step = if (count > 1) (endAngle - startAngle) / (count - 1) else 0f
        return (0 until count).map { i ->
            val offset = startAngle + (i * step)
            val newHsv = hsv.clone().apply { this[0] = (this[0] + offset + 360) % 360 }
            Color(android.graphics.Color.HSVToColor(newHsv))
        }
    }

    fun getTriadic(color: Color): List<Color> {
        val hsv = FloatArray(3).apply { android.graphics.Color.colorToHSV(color.toArgb(), this) }
        return listOf(0f, 120f, 240f).map { offset ->
            val newHsv = hsv.clone().apply { this[0] = (this[0] + offset) % 360 }
            Color(android.graphics.Color.HSVToColor(newHsv))
        }
    }

    fun hsvToColor(hue: Float, saturation: Float = 1f, value: Float = 1f): Color {
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    fun isDark(color: Color): Boolean {
        val r = color.red * 255
        val g = color.green * 255
        val b = color.blue * 255
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
        return luminance < 128
    }

    fun getContrastRatio(color1: Color, color2: Color): Double {
        fun luminance(c: Color): Double {
            val r = if (c.red <= 0.03928) c.red / 12.92 else ((c.red + 0.055) / 1.055).pow(2.4)
            val g = if (c.green <= 0.03928) c.green / 12.92 else ((c.green + 0.055) / 1.055).pow(2.4)
            val b = if (c.blue <= 0.03928) c.blue / 12.92 else ((c.blue + 0.055) / 1.055).pow(2.4)
            return 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        val l1 = luminance(color1)
        val l2 = luminance(color2)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    fun simulateColorBlindness(color: Color, type: String): Color {
        if (type == "None") return color
        
        // sRGB to Linear
        fun toLinear(c: Float): Float = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
        // Linear to sRGB
        fun fromLinear(c: Float): Float = if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f

        val rL = toLinear(color.red)
        val gL = toLinear(color.green)
        val bL = toLinear(color.blue)

        val (nrL, ngL, nbL) = when (type) {
            "Protanopia" -> Triple(
                0.56667f * rL + 0.43333f * gL + 0f * bL,
                0.55833f * rL + 0.44167f * gL + 0f * bL,
                0f * rL + 0.24167f * gL + 0.75833f * bL
            )
            "Deuteranopia" -> Triple(
                0.625f * rL + 0.375f * gL + 0f * bL,
                0.7f * rL + 0.3f * gL + 0f * bL,
                0f * rL + 0.3f * gL + 0.7f * bL
            )
            "Tritanopia" -> Triple(
                1.01f * rL + 0.02f * gL - 0.03f * bL,
                0.10f * rL + 0.73f * gL + 0.17f * bL,
                0f * rL + 0.85f * gL + 0.15f * bL
            )
            else -> Triple(rL, gL, bL)
        }

        return Color(
            fromLinear(nrL).coerceIn(0f, 1f),
            fromLinear(ngL).coerceIn(0f, 1f),
            fromLinear(nbL).coerceIn(0f, 1f),
            color.alpha
        )
    }
}
