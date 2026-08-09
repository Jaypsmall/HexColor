package com.example.hexcolor.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hexcolor.ColorManager
import com.example.hexcolor.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(isDarkMode: Boolean, favorites: Set<String>, savedPalettes: Set<String>, onColorSelect: (String) -> Unit, onDeleteFavorite: (String) -> Unit, onDeletePalette: (String) -> Unit, onSavePalette: (String, List<String>) -> Unit, isGoldMode: Boolean) {
    val bgColor = remember(isDarkMode) { if (isDarkMode) Color.Black else Color(0xFFF2F4F7) }
    val cardColor = remember(isDarkMode) { if (isDarkMode) Color(0xFF1E1E1E) else Color.White }
    val borderColor = remember(isDarkMode) { if (isDarkMode) Color.White.copy(0.12f) else Color.Black.copy(0.08f) }
    val boxShape = RoundedCornerShape(10.dp)

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedColors by remember { mutableStateOf(setOf<String>()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var paletteName by remember { mutableStateOf("") }

    // Parseamos las paletas una sola vez cuando cambian las guardadas
    val parsedPalettes = remember(savedPalettes) {
        savedPalettes.map { paletteJson ->
            try {
                val name = paletteJson.substringAfter("\"name\":\"").substringBefore("\"")
                val colorsStr = paletteJson.substringAfter("\"colors\":[").substringBefore("]")
                val colors = colorsStr.split(",").map { it.replace("\"", "").trim() }.filter { it.isNotEmpty() }
                Triple(name, colors, paletteJson)
            } catch (e: Exception) {
                Triple("Imported Palette", emptyList<String>(), paletteJson)
            }
        }
    }

    if (showNameDialog) {
        BasicAlertDialog(
            onDismissRequest = { showNameDialog = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(24.dp)) else Modifier)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.save_palette).uppercase(), style = TextStyle(fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp, color = if (isDarkMode) Color.White else Color.Black), modifier = if (isGoldMode) Modifier.goldMask() else Modifier)
                Spacer(Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = paletteName,
                    onValueChange = { paletteName = it },
                    placeholder = { Text(stringResource(R.string.palette_name), color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        focusedBorderColor = if (isGoldMode) Color(0xFFC29B47) else (if (isDarkMode) Color.White else Color.Black),
                        unfocusedBorderColor = if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f)
                    )
                )
                
                Spacer(Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        onClick = {
                            if (paletteName.isNotBlank() && selectedColors.isNotEmpty()) {
                                onSavePalette(paletteName, selectedColors.toList())
                                showNameDialog = false
                                isSelectionMode = false
                                selectedColors = emptySet()
                                paletteName = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGoldMode) Color.Transparent else (if (isDarkMode) Color.White else Color.Black)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.accept).uppercase(), color = if (isGoldMode) Color(0xFF543B14) else (if (isDarkMode) Color.Black else Color.White), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(18.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.favorites_header).uppercase(), color = if (isDarkMode) Color.LightGray else Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, modifier = if (isGoldMode) Modifier.goldMask() else Modifier)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode && selectedColors.isNotEmpty()) {
                    IconButton(onClick = { showNameDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                
                IconButton(onClick = { 
                    isSelectionMode = !isSelectionMode
                    if (!isSelectionMode) selectedColors = emptySet()
                }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isSelectionMode) Icons.Default.Close else Icons.Default.LibraryAdd,
                        null,
                        tint = if (isSelectionMode) Color.Red else (if (isGoldMode) Color(0xFFC29B47) else Color.Gray)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (favorites.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(cardColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("Sin colores guardados", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        else FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { 
            favorites.forEach { favHex -> 
                val color = ColorManager.hexToColor(favHex) ?: Color.Gray
                val isSelected = selectedColors.contains(favHex)
                Box(modifier = Modifier.size(60.dp).shadow(4.dp, boxShape).clip(boxShape).background(color)
                    .then(if (isSelectionMode && isSelected) Modifier.border(3.dp, if (isGoldMode) Color(0xFFC29B47) else Color.White, boxShape) else Modifier)
                    .then(if (isGoldMode) Modifier.goldBorder(boxShape) else Modifier.border(1.5.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f), boxShape))
                    .combinedClickable(
                        onClick = { 
                            if (isSelectionMode) {
                                selectedColors = if (isSelected) selectedColors - favHex else selectedColors + favHex
                            } else {
                                onColorSelect(favHex) 
                            }
                        }, 
                        onLongClick = { if (!isSelectionMode) onDeleteFavorite(favHex) }
                    )
                ) { 
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent))))
                    if (isSelectionMode && isSelected) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(24.dp).shadow(2.dp, CircleShape))
                    }
                } 
            } 
        }
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, tint = Color.Gray, modifier = Modifier.size(18.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.palettes_tab).uppercase(), color = if (isDarkMode) Color.LightGray else Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
        Spacer(Modifier.height(16.dp))
        if (parsedPalettes.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(cardColor, RoundedCornerShape(12.dp)).then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(12.dp)) else Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Palette, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(32.dp)); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.no_palettes), color = Color.Gray, fontSize = 11.sp) } }
        else parsedPalettes.forEach { (name, colors, originalJson) -> val cardShape = RoundedCornerShape(16.dp); Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).then(if (isGoldMode) Modifier.goldBorder(cardShape) else Modifier), shape = cardShape, colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), border = if (isGoldMode) null else BorderStroke(1.dp, borderColor)) { Column(modifier = Modifier.padding(14.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(name.replace(".css", ""), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isDarkMode) Color.White else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${colors.size} COLORES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp) }; IconButton(onClick = { onDeletePalette(originalJson) }, modifier = Modifier.size(32.dp).background(Color.Red.copy(0.1f), CircleShape)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.8f), modifier = Modifier.size(16.dp)) } } ; Spacer(Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(8.dp)) else Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp)))) { colors.forEach { colorHex -> val color = ColorManager.hexToColor(colorHex) ?: Color.Gray; Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color).clickable { onColorSelect(colorHex) }) } } } } }
    }
}
