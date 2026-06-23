package com.example.hexcolor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.paint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hexcolor.ui.theme.HexColorTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

// DataStore for persistence
val Context.dataStore by preferencesDataStore(name = "favorites")

enum class HarmonyMode { COMPLEMENTARY, TRIADIC, ANALOGOUS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val colorBlindnessKey = remember { stringPreferencesKey("color_blindness_mode") }
            val colorBlindnessMode by context.dataStore.data.map { it[colorBlindnessKey] ?: "None" }.collectAsState(initial = "None")
            
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            HexColorTheme(darkTheme = isDarkMode, colorBlindnessMode = colorBlindnessMode) {
                HexColorApp(isDarkMode = isDarkMode, onToggleDarkMode = { isDarkMode = !isDarkMode })
            }
        }
    }

    fun checkOverlayPermission(context: Context, onGranted: () -> Unit) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            context.startActivity(intent)
            Toast.makeText(context, getString(R.string.overlay_permission), Toast.LENGTH_LONG).show()
        } else {
            onGranted()
        }
    }
}

// Color Utility Functions
object ColorUtils {
    fun hexToColor(hex: String): Color? {
        return try {
            val h = if (hex.startsWith("#")) hex else "#$hex"
            Color(h.toColorInt())
        } catch (_: Exception) {
            null
        }
    }

    fun colorToHex(color: Color): String {
        return String.format(Locale.US, "#%06X", (0xFFFFFF and color.toArgb()))
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

data class ColorItem(val title: String, val color: Color)

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
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8C6221), Color(0xFFFFF3A8), Color(0xFFC29B47)),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            )
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

