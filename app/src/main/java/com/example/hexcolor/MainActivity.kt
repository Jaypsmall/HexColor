package com.example.hexcolor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.onSizeChanged
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hexcolor.ui.theme.HexColorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

// DataStore for persistence
val Context.dataStore by preferencesDataStore(name = "favorites")

enum class HarmonyMode { COMPLEMENTARY, TRIADIC, ANALOGOUS }
enum class SniperState { OFF, WINDOWED, FULLSCREEN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            val colorBlindnessKey = remember { stringPreferencesKey("color_blindness_mode") }
            val goldModeKey = remember { booleanPreferencesKey("gold_mode") }
            
            val colorBlindnessMode by remember(context.dataStore.data) {
                context.dataStore.data.map { it[colorBlindnessKey] ?: "None" }
            }.collectAsState(initial = "None")
            val isGoldModeSetting by remember(context.dataStore.data) {
                context.dataStore.data.map { it[goldModeKey] ?: false }
            }.collectAsState(initial = false)
            
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            var showStartup by rememberSaveable { mutableStateOf(true) }
            
            LaunchedEffect(Unit) {
                delay(2000L) // Usando Long explícito para evitar confusión si no hay import de Duration
                showStartup = false
            }
            
            DisposableEffect(isDarkMode, isGoldModeSetting) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode }
                )
                onDispose {}
            }

            HexColorTheme(darkTheme = isDarkMode, colorBlindnessMode = colorBlindnessMode) {
                Crossfade(targetState = showStartup, animationSpec = tween(800), label = "StartupTransition") { startup ->
                    if (startup) {
                        StartupScreen()
                    } else {
                        HexColorApp(isDarkMode = isDarkMode, onToggleDarkMode = { isDarkMode = !isDarkMode })
                    }
                }
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

fun Modifier.goldBorder(shape: RoundedCornerShape) = this
    .border(1.dp, GoldGradient, shape)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HexColorApp(isDarkMode: Boolean, onToggleDarkMode: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val favoritesKey = remember { stringSetPreferencesKey("fav_colors") }
    val palettesKey = remember { stringSetPreferencesKey("user_palettes") }
    val colorBlindnessKey = remember { stringPreferencesKey("color_blindness_mode") }
    val languageKey = remember { stringPreferencesKey("language_code") }
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
            val lang = prefs[languageKey] ?: Locale.getDefault().language
            val extractCount = prefs[intPreferencesKey("extract_count")] ?: 12
            listOf(caos, count, hex, blind, gold, extractCount, lang)
        } 
    }
    val settings by settingsFlow.collectAsState(initial = listOf(true, 7, "#268CEF", "None", false, 12, "en"))
    val isCaosMode = settings[0] as Boolean
    val analogousCount = settings[1] as Int
    val fixedUiColorHex = settings[2] as String
    val colorBlindnessMode = settings[3] as String
    val isGoldMode = settings[4] as Boolean
    val extractCount = settings[5] as Int
    val savedLanguage = settings[6] as String
    val fixedUiColor = ColorManager.hexToColor(fixedUiColorHex) ?: Color(0xFF268CEF)

    // --- SAVERS ---
    val colorSaver = remember {
        Saver<Color, Int>(
            save = { it.toArgb() },
            restore = { Color(it) }
        )
    }
    val floatArraySaver = remember {
        listSaver<FloatArray, Float>(
            save = { it.toList() },
            restore = { it.toFloatArray() }
        )
    }
    val colorListSaver = remember {
        Saver<List<Color>, IntArray>(
            save = { it.map { c -> c.toArgb() }.toIntArray() },
            restore = { it.map { argb -> Color(argb) } }
        )
    }
    
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
    var sniperState by remember { mutableStateOf(SniperState.OFF) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val uiAccentColor = remember(isGoldMode, isCaosMode, hsvValue, fixedUiColor, colorBlindnessMode) {
        val raw = if (isGoldMode) {
            Color(0xFFC29B47)
        } else if (isCaosMode) {
            ColorManager.hsvToColor(hsvValue[0], hsvValue[1], 1f)
        } else fixedUiColor
        if (colorBlindnessMode == "None") raw else ColorManager.simulateColorBlindness(raw, colorBlindnessMode)
    }
    
    val focusManager = LocalFocusManager.current
    val statusView = LocalView.current

    LaunchedEffect(isDarkMode, isGoldMode) {
        val window = (context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, statusView)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        // statusBarColor is deprecated but still the standard for transparency in many setups.
        // If we want to be fully modern, we use WindowCompat.setDecorFitsSystemWindows(window, false)
        // which is already likely called or handled by the theme/Scaffold.
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }
    
    var currentLocale by remember { mutableStateOf("en") }
    
    LaunchedEffect(savedLanguage) {
        if (savedLanguage != currentLocale) {
            val locale = Locale.forLanguageTag(savedLanguage)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            currentLocale = savedLanguage
            if (context is MainActivity) context.recreate()
        }
    }

    fun toggleLanguage() {
        val newLang = if (currentLocale == "es") "en" else "es"
        scope.launch {
            context.dataStore.edit { it[languageKey] = newLang }
        }
    }

    val favoritesFlow = remember { context.dataStore.data.map { it[favoritesKey] ?: emptySet() } }
    val favorites by favoritesFlow.collectAsState(initial = emptySet())
    
    val palettesFlow = remember { context.dataStore.data.map { it[palettesKey] ?: emptySet() } }
    val savedPalettes by palettesFlow.collectAsState(initial = emptySet())

    val harmonyColors = remember(currentColor, harmonyMode, analogousCount) {
        when (harmonyMode) {
            HarmonyMode.COMPLEMENTARY -> listOf(currentColor, ColorManager.getComplementary(currentColor))
            HarmonyMode.TRIADIC -> ColorManager.getTriadic(currentColor)
            HarmonyMode.ANALOGOUS -> ColorManager.getAnalogous(currentColor, analogousCount)
        }
    }

    LaunchedEffect(currentColor, harmonyMode, harmonyColors, isDarkMode) {
        if (FloatingService.isRunning) {
            FloatingService.currentHex = ColorManager.colorToHex(currentColor)
            FloatingService.currentHarmony = harmonyColors.map { ColorManager.colorToHex(it) }
            FloatingService.isDarkMode = isDarkMode
            FloatingService.originalIndex = if (harmonyMode == HarmonyMode.ANALOGOUS) analogousCount / 2 else 0
            context.startService(Intent(context, FloatingService::class.java))
        }
    }

    val originalLabel = stringResource(R.string.original)
    val complementaryLabel = stringResource(R.string.complementary)
    val analogousLabel = stringResource(R.string.analogous)
    val triadicLabel = stringResource(R.string.triadic)

    val colorItems = remember(currentColor, analogousCount, originalLabel, complementaryLabel, analogousLabel, triadicLabel) {
        val comp = ColorManager.getComplementary(currentColor)
        val analogous = ColorManager.getAnalogous(currentColor, analogousCount)
        val triadic = ColorManager.getTriadic(currentColor)
        val list = mutableListOf(
            ColorItem("① $originalLabel", currentColor),
            ColorItem("② $complementaryLabel", comp),
        )
        if (analogous.size >= 3) {
            list.add(ColorItem("③ $analogousLabel", analogous[analogous.size / 2 - 1]))
            list.add(ColorItem("④ $analogousLabel", analogous[analogous.size / 2 + 1]))
        }
        list.add(ColorItem("⑤ $triadicLabel", triadic[1]))
        list.add(ColorItem("⑥ $triadicLabel", triadic[2]))
        list.toList()
    }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val beyondBoundsPageCount = 1

    LaunchedEffect(pagerState.currentPage, drawerState.isOpen) {
        focusManager.clearFocus()
    }

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
                                val newPaletteJson = "{\"name\":\"$fileName\", \"colors\":${matches.map { color -> "\"$color\"" }}}"
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
                drawerContainerColor = if (isDarkMode) Color.Black else Color.White,
                drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().background(if (isDarkMode) Color.Black else Color.White)) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = if (isGoldMode) R.drawable.icono_hex_transparente else R.drawable.logo_nuevo1),
                                contentDescription = "Logo",
                                modifier = Modifier.size(75.dp)
                            )
                            Image(
                                painter = painterResource(id = if (isGoldMode) R.drawable.hexcolor_oro else R.drawable.hexcolor),
                                contentDescription = "HexColor Text",
                                modifier = Modifier.height(32.dp).padding(top = 8.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE))
                        
                        val itemModifier = Modifier.height(48.dp).padding(horizontal = 8.dp)
                        val labelStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Surface(
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = itemModifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent
                        ) {
                            Box(modifier = Modifier.fillMaxSize().goldButtonStyle(), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF543B14), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.premium), style = labelStyle, color = Color(0xFF543B14))
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(4.dp))

                        Surface(
                            onClick = { scope.launch { sniperState = SniperState.WINDOWED; drawerState.close() } },
                            modifier = itemModifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent
                        ) {
                            Box(modifier = Modifier.fillMaxSize().goldButtonStyle(), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color(0xFF543B14), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.sniper_mode), style = labelStyle, color = Color(0xFF543B14))
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        NavigationDrawerItem(label = { Text(stringResource(R.string.palette), style = labelStyle) }, selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0); drawerState.close() } }, icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 0) (if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(100)) else Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100))) else Modifier))
                        NavigationDrawerItem(label = { Text(stringResource(R.string.wheel), style = labelStyle) }, selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.animateScrollToPage(1); drawerState.close() } }, icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 1) (if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(100)) else Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100))) else Modifier))
                        NavigationDrawerItem(label = { Text(stringResource(R.string.picker), style = labelStyle) }, selected = pagerState.currentPage == 2, onClick = { scope.launch { pagerState.animateScrollToPage(2); drawerState.close() } }, icon = { Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 2) (if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(100)) else Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100))) else Modifier))
                        NavigationDrawerItem(label = { Text(stringResource(R.string.favorites), style = labelStyle) }, selected = pagerState.currentPage == 3, onClick = { scope.launch { pagerState.animateScrollToPage(3); drawerState.close() } }, icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = uiAccentColor.copy(alpha = 0.15f), selectedTextColor = uiAccentColor, selectedIconColor = uiAccentColor), modifier = itemModifier.then(if(pagerState.currentPage == 3) (if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(100)) else Modifier.border(0.5.dp, uiAccentColor, RoundedCornerShape(100))) else Modifier))
                        NavigationDrawerItem(label = { Text(stringResource(R.string.import_palette), style = labelStyle) }, selected = false, onClick = { scope.launch { drawerState.close(); importLauncher.launch("text/css") } }, icon = { Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent), modifier = itemModifier)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE))
                        NavigationDrawerItem(label = { Text(stringResource(R.string.settings), style = labelStyle) }, selected = false, onClick = { scope.launch { showSettingsDialog = true; drawerState.close() } }, icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent), modifier = itemModifier)
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "HexColor Pro v1.0.4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.Gray else Color.DarkGray)
                        Text(text = "Created by JAYLIZ with ❤️", fontSize = 9.sp, color = if (isDarkMode) Color.Gray.copy(0.6f) else Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isDarkMode) Color.Black else Color(0xFFF2F4F7),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier.background(
                        if (isDarkMode) Color.Black else Color(
                            0xFFF2F4F7
                        )
                    ).statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = if (isDarkMode) Color.Gray else Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = if (isGoldMode) R.drawable.icono_hex_transparente else R.drawable.logo_nuevo1),
                            contentDescription = "Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Image(
                            painter = painterResource(id = if (isGoldMode) R.drawable.hexcolor_oro else R.drawable.hexcolor),
                            contentDescription = "HexColor Text",
                            modifier = Modifier.height(32.dp)
                                .then(if (isGoldMode) Modifier.goldMask() else Modifier),
                            contentScale = ContentScale.FillHeight
                        )
                    }

                    // Borde plata profesional separador (Efecto Metal Pulido)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(3.dp).background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.5.dp).background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF808080),
                                    Color(0xFFE0E0E0),
                                    Color(0xFFFFFFFF),
                                    Color(0xFFE0E0E0),
                                    Color(0xFF808080),
                                    Color.Transparent
                                )
                            )
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            R.string.palette,
                            R.string.wheel,
                            R.string.picker,
                            R.string.favorites
                        )
                        tabs.forEachIndexed { index, resId ->
                            val isSelected = pagerState.currentPage == index
                            val shape = RoundedCornerShape(12.dp)
                            Surface(
                                onClick = {
                                    focusManager.clearFocus()
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier.weight(1f).height(38.dp)
                                    .shadow(if (isSelected) 4.dp else 0.dp, shape),
                                shape = shape,
                                color = if (isSelected) (if (isGoldMode) Color.Transparent else uiAccentColor) else (if (isDarkMode) Color(
                                    0xFF1A1A1A
                                ) else Color.Transparent),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White.copy(0.5f) else (if (isDarkMode) Color.White.copy(
                                        0.05f
                                    ) else Color.Black.copy(0.15f))
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().then(
                                        if (isSelected && isGoldMode) Modifier.goldButtonStyle() else Modifier.background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    ), contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(resId).uppercase(),
                                        color = if (isSelected) (if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(
                                                uiAccentColor
                                            )
                                        ) Color.White else Color.Black)) else Color.Gray,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.sp,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top, beyondViewportPageCount = beyondBoundsPageCount) { page ->
                    when (page) {
                        0 -> Box(modifier = Modifier.fillMaxSize()) {
                            PaletteScreen(isDarkMode, hexInput, { hexInput = it }, currentColor, { currentColor = it; hexInput = ColorManager.colorToHex(it); val h = FloatArray(3); android.graphics.Color.colorToHSV(it.toArgb(), h); hsvValue = h }, hsvValue, { hsvValue = it; currentColor = ColorManager.hsvToColor(it[0], it[1], it[2]); hexInput = ColorManager.colorToHex(currentColor) }, colorItems, { color -> val hexValue = ColorManager.colorToHex(if (colorBlindnessMode == "None") color else ColorManager.simulateColorBlindness(color, colorBlindnessMode)); scope.launch { context.dataStore.edit { prefs -> val current = prefs[favoritesKey] ?: emptySet(); prefs[favoritesKey] = current + hexValue }; Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show() } }, { color -> val displayColor = if (colorBlindnessMode == "None") color else ColorManager.simulateColorBlindness(color, colorBlindnessMode); val hexValue = ColorManager.colorToHex(displayColor); clipboardManager.setText(AnnotatedString(hexValue)); Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show() }, sniperState != SniperState.OFF, { sniperState = if (sniperState == SniperState.OFF) SniperState.WINDOWED else SniperState.OFF }, uiAccentColor, colorBlindnessMode, isGoldMode)
                            if (sniperState != SniperState.OFF) SniperGodOverlay(sniperState, isDarkMode, isGoldMode, currentColor, uiAccentColor, { sniperState = it }, { currentColor = it; hexInput = ColorManager.colorToHex(it); val h = FloatArray(3); android.graphics.Color.colorToHSV(it.toArgb(), h); hsvValue = h }, { val hex = ColorManager.colorToHex(if (colorBlindnessMode == "None") it else ColorManager.simulateColorBlindness(it, colorBlindnessMode)); scope.launch { context.dataStore.edit { prefs -> val current = prefs[favoritesKey] ?: emptySet(); prefs[favoritesKey] = current + hex }; Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show() } })
                        }
                        1 -> WheelScreen(isDarkMode, onToggleDarkMode, currentColor, { currentColor = it; hexInput = ColorManager.colorToHex(it); val h = FloatArray(3); android.graphics.Color.colorToHSV(it.toArgb(), h); hsvValue = h }, { color -> val displayColor = if (colorBlindnessMode == "None") color else ColorManager.simulateColorBlindness(color, colorBlindnessMode); val hexValue = ColorManager.colorToHex(displayColor); clipboardManager.setText(AnnotatedString(hexValue)); Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show() }, harmonyMode, { harmonyMode = it }, harmonyColors, hsvValue, analogousCount, { v -> val newHsv = hsvValue.clone().apply { this[2] = v }; hsvValue = newHsv; currentColor = ColorManager.hsvToColor(newHsv[0], newHsv[1], newHsv[2]); hexInput = ColorManager.colorToHex(currentColor) }, { scope.launch { pagerState.animateScrollToPage(3) } }, currentLocale, uiAccentColor, colorBlindnessMode, { blind -> scope.launch { context.dataStore.edit { it[colorBlindnessKey] = blind } } }, isGoldMode)
                        2 -> PickerScreen(isDarkMode, pickerBitmap, { pickerBitmap = it }, detectedPickerColors, { detectedPickerColors = it }, { val selected = if (colorBlindnessMode == "None") it else ColorManager.simulateColorBlindness(it, colorBlindnessMode); currentColor = selected; hexInput = ColorManager.colorToHex(selected); val h = FloatArray(3); android.graphics.Color.colorToHSV(selected.toArgb(), h); hsvValue = h; scope.launch { pagerState.animateScrollToPage(1) } }, uiAccentColor, colorBlindnessMode, { color -> val hex = ColorManager.colorToHex(if (colorBlindnessMode == "None") color else ColorManager.simulateColorBlindness(color, colorBlindnessMode)); scope.launch { context.dataStore.edit { prefs -> val current = prefs[favoritesKey] ?: emptySet(); prefs[favoritesKey] = current + hex }; Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show() } }, { color -> val displayColor = if (colorBlindnessMode == "None") color else ColorManager.simulateColorBlindness(color, colorBlindnessMode); val hex = ColorManager.colorToHex(displayColor); clipboardManager.setText(AnnotatedString(hex)); Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show() }, isGoldMode, extractCount, { count -> scope.launch { context.dataStore.edit { it[intPreferencesKey("extract_count")] = count } } }, currentLocale)
                        3 -> FavoritesScreen(isDarkMode, favorites, savedPalettes, { favHex -> val favColor = ColorManager.hexToColor(favHex); if (favColor != null) { currentColor = favColor; hexInput = favHex; val h = FloatArray(3); android.graphics.Color.colorToHSV(favColor.toArgb(), h); hsvValue = h; scope.launch { pagerState.animateScrollToPage(1) } } }, { favHex -> scope.launch { context.dataStore.edit { prefs -> val current = prefs[favoritesKey] ?: emptySet(); prefs[favoritesKey] = current - favHex }; Toast.makeText(context, context.getString(R.string.deleted), Toast.LENGTH_SHORT).show() } }, { paletteJson -> scope.launch { context.dataStore.edit { prefs -> val current = prefs[palettesKey] ?: emptySet(); prefs[palettesKey] = current - paletteJson } } }, isGoldMode)
                    }
                }
            }
        }
    }
    if (showSettingsDialog) SettingsDialog(isDarkMode, onToggleDarkMode, currentLocale, { toggleLanguage() }, isCaosMode, analogousCount, fixedUiColorHex, colorBlindnessMode, favorites, { showSettingsDialog = false }, { caos, count, hex, blind, gold -> scope.launch { context.dataStore.edit { prefs -> prefs[caosModeKey] = caos; prefs[analogousCountKey] = count; prefs[fixedUiColorKey] = hex; prefs[colorBlindnessKey] = blind; prefs[goldModeKey] = gold } } }, isGoldMode)
}

