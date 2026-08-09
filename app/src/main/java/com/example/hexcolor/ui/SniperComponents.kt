package com.example.hexcolor.ui

import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hexcolor.ColorManager
import com.example.hexcolor.SniperState
import kotlinx.coroutines.launch

@Composable
fun SniperGodOverlay(state: SniperState, isDarkMode: Boolean, isGoldMode: Boolean, initialColor: Color, uiAccentColor: Color, onStateChange: (SniperState) -> Unit, onColorConfirmed: (Color) -> Unit) {
    var liveColor by remember { mutableStateOf(initialColor) }
    val buttonShape = RoundedCornerShape(16.dp); val fineBorder = BorderStroke(1.5.dp, if (isGoldMode) Color(0xFFC29B47) else (if (isDarkMode) Color.White.copy(0.3f) else Color.Black.copy(0.2f)))
    val flashAnim = remember { Animatable(0f) }; val scope = rememberCoroutineScope()
    val modifier = if (state == SniperState.FULLSCREEN) Modifier.fillMaxSize().background(Color.Black) else Modifier.fillMaxSize().padding(16.dp).wrapContentSize(Alignment.TopCenter).size(width = 320.dp, height = 450.dp).shadow(24.dp, buttonShape).clip(buttonShape).background(if (isDarkMode) Color.Black else Color.White).border(fineBorder, buttonShape)
    Box(modifier = modifier) {
        CameraSniper(onColorCaptured = { liveColor = it }, onColorConfirmed = { scope.launch { flashAnim.snapTo(1f); flashAnim.animateTo(0f, animationSpec = tween(300)) }; onColorConfirmed(it) })
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAnim.value)))
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onStateChange(SniperState.OFF) }, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                Text(text = if (state == SniperState.FULLSCREEN) "FULLSCREEN SNIPER" else "WINDOWED SNIPER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.shadow(4.dp))
                IconButton(onClick = { onStateChange(if (state == SniperState.FULLSCREEN) SniperState.WINDOWED else SniperState.FULLSCREEN) }, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) { Icon(if (state == SniperState.FULLSCREEN) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Toggle", tint = Color.White) }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(12.dp).shadow(12.dp, RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = (if (isDarkMode) Color(0xFF262626) else Color.White).copy(alpha = 0.9f)), shape = RoundedCornerShape(12.dp), border = fineBorder) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)).background(liveColor).border(1.dp, if (isDarkMode) Color.White.copy(0.2f) else Color.Black.copy(0.1f), RoundedCornerShape(8.dp))) { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent)))) }
                    Column(modifier = Modifier.weight(1f)) { Text(text = ColorManager.colorToHex(liveColor).uppercase(), color = if (isDarkMode) Color.White else Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(text = "RGB: (${(liveColor.red * 255).toInt()}, ${(liveColor.green * 255).toInt()}, ${(liveColor.blue * 255).toInt()})", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = { scope.launch { flashAnim.snapTo(1f); flashAnim.animateTo(0f, animationSpec = tween(300)) }; onColorConfirmed(liveColor) }, modifier = Modifier.size(40.dp).then(if (isGoldMode) Modifier.goldButtonStyle() else Modifier.background(uiAccentColor, CircleShape)).clip(CircleShape)) { Icon(Icons.Default.Check, "Capture", tint = if (isGoldMode) Color(0xFF543B14) else (if (ColorManager.isDark(uiAccentColor)) Color.White else Color.Black)) }
                }
            }
        }
    }
}