fun Modifier.goldButtonStyle() = this
    .background(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8C6221), Color(0xFFFFF3A8), Color(0xFFC29B47)),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        ),
        shape = RoundedCornerShape(8.dp)
    )
    .border(1.2.dp, Color(0xFFF3E5AB), RoundedCornerShape(8.dp))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HexColorApp(isDarkMode: Boolean, onToggleDarkMode: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val favoritesKey = remember { stringSetPreferencesKey("fav_colors") }
    val palettesKey = remember { stringSetPreferencesKey("user_palettes") } // Almacena paletas como JSON: {"name": "X", "colors": ["#111", "#222"]}
    val colorBlindnessKey = remember { stringPreferencesKey("color_blindness_mode") }
    val caosModeKey = remember { booleanPreferencesKey("caos_mode") }
    val goldModeKey = remember { booleanPreferencesKey("gold_mode") }
    val analogousCountKey = remember { intPreferencesKey("analogous_count") }
    val fixedUiColorKey = remember { stringPreferencesKey("fixed_ui_color") }

    val settingsFlow = remember { 
        context.dataStore.data.map { prefs ->
            val caos = prefs[caosModeKey] ?: true
            val gold = prefs[goldModeKey] ?: false
            val count = prefs[analogousCountKey] ?: 7
            val hex = prefs[fixedUiColorKey] ?: "#268CEF"
            val blind = prefs[colorBlindnessKey] ?: "None"
            listOf(caos, count, hex, blind, gold)
        } 
    }
    val settings by settingsFlow.collectAsState(initial = listOf(true, 7, "#268CEF", "None", false))
    val isCaosMode = settings[0] as Boolean
    val analogousCount = settings[1] as Int
    val fixedUiColorHex = settings[2] as String
    val colorBlindnessMode = settings[3] as String
    val isGoldMode = settings[4] as Boolean
    val fixedUiColor = ColorUtils.hexToColor(fixedUiColorHex) ?: Color(0xFF268CEF)
    
    var hexInput by remember { mutableStateOf("#21DD10") }
    var currentColor by remember { mutableStateOf(Color(0xFF21DD10)) }
    
    var hsvValue by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
        mutableStateOf(hsv)
    }

    var pickerBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedPickerColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    var harmonyMode by remember { mutableStateOf(HarmonyMode.COMPLEMENTARY) }
    var isSniperMode by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val uiAccentColorRaw = if (isCaosMode) {
        // Para la UI usamos el color actual pero siempre con brillo máximo para que no se apague la interfaz
        ColorUtils.hsvToColor(hsvValue[0], hsvValue[1], 1f)
    } else fixedUiColor
    val uiAccentColor = if (colorBlindnessMode == "None") uiAccentColorRaw else ColorUtils.simulateColorBlindness(uiAccentColorRaw, colorBlindnessMode)
    val statusView = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val window = (context as android.app.Activity).window
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, statusView)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }
    
    var currentLocale by remember { mutableStateOf(Locale.getDefault().language) }
    
    fun toggleLanguage() {
        val newLang = if (currentLocale == "es") "en" else "es"
        val locale = Locale.forLanguageTag(newLang)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        currentLocale = newLang
        if (context is MainActivity) context.recreate()
    }

    val favoritesFlow = remember { context.dataStore.data.map { it[favoritesKey] ?: emptySet() } }
    val favorites by favoritesFlow.collectAsState(initial = emptySet())
    
    val palettesFlow = remember { context.dataStore.data.map { it[palettesKey] ?: emptySet() } }
    val savedPalettes by palettesFlow.collectAsState(initial = emptySet())

    val harmonyColors = remember(currentColor, harmonyMode, analogousCount) {
        when (harmonyMode) {
            HarmonyMode.COMPLEMENTARY -> listOf(currentColor, ColorUtils.getComplementary(currentColor))
            HarmonyMode.TRIADIC -> ColorUtils.getTriadic(currentColor)
            HarmonyMode.ANALOGOUS -> ColorUtils.getAnalogous(currentColor, analogousCount)
        }
    }

    LaunchedEffect(currentColor, harmonyMode, harmonyColors, isDarkMode) {
        if (FloatingService.isRunning) {
            FloatingService.currentHex = ColorUtils.colorToHex(currentColor)
            FloatingService.currentHarmony = harmonyColors.map { ColorUtils.colorToHex(it) }
            FloatingService.isDarkMode = isDarkMode
            FloatingService.originalIndex = if (harmonyMode == HarmonyMode.ANALOGOUS) analogousCount / 2 else 0
            context.startService(Intent(context, FloatingService::class.java))
        }
    }

    val colorItems = remember(currentColor, analogousCount) {
        val comp = ColorUtils.getComplementary(currentColor)
        val analogous = ColorUtils.getAnalogous(currentColor, analogousCount)
        val triadic = ColorUtils.getTriadic(currentColor)
        val list = mutableListOf(
            ColorItem("① " + context.getString(R.string.original), currentColor),
            ColorItem("② " + context.getString(R.string.complementary), comp),
        )
        if (analogous.size >= 3) {
            list.add(ColorItem("③ " + context.getString(R.string.analogous), analogous[analogous.size / 2 - 1]))
            list.add(ColorItem("④ " + context.getString(R.string.analogous), analogous[analogous.size / 2 + 1]))
        }
        list.add(ColorItem("⑤ " + context.getString(R.string.triadic), triadic[1]))
        list.add(ColorItem("⑥ " + context.getString(R.string.triadic), triadic[2]))
        list.toList()
    }

    val pagerState = rememberPagerState(pageCount = { 4 })

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val fileName = it.path?.substringAfterLast("/") ?: "Imported"
                    val content = stream.bufferedReader().readText()
                    val hexRegex = Regex("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})")
                    val matches = hexRegex.findAll(content).map { m -> m.value.uppercase() }.distinct().toList()
                    if (matches.isNotEmpty()) {
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                val current = prefs[palettesKey] ?: emptySet()
                                // Guardamos como un "lote" tipo lista de reproducción
                                val newPaletteJson = "{\"name\":\"$fileName\", \"colors\":${matches.map { "\"$it\"" }}}"
                                prefs[palettesKey] = current + newPaletteJson
                            }
                            Toast.makeText(context, context.getString(R.string.import_success) + ": ${matches.size}", Toast.LENGTH_SHORT).show()
                            pagerState.animateScrollToPage(2) 
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.import_error), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.import_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFE0E6ED),
                drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = if (isGoldMode) R.drawable.logo_oro else R.drawable.logo_nuevo1),
                            contentDescription = "Logo",
                            modifier = Modifier.size(75.dp)
                        )
                        Image(
                            painter = painterResource(id = if (isGoldMode) R.drawable.hexcolor_oro else R.drawable.hexcolor),
                            contentDescription = "HexColor Text",
                            modifier = Modifier.height(32.dp).padding(top = 8.dp),
                            contentScale = ContentScale.FillHeight
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE))
                    
                    val itemModifier = Modifier.height(48.dp).padding(horizontal = 8.dp)
                    val labelStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    // --- BOTÓN PREMIUM ORO ---
                    Surface(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = itemModifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .goldButtonStyle(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF543B14), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.premium), style = labelStyle, color = Color(0xFF543B14))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))

                    // --- MODO SNIPER ORO ---
                    Surface(
                        onClick = { scope.launch { drawerState.close(); Toast.makeText(context, "¡Hazme Premium Bro! 🎯", Toast.LENGTH_LONG).show() } },
                        modifier = itemModifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .goldButtonStyle(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color(0xFF543B14), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.sniper_mode), style = labelStyle, color = Color(0xFF543B14))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    NavigationDrawerItem(label = { Text(stringResource(R.string.palette), style = labelStyle) }, selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0); drawerState.close() } }, icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 0) Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100)) else Modifier))
                    NavigationDrawerItem(label = { Text(stringResource(R.string.wheel), style = labelStyle) }, selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.animateScrollToPage(1); drawerState.close() } }, icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 1) Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100)) else Modifier))
                    NavigationDrawerItem(label = { Text(stringResource(R.string.picker), style = labelStyle) }, selected = pagerState.currentPage == 2, onClick = { scope.launch { pagerState.animateScrollToPage(2); drawerState.close() } }, icon = { Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 2) Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100)) else Modifier))
                    NavigationDrawerItem(label = { Text(stringResource(R.string.favorites), style = labelStyle) }, selected = pagerState.currentPage == 3, onClick = { scope.launch { pagerState.animateScrollToPage(3); drawerState.close() } }, icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 3) Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100)) else Modifier))
                    NavigationDrawerItem(label = { Text(stringResource(R.string.import_palette), style = labelStyle) }, selected = false, onClick = { scope.launch { drawerState.close(); importLauncher.launch("text/css") } }, icon = { Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent), modifier = itemModifier)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE))
                    NavigationDrawerItem(label = { Text(stringResource(R.string.settings), style = labelStyle) }, selected = false, onClick = { scope.launch { showSettingsDialog = true; drawerState.close() } }, icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent), modifier = itemModifier)
                    NavigationDrawerItem(label = { Text(stringResource(R.string.rate_us), style = labelStyle) }, selected = false, onClick = { scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent), modifier = itemModifier)
                    NavigationDrawerItem(label = { Text(stringResource(R.string.help), style = labelStyle) }, selected = false, onClick = { scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent), modifier = itemModifier)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF2F4F7),
            topBar = {
                Column(modifier = Modifier.background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF2F4F7)).statusBarsPadding()) {
                    // Row 1: Menú + Logo + Título
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = if (isDarkMode) Color.Gray else Color.Black)
                        }
                        Spacer(Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = if (isGoldMode) R.drawable.logo_oro else R.drawable.logo_nuevo1),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = if (isGoldMode) R.drawable.hexcolor_oro else R.drawable.hexcolor),
                            contentDescription = "HexColor Text",
                            modifier = Modifier.height(28.dp),
                            contentScale = ContentScale.FillHeight
                        )
                    }

                    // Franja decorativa superior (Efecto Metal Pulido)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)))
                    )
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(
                            Color.Transparent, 
                            Color(0xFF808080), 
                            Color(0xFFE0E0E0), 
                            Color(0xFFFFFFFF), 
                            Color(0xFFE0E0E0), 
                            Color(0xFF808080), 
                            Color.Transparent
                        )))
                    )

                    // Row 2: Botones de navegación (Tabs - Estáticos)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(R.string.palette, R.string.wheel, R.string.picker, R.string.favorites)
                        tabs.forEachIndexed { index, resId ->
                            val isSelected = pagerState.currentPage == index
                            val shape = RoundedCornerShape(12.dp)
                            val baseColor = if (isDarkMode) Color.Black else Color.White
                            
                            Surface(
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                modifier = Modifier.weight(1f).height(38.dp).shadow(if(isSelected) 6.dp else 2.dp, shape),
                                shape = shape,
                                color = if (isSelected) {
                                    if (isGoldMode) Color.Transparent else uiAccentColor
                                } else {
                                    if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFD1D5D8)
                                },
                                border = BorderStroke(1.dp, if (isSelected) Color.White.copy(0.5f) else (if (isDarkMode) Color.White.copy(0.15f) else Color.Black.copy(0.1f)))
                            ) {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (isSelected && isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(resId).uppercase(),
                                        color = if (isSelected) {
                                            if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black)
                                        } else Color.Gray,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.sp,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    // Borde plata profesional separador (Efecto Metal Pulido)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)))
                    )
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(
                            Color.Transparent, 
                            Color(0xFF808080), 
                            Color(0xFFE0E0E0), 
                            Color(0xFFFFFFFF), 
                            Color(0xFFE0E0E0), 
                            Color(0xFF808080), 
                            Color.Transparent
                        )))
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) { page ->
                    when (page) {
                        0 -> PaletteScreen(
                            isDarkMode, 
                            hexInput, 
                            { hexInput = it }, 
                            currentColor, 
                            { 
                                currentColor = it
                                hexInput = ColorUtils.colorToHex(it)
                                val h = FloatArray(3)
                                android.graphics.Color.colorToHSV(it.toArgb(), h)
                                hsvValue = h 
                            }, 
                            hsvValue, 
                            { hsvValue = it; currentColor = ColorUtils.hsvToColor(it[0], it[1], it[2]); hexInput = ColorUtils.colorToHex(currentColor) }, 
                            colorItems, 
                            { color -> 
                                val hex = ColorUtils.colorToHex(if (colorBlindnessMode == "None") color else ColorUtils.simulateColorBlindness(color, colorBlindnessMode))
                                scope.launch { 
                                    context.dataStore.edit { prefs -> 
                                        val current = prefs[favoritesKey] ?: emptySet()
                                        prefs[favoritesKey] = current + hex 
                                    }
                                    Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show()
                                } 
                            }, 
                            { color -> 
                                val displayColor = if (colorBlindnessMode == "None") color else ColorUtils.simulateColorBlindness(color, colorBlindnessMode)
                                val hex = ColorUtils.colorToHex(displayColor)
                                val rgb = "(${String.format(Locale.US, "%.2f", displayColor.red)}, ${String.format(Locale.US, "%.2f", displayColor.green)}, ${String.format(Locale.US, "%.2f", displayColor.blue)}, 1)"
                                clipboardManager.setText(AnnotatedString("$hex $rgb"))
                                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show() 
                            }, 
                            isSniperMode, 
                            { isSniperMode = !isSniperMode }, 
                            uiAccentColor, 
                            colorBlindnessMode,
                            isGoldMode
                        )
                        1 -> WheelScreen(
                            isDarkMode, 
                            onToggleDarkMode, 
                            currentColor, 
                            { 
                                currentColor = it
                                hexInput = ColorUtils.colorToHex(it)
                                val h = FloatArray(3)
                                android.graphics.Color.colorToHSV(it.toArgb(), h)
                                hsvValue = h 
                            }, 
                            { color -> 
                                val displayColor = if (colorBlindnessMode == "None") color else ColorUtils.simulateColorBlindness(color, colorBlindnessMode)
                                val hex = ColorUtils.colorToHex(displayColor)
                                val rgb = "(${String.format(Locale.US, "%.2f", displayColor.red)}, ${String.format(Locale.US, "%.2f", displayColor.green)}, ${String.format(Locale.US, "%.2f", displayColor.blue)}, 1)"
                                clipboardManager.setText(AnnotatedString("$hex $rgb"))
                                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show() 
                            }, 
                            harmonyMode, 
                            { harmonyMode = it }, 
                            harmonyColors, 
                            hsvValue, 
                            analogousCount, 
                            { v -> val newHsv = hsvValue.clone().apply { this[2] = v }; hsvValue = newHsv; currentColor = ColorUtils.hsvToColor(newHsv[0], newHsv[1], newHsv[2]); hexInput = ColorUtils.colorToHex(currentColor) }, 
                            { scope.launch { pagerState.animateScrollToPage(3) } }, 
                            currentLocale, 
                            uiAccentColor, 
                            colorBlindnessMode, 
                            { blind -> scope.launch { context.dataStore.edit { it[colorBlindnessKey] = blind } } },
                            isGoldMode
                        )
                        2 -> PickerScreen(
                            isDarkMode = isDarkMode,
                            bitmap = pickerBitmap,
                            onBitmapChange = { pickerBitmap = it },
                            detectedColors = detectedPickerColors,
                            onDetectedColorsChange = { detectedPickerColors = it },
                            onColorSelect = { 
                                val selected = if (colorBlindnessMode == "None") it else ColorUtils.simulateColorBlindness(it, colorBlindnessMode)
                                currentColor = selected
                                hexInput = ColorUtils.colorToHex(selected)
                                val h = FloatArray(3)
                                android.graphics.Color.colorToHSV(selected.toArgb(), h)
                                hsvValue = h
                                scope.launch { pagerState.animateScrollToPage(1) } 
                            },
                            uiAccentColor = uiAccentColor,
                            colorBlindnessMode = colorBlindnessMode,
                            onSaveFavorite = { color -> 
                                val hex = ColorUtils.colorToHex(if (colorBlindnessMode == "None") color else ColorUtils.simulateColorBlindness(color, colorBlindnessMode))
                                scope.launch { 
                                    context.dataStore.edit { prefs -> 
                                        val current = prefs[favoritesKey] ?: emptySet()
                                        prefs[favoritesKey] = current + hex 
                                    }
                                    Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show()
                                } 
                            },
                            onCopyColor = { color -> 
                                val displayColor = if (colorBlindnessMode == "None") color else ColorUtils.simulateColorBlindness(color, colorBlindnessMode)
                                val hex = ColorUtils.colorToHex(displayColor)
                                val rgb = "(${String.format(Locale.US, "%.2f", displayColor.red)}, ${String.format(Locale.US, "%.2f", displayColor.green)}, ${String.format(Locale.US, "%.2f", displayColor.blue)}, 1)"
                                clipboardManager.setText(AnnotatedString("$hex $rgb"))
                                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                            },
                            isGoldMode = isGoldMode
                        )
                        3 -> FavoritesScreen(isDarkMode, favorites, savedPalettes, { favHex -> val favColor = ColorUtils.hexToColor(favHex); if (favColor != null) { currentColor = favColor; hexInput = favHex; val h = FloatArray(3); android.graphics.Color.colorToHSV(favColor.toArgb(), h); hsvValue = h; scope.launch { pagerState.animateScrollToPage(1) } } }, { favHex -> scope.launch { context.dataStore.edit { prefs -> val current = prefs[favoritesKey] ?: emptySet(); prefs[favoritesKey] = current - favHex }; Toast.makeText(context, context.getString(R.string.deleted), Toast.LENGTH_SHORT).show() } }, { paletteJson -> scope.launch { context.dataStore.edit { prefs -> val current = prefs[palettesKey] ?: emptySet(); prefs[palettesKey] = current - paletteJson } } })
                    }
                }
            }
        }
    }
    if (showSettingsDialog) {
        SettingsDialog(isDarkMode, onToggleDarkMode, currentLocale, { toggleLanguage() }, isCaosMode, analogousCount, fixedUiColorHex, colorBlindnessMode, favorites, { showSettingsDialog = false }, { caos, count, hex, blind, gold -> scope.launch { context.dataStore.edit { prefs -> prefs[caosModeKey] = caos; prefs[analogousCountKey] = count; prefs[fixedUiColorKey] = hex; prefs[colorBlindnessKey] = blind; prefs[goldModeKey] = gold } } }, isGoldMode)
    }
}

