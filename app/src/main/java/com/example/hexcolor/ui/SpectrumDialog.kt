package com.example.hexcolor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hexcolor.ColorManager
import com.example.hexcolor.R
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumDialog(
    isDarkMode: Boolean,
    isGoldMode: Boolean,
    uiAccentColor: Color,
    onDismiss: () -> Unit,
    onSelectColor: (Color) -> Unit
) {
    var wavelength by remember { mutableFloatStateOf(520.0f) }
    var textInput by remember { mutableStateOf("520.0") }
    val focusManager = LocalFocusManager.current

    val currentColor = remember(wavelength) {
        ColorManager.wavelengthToColor(wavelength)
    }

    val dialogBg = if (isDarkMode) Color.Black else Color(0xFFF2F4F7)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val buttonShape = RoundedCornerShape(14.dp)
    val cardShape = RoundedCornerShape(20.dp)

    // Gradient representing the visible spectrum 380nm..780nm
    val spectrumGradientColors = remember {
        listOf(
            ColorManager.wavelengthToColor(380f),
            ColorManager.wavelengthToColor(440f),
            ColorManager.wavelengthToColor(490f),
            ColorManager.wavelengthToColor(510f),
            ColorManager.wavelengthToColor(580f),
            ColorManager.wavelengthToColor(645f),
            ColorManager.wavelengthToColor(780f)
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 16.dp)
            .shadow(24.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(dialogBg)
            .then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(28.dp)) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if (isGoldMode) Color(0xFFC29B47) else uiAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.spectrum_title),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = textColor
                        ),
                        modifier = if (isGoldMode) Modifier.goldMask() else Modifier
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Close, null, tint = textColor.copy(0.6f))
                }
            }

            // Input field for wavelength (nm)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.wavelength) + ":",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        textInput = input
                        val parsed = input.toFloatOrNull()
                        if (parsed != null && parsed in 380f..780f) {
                            wavelength = parsed
                        }
                    },
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDarkMode) Color(0xFF1E1E2E) else Color.White,
                        unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E2E) else Color.White,
                        focusedBorderColor = if (isGoldMode) Color(0xFFC29B47) else uiAccentColor,
                        unfocusedBorderColor = if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.15f)
                    )
                )
            }

            // Spectrum Slider bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(spectrumGradientColors),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (isGoldMode) GoldGradient else SolidColor(if (isDarkMode) Color.White.copy(0.25f) else Color.Black.copy(0.15f)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = wavelength,
                        onValueChange = {
                            wavelength = (it * 10f).roundToInt() / 10f
                            textInput = String.format(Locale.US, "%.1f", wavelength)
                        },
                        valueRange = 380f..780f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("380 nm (UV)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("780 nm (IR)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            // Color Display Box
            val hexCode = ColorManager.colorToHex(currentColor)
            val rInt = (currentColor.red * 255f).roundToInt().coerceIn(0, 255)
            val gInt = (currentColor.green * 255f).roundToInt().coerceIn(0, 255)
            val bInt = (currentColor.blue * 255f).roundToInt().coerceIn(0, 255)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .shadow(8.dp, cardShape)
                    .clip(cardShape)
                    .background(currentColor)
                    .then(if (isGoldMode) Modifier.goldBorder(cardShape) else Modifier.border(1.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f), cardShape)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))
                )
                val isDarkBg = ColorManager.isDark(currentColor)
                val boxTextColor = if (isDarkBg) Color.White else Color.Black

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "HEX: $hexCode",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = boxTextColor
                        )
                    )
                    Text(
                        text = "RGB: ($rInt, $gInt, $bInt)",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = boxTextColor.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            // Action Button: Use Color
            Surface(
                onClick = {
                    onSelectColor(currentColor)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(4.dp, buttonShape),
                shape = buttonShape,
                color = if (isGoldMode) Color.Transparent else uiAccentColor,
                border = BorderStroke(1.dp, Color.White.copy(0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isGoldMode) Modifier.goldButtonStyle()
                            else Modifier.background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(0.2f), Color.Transparent)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.use_color).uppercase(),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)
                        )
                    )
                }
            }
        }
    }
}