@Composable
fun CameraSniper(onColorCaptured: (Color) -> Unit, onColorConfirmed: (Color) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var lastColor by remember { mutableStateOf(Color.White) }
    var crosshairOffset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { containerSize = it }
        .pointerInput(Unit) {
            detectTransformGestures { _, _, zoom, _ ->
                val nZ = (zoomRatio * zoom).coerceIn(1f, 10f)
                zoomRatio = nZ
                cameraControl?.setZoomRatio(nZ)
            }
        }
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                crosshairOffset = Offset(
                    (crosshairOffset.x + dragAmount.x).coerceIn(-containerSize.width / 2f + 20.dp.toPx(), containerSize.width / 2f - 20.dp.toPx()),
                    (crosshairOffset.y + dragAmount.y).coerceIn(-containerSize.height / 2f + 20.dp.toPx(), containerSize.height / 2f - 20.dp.toPx())
                )
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { onColorConfirmed(lastColor) }
        }
    ) {
        AndroidView(
            factory = { ctx ->
                val pV = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cP = cameraProviderFuture.get()
                    val p = Preview.Builder().build().also {
                        it.surfaceProvider = pV.surfaceProvider
                    }
                    val iA = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    iA.setAnalyzer(executor) { iP ->
                        val yB = iP.planes[0].buffer
                        val uB = iP.planes[1].buffer
                        val vB = iP.planes[2].buffer
                        val w = iP.width
                        val h = iP.height
                        
                        val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()
                        val bufferRatio = h.toFloat() / w.toFloat()
                        
                        var scale: Float
                        var offsetX = 0f
                        var offsetY = 0f
                        
                        if (containerRatio > bufferRatio) {
                            scale = containerSize.width.toFloat() / h.toFloat()
                            offsetY = (scale * w - containerSize.height) / 2f
                        } else {
                            scale = containerSize.height.toFloat() / w.toFloat()
                            offsetX = (scale * h - containerSize.width) / 2f
                        }
                        
                        val screenX = containerSize.width / 2f + crosshairOffset.x
                        val screenY = containerSize.height / 2f + crosshairOffset.y
                        val stretchedX = screenX + offsetX
                        val stretchedY = screenY + offsetY
                        val normX = stretchedX / (h * scale)
                        val normY = stretchedY / (w * scale)
                        
                        val centerX = (normY * w).toInt().coerceIn(0, w - 1)
                        val centerY = ((1f - normX) * h).toInt().coerceIn(0, h - 1)
                        
                        var sY = 0L; var sU = 0L; var sV = 0L; val s = 8
                        val startX = (centerX - s / 2).coerceIn(0, w - s)
                        val startY = (centerY - s / 2).coerceIn(0, h - s)
                        
                        for (x in 0 until s) {
                            for (y in 0 until s) {
                                val px = startX + x
                                val py = startY + y
                                sY += yB.get(py * w + px).toInt() and 0xFF
                                val uvI = (py / 2) * (iP.planes[1].rowStride) + (px / 2) * (iP.planes[1].pixelStride)
                                if (uvI < uB.remaining()) sU += uB.get(uvI).toInt() and 0xFF
                                if (uvI < vB.remaining()) sV += vB.get(uvI).toInt() and 0xFF
                            }
                        }
                        
                        val aY = (sY / (s * s)).toFloat()
                        val aU = (sU / (s * s)).toFloat() - 128f
                        val aV = (sV / (s * s)).toFloat() - 128f
                        
                        val r = (aY + 1.402f * aV).coerceIn(0f, 255f)
                        val g = (aY - 0.344136f * aU - 0.714136f * aV).coerceIn(0f, 255f)
                        val b = (aY + 1.772f * aU).coerceIn(0f, 255f)
                        
                        val nC = Color(r / 255f, g / 255f, b / 255f)
                        val lC = Color(lastColor.red * 0.8f + nC.red * 0.2f, lastColor.green * 0.8f + nC.green * 0.2f, lastColor.blue * 0.8f + nC.blue * 0.2f)
                        lastColor = lC
                        onColorCaptured(lC)
                        iP.close()
                    }
                    try {
                        cP.unbindAll()
                        val c = cP.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, p, iA)
                        cameraControl = c.cameraControl
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)
                pV
            },
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + crosshairOffset.x, size.height / 2f + crosshairOffset.y)
            drawCircle(Color.White, radius = 14.dp.toPx(), center = center, style = Stroke(2.5.dp.toPx()))
            drawLine(Color.White, Offset(center.x - 28.dp.toPx(), center.y), Offset(center.x + 28.dp.toPx(), center.y), strokeWidth = 2.dp.toPx())
            drawLine(Color.White, Offset(center.x, center.y - 28.dp.toPx()), Offset(center.x, center.y + 28.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawCircle(lastColor, radius = 4.dp.toPx(), center = center)
        }
    }
}