@Composable
fun PickerScreen(
    isDarkMode: Boolean, 
    bitmap: Bitmap?, 
    onBitmapChange: (Bitmap?) -> Unit, 
    detectedColors: List<Color>, 
    onDetectedColorsChange: (List<Color>) -> Unit, 
    onColorSelect: (Color) -> Unit, 
    uiAccentColor: Color, 
    colorBlindnessMode: String,
    onSaveFavorite: (Color) -> Unit,
    onCopyColor: (Color) -> Unit,
    isGoldMode: Boolean
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val b = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { d, _, _ -> d.isMutableRequired = true }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
            } catch (e: Exception) {
                null
            }
            onBitmapChange(b)
            onDetectedColorsChange(emptyList())
        }
    }
    
    val buttonShape = RoundedCornerShape(12.dp); val fineBorder = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8))
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.picker).uppercase(), style = TextStyle(color = uiAccentColor, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp))
                Box(modifier = Modifier.height(300.dp).fillMaxWidth().shadow(8.dp, buttonShape).clip(buttonShape).background(if (isDarkMode) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)).border(fineBorder, buttonShape), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Canvas(modifier = Modifier.fillMaxSize().pointerInput(bitmap) { detectTapGestures { offset -> bitmap.let { b -> val cW = size.width.toFloat(); val cH = size.height.toFloat(); val bW = b.width.toFloat(); val bH = b.height.toFloat(); val s = min(cW / bW, cH / bH); val dx = (cW - bW * s) / 2; val dy = (cH - bH * s) / 2; val x = ((offset.x - dx) / s).toInt(); val y = ((offset.y - dy) / s).toInt(); if (x in 0 until b.width && y in 0 until b.height) onColorSelect(Color(b.getPixel(x, y))) } } }) {
                            bitmap.let { b -> val cW = size.width; val cH = size.height; val bW = b.width.toFloat(); val bH = b.height.toFloat(); val s = min(cW / bW, cH / bH); val dx = (cW - bW * s) / 2; val dy = (cH - bH * s) / 2; drawImage(image = b.asImageBitmap(), dstOffset = IntOffset(dx.toInt(), dy.toInt()), dstSize = IntSize((bW * s).toInt(), (bH * s).toInt())) }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(64.dp), tint = Color.Gray); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.no_image), color = Color.Gray) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(50.dp).shadow(4.dp, buttonShape),
                        shape = buttonShape,
                        color = if (isGoldMode) Color.Transparent else uiAccentColor,
                        border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f))
                    ) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.select_image), fontWeight = FontWeight.Bold, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black))
                        }
                    }
                    
                    if (bitmap != null) {
                        Surface(
                            onClick = { 
                                Palette.from(bitmap).generate { p -> 
                                    onDetectedColorsChange(listOfNotNull(p?.vibrantSwatch?.rgb, p?.lightVibrantSwatch?.rgb, p?.darkVibrantSwatch?.rgb, p?.mutedSwatch?.rgb, p?.lightMutedSwatch?.rgb, p?.darkMutedSwatch?.rgb).map { Color(it) }.distinct()) 
                                } 
                            },
                            modifier = Modifier.weight(1f).height(50.dp).shadow(4.dp, buttonShape),
                            shape = buttonShape,
                            color = Color.Transparent
                        ) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .goldButtonStyle(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.detect_colors), fontWeight = FontWeight.ExtraBold, color = Color(0xFF543B14))
                            }
                        }
                    }
                }
                
                if (detectedColors.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Colores Detectados", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Surface(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.size(42.dp).shadow(2.dp, CircleShape),
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFEEEEEE),
                            border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.15f) else Color.Black.copy(0.05f))
                        ) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Share, "Export Palette", tint = uiAccentColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        
        items(detectedColors) { color ->
            ColorCard(
                isDarkMode = isDarkMode,
                item = ColorItem("Extraído", color),
                colorBlindnessMode = colorBlindnessMode,
                onLongClick = { onSaveFavorite(color) },
                onClick = { onColorSelect(color) }
            )
        }
    }
    
    if (showExportDialog) ExportDialog(harmonyColors = detectedColors, onDismiss = { showExportDialog = false }, currentColor = uiAccentColor)
}

