package com.example.hexcolor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.get
import androidx.palette.graphics.Palette
import com.example.hexcolor.ColorItem
import com.example.hexcolor.ColorManager
import com.example.hexcolor.R
import kotlin.math.min

@Composable
fun PickerScreen(isDarkMode: Boolean, bitmap: Bitmap?, onBitmapChange: (Bitmap?) -> Unit, detectedColors: List<Color>, onDetectedColorsChange: (List<Color>) -> Unit, onColorSelect: (Color) -> Unit, uiAccentColor: Color, colorBlindnessMode: String, onSaveFavorite: (Color) -> Unit, isGoldMode: Boolean, extractCount: Int, onUpdateExtractCount: (Int) -> Unit, currentLocale: String) {
    val context = LocalContext.current; var showExportDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { safeUri -> 
            onBitmapChange(try { 
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(safeUri), null, BitmapFactory.Options().apply { inMutable = true })
            } catch (_: Exception) { 
                null 
            })
            onDetectedColorsChange(emptyList()) 
        } 
    }
    val buttonShape = RoundedCornerShape(12.dp); val fineBorder = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8))
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.picker).uppercase(), style = TextStyle(color = uiAccentColor, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp), modifier = if (isGoldMode) Modifier.goldMask() else Modifier)
                Box(modifier = Modifier.height(300.dp).fillMaxWidth().shadow(8.dp, buttonShape).clip(buttonShape).background(if (isDarkMode) Color.Black else Color(0xFFF2F4F7)).then(if (isGoldMode) Modifier.goldBorder(buttonShape) else Modifier.border(fineBorder, buttonShape)), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        var scale by remember { mutableFloatStateOf(1f) }; var offset by remember { mutableStateOf(Offset.Zero) }
                        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); offset += pan } }) {
                            Canvas(modifier = Modifier.fillMaxSize().pointerInput(bitmap) { detectTapGestures { tapOffset -> bitmap.let { b -> val cW = size.width.toFloat(); val cH = size.height.toFloat(); val bW = b.width.toFloat(); val bH = b.height.toFloat(); val sBase = min(cW / bW, cH / bH); val visualX = (tapOffset.x - offset.x - cW / 2) / scale + cW / 2; val visualY = (tapOffset.y - offset.y - cH / 2) / scale + cH / 2; val dx = (cW - bW * sBase) / 2; val dy = (cH - bH * sBase) / 2; val x = ((visualX - dx) / sBase).toInt().coerceIn(0, b.width - 1); val y = ((visualY - dy) / sBase).toInt().coerceIn(0, b.height - 1); onColorSelect(Color(b[x, y])) } } }) {
                                bitmap.let { b -> val cW = size.width; val cH = size.height; val bW = b.width.toFloat(); val bH = b.height.toFloat(); val sBase = min(
                                    cW / bW, cH / bH); val dx = (cW - bW * sBase) / 2; val dy = (cH - bH * sBase) / 2; withTransform({ translate(offset.x, offset.y); scale(scale, scale, pivot = center) }) { drawImage(image = b.asImageBitmap(), dstOffset = IntOffset(dx.toInt(), dy.toInt()), dstSize = IntSize((bW * sBase).toInt(), (bH * sBase).toInt())) } }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(64.dp), tint = Color.Gray); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.no_image), color = Color.Gray) }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text("${if (currentLocale == "es") "Colores a extraer" else "Colors to extract"}: $extractCount", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(value = extractCount.toFloat(), onValueChange = { onUpdateExtractCount(it.toInt()) }, valueRange = 4f..32f, steps = 27, colors = SliderDefaults.colors(thumbColor = if (isGoldMode) Color(0xFFFFF3A8) else uiAccentColor, activeTrackColor = if (isGoldMode) Color(0xFFC29B47) else uiAccentColor))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(onClick = { launcher.launch("image/*") }, modifier = Modifier.weight(1f).height(50.dp).shadow(4.dp, buttonShape), shape = buttonShape, color = if (isGoldMode) Color.Transparent else uiAccentColor, border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f))) {
                        Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(if (currentLocale == "es") "AÑADIR IMAGEN" else "ADD IMAGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) }
                    }
                    if (bitmap != null) Surface(onClick = { Palette.from(bitmap).maximumColorCount(extractCount).generate { p -> val colors = mutableListOf<Color>(); p?.swatches?.forEach { colors.add(Color(it.rgb)) }; onDetectedColorsChange(colors.distinct()) } }, modifier = Modifier.weight(1f).height(50.dp).shadow(4.dp, buttonShape), shape = buttonShape, color = Color.Transparent) {
                        Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(uiAccentColor, buttonShape)), contentAlignment = Alignment.Center) { Text(if (currentLocale == "es") "DETECTAR COLORES" else "DETECT COLORS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) }
                    }
                }
                if (detectedColors.isNotEmpty()) Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (currentLocale == "es") "Colores Detectados" else "Detected Colors", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Surface(onClick = { showExportDialog = true }, modifier = Modifier.size(50.dp).shadow(2.dp, CircleShape), shape = CircleShape, color = if (isDarkMode) Color(0xFF1A1A1A) else Color.Transparent, border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.15f) else Color.Transparent)) {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Share, "Export Palette", tint = uiAccentColor, modifier = Modifier.size(20.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier)) }
                    }
                }
            }
        }
        items(detectedColors) { color -> ColorCard(isDarkMode, ColorItem("Extraído", color), colorBlindnessMode, { onColorSelect(color) }, { onSaveFavorite(color) }, isGoldMode) }
    }
    if (showExportDialog) ExportDialog(detectedColors, { showExportDialog = false }, uiAccentColor, isGoldMode, isDarkMode)
}
