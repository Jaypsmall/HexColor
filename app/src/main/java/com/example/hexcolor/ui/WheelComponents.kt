package com.example.hexcolor.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hexcolor.*
import com.example.hexcolor.R
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, currentColor: Color, onColorSelect: (Color) -> Unit, onCopyColor: (Color) -> Unit, harmonyMode: HarmonyMode, onModeChange: (HarmonyMode) -> Unit, harmonyColors: List<Color>, hsvValue: FloatArray, analogousCount: Int, onValueChange: (Float) -> Unit, onNavigateToFavorites: () -> Unit, currentLocale: String, uiAccentColor: Color, colorBlindnessMode: String, onColorBlindnessChange: (String) -> Unit, isGoldMode: Boolean) {
    val activity = LocalActivity.current as? MainActivity
    val modes = remember { listOf(HarmonyMode.COMPLEMENTARY, HarmonyMode.TRIADIC, HarmonyMode.ANALOGOUS) }
    var cardOffset by remember { mutableStateOf(Offset.Zero) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp || (maxWidth > maxHeight && maxHeight < 500.dp)
        val scrollState = rememberScrollState()
        
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    WheelContent(
                        isDarkMode, colorBlindnessMode, onColorBlindnessChange, currentColor, 
                        hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, 
                        onToggleDarkMode, onNavigateToFavorites, uiAccentColor, isGoldMode,
                        isLandscape = true
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    HeaderControls(modes, harmonyMode, onModeChange, isDarkMode, { showExportDialog = true }, uiAccentColor,
                        isGoldMode)
                    BrightnessSlider(currentLocale, hsvValue[2], onValueChange, currentColor, isGoldMode)
                    InfoCard(isDarkMode, currentColor, harmonyColors, onCopyColor, activity, harmonyMode, cardOffset, { cardOffset += it }, uiAccentColor, colorBlindnessMode, isGoldMode)
                    Spacer(Modifier.height(16.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderControls(modes, harmonyMode, onModeChange, isDarkMode, { showExportDialog = true }, uiAccentColor,
                    isGoldMode)
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).widthIn(max = 450.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WheelContent(
                        isDarkMode, colorBlindnessMode, onColorBlindnessChange, currentColor, 
                        hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, 
                        onToggleDarkMode, onNavigateToFavorites, uiAccentColor, isGoldMode,
                        isLandscape = false
                    )
                }
                BrightnessSlider(currentLocale, hsvValue[2], onValueChange, currentColor, isGoldMode)
                InfoCard(isDarkMode, currentColor, harmonyColors, onCopyColor, activity, harmonyMode, cardOffset, { cardOffset += it }, uiAccentColor, colorBlindnessMode, isGoldMode)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    if (showExportDialog) ExportDialog(harmonyColors, { showExportDialog = false }, uiAccentColor, isGoldMode, isDarkMode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderControls(modes: List<HarmonyMode>, harmonyMode: HarmonyMode, onModeChange: (HarmonyMode) -> Unit, isDarkMode: Boolean, onExport: () -> Unit, uiAccentColor: Color,
                           isGoldMode: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.forEach { itemMode ->
                val isSelected = harmonyMode == itemMode; val shape = RoundedCornerShape(10.dp); val label = when(itemMode) { HarmonyMode.COMPLEMENTARY -> stringResource(R.string.complementary); HarmonyMode.TRIADIC -> stringResource(R.string.triadic); else -> stringResource(R.string.analogous) }
                Surface(onClick = { onModeChange(itemMode) }, modifier = Modifier.weight(1f).fillMaxHeight().shadow(isSelected.let { if (it) 2.dp else 0.dp }, shape), shape = shape, color = if (isSelected) (if (isGoldMode) Color.Transparent else uiAccentColor) else (if (isDarkMode) Color(0xFF111111) else Color.White), border = BorderStroke(1.dp, if (isSelected) Color.White.copy(0.5f) else (if (isDarkMode) Color.White.copy(0.15f) else Color.Black.copy(0.1f)))) {
                    Box(modifier = Modifier.fillMaxSize().then(if (isSelected && isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.25f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (isSelected) (if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Surface(onClick = onExport, modifier = Modifier.size(50.dp).shadow(if (isDarkMode) 2.dp else 0.dp, CircleShape), shape = CircleShape, color = if (isDarkMode) Color(0xFF1A1A1A) else Color.Transparent, border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.15f) else Color.Transparent)) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Share, "Export", tint = uiAccentColor, modifier = Modifier.size(20.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier)) }
        }
    }
}

@Composable
private fun WheelContent(isDarkMode: Boolean, colorBlindnessMode: String, onColorBlindnessChange: (String) -> Unit, currentColor: Color, hsvValue: FloatArray, harmonyMode: HarmonyMode, analogousCount: Int, onColorSelect: (Color) -> Unit, onCopyColor: (Color) -> Unit, onToggleDarkMode: () -> Unit, onNavigateToFavorites: () -> Unit, uiAccentColor: Color, isGoldMode: Boolean, isLandscape: Boolean) {
    val moonResource = if (hsvValue[2] > 0.5f) R.drawable.moon_light else R.drawable.moon_shadow; var showBlindnessMenu by remember { mutableStateOf(false) }
    val wheelPadding = if (isLandscape) 20.dp else 40.dp
    Box(contentAlignment = Alignment.Center) {
        ColorWheel(isDarkMode, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, Modifier.fillMaxSize().padding(wheelPadding), uiAccentColor, colorBlindnessMode, isGoldMode)
        Box(modifier = Modifier.fillMaxSize().padding(wheelPadding), contentAlignment = Alignment.Center) { Crossfade(targetState = moonResource, animationSpec = tween(400), label = "MoonTransition") { targetResource -> Image(painter = painterResource(id = targetResource), contentDescription = "Moon", modifier = Modifier.fillMaxSize(0.65f), contentScale = ContentScale.Fit) } }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).offset(y = if (isLandscape) (-4).dp else (-11).dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showBlindnessMenu = true }) { Icon(imageVector = Icons.Default.Visibility, contentDescription = "Sim", tint = if (colorBlindnessMode != "None") uiAccentColor else (if (isDarkMode) Color.Gray else Color.DarkGray), modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
                DropdownMenu(expanded = showBlindnessMenu, onDismissRequest = { showBlindnessMenu = false }, containerColor = if (isDarkMode) Color(0xFF262626) else Color.White) { listOf("None", "Protanopia", "Deuteranopia", "Tritanopia").forEach { mode -> DropdownMenuItem(text = { Text(mode, color = if (colorBlindnessMode == mode) uiAccentColor else (if (isDarkMode) Color.White else Color.Black)) }, onClick = { onColorBlindnessChange(mode); showBlindnessMenu = false }) } }
            }
            IconButton(onClick = onToggleDarkMode) { Icon(imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = "Theme", tint = if (isDarkMode) uiAccentColor else Color.DarkGray, modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
        }
        IconButton(onClick = onNavigateToFavorites, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).offset(y = (-11).dp)) { Icon(imageVector = Icons.Default.Star, contentDescription = "Favorites", tint = if (isDarkMode) uiAccentColor else Color.DarkGray, modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
    }
}

@Composable
private fun BrightnessSlider(currentLocale: String, value: Float, onValueChange: (Float) -> Unit, currentColor: Color, isGoldMode: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-11).dp)) {
        Text(if (currentLocale == "es") "Brillo / Sombra" else "Brightness / Shadow", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(32.dp).shadow(2.dp, RoundedCornerShape(16.dp)).background(if (value > 0.5f) Color.White.copy(0.1f) else Color.Black.copy(0.3f), RoundedCornerShape(16.dp)).border(width = 1.dp, brush = if (isGoldMode) GoldGradient else SolidColor(if (value > 0.5f) Color.White.copy(0.3f) else Color.White.copy(0.15f)), shape = RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Slider(value = value, onValueChange = onValueChange, colors = SliderDefaults.colors(thumbColor = if (isGoldMode) Color(0xFFFFF3A8) else currentColor, activeTrackColor = if (isGoldMode) Color(0xFFC29B47) else currentColor, inactiveTrackColor = Color.Gray.copy(0.3f)), modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun InfoCard(isDarkMode: Boolean, currentColor: Color, harmonyColors: List<Color>, onCopyColor: (Color) -> Unit, activity: MainActivity?, harmonyMode: HarmonyMode, offset: Offset, onOffsetChange: (Offset) -> Unit, uiAccentColor: Color, colorBlindnessMode: String, isGoldMode: Boolean) {
    val shape = RoundedCornerShape(18.dp); val defaultBorder = BorderStroke(1.dp, if (isDarkMode) Color(0xFF333333) else Color(0xFFAAAAAA))
    Card(colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF262626).copy(alpha = 0.95f) else Color(0xFFDDDDDD).copy(alpha = 0.95f)), shape = shape, border = if (isGoldMode) null else defaultBorder, modifier = Modifier.fillMaxWidth().offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt() - 5.dp.toPx().toInt()) }.then(if (isGoldMode) Modifier.goldBorder(shape) else Modifier).pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onOffsetChange(dragAmount) } }) {
        Box(modifier = Modifier.padding(12.dp)) {
            IconButton(onClick = { activity?.let { act -> act.checkOverlayPermission(act) { FloatingService.currentHex = ColorManager.colorToHex(currentColor); FloatingService.currentHarmony = harmonyColors.map { ColorManager.colorToHex(it) }; FloatingService.isDarkMode = isDarkMode; FloatingService.isGoldMode = isGoldMode; FloatingService.originalIndex = if (harmonyMode == HarmonyMode.ANALOGOUS) harmonyColors.size / 2 else 0; act.startService(Intent(act, FloatingService::class.java)) } } }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) { Icon(Icons.Default.PictureInPictureAlt, "Floating", tint = uiAccentColor, modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(ColorManager.colorToHex(currentColor).uppercase(), color = if (isDarkMode) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp).border(1.dp, if (isDarkMode) Color(0xFF444444) else Color(0xFFAAAAAA), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                val cW = ColorManager.getContrastRatio(currentColor, Color.White); val cB = ColorManager.getContrastRatio(currentColor, Color.Black); val r = max(cW, cB)
                Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Surface(color = if (r >= 4.5) Color(0xFF4CAF50) else Color(0xFFF44336), shape = RoundedCornerShape(4.dp)) { Text(if (r >= 4.5) " WCAG PASS " else " WCAG FAIL ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }; Text("Ratio: ${String.format(Locale.US, "%.1f", r)}:1 (${if (cW > cB) "White" else "Black"})", color = Color.Gray, fontSize = 10.sp) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, contentPadding = PaddingValues(horizontal = 4.dp)) { items(harmonyColors.size) { index -> val color = harmonyColors[index]; val displayColor = if (colorBlindnessMode == "None") color else ColorManager.simulateColorBlindness(color, colorBlindnessMode); Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(displayColor).border(1.dp, if (isDarkMode) Color(0xFF444444) else Color(0xFFAAAAAA), RoundedCornerShape(8.dp)).clickable { onCopyColor(color) }, contentAlignment = Alignment.Center) { Text(text = (index + 1).toString(), color = if (ColorManager.isDark(displayColor)) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp) }; Text(ColorManager.colorToHex(color).substring(1), color = Color.Gray, fontSize = 9.sp) } } }
            }
        }
    }
}

@Composable
fun ColorWheel(isDarkMode: Boolean, currentColor: Color, actualHsv: FloatArray, harmonyMode: HarmonyMode, analogousCount: Int, onColorChange: (Color) -> Unit, onColorClick: (Color) -> Unit, modifier: Modifier = Modifier, uiAccentColor: Color, colorBlindnessMode: String, isGoldMode: Boolean) {
    val textMeasurer = rememberTextMeasurer(); val wheelGradientColors = remember(colorBlindnessMode) { listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red).map { if (colorBlindnessMode == "None") it else ColorManager.simulateColorBlindness(it, colorBlindnessMode) } }
    val targets = remember(harmonyMode, analogousCount) { when(harmonyMode) { HarmonyMode.COMPLEMENTARY -> listOf(0f, 180f); HarmonyMode.TRIADIC -> listOf(0f, 120f, 240f); else -> { val step = if (analogousCount > 1) 180f / (analogousCount - 1) else 0f; (0 until analogousCount).map { -90f + (it * step) } } } }
    val animatedOffsets = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }
    LaunchedEffect(targets) { while (animatedOffsets.size < targets.size) { animatedOffsets.add(Animatable(0f)) }; while (animatedOffsets.size > targets.size) { animatedOffsets.removeAt(animatedOffsets.size - 1) }; targets.forEachIndexed { i, target -> launch { animatedOffsets[i].animateTo(targetValue = target, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) } } }
    Canvas(modifier = modifier.pointerInput(actualHsv[2]) { detectDragGestures { change, _ -> val center = Offset(size.width / 2f, size.height / 2f); val pos = change.position - center; val hue = (atan2(pos.y, pos.x) * (180f / PI.toFloat()) + 360f) % 360f; val dist = sqrt(pos.x.pow(2) + pos.y.pow(2)); val radius = min(size.width, size.height) / 2f; val s = (dist / radius).coerceIn(0f, 1f); onColorChange(ColorManager.hsvToColor(hue, s, actualHsv[2])) } }.pointerInput(harmonyMode, actualHsv[0], actualHsv[1], actualHsv[2]) { detectTapGestures { offset -> val center = Offset(size.width / 2f, size.height / 2f); val radius = min(size.width, size.height) / 2f; val pos = offset - center; val dist = sqrt(pos.x.pow(2) + pos.y.pow(2)); if (dist < radius * 0.58f) { val newVal = if (actualHsv[2] > 0.5f) 0.4f else 1.0f; onColorChange(ColorManager.hsvToColor(actualHsv[0], actualHsv[1], newVal)) } else if (dist <= radius) { val hue = (atan2(pos.y, pos.x) * (180f / PI.toFloat()) + 360f) % 360f; val s = (dist / radius).coerceIn(0f, 1f); onColorChange(ColorManager.hsvToColor(hue, s, actualHsv[2])) } else { val rad = actualHsv[0] * PI.toFloat() / 180f; val d = sqrt((offset.x - (center.x + radius * cos(rad))).pow(2) + (offset.y - (center.y + radius * sin(rad))).pow(2)); if (d < 40f) onColorClick(currentColor) } } }) {
        val center = Offset(size.width / 2f, size.height / 2f); val radius = min(size.width, size.height) / 2f; val ringThickness = radius * 0.12f; val gap = radius * 0.02f
        for (i in 0..2) { val r = radius - (i * (ringThickness + gap)) - (ringThickness / 2f); val ringSaturation = when(i) { 0 -> 1f; 1 -> 0.7f; else -> 0.4f }; drawCircle(brush = Brush.sweepGradient(wheelGradientColors), radius = r, center = center, style = Stroke(width = ringThickness)); drawCircle(color = Color.Black.copy(alpha = 1f - actualHsv[2]), radius = r, center = center, style = Stroke(width = ringThickness)); drawCircle(color = Color.Gray.copy(alpha = (1f - (actualHsv[1] * ringSaturation)) * 0.5f), radius = r, center = center, style = Stroke(width = ringThickness)) }
        val gC = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f); val mRad = actualHsv[0] * PI.toFloat() / 180f; drawLine(gC.copy(alpha = 0.5f), center, Offset(center.x + radius * cos(mRad), center.y + radius * sin(mRad)), strokeWidth = 2.dp.toPx())
        animatedOffsets.forEachIndexed { i, anim -> val h = (actualHsv[0] + anim.value + 360f) % 360f; val rad = h * PI.toFloat() / 180f; val p = Offset(center.x + radius * cos(rad), center.y + radius * sin(rad)); if (anim.value != 0f) { drawLine(gC.copy(alpha = 0.3f), center, p, strokeWidth = 1.dp.toPx()); drawCircle(if (isDarkMode) Color.White else Color.Black, radius = 6.dp.toPx(), center = p) }; val lR = radius + 22.dp.toPx(); val lP = Offset(center.x + lR * cos(rad), center.y + lR * sin(rad)); if (isGoldMode) drawCircle(brush = GoldGradient, radius = 10.dp.toPx(), center = lP) else drawCircle(if (colorBlindnessMode == "None") uiAccentColor else ColorManager.simulateColorBlindness(uiAccentColor, colorBlindnessMode), radius = 10.dp.toPx(), center = lP); val textColor = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black); val tr = textMeasurer.measure((i + 1).toString(), TextStyle(color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)); drawText(tr, topLeft = Offset(lP.x - tr.size.width / 2, lP.y - tr.size.height / 2)) }
        val hp = Offset(center.x + (actualHsv[1] * radius) * cos(actualHsv[0] * PI.toFloat() / 180f), center.y + (actualHsv[1] * radius) * sin(actualHsv[0] * PI.toFloat() / 180f)); drawCircle(Color.Black, radius = 10.dp.toPx(), center = hp); drawCircle(Color.White, radius = 8.dp.toPx(), center = hp); drawCircle(if (colorBlindnessMode == "None") currentColor else ColorManager.simulateColorBlindness(currentColor, colorBlindnessMode), radius = 6.dp.toPx(), center = hp)
    }
}