@Composable
fun PaletteScreen(isDarkMode: Boolean, hexInput: String, onHexChange: (String) -> Unit, currentColor: Color, onColorChange: (Color) -> Unit, hsvValue: FloatArray, onHsvChange: (FloatArray) -> Unit, colorItems: List<ColorItem>, onSaveFavorite: (Color) -> Unit, onCopyColor: (Color) -> Unit, isSniperMode: Boolean, onSniperToggle: () -> Unit, uiAccentColor: Color, colorBlindnessMode: String, isGoldMode: Boolean) {
    val context = LocalContext.current
    val buttonShape = RoundedCornerShape(12.dp)
    // Borde exterior más trabajado (un pelín más grueso y definido)
    val fineBorder = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8))
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) onSniperToggle() else Toast.makeText(context, "Permiso necesario", Toast.LENGTH_SHORT).show() }
    
    val hueGradientColors = remember(colorBlindnessMode) {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red).map {
            if (colorBlindnessMode == "None") it else ColorUtils.simulateColorBlindness(it, colorBlindnessMode)
        }
    }

    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (isSniperMode) { Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(buttonShape)) { CameraSniper(onColorCaptured = onColorChange, onColorConfirmed = { onColorChange(it); Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show() }); IconButton(onClick = onSniperToggle, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) { Icon(Icons.Default.Close, "Close Sniper", tint = Color.White) } } }
                
                Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hexInput, 
                        onValueChange = onHexChange, 
                        modifier = Modifier.weight(1f).fillMaxHeight().border(fineBorder, buttonShape), 
                        placeholder = { Text("#RRGGBB", fontSize = 14.sp, color = Color.Gray) }, 
                        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), 
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkMode) Color(0xFF0A0A0A) else Color(0xFFF2F4F7),
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF0A0A0A) else Color(0xFFF2F4F7),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedBorderColor = uiAccentColor.copy(0.5f),
                            unfocusedBorderColor = Color.Transparent
                        ), 
                        shape = buttonShape, 
                        singleLine = true, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    
                    Surface(
                        onClick = { val color = ColorUtils.hexToColor(hexInput); if (color != null) onColorChange(color) else Toast.makeText(context, context.getString(R.string.invalid_hex), Toast.LENGTH_SHORT).show() }, 
                        modifier = Modifier.width(85.dp).fillMaxHeight().shadow(4.dp, buttonShape), 
                        shape = buttonShape, 
                        color = if (isGoldMode) Color.Transparent else uiAccentColor,
                        border = BorderStroke(1.dp, Color.White.copy(0.4f))
                    ) { 
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.show), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black)) 
                        }
                    }
                    
                    Surface(
                        onClick = { if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) onSniperToggle() else permissionLauncher.launch(android.Manifest.permission.CAMERA) }, 
                        modifier = Modifier.size(50.dp).shadow(4.dp, buttonShape),
                        shape = buttonShape,
                        color = if (isGoldMode) Color.Transparent else uiAccentColor,
                        border = BorderStroke(1.dp, Color.White.copy(0.4f))
                    ) { 
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, "Sniper", tint = if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(hueGradientColors), 
                            RoundedCornerShape(16.dp)
                        )
                        .border(fineBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { 
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .border(1.2.dp, Color.Black.copy(0.3f), RoundedCornerShape(16.dp)) // Borde de seguridad oscuro
                            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent)), RoundedCornerShape(16.dp))
                    )
                    Slider(
                        value = hsvValue[0], 
                        onValueChange = { onHsvChange(floatArrayOf(it, hsvValue[1], hsvValue[2])) }, 
                        valueRange = 0f..360f, 
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White, 
                            activeTrackColor = Color.Transparent, 
                            inactiveTrackColor = Color.Transparent
                        ), 
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { onColorChange(Color((0..255).random() / 255f, (0..255).random() / 255f, (0..255).random() / 255f)) }, 
                        modifier = Modifier.weight(1f).fillMaxHeight().shadow(4.dp, buttonShape), 
                        shape = buttonShape, 
                        color = if (isGoldMode) Color.Transparent else uiAccentColor,
                        border = BorderStroke(1.dp, Color.White.copy(0.4f))
                    ) { 
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.random), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black)) 
                        }
                    }
                    Surface(
                        onClick = { onColorChange(ColorUtils.getComplementary(currentColor)) }, 
                        modifier = Modifier.weight(1f).fillMaxHeight().shadow(4.dp, buttonShape), 
                        shape = buttonShape, 
                        color = if (isGoldMode) Color.Transparent else uiAccentColor,
                        border = BorderStroke(1.dp, Color.White.copy(0.4f))
                    ) { 
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.complementary), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black))
                        }
                    }
                }
            }
        }
        items(colorItems) { item -> ColorCard(isDarkMode = isDarkMode, item = item, colorBlindnessMode = colorBlindnessMode, onLongClick = { onSaveFavorite(item.color) }, onClick = { onCopyColor(item.color) }) }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, currentColor: Color, onColorSelect: (Color) -> Unit, onCopyColor: (Color) -> Unit, harmonyMode: HarmonyMode, onModeChange: (HarmonyMode) -> Unit, harmonyColors: List<Color>, hsvValue: FloatArray, analogousCount: Int, onValueChange: (Float) -> Unit, onNavigateToFavorites: () -> Unit, currentLocale: String, uiAccentColor: Color, colorBlindnessMode: String, onColorBlindnessChange: (String) -> Unit, isGoldMode: Boolean) {
    val activity = LocalContext.current as? MainActivity
    val modes = listOf(HarmonyMode.COMPLEMENTARY, HarmonyMode.TRIADIC, HarmonyMode.ANALOGOUS)
    var cardOffset by remember { mutableStateOf(Offset(0f, 0f)) }; var showExportDialog by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp; val scrollState = rememberScrollState()
        if (isWide) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) { WheelContent(isDarkMode, colorBlindnessMode, onColorBlindnessChange, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, onToggleDarkMode, onNavigateToFavorites, uiAccentColor) }
                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) { HeaderControls(modes, harmonyMode, onModeChange, isDarkMode, { showExportDialog = true }, uiAccentColor, hsvValue[2], isGoldMode); BrightnessSlider(currentLocale, hsvValue[2], onValueChange, uiAccentColor); InfoCard(isDarkMode, currentColor, harmonyColors, onCopyColor, activity, harmonyMode, cardOffset, { cardOffset += it }, uiAccentColor, colorBlindnessMode) }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HeaderControls(modes, harmonyMode, onModeChange, isDarkMode, { showExportDialog = true }, uiAccentColor, hsvValue[2], isGoldMode)
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).widthIn(max = 450.dp), contentAlignment = Alignment.Center) { WheelContent(isDarkMode, colorBlindnessMode, onColorBlindnessChange, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, onToggleDarkMode, onNavigateToFavorites, uiAccentColor) }
                BrightnessSlider(currentLocale, hsvValue[2], onValueChange, uiAccentColor)
                InfoCard(isDarkMode, currentColor, harmonyColors, onCopyColor, activity, harmonyMode, cardOffset, { cardOffset += it }, uiAccentColor, colorBlindnessMode)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
    if (showExportDialog) ExportDialog(harmonyColors = harmonyColors, onDismiss = { showExportDialog = false }, currentColor = uiAccentColor)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderControls(modes: List<HarmonyMode>, harmonyMode: HarmonyMode, onModeChange: (HarmonyMode) -> Unit, isDarkMode: Boolean, onExport: () -> Unit, uiAccentColor: Color, brightness: Float, isGoldMode: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.weight(1f).height(42.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.forEachIndexed { index, itemMode ->
                val isSelected = harmonyMode == itemMode
                val shape = RoundedCornerShape(10.dp)
                val label = when(itemMode) { HarmonyMode.COMPLEMENTARY -> stringResource(R.string.complementary); HarmonyMode.TRIADIC -> stringResource(R.string.triadic); HarmonyMode.ANALOGOUS -> stringResource(R.string.analogous) }
                
                Surface(
                    onClick = { onModeChange(itemMode) },
                    modifier = Modifier.weight(1f).fillMaxHeight().shadow(isSelected.let { if (it) 6.dp else 2.dp }, shape),
                    shape = shape,
                    color = if (isSelected) (if (isGoldMode) Color.Transparent else uiAccentColor) else (if (isDarkMode) Color(0xFF111111) else Color(0xFFE0E0E0)),
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(0.5f) else (if (isDarkMode) Color.White.copy(0.15f) else Color.Black.copy(0.1f)))
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .then(if (isSelected && isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.25f), Color.Transparent)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (isSelected) (if (isGoldMode) Color(0xFF543B14) else (if (ColorUtils.isDark(uiAccentColor)) Color.White else Color.Black)) else Color.Gray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Surface(
            onClick = onExport,
            modifier = Modifier.size(42.dp).shadow(2.dp, CircleShape),
            shape = CircleShape,
            color = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFEEEEEE),
            border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.15f) else Color.Black.copy(0.05f))
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Share, "Export", tint = uiAccentColor, modifier = Modifier.size(20.dp)) 
            }
        }
    }
}

