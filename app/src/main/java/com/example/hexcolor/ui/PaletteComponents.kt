package com.example.hexcolor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hexcolor.ColorItem
import com.example.hexcolor.ColorManager
import com.example.hexcolor.R

@Composable
fun PaletteScreen(isDarkMode: Boolean, hexInput: String, onHexChange: (String) -> Unit, currentColor: Color, onColorChange: (Color) -> Unit, hsvValue: FloatArray, onHsvChange: (FloatArray) -> Unit, colorItems: List<ColorItem>, onSaveFavorite: (Color) -> Unit, onCopyColor: (Color) -> Unit, onSniperToggle: () -> Unit, uiAccentColor: Color, colorBlindnessMode: String, isGoldMode: Boolean) {
    val context = LocalContext.current; val focusManager = LocalFocusManager.current; val buttonShape = RoundedCornerShape(12.dp); val fineBorder = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8))
    val invalidHexMsg = stringResource(R.string.invalid_hex)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) onSniperToggle() else Toast.makeText(context, "Permiso necesario", Toast.LENGTH_SHORT).show() }
    val hueGradientColors = remember(colorBlindnessMode) { listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red).map { if (colorBlindnessMode == "None") it else ColorManager.simulateColorBlindness(it, colorBlindnessMode) } }
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = onHexChange,
                        modifier = Modifier.weight(1f).fillMaxHeight().border(fineBorder, buttonShape),
                        placeholder = { Text("#RRGGBB", fontSize = 14.sp, color = Color.Gray) },
                        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkMode) Color.Black else Color(0xFFF2F4F7),
                            unfocusedContainerColor = if (isDarkMode) Color.Black else Color(0xFFF2F4F7),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedBorderColor = uiAccentColor.copy(0.5f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = buttonShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                    Surface(onClick = { val color = ColorManager.hexToColor(hexInput); if (color != null) onColorChange(color) else Toast.makeText(context, invalidHexMsg, Toast.LENGTH_SHORT).show() }, modifier = Modifier.width(85.dp).fillMaxHeight().shadow(4.dp, buttonShape), shape = buttonShape, color = if (isGoldMode) Color.Transparent else uiAccentColor, border = BorderStroke(1.dp, Color.White.copy(0.4f))) { Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(stringResource(R.string.show), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) } }
                    Surface(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) onSniperToggle() else permissionLauncher.launch(
                        Manifest.permission.CAMERA) }, modifier = Modifier.size(50.dp).shadow(4.dp, buttonShape), shape = buttonShape, color = if (isGoldMode) Color.Transparent else uiAccentColor, border = BorderStroke(1.dp, Color.White.copy(0.4f))) { Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, "Sniper", tint = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) } }
                }
                Box(modifier = Modifier.fillMaxWidth().height(32.dp).shadow(2.dp, RoundedCornerShape(16.dp)).background(Brush.linearGradient(hueGradientColors), RoundedCornerShape(16.dp)).border(width = 1.dp, brush = if (isGoldMode) Brush.linearGradient(listOf(Color(0xFF8C6221), Color(0xFFFFF3A8), Color(0xFFC29B47))) else SolidColor(if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8)), shape = RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Box(modifier = Modifier.fillMaxSize().padding(2.dp).border(1.2.dp, Color.Black.copy(0.3f), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent)), RoundedCornerShape(16.dp))); Slider(value = hsvValue[0], onValueChange = { onHsvChange(floatArrayOf(it, hsvValue[1], hsvValue[2])) }, valueRange = 0f..360f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) }
                Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(onClick = { onColorChange(Color((0..255).random() / 255f, (0..255).random() / 255f, (0..255).random() / 255f)) }, modifier = Modifier.weight(1f).fillMaxHeight().shadow(4.dp, buttonShape), shape = buttonShape, color = if (isGoldMode) Color.Transparent else uiAccentColor, border = BorderStroke(1.dp, Color.White.copy(0.4f))) { Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(stringResource(R.string.random), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) } }
                    Surface(onClick = { onColorChange(ColorManager.getComplementary(currentColor)) }, modifier = Modifier.weight(1f).fillMaxHeight().shadow(4.dp, buttonShape), shape = buttonShape, color = if (isGoldMode) Color.Transparent else uiAccentColor, border = BorderStroke(1.dp, Color.White.copy(0.4f))) { Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(stringResource(R.string.complementary), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) } }
                }
            }
        }
        items(colorItems) { item -> ColorCard(isDarkMode, item, colorBlindnessMode, { onCopyColor(item.color) }, { onSaveFavorite(item.color) }, isGoldMode) }
    }
}