@Composable
fun PickerScreen(isDarkMode: Boolean, bitmap: Bitmap?, onBitmapChange: (Bitmap?) -> Unit, detectedColors: List<Color>, onDetectedColorsChange: (List<Color>) -> Unit, onColorSelect: (Color) -> Unit, uiAccentColor: Color, colorBlindnessMode: String, onSaveFavorite: (Color) -> Unit, onCopyColor: (Color) -> Unit, isGoldMode: Boolean, extractCount: Int, onUpdateExtractCount: (Int) -> Unit, currentLocale: String) {
    val context = LocalContext.current; var showExportDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onBitmapChange(try { BitmapFactory.decodeStream(context.contentResolver.openInputStream(it), null, BitmapFactory.Options().apply { inMutable = true }) } catch (e: Exception) { null }); onDetectedColorsChange(emptyList()) } }
    val buttonShape = RoundedCornerShape(12.dp); val fineBorder = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8))
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.picker).uppercase(), style = TextStyle(color = uiAccentColor, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp), modifier = if (isGoldMode) Modifier.goldMask() else Modifier)
                Box(modifier = Modifier.height(300.dp).fillMaxWidth().shadow(8.dp, buttonShape).clip(buttonShape).background(if (isDarkMode) Color.Black else Color(0xFFF2F4F7)).then(if (isGoldMode) Modifier.goldBorder(buttonShape) else Modifier.border(fineBorder, buttonShape)), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        var scale by remember { mutableFloatStateOf(1f) }; var offset by remember { mutableStateOf(Offset.Zero) }
                        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); offset += pan } }) {
                            Canvas(modifier = Modifier.fillMaxSize().pointerInput(bitmap) { detectTapGestures { tapOffset -> bitmap.let { b -> val cW = size.width.toFloat(); val cH = size.height.toFloat(); val bW = b.width.toFloat(); val bH = b.height.toFloat(); val sBase = min(cW / bW, cH / bH); val visualX = (tapOffset.x - offset.x - cW / 2) / scale + cW / 2; val visualY = (tapOffset.y - offset.y - cH / 2) / scale + cH / 2; val dx = (cW - bW * sBase) / 2; val dy = (cH - bH * sBase) / 2; val x = ((visualX - dx) / sBase).toInt().coerceIn(0, b.width - 1); val y = ((visualY - dy) / sBase).toInt().coerceIn(0, b.height - 1); onColorSelect(Color(b.getPixel(x, y))) } } }) {
                                bitmap.let { b -> val cW = size.width; val cH = size.height; val bW = b.width.toFloat(); val bH = b.height.toFloat(); val sBase = min(cW.toFloat() / bW, cH.toFloat() / bH); val dx = (cW - bW * sBase) / 2; val dy = (cH - bH * sBase) / 2; withTransform({ translate(offset.x, offset.y); scale(scale, scale, pivot = center) }) { drawImage(image = b.asImageBitmap(), dstOffset = IntOffset(dx.toInt(), dy.toInt()), dstSize = IntSize((bW * sBase).toInt(), (bH * sBase).toInt())) } }
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

@Composable
fun PaletteScreen(isDarkMode: Boolean, hexInput: String, onHexChange: (String) -> Unit, currentColor: Color, onColorChange: (Color) -> Unit, hsvValue: FloatArray, onHsvChange: (FloatArray) -> Unit, colorItems: List<ColorItem>, onSaveFavorite: (Color) -> Unit, onCopyColor: (Color) -> Unit, isSniperMode: Boolean, onSniperToggle: () -> Unit, uiAccentColor: Color, colorBlindnessMode: String, isGoldMode: Boolean) {
    val context = LocalContext.current; val buttonShape = RoundedCornerShape(12.dp); val fineBorder = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.25f) else Color(0xFFD1D5D8))
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) onSniperToggle() else Toast.makeText(context, "Permiso necesario", Toast.LENGTH_SHORT).show() }
    val hueGradientColors = remember(colorBlindnessMode) { listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red).map { if (colorBlindnessMode == "None") it else ColorManager.simulateColorBlindness(it, colorBlindnessMode) } }
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = hexInput, onValueChange = onHexChange, modifier = Modifier.weight(1f).fillMaxHeight().border(fineBorder, buttonShape), placeholder = { Text("#RRGGBB", fontSize = 14.sp, color = Color.Gray) }, textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = if (isDarkMode) Color.Black else Color(0xFFF2F4F7), unfocusedContainerColor = if (isDarkMode) Color.Black else Color(0xFFF2F4F7), focusedTextColor = if (isDarkMode) Color.White else Color.Black, unfocusedTextColor = if (isDarkMode) Color.White else Color.Black, focusedBorderColor = uiAccentColor.copy(0.5f), unfocusedBorderColor = Color.Transparent), shape = buttonShape, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    Surface(onClick = { val color = ColorManager.hexToColor(hexInput); if (color != null) onColorChange(color) else Toast.makeText(context, context.getString(R.string.invalid_hex), Toast.LENGTH_SHORT).show() }, modifier = Modifier.width(85.dp).fillMaxHeight().shadow(4.dp, buttonShape), shape = buttonShape, color = if (isGoldMode) Color.Transparent else uiAccentColor, border = BorderStroke(1.dp, Color.White.copy(0.4f))) { Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(stringResource(R.string.show), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) } }
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

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, currentColor: Color, onColorSelect: (Color) -> Unit, onCopyColor: (Color) -> Unit, harmonyMode: HarmonyMode, onModeChange: (HarmonyMode) -> Unit, harmonyColors: List<Color>, hsvValue: FloatArray, analogousCount: Int, onValueChange: (Float) -> Unit, onNavigateToFavorites: () -> Unit, currentLocale: String, uiAccentColor: Color, colorBlindnessMode: String, onColorBlindnessChange: (String) -> Unit, isGoldMode: Boolean) {
    val activity = LocalContext.current as? MainActivity; val modes = remember { listOf(HarmonyMode.COMPLEMENTARY, HarmonyMode.TRIADIC, HarmonyMode.ANALOGOUS) }
    var cardOffset by remember { mutableStateOf(Offset(0f, 0f)) }; var showExportDialog by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp; val scrollState = rememberScrollState()
        if (isWide) Row(modifier = Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(modifier = Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) { WheelContent(isDarkMode, colorBlindnessMode, onColorBlindnessChange, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, onToggleDarkMode, onNavigateToFavorites, uiAccentColor, isGoldMode) }
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(12.dp)) { HeaderControls(modes, harmonyMode, onModeChange, isDarkMode, { showExportDialog = true }, uiAccentColor, hsvValue[2], isGoldMode); BrightnessSlider(currentLocale, hsvValue[2], onValueChange, currentColor, isGoldMode, uiAccentColor); InfoCard(isDarkMode, currentColor, harmonyColors, onCopyColor, activity, harmonyMode, cardOffset, { cardOffset += it }, uiAccentColor, colorBlindnessMode, isGoldMode) }
        } else Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderControls(modes, harmonyMode, onModeChange, isDarkMode, { showExportDialog = true }, uiAccentColor, hsvValue[2], isGoldMode)
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).widthIn(max = 450.dp), contentAlignment = Alignment.Center) { WheelContent(isDarkMode, colorBlindnessMode, onColorBlindnessChange, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, onToggleDarkMode, onNavigateToFavorites, uiAccentColor, isGoldMode) }
            BrightnessSlider(currentLocale, hsvValue[2], onValueChange, currentColor, isGoldMode, uiAccentColor)
            InfoCard(isDarkMode, currentColor, harmonyColors, onCopyColor, activity, harmonyMode, cardOffset, { cardOffset += it }, uiAccentColor, colorBlindnessMode, isGoldMode)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
    if (showExportDialog) ExportDialog(harmonyColors, { showExportDialog = false }, uiAccentColor, isGoldMode, isDarkMode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderControls(modes: List<HarmonyMode>, harmonyMode: HarmonyMode, onModeChange: (HarmonyMode) -> Unit, isDarkMode: Boolean, onExport: () -> Unit, uiAccentColor: Color, brightness: Float, isGoldMode: Boolean) {
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
private fun WheelContent(isDarkMode: Boolean, colorBlindnessMode: String, onColorBlindnessChange: (String) -> Unit, currentColor: Color, hsvValue: FloatArray, harmonyMode: HarmonyMode, analogousCount: Int, onColorSelect: (Color) -> Unit, onCopyColor: (Color) -> Unit, onToggleDarkMode: () -> Unit, onNavigateToFavorites: () -> Unit, uiAccentColor: Color, isGoldMode: Boolean) {
    val moonResource = if (hsvValue[2] > 0.5f) R.drawable.moon_light else R.drawable.moon_shadow; var showBlindnessMenu by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        ColorWheel(isDarkMode, currentColor, hsvValue, harmonyMode, analogousCount, onColorSelect, onCopyColor, Modifier.fillMaxSize().padding(40.dp), uiAccentColor, colorBlindnessMode, isGoldMode)
        Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) { Crossfade(targetState = moonResource, animationSpec = tween(400), label = "MoonTransition") { targetResource -> Image(painter = painterResource(id = targetResource), contentDescription = "Moon", modifier = Modifier.fillMaxSize(0.65f), contentScale = ContentScale.Fit) } }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).offset(y = (-11).dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun BrightnessSlider(currentLocale: String, value: Float, onValueChange: (Float) -> Unit, currentColor: Color, isGoldMode: Boolean, uiAccentColor: Color) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(isDarkMode: Boolean, favorites: Set<String>, savedPalettes: Set<String>, onColorSelect: (String) -> Unit, onDeleteFavorite: (String) -> Unit, onDeletePalette: (String) -> Unit, isGoldMode: Boolean) {
    val bgColor = remember(isDarkMode) { if (isDarkMode) Color.Black else Color(0xFFF2F4F7) }
    val cardColor = remember(isDarkMode) { if (isDarkMode) Color(0xFF1E1E1E) else Color.White }
    val borderColor = remember(isDarkMode) { if (isDarkMode) Color.White.copy(0.12f) else Color.Black.copy(0.08f) }
    val boxShape = RoundedCornerShape(10.dp)
    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(18.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.favorites_header).uppercase(), color = if (isDarkMode) Color.LightGray else Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
        Spacer(Modifier.height(16.dp))
        if (favorites.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(cardColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("Sin colores guardados", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        else FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { favorites.forEach { favHex -> val color = ColorManager.hexToColor(favHex) ?: Color.Gray; Box(modifier = Modifier.size(60.dp).shadow(4.dp, boxShape).clip(boxShape).background(color).then(if (isGoldMode) Modifier.goldBorder(boxShape) else Modifier.border(1.5.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f), boxShape)).combinedClickable(onClick = { onColorSelect(favHex) }, onLongClick = { onDeleteFavorite(favHex) })) { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))) } } }
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, tint = Color.Gray, modifier = Modifier.size(18.dp).then(if (isGoldMode) Modifier.goldMask() else Modifier)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.palettes_tab).uppercase(), color = if (isDarkMode) Color.LightGray else Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, modifier = if (isGoldMode) Modifier.goldMask() else Modifier) }
        Spacer(Modifier.height(16.dp))
        if (savedPalettes.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(cardColor, RoundedCornerShape(12.dp)).then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(12.dp)) else Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Palette, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(32.dp)); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.no_palettes), color = Color.Gray, fontSize = 11.sp) } }
        else savedPalettes.forEach { paletteJson -> val palette = try { val name = paletteJson.substringAfter("\"name\":\"").substringBefore("\""); val colorsStr = paletteJson.substringAfter("\"colors\":[").substringBefore("]"); val colors = colorsStr.split(",").map { it.replace("\"", "").trim() }.filter { it.isNotEmpty() }; Pair(name, colors) } catch (e: Exception) { Pair("Imported Palette", emptyList<String>()) }; val cardShape = RoundedCornerShape(16.dp); Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).then(if (isGoldMode) Modifier.goldBorder(cardShape) else Modifier), shape = cardShape, colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), border = if (isGoldMode) null else BorderStroke(1.dp, borderColor)) { Column(modifier = Modifier.padding(14.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(palette.first.replace(".css", ""), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isDarkMode) Color.White else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${palette.second.size} COLORES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp) }; IconButton(onClick = { onDeletePalette(paletteJson) }, modifier = Modifier.size(32.dp).background(Color.Red.copy(0.1f), CircleShape)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.8f), modifier = Modifier.size(16.dp)) } } ; Spacer(Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(8.dp)) else Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp)))) { palette.second.forEach { colorHex -> val color = ColorManager.hexToColor(colorHex) ?: Color.Gray; Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color).clickable { onColorSelect(colorHex) }) } } } } }
    }
}