@Composable
private fun WheelContent(isDarkMode: Boolean, colorBlindnessMode: String, onColorBlindnessChange: (String) -> Unit, currentColor: Color, hsvValue: FloatArray, harmonyMode: HarmonyMode, analogousCount: Int, onColorSelect: (Color) -> Unit, onCopyColor: (Color) -> Unit, onToggleDarkMode: () -> Unit, onNavigateToFavorites: () -> Unit, uiAccentColor: Color) {
    val brightness = hsvValue[2]; val moonResource = if (brightness > 0.5f) R.drawable.moon_light else R.drawable.moon_shadow
    var showBlindnessMenu by remember { mutableStateOf(false) }
    
    Box(contentAlignment = Alignment.Center) {
        ColorWheel(isDarkMode, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, Modifier.fillMaxSize().padding(40.dp), uiAccentColor, colorBlindnessMode)
        Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) { androidx.compose.animation.Crossfade(targetState = moonResource, animationSpec = tween(400), label = "MoonTransition") { targetResource -> Image(painter = painterResource(id = targetResource), contentDescription = "Moon", modifier = Modifier.fillMaxSize(0.65f), contentScale = ContentScale.Fit) } }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).offset(y = (-11).dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showBlindnessMenu = true }) { 
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "Sim", tint = if (colorBlindnessMode != "None") uiAccentColor else (if (isDarkMode) Color.Gray else Color.DarkGray)) 
                }
                DropdownMenu(expanded = showBlindnessMenu, onDismissRequest = { showBlindnessMenu = false }, containerColor = if (isDarkMode) Color(0xFF262626) else Color.White) { 
                    listOf("None", "Protanopia", "Deuteranopia", "Tritanopia").forEach { mode -> 
                        DropdownMenuItem(
                            text = { Text(mode, color = if (colorBlindnessMode == mode) uiAccentColor else (if (isDarkMode) Color.White else Color.Black)) }, 
                            onClick = { onColorBlindnessChange(mode); showBlindnessMenu = false }
                        ) 
                    } 
                }
            }
            IconButton(onClick = onToggleDarkMode) { Icon(imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = "Theme", tint = if (isDarkMode) uiAccentColor else Color.DarkGray) }
        }
        IconButton(onClick = onNavigateToFavorites, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).offset(y = (-11).dp)) { Icon(imageVector = Icons.Default.Star, contentDescription = "Favorites", tint = if (isDarkMode) uiAccentColor else Color.DarkGray) }
    }
}

