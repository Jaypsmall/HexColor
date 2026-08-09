package com.example.hexcolor.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hexcolor.ColorItem
import com.example.hexcolor.ColorManager
import com.example.hexcolor.R
import java.util.Locale

@Composable
fun StartupScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.portada1),
            contentDescription = "Startup",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 250.dp, height = 60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush = GoldGradient)
            .border(1.2.dp, Color(0xFFF3E5AB), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = Color(0xFF111111),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

val GoldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8C6221), Color(0xFFFFF3A8), Color(0xFFC29B47)),
    start = Offset(0f, 0f),
    end = Offset.Infinite
)

fun Modifier.goldButtonStyle() = this
    .background(
        brush = GoldGradient,
        shape = RoundedCornerShape(8.dp)
    )
    .border(1.2.dp, Color(0xFFF3E5AB), RoundedCornerShape(8.dp))

fun Modifier.goldMask() = this
    .graphicsLayer(alpha = 0.99f)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = GoldGradient,
            blendMode = BlendMode.SrcIn
        )
    }

fun Modifier.goldBorder(shape: Shape) = this
    .border(1.dp, GoldGradient, shape)

@Composable
fun ColorCard(isDarkMode: Boolean, item: ColorItem, colorBlindnessMode: String, onClick: () -> Unit, onLongClick: () -> Unit, isGoldMode: Boolean) {
    val displayColor = if (colorBlindnessMode == "None") item.color else ColorManager.simulateColorBlindness(item.color, colorBlindnessMode)
    val textColor = if (ColorManager.isDark(displayColor)) Color.White else Color.Black; val shape = RoundedCornerShape(18.dp)
    Box(modifier = Modifier.aspectRatio(1f).shadow(6.dp, shape).clip(shape).background(displayColor).then(if (isGoldMode) Modifier.goldBorder(shape) else Modifier.border(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color.Black.copy(0.1f), shape)).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))) {
            Column(modifier = Modifier.padding(8.dp)) { Text(text = item.title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1); Text(text = ColorManager.colorToHex(item.color).uppercase(), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(text = "(${String.format(Locale.US, "%.2f", item.color.red)}, ${String.format(Locale.US, "%.2f", item.color.green)}, ${String.format(Locale.US, "%.2f", item.color.blue)}, 1)", color = textColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Normal) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, currentLocale: String, onToggleLanguage: () -> Unit, isCaosMode: Boolean, analogousCount: Int, fixedUiColorHex: String, colorBlindnessMode: String, favorites: Set<String>, onDismiss: () -> Unit, onUpdateSettings: (Boolean, Int, String, String, Boolean) -> Unit, isGoldMode: Boolean) {
    var showBlindnessDropdown by remember { mutableStateOf(false) }; val dialogBg = if (isDarkMode) Color.Black else Color(0xFFF2F4F7); val textColor = if (isDarkMode) Color.White else Color.Black; val sectionTitleColor = if (isDarkMode) Color.Gray else Color.DarkGray; val cardBg = if (isDarkMode) Color.White.copy(0.05f) else Color.White; val cardShape = RoundedCornerShape(16.dp)
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 16.dp).shadow(24.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).background(dialogBg).then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(28.dp)) else Modifier)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings).uppercase(), modifier = Modifier.align(Alignment.Center).then(if (isGoldMode) Modifier.goldMask() else Modifier), style = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp, color = textColor)); IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Default.Close, null, tint = textColor.copy(0.6f)) } }
            Spacer(Modifier.height(24.dp))
            SettingsSection(stringResource(R.string.appearance), sectionTitleColor) { SettingsItem(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, stringResource(R.string.dark_mode), cardBg, textColor, isDarkMode) { Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() }) }; SettingsItem(Icons.Default.WorkspacePremium, stringResource(R.string.gold_mode_premium), cardBg, textColor, isDarkMode, Color(0xFFC29B47)) { Switch(checked = isGoldMode, onCheckedChange = { onUpdateSettings(isCaosMode, analogousCount, fixedUiColorHex, colorBlindnessMode, it) }) } }
            Spacer(Modifier.height(16.dp))
            SettingsSection(stringResource(R.string.accessibility), sectionTitleColor) { Box { SettingsItem(Icons.Default.Visibility, if (colorBlindnessMode == "None") stringResource(R.string.none) else colorBlindnessMode, cardBg, textColor, isDarkMode, onClick = { showBlindnessDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = sectionTitleColor) }; DropdownMenu(expanded = showBlindnessDropdown, onDismissRequest = { showBlindnessDropdown = false }, modifier = Modifier.fillMaxWidth(0.5f).background(if (isDarkMode) Color(0xFF262626) else Color.White)) { listOf("None", "Protanopia", "Deuteranopia", "Tritanopia").forEach { mode -> DropdownMenuItem(text = { Text(if (mode == "None") stringResource(R.string.none) else mode, color = if (isDarkMode) Color.White else Color.Black) }, onClick = { onUpdateSettings(isCaosMode, analogousCount, fixedUiColorHex, mode, isGoldMode); showBlindnessDropdown = false }) } } } }
            Spacer(Modifier.height(16.dp))
            SettingsSection(stringResource(R.string.system), sectionTitleColor) { SettingsItem(Icons.Default.Language, stringResource(R.string.language), cardBg, textColor, isDarkMode, onClick = onToggleLanguage) { Text(if (currentLocale == "es") "ES \uD83C\uDDEA\uD83C\uDDF8" else "EN \uD83C\uDDFA\uD83C\uDDF8", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp) } }
            Spacer(Modifier.height(16.dp))
            SettingsSection(stringResource(R.string.customization), sectionTitleColor) { SettingsItem(Icons.Default.Sync, stringResource(R.string.chaos_mode), cardBg, textColor, isDarkMode) { Switch(checked = isCaosMode, onCheckedChange = { onUpdateSettings(it, analogousCount, fixedUiColorHex, colorBlindnessMode, isGoldMode) }) }; Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.analogous_count, analogousCount), style = TextStyle(fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)); Slider(value = analogousCount.toFloat(), onValueChange = { onUpdateSettings(isCaosMode, it.toInt(), fixedUiColorHex, colorBlindnessMode, isGoldMode) }, valueRange = 5f..10f, steps = 4, colors = SliderDefaults.colors(thumbColor = if (isGoldMode) Color(0xFFC29B47) else textColor, activeTrackColor = if (isGoldMode) Color(0xFFC29B47) else textColor.copy(0.4f))); Spacer(Modifier.height(12.dp)); Text(stringResource(R.string.fixed_ui_color).uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionTitleColor, letterSpacing = 1.sp)); Spacer(Modifier.height(8.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(listOf("#268CEF", "#FFD700", "#FF5722", "#4CAF50") + favorites.toList()) { hex -> val color = ColorManager.hexToColor(hex) ?: Color.Gray; Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color).border(if (hex == fixedUiColorHex) 3.dp else 1.dp, if (hex == fixedUiColorHex) (if (isGoldMode) Color(0xFFC29B47) else textColor) else textColor.copy(0.3f), CircleShape).clickable { onUpdateSettings(isCaosMode, analogousCount, hex, colorBlindnessMode, isGoldMode) }) } } }
            Spacer(Modifier.height(32.dp)); Text("Version 1.0.5 PRO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = sectionTitleColor.copy(0.5f)); Text("Created with ❤️ by JAYLIZ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = sectionTitleColor.copy(0.3f))
        }
    }
}

@Composable
fun SettingsSection(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) { Column(modifier = Modifier.fillMaxWidth()) { Text(title, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = color, letterSpacing = 1.5.sp)); Spacer(Modifier.height(8.dp)); Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } } }

@Composable
fun SettingsItem(icon: ImageVector, title: String, bgColor: Color, textColor: Color, isDarkMode: Boolean, iconColor: Color? = null, onClick: (() -> Unit)? = null, content: @Composable (() -> Unit)? = null) { Surface(onClick = { onClick?.invoke() }, enabled = onClick != null, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), color = bgColor, border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.1f) else Color.Black.copy(0.05f))) { Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = iconColor ?: (if (isDarkMode) Color.Gray else Color.DarkGray), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(16.dp)); Text(title, modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)); content?.invoke() } } }