@Composable
fun SniperGodOverlay(state: SniperState, isDarkMode: Boolean, isGoldMode: Boolean, currentColor: Color, uiAccentColor: Color, onStateChange: (SniperState) -> Unit, onColorCaptured: (Color) -> Unit, onColorConfirmed: (Color) -> Unit) {
    val buttonShape = RoundedCornerShape(16.dp); val fineBorder = BorderStroke(1.5.dp, if (isGoldMode) Color(0xFFC29B47) else (if (isDarkMode) Color.White.copy(0.3f) else Color.Black.copy(0.2f)))
    val flashAnim = remember { Animatable(0f) }; val scope = rememberCoroutineScope()
    val modifier = if (state == SniperState.FULLSCREEN) Modifier.fillMaxSize().background(Color.Black) else Modifier.fillMaxSize().padding(16.dp).wrapContentSize(Alignment.TopCenter).size(width = 320.dp, height = 450.dp).shadow(24.dp, buttonShape).clip(buttonShape).background(if (isDarkMode) Color.Black else Color.White).border(fineBorder, buttonShape)
    Box(modifier = modifier) {
        CameraSniper(onColorCaptured = onColorCaptured, onColorConfirmed = { scope.launch { flashAnim.snapTo(1f); flashAnim.animateTo(0f, animationSpec = tween(300)) }; onColorConfirmed(it) })
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAnim.value)))
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onStateChange(SniperState.OFF) }, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                Text(text = if (state == SniperState.FULLSCREEN) "FULLSCREEN SNIPER" else "WINDOWED SNIPER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.shadow(4.dp))
                IconButton(onClick = { onStateChange(if (state == SniperState.FULLSCREEN) SniperState.WINDOWED else SniperState.FULLSCREEN) }, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) { Icon(if (state == SniperState.FULLSCREEN) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Toggle", tint = Color.White) }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(12.dp).shadow(12.dp, RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = (if (isDarkMode) Color(0xFF262626) else Color.White).copy(alpha = 0.9f)), shape = RoundedCornerShape(12.dp), border = fineBorder) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)).background(currentColor).border(1.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f), RoundedCornerShape(8.dp))) { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))) }
                    Column(modifier = Modifier.weight(1f)) { Text(text = ColorManager.colorToHex(currentColor).uppercase(), color = if (isDarkMode) Color.White else Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(text = "RGB: (${(currentColor.red * 255).toInt()}, ${(currentColor.green * 255).toInt()}, ${(currentColor.blue * 255).toInt()})", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = { scope.launch { flashAnim.snapTo(1f); flashAnim.animateTo(0f, animationSpec = tween(300)) }; onColorConfirmed(currentColor) }, modifier = Modifier.size(40.dp).then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(uiAccentColor, CircleShape)).clip(CircleShape)) { Icon(Icons.Default.Check, "Capture", tint = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) }
                }
            }
        }
    }
}