@Composable
private fun BrightnessSlider(currentLocale: String, value: Float, onValueChange: (Float) -> Unit, currentColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-11).dp)) {
        Text(if (currentLocale == "es") "Brillo / Sombra" else "Brightness / Shadow", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .background(if (value > 0.5f) Color.White.copy(0.1f) else Color.Black.copy(0.3f), RoundedCornerShape(16.dp))
                .border(1.dp, if (value > 0.5f) Color.White.copy(0.3f) else Color.White.copy(0.15f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Slider(value = value, onValueChange = onValueChange, colors = SliderDefaults.colors(thumbColor = currentColor, activeTrackColor = currentColor, inactiveTrackColor = Color.Gray.copy(0.3f)), modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun InfoCard(isDarkMode: Boolean, currentColor: Color, harmonyColors: List<Color>, onCopyColor: (Color) -> Unit, activity: MainActivity?, harmonyMode: HarmonyMode, offset: Offset, onOffsetChange: (Offset) -> Unit, uiAccentColor: Color, colorBlindnessMode: String) {
    Card(colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF262626).copy(alpha = 0.95f) else Color(0xFFDDDDDD).copy(alpha = 0.95f)), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF333333) else Color(0xFFAAAAAA)), modifier = Modifier.fillMaxWidth().offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt() - 5.dp.toPx().toInt()) }.pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onOffsetChange(dragAmount) } }) {
        Box(modifier = Modifier.padding(12.dp)) {
            IconButton(onClick = { activity?.let { act -> act.checkOverlayPermission(act) { FloatingService.currentHex = ColorUtils.colorToHex(currentColor); FloatingService.currentHarmony = harmonyColors.map { ColorUtils.colorToHex(it) }; FloatingService.isDarkMode = isDarkMode; FloatingService.originalIndex = if (harmonyMode == HarmonyMode.ANALOGOUS) harmonyColors.size / 2 else 0; act.startService(Intent(act, FloatingService::class.java)) } } }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) { Icon(Icons.Default.PictureInPictureAlt, "Floating", tint = uiAccentColor) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(ColorUtils.colorToHex(currentColor).uppercase(), color = if (isDarkMode) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp).border(1.dp, if (isDarkMode) Color(0xFF444444) else Color(0xFFAAAAAA), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                val cW = ColorUtils.getContrastRatio(currentColor, Color.White); val cB = ColorUtils.getContrastRatio(currentColor, Color.Black); val r = max(cW, cB)
                Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Surface(color = if (r >= 4.5) Color(0xFF4CAF50) else Color(0xFFF44336), shape = RoundedCornerShape(4.dp)) { Text(if (r >= 4.5) " WCAG PASS " else " WCAG FAIL ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }; Text("Ratio: ${String.format(Locale.US, "%.1f", r)}:1 (${if (cW > cB) "White" else "Black"})", color = Color.Gray, fontSize = 10.sp) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, contentPadding = PaddingValues(horizontal = 4.dp)) { 
                    items(harmonyColors.size) { index -> 
                        val color = harmonyColors[index]
                        val displayColor = if (colorBlindnessMode == "None") color else ColorUtils.simulateColorBlindness(color, colorBlindnessMode)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(displayColor).border(1.dp, if (isDarkMode) Color(0xFF444444) else Color(0xFFAAAAAA), RoundedCornerShape(8.dp)).clickable { onCopyColor(color) }, contentAlignment = Alignment.Center) { 
                                Text(text = (index + 1).toString(), color = if (ColorUtils.isDark(displayColor)) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp) 
                            }
                            Text(ColorUtils.colorToHex(color).substring(1), color = Color.Gray, fontSize = 9.sp) 
                        } 
                    } 
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(isDarkMode: Boolean, favorites: Set<String>, savedPalettes: Set<String>, onColorSelect: (String) -> Unit, onDeleteFavorite: (String) -> Unit, onDeletePalette: (String) -> Unit) {
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF2F4F7)
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (isDarkMode) Color.White.copy(0.12f) else Color.Black.copy(0.08f)
    
    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(16.dp).verticalScroll(rememberScrollState())) {
        // --- COLORES INDIVIDUALES ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.favorites_header).uppercase(), color = if (isDarkMode) Color.LightGray else Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(cardColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text("Sin colores guardados", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                favorites.forEach { favHex ->
                    val color = ColorUtils.hexToColor(favHex) ?: Color.Gray
                    Box(modifier = Modifier
                        .size(60.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .border(1.5.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f), RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { onColorSelect(favHex) }, onLongClick = { onDeleteFavorite(favHex) })
                    ) {
                        // Brillo premium tipo cristal
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent))))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // --- MIS ÁLBUMES DE COLOR (Lotes / Playlists) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LibraryBooks, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.palettes_tab).uppercase(), color = if (isDarkMode) Color.LightGray else Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(16.dp))

        if (savedPalettes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(cardColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Palette, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.no_palettes), color = Color.Gray, fontSize = 11.sp)
                }
            }
        } else {
            savedPalettes.forEach { paletteJson ->
                val palette = try {
                    val name = paletteJson.substringAfter("\"name\":\"").substringBefore("\"")
                    val colorsStr = paletteJson.substringAfter("\"colors\":[").substringBefore("]")
                    val colors = colorsStr.split(",").map { it.replace("\"", "").trim() }.filter { it.isNotEmpty() }
                    Pair(name, colors)
                } catch (e: Exception) { Pair("Imported Palette", emptyList<String>()) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(palette.first.replace(".css", ""), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isDarkMode) Color.White else Color.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("${palette.second.size} COLORES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                            }
                            IconButton(onClick = { onDeletePalette(paletteJson) }, modifier = Modifier.size(32.dp).background(Color.Red.copy(0.1f), CircleShape)) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.8f), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Barra de colores tipo "Playlist"
                        Row(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, borderColor, RoundedCornerShape(8.dp))) {
                            palette.second.forEach { colorHex ->
                                val color = ColorUtils.hexToColor(colorHex) ?: Color.Gray
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color).clickable { onColorSelect(colorHex) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraSniper(onColorCaptured: (Color) -> Unit, onColorConfirmed: (Color) -> Unit) {
    val context = LocalContext.current; val lifecycleOwner = LocalLifecycleOwner.current; val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }; var zoomRatio by remember { mutableFloatStateOf(1f) }; var lastColor by remember { mutableStateOf(Color.White) }
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> val nZ = (zoomRatio * zoom).coerceIn(1f, 10f); zoomRatio = nZ; cameraControl?.setZoomRatio(nZ) } }.pointerInput(Unit) { detectTapGestures { onColorConfirmed(lastColor) } }) {
        AndroidView(factory = { ctx -> val pV = PreviewView(ctx); val executor = ContextCompat.getMainExecutor(ctx); cameraProviderFuture.addListener({ val cP = cameraProviderFuture.get(); val p = Preview.Builder().build().also { it.surfaceProvider = pV.surfaceProvider }; val iA = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build(); iA.setAnalyzer(executor) { iP -> val yB = iP.planes[0].buffer; val uB = iP.planes[1].buffer; val vB = iP.planes[2].buffer; val w = iP.width; val h = iP.height; var sY = 0L; var sU = 0L; var sV = 0L; val s = 12; val sX = w / 2 - s / 2; val sYp = h / 2 - s / 2; for (x in 0 until s) for (y in 0 until s) { val px = sX + x; val py = sYp + y; sY += yB.get(py * w + px).toInt() and 0xFF; val uvI = (py / 2) * (iP.planes[1].rowStride) + (px / 2) * (iP.planes[1].pixelStride); if (uvI < uB.remaining()) sU += uB.get(uvI).toInt() and 0xFF; if (uvI < vB.remaining()) sV += vB.get(uvI).toInt() and 0xFF }; 
                val aY = (sY / (s * s)).toInt(); val aU = (sU / (s * s)).toInt(); val aV = (sV / (s * s)).toInt(); 
                val r = (aY + 1.402 * (aV - 128)).toInt().coerceIn(0, 255); 
                val g = (aY - 0.34414 * (aU - 128) - 0.71414 * (aV - 128)).toInt().coerceIn(0, 255); 
                val b = (aY + 1.772 * (aU - 128)).toInt().coerceIn(0, 255); 
                val nC = Color(r / 255f, g / 255f, b / 255f); 
                val lC = Color(lastColor.red * 0.9f + nC.red * 0.1f, lastColor.green * 0.9f + nC.green * 0.1f, lastColor.blue * 0.9f + nC.blue * 0.1f); lastColor = lC; onColorCaptured(lC); iP.close() }; try { cP.unbindAll(); val c = cP.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, p, iA); cameraControl = c.cameraControl } catch (e: Exception) { e.printStackTrace() } }, executor); pV }, modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) { val center = Offset(size.width / 2f, size.height / 2f); drawCircle(Color.White, radius = 12.dp.toPx(), center = center, style = Stroke(2.dp.toPx())); drawLine(Color.White, Offset(center.x - 24.dp.toPx(), center.y), Offset(center.x + 24.dp.toPx(), center.y), strokeWidth = 2.dp.toPx()); drawLine(Color.White, Offset(center.x, center.y - 24.dp.toPx()), Offset(center.x, center.y + 24.dp.toPx()), strokeWidth = 2.dp.toPx()) }
    }
}

