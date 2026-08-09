package com.example.hexcolor.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hexcolor.ColorManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(harmonyColors: List<Color>, onDismiss: () -> Unit, currentColor: Color, isGoldMode: Boolean, isDarkMode: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Obtenemos el idioma actual para los mensajes
    val currentLocale = remember { java.util.Locale.getDefault().language }
    var exportFormat by remember { mutableStateOf("css") }

    // Launcher para guardar archivos (SAF)
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (exportFormat == "json") "application/json" else if (exportFormat == "xml") "text/xml" else "text/css"
        )
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val content = when (exportFormat) {
                            "css" -> harmonyColors.mapIndexed { i, c -> "--color-${i + 1}: ${ColorManager.colorToHex(c)};" }.joinToString("\n")
                            "json" -> "[\n" + harmonyColors.joinToString(",\n") { "  \"${ColorManager.colorToHex(it)}\"" } + "\n]"
                            "xml" -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" + harmonyColors.mapIndexed { i, c -> "    <color name=\"palette_${i + 1}\">${ColorManager.colorToHex(c)}</color>" }.joinToString("\n") + "\n</resources>"
                            else -> ""
                        }
                        outputStream.write(content.toByteArray())
                    }
                    val successMsg = if (currentLocale == "es") "Archivo guardado con éxito" else "File saved successfully"
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                    onDismiss()
                } catch (e: Exception) {
                    val errorMsg = if (currentLocale == "es") "Error al guardar" else "Error saving file"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(24.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDarkMode) Color.Black else (if (ColorManager.isDark(currentColor)) Color(0xFF1A1A1A) else Color.White))
            .then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(24.dp)) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "EXPORT PALETTE",
                style = TextStyle(fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp, color = if (isDarkMode) Color.White else (if (ColorManager.isDark(currentColor)) Color.White else Color.Black)),
                modifier = if (isGoldMode) Modifier.goldMask() else Modifier
            )

            // Selector de formato más bonito
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                val formats = listOf(
                    Triple("CSS Variables", "css", "palette.css"),
                    Triple("JSON Array", "json", "palette.json"),
                    Triple("Android XML", "xml", "colors.xml")
                )
                
                formats.forEach { (label, ext, defaultName) ->
                    Surface(
                        onClick = {
                            exportFormat = ext
                            createFileLauncher.launch(defaultName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isGoldMode) Color.Transparent else currentColor,
                        border = BorderStroke(1.dp, Color.White.copy(0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (ext == "css") Icons.Default.Code else if (ext == "json") Icons.Default.DataArray else Icons.Default.Android,
                                    contentDescription = null,
                                    tint = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(currentColor)) Color.White else Color.Black),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    label,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(currentColor)) Color.White else Color.Black)
                                )
                            }
                        }
                    }
                }
            }
            
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}