@Composable
fun CameraSniper(onColorCaptured: (Color) -> Unit, onColorConfirmed: (Color) -> Unit) {
    val context = LocalContext.current; val lifecycleOwner = LocalLifecycleOwner.current; val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }; var zoomRatio by remember { mutableFloatStateOf(1f) }; var lastColor by remember { mutableStateOf(Color.White) }
    var crosshairOffset by remember { mutableStateOf(Offset.Zero) }; var containerSize by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier = Modifier.fillMaxSize().onSizeChanged { containerSize = it }.pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> val nZ = (zoomRatio * zoom).coerceIn(1f, 10f); zoomRatio = nZ; cameraControl?.setZoomRatio(nZ) } }.pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); crosshairOffset = Offset((crosshairOffset.x + dragAmount.x).coerceIn(-containerSize.width/2f + 20.dp.toPx(), containerSize.width/2f - 20.dp.toPx()), (crosshairOffset.y + dragAmount.y).coerceIn(-containerSize.height/2f + 20.dp.toPx(), containerSize.height/2f - 20.dp.toPx())) } }.pointerInput(Unit) { detectTapGestures { onColorConfirmed(lastColor) } }) {
        AndroidView(factory = { ctx -> val pV = PreviewView(ctx); val executor = ContextCompat.getMainExecutor(ctx); cameraProviderFuture.addListener({ val cP = cameraProviderFuture.get(); val p = Preview.Builder().build().also { it.surfaceProvider = pV.surfaceProvider }; val iA = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build(); iA.setAnalyzer(executor) { iP -> 
                    val yB = iP.planes[0].buffer; val uB = iP.planes[1].buffer; val vB = iP.planes[2].buffer; val w = iP.width; val h = iP.height
                    val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat(); val bufferRatio = h.toFloat() / w.toFloat()
                    var scale: Float; var offsetX = 0f; var offsetY = 0f
                    if (containerRatio > bufferRatio) { scale = containerSize.width.toFloat() / h.toFloat(); offsetY = (scale * w - containerSize.height) / 2f } else { scale = containerSize.height.toFloat() / w.toFloat(); offsetX = (scale * h - containerSize.width) / 2f }
                    val screenX = containerSize.width / 2f + crosshairOffset.x; val screenY = containerSize.height / 2f + crosshairOffset.y
                    val stretchedX = screenX + offsetX; val stretchedY = screenY + offsetY
                    val normX = stretchedX / (h * scale); val normY = stretchedY / (w * scale)
                    val centerX = (normY * w).toInt().coerceIn(0, w - 1); val centerY = ((1f - normX) * h).toInt().coerceIn(0, h - 1)
                    var sY = 0L; var sU = 0L; var sV = 0L; val s = 8; val startX = (centerX - s / 2).coerceIn(0, w - s); val startY = (centerY - s / 2).coerceIn(0, h - s)
                    for (x in 0 until s) for (y in 0 until s) { val px = startX + x; val py = startY + y; sY += yB.get(py * w + px).toInt() and 0xFF; val uvI = (py / 2) * (iP.planes[1].rowStride) + (px / 2) * (iP.planes[1].pixelStride); if (uvI < uB.remaining()) sU += uB.get(uvI).toInt() and 0xFF; if (uvI < vB.remaining()) sV += vB.get(uvI).toInt() and 0xFF }
                    val aY = (sY / (s * s)).toFloat(); val aU = (sU / (s * s)).toFloat() - 128f; val aV = (sV / (s * s)).toFloat() - 128f
                    val r = (aY + 1.402f * aV).coerceIn(0f, 255f); val g = (aY - 0.344136f * aU - 0.714136f * aV).coerceIn(0f, 255f); val b = (aY + 1.772f * aU).coerceIn(0f, 255f)
                    val nC = Color(r / 255f, g / 255f, b / 255f); val lC = Color(lastColor.red * 0.8f + nC.red * 0.2f, lastColor.green * 0.8f + nC.green * 0.2f, lastColor.blue * 0.8f + nC.blue * 0.2f); lastColor = lC; onColorCaptured(lC); iP.close() }
                try { cP.unbindAll(); val c = cP.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, p, iA); cameraControl = c.cameraControl } catch (e: Exception) { e.printStackTrace() } }, executor); pV }, modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) { val center = Offset(size.width / 2f + crosshairOffset.x, size.height / 2f + crosshairOffset.y); drawCircle(Color.White, radius = 14.dp.toPx(), center = center, style = Stroke(2.5.dp.toPx())); drawLine(Color.White, Offset(center.x - 28.dp.toPx(), center.y), Offset(center.x + 28.dp.toPx(), center.y), strokeWidth = 2.dp.toPx()); drawLine(Color.White, Offset(center.x, center.y - 28.dp.toPx()), Offset(center.x, center.y + 28.dp.toPx()), strokeWidth = 2.dp.toPx()); drawCircle(lastColor, radius = 4.dp.toPx(), center = center) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(harmonyColors: List<Color>, onDismiss: () -> Unit, currentColor: Color, isGoldMode: Boolean, isDarkMode: Boolean) {
    val clipboard = LocalClipboardManager.current; val context = LocalContext.current
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth(0.9f).shadow(24.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(if (isDarkMode) Color.Black else (if (ColorManager.isDark(currentColor)) Color(0xFF1A1A1A) else Color.White)).then(if (isGoldMode) Modifier.goldBorder(RoundedCornerShape(24.dp)) else Modifier)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("EXPORT PALETTE", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp, color = if (isDarkMode) Color.White else (if (ColorManager.isDark(currentColor)) Color.White else Color.Black)), modifier = if (isGoldMode) Modifier.goldMask() else Modifier)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Pair("CSS Variables") { clipboard.setText(AnnotatedString(harmonyColors.mapIndexed { i, c -> "--color-${i+1}: ${ColorManager.colorToHex(c)};" }.joinToString("\n"))) }, Pair("JSON Array") { clipboard.setText(AnnotatedString("[\n" + harmonyColors.joinToString(",\n") { "  \"${ColorManager.colorToHex(it)}\"" } + "\n]")) }, Pair("Android XML") { clipboard.setText(AnnotatedString(harmonyColors.mapIndexed { i, c -> "<color name=\"palette_${i+1}\">${ColorManager.colorToHex(c)}</color>" }.joinToString("\n"))) }).forEach { (label, action) ->
                    Surface(onClick = { action(); Toast.makeText(context, "$label Copied!", Toast.LENGTH_SHORT).show(); onDismiss() }, modifier = Modifier.fillMaxWidth().height(48.dp).shadow(4.dp, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), color = if (isGoldMode) Color.Transparent else currentColor, border = BorderStroke(1.dp, Color.White.copy(0.3f))) { Box(modifier = Modifier.fillMaxSize().then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(currentColor)) Color.White else Color.Black)) } }
                }
            }
            TextButton(onClick = onDismiss) { Text("CLOSE", fontWeight = FontWeight.Bold, color = Color.Gray) }
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
            Spacer(Modifier.height(32.dp)); Text("Version 1.0.4 PRO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = sectionTitleColor.copy(0.5f)); Text("Created with ❤️ by JAYLIZ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = sectionTitleColor.copy(0.3f))
        }
    }
}

@Composable
fun SettingsSection(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) { Column(modifier = Modifier.fillMaxWidth()) { Text(title, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = color, letterSpacing = 1.5.sp)); Spacer(Modifier.height(8.dp)); Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } } }

@Composable
fun SettingsItem(icon: ImageVector, title: String, bgColor: Color, textColor: Color, isDarkMode: Boolean, iconColor: Color? = null, onClick: (() -> Unit)? = null, content: @Composable (() -> Unit)? = null) { Surface(onClick = { onClick?.invoke() }, enabled = onClick != null, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), color = bgColor, border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(0.1f) else Color.Black.copy(0.05f))) { Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = iconColor ?: (if (isDarkMode) Color.Gray else Color.DarkGray), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(16.dp)); Text(title, modifier = Modifier.weight(1f), style = TextStyle(fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)); content?.invoke() } } }

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