@Composable
fun ExportDialog(harmonyColors: List<Color>, onDismiss: () -> Unit, currentColor: Color) {
    val clipboard = LocalClipboardManager.current; val context = LocalContext.current; val bC = ButtonDefaults.buttonColors(containerColor = currentColor, contentColor = if (ColorUtils.isDark(currentColor)) Color.White else Color.Black)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Export Palette") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { val css = harmonyColors.mapIndexed { i, c -> "--color-${i+1}: ${ColorUtils.colorToHex(c)};" }.joinToString("\n"); clipboard.setText(AnnotatedString(css)); Toast.makeText(context, "CSS Copied!", Toast.LENGTH_SHORT).show(); onDismiss() }, modifier = Modifier.fillMaxWidth(), colors = bC) { Text("CSS Variables") }; Button(onClick = { val json = "[\n" + harmonyColors.joinToString(",\n") { "  \"${ColorUtils.colorToHex(it)}\"" } + "\n]"; clipboard.setText(AnnotatedString(json)); Toast.makeText(context, "JSON Copied!", Toast.LENGTH_SHORT).show(); onDismiss() }, modifier = Modifier.fillMaxWidth(), colors = bC) { Text("JSON Array") }; Button(onClick = { val xml = harmonyColors.mapIndexed { i, c -> "<color name=\"palette_${i+1}\">${ColorUtils.colorToHex(c)}</color>" }.joinToString("\n"); clipboard.setText(AnnotatedString(xml)); Toast.makeText(context, "Android XML Copied!", Toast.LENGTH_SHORT).show(); onDismiss() }, modifier = Modifier.fillMaxWidth(), colors = bC) { Text("Android XML") } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun SettingsDialog(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, currentLocale: String, onToggleLanguage: () -> Unit, isCaosMode: Boolean, analogousCount: Int, fixedUiColorHex: String, colorBlindnessMode: String, favorites: Set<String>, onDismiss: () -> Unit, onUpdateSettings: (Boolean, Int, String, String, Boolean) -> Unit, isGoldMode: Boolean) {
    var showBlindnessDropdown by remember { mutableStateOf(false) }
    val dialogBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFE0E6ED)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val sectionTitleColor = if (isDarkMode) Color.Gray else Color.DarkGray
    
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { 
            Text(
                stringResource(R.string.settings).uppercase(), 
                style = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp, color = textColor)
            ) 
        }, 
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        text = { 
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) { 
                // --- APARIENCIA ---
                Column {
                    Text(stringResource(R.string.appearance), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionTitleColor, letterSpacing = 1.sp))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(if(isDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.4f)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { 
                        Icon(if(isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null, tint = sectionTitleColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.dark_mode), modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Medium, color = textColor))
                        Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() }) 
                    }

                    Spacer(Modifier.height(8.dp))
                    // --- MODO ORO TOGGLE ---
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(if(isDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.4f)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { 
                        Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF8C6221), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.gold_mode_premium), modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Medium, color = textColor))
                        Switch(checked = isGoldMode, onCheckedChange = { onUpdateSettings(isCaosMode, analogousCount, fixedUiColorHex, colorBlindnessMode, it) }) 
                    }
                }

                // --- ACCESIBILIDAD ---
                Column {
                    Text(stringResource(R.string.accessibility), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionTitleColor, letterSpacing = 1.sp))
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { showBlindnessDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if(isDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.4f),
                            border = BorderStroke(1.dp, if(isDarkMode) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, null, tint = sectionTitleColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(if (colorBlindnessMode == "None") stringResource(R.string.none) else colorBlindnessMode, modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Medium, color = textColor))
                                Icon(Icons.Default.ArrowDropDown, null, tint = sectionTitleColor)
                            }
                        }
                        DropdownMenu(
                            expanded = showBlindnessDropdown,
                            onDismissRequest = { showBlindnessDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.6f).background(if (isDarkMode) Color(0xFF262626) else Color.White)
                        ) {
                            listOf("None", "Protanopia", "Deuteranopia", "Tritanopia").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(if (mode == "None") stringResource(R.string.none) else mode, color = if(isDarkMode) Color.White else Color.Black) },
                                    onClick = { 
                                        onUpdateSettings(isCaosMode, analogousCount, fixedUiColorHex, mode, isGoldMode)
                                        showBlindnessDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // --- IDIOMA ---
                Column {
                    Text(stringResource(R.string.system), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionTitleColor, letterSpacing = 1.sp))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = onToggleLanguage,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if(isDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.4f)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, null, tint = sectionTitleColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.language), modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Medium, color = textColor))
                            Text(if (currentLocale == "es") "Español 🇪🇸" else "English 🇺🇸", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                        }
                    }
                }

                // --- SINCRONIZACIÓN ---
                Column {
                    Text(stringResource(R.string.customization), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionTitleColor, letterSpacing = 1.sp))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(if(isDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.4f)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { 
                        Icon(Icons.Default.Sync, null, tint = sectionTitleColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.chaos_mode), modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Medium, color = textColor))
                        Switch(checked = isCaosMode, onCheckedChange = { onUpdateSettings(it, analogousCount, fixedUiColorHex, colorBlindnessMode, isGoldMode) }) 
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.analogous_count, analogousCount), style = TextStyle(fontWeight = FontWeight.Medium, color = textColor, fontSize = 13.sp))
                    Slider(
                        value = analogousCount.toFloat(), 
                        onValueChange = { onUpdateSettings(isCaosMode, it.toInt(), fixedUiColorHex, colorBlindnessMode, isGoldMode) }, 
                        valueRange = 5f..10f, 
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = if(isDarkMode) Color.White else Color.Black,
                            activeTrackColor = if(isDarkMode) Color.White.copy(0.5f) else Color.Black.copy(0.5f)
                        )
                    ) 
                }

                // --- COLOR FIJO ---
                Column { 
                    Text(stringResource(R.string.fixed_ui_color), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionTitleColor, letterSpacing = 1.sp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { 
                        val colors = listOf("#268CEF", "#FFD700", "#FF5722", "#4CAF50") + favorites.toList()
                        items(colors) { hex -> 
                            val color = ColorUtils.hexToColor(hex) ?: Color.Gray
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(if (hex == fixedUiColorHex) 3.dp else 1.dp, textColor, CircleShape)
                                    .clickable { onUpdateSettings(isCaosMode, analogousCount, hex, colorBlindnessMode, isGoldMode) }
                            ) 
                        } 
                    } 
                } 
            } 
        }, 
        confirmButton = { 
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(100),
                color = if(isDarkMode) Color.White else Color.Black,
                modifier = Modifier.padding(bottom = 12.dp, end = 12.dp)
            ) {
                Text(
                    stringResource(R.string.accept),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = TextStyle(fontWeight = FontWeight.Black, color = if(isDarkMode) Color.Black else Color.White, fontSize = 12.sp)
                )
            }
        }
    )
}

