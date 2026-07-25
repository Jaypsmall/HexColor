package com.example.hexcolor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, widthDp = 1080, heightDp = 1920)
@Composable
fun GoldBackgroundPreview() {
    val goldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8C6221), // Oro Oscuro / Sombra
            Color(0xFFFFF3A8), // Oro Brillante / Luz
            Color(0xFFC29B47)  // Oro Medio / Base
        ),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = goldGradient)
    )
}