@Composable
fun ColorWheel(isDarkMode: Boolean, currentColor: Color, actualHsv: FloatArray, harmonyMode: HarmonyMode, analogousCount: Int, onColorChange: (Color) -> Unit, onColorClick: (Color) -> Unit, modifier: Modifier = Modifier, uiAccentColor: Color, colorBlindnessMode: String) {
    val textMeasurer = rememberTextMeasurer()
    
    val wheelGradientColors = remember(colorBlindnessMode) {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red).map {
            if (colorBlindnessMode == "None") it else ColorUtils.simulateColorBlindness(it, colorBlindnessMode)
        }
    }
    
    // Generar offsets según el número de análogos configurado
    val targets = remember(harmonyMode, analogousCount) {
        when(harmonyMode) { 
            HarmonyMode.COMPLEMENTARY -> listOf(0f, 180f)
            HarmonyMode.TRIADIC -> listOf(0f, 120f, 240f)
            HarmonyMode.ANALOGOUS -> {
                val startAngle = -90f
                val endAngle = 90f
                val step = if (analogousCount > 1) (endAngle - startAngle) / (analogousCount - 1) else 0f
                (0 until analogousCount).map { startAngle + (it * step) }
            }
        }
    }
    
    val animatedOffsets = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }
    
    LaunchedEffect(targets) {
        while (animatedOffsets.size < targets.size) { animatedOffsets.add(Animatable(0f)) }
        while (animatedOffsets.size > targets.size) { animatedOffsets.removeAt(animatedOffsets.size - 1) }
        targets.forEachIndexed { i, target -> launch { animatedOffsets[i].animateTo(targetValue = target, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) } }
    }

    Canvas(modifier = modifier.pointerInput(actualHsv[2]) { detectDragGestures { change, _ -> val center = Offset(size.width / 2f, size.height / 2f); val pos = change.position - center; val hue = (atan2(pos.y, pos.x) * (180f / PI.toFloat()) + 360f) % 360f; val dist = sqrt(pos.x.pow(2) + pos.y.pow(2)); val radius = min(size.width, size.height) / 2f; val s = (dist / radius).coerceIn(0f, 1f); onColorChange(ColorUtils.hsvToColor(hue, s, actualHsv[2])) } }.pointerInput(harmonyMode, actualHsv[0], actualHsv[1], actualHsv[2]) { detectTapGestures { offset -> val center = Offset(size.width / 2f, size.height / 2f); val radius = min(size.width, size.height) / 2f; val pos = offset - center; val dist = sqrt(pos.x.pow(2) + pos.y.pow(2)); if (dist < radius * 0.58f) { val newVal = if (actualHsv[2] > 0.5f) 0.4f else 1.0f; onColorChange(ColorUtils.hsvToColor(actualHsv[0], actualHsv[1], newVal)) } else if (dist <= radius) { val hue = (atan2(pos.y, pos.x) * (180f / PI.toFloat()) + 360f) % 360f; val s = (dist / radius).coerceIn(0f, 1f); onColorChange(ColorUtils.hsvToColor(hue, s, actualHsv[2])) } else { val rad = actualHsv[0] * PI.toFloat() / 180f; val d = sqrt((offset.x - (center.x + radius * cos(rad))).pow(2) + (offset.y - (center.y + radius * sin(rad))).pow(2)); if (d < 40f) onColorClick(currentColor) } } }) {
        val center = Offset(size.width / 2f, size.height / 2f); val radius = min(size.width, size.height) / 2f; val ringThickness = radius * 0.12f; val gap = radius * 0.02f
        for (i in 0..2) { val r = radius - (i * (ringThickness + gap)) - (ringThickness / 2f); val ringSaturation = when(i) { 0 -> 1f; 1 -> 0.7f; else -> 0.4f }; drawCircle(brush = Brush.sweepGradient(wheelGradientColors), radius = r, center = center, style = Stroke(width = ringThickness)); drawCircle(color = Color.Black.copy(alpha = 1f - actualHsv[2]), radius = r, center = center, style = Stroke(width = ringThickness)); drawCircle(color = Color.Gray.copy(alpha = (1f - (actualHsv[1] * ringSaturation)) * 0.5f), radius = r, center = center, style = Stroke(width = ringThickness)) }
        val gC = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f); val mRad = actualHsv[0] * PI.toFloat() / 180f; drawLine(gC.copy(alpha = 0.5f), center, Offset(center.x + radius * cos(mRad), center.y + radius * sin(mRad)), strokeWidth = 2.dp.toPx())
        
        animatedOffsets.forEachIndexed { i, anim -> 
            val h = (actualHsv[0] + anim.value + 360f) % 360f
            val rad = h * PI.toFloat() / 180f
            val p = Offset(center.x + radius * cos(rad), center.y + radius * sin(rad))
            if (anim.value != 0f) { drawLine(gC.copy(alpha = 0.3f), center, p, strokeWidth = 1.dp.toPx()); drawCircle(if (isDarkMode) Color.White else Color.Black, radius = 6.dp.toPx(), center = p) }
            val lR = radius + 22.dp.toPx(); val lP = Offset(center.x + lR * cos(rad), center.y + lR * sin(rad))
            val dotColor = if (colorBlindnessMode == "None") uiAccentColor else ColorUtils.simulateColorBlindness(uiAccentColor, colorBlindnessMode)
            drawCircle(dotColor, radius = 10.dp.toPx(), center = lP)
            val tr = textMeasurer.measure((i + 1).toString(), TextStyle(color = if (ColorUtils.isDark(dotColor)) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            drawText(tr, topLeft = Offset(lP.x - tr.size.width / 2, lP.y - tr.size.height / 2)) 
        }
        val hd = actualHsv[1] * radius; val hp = Offset(center.x + hd * cos(actualHsv[0] * PI.toFloat() / 180f), center.y + hd * sin(actualHsv[0] * PI.toFloat() / 180f))
        val thumbColor = if (colorBlindnessMode == "None") currentColor else ColorUtils.simulateColorBlindness(currentColor, colorBlindnessMode)
        drawCircle(Color.Black, radius = 10.dp.toPx(), center = hp); drawCircle(Color.White, radius = 8.dp.toPx(), center = hp); drawCircle(thumbColor, radius = 6.dp.toPx(), center = hp)
    }
}

@Composable
fun ColorCard(isDarkMode: Boolean, item: ColorItem, colorBlindnessMode: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    val displayColor = if (colorBlindnessMode == "None") item.color else ColorUtils.simulateColorBlindness(item.color, colorBlindnessMode)
    val isDark = ColorUtils.isDark(displayColor); val textColor = if (isDark) Color.White else Color.Black
    val shape = RoundedCornerShape(18.dp)
    Box(modifier = Modifier.aspectRatio(1f).shadow(6.dp, shape).clip(shape).background(displayColor).border(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color.Black.copy(0.1f), shape).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))) {
            Column(modifier = Modifier.padding(10.dp)) { 
                Text(text = item.title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1); 
                Spacer(modifier = Modifier.height(4.dp)); 
                Text(text = ColorUtils.colorToHex(item.color).uppercase(), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium); 
                Text(text = "(${String.format(Locale.US, "%.2f", item.color.red)}, ${String.format(Locale.US, "%.2f", item.color.green)}, ${String.format(Locale.US, "%.2f", item.color.blue)}, 1)", color = textColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}
