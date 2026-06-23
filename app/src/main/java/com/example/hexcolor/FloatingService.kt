package com.example.hexcolor

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.TypedValue

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var harmonyRow: LinearLayout
    private lateinit var hexText: TextView

    companion object {
        var currentHex: String = "#FFFFFF"
        var currentHarmony: List<String> = emptyList()
        var originalIndex: Int = 0
        var isDarkMode: Boolean = true
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        this.root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(12f)
            setPadding(padding, padding, padding, padding)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        hexText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        val closeButton = TextView(this).apply {
            text = "✕"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dpToPx(8f), 0, dpToPx(8f), 0)
            setOnClickListener { stopSelf() }
        }

        header.addView(hexText)
        header.addView(spacer)
        header.addView(closeButton)
        root.addView(header)

        harmonyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val marginTop = dpToPx(8f)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, marginTop, 0, 0)
            layoutParams = params
        }
        
        root.addView(harmonyRow)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 200
        }

        updateUI()

        this.root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var lastClickTime: Long = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        
                        val clickTime = System.currentTimeMillis()
                        if (clickTime - lastClickTime < 300) {
                            val intent = Intent(this@FloatingService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                            stopSelf()
                        }
                        lastClickTime = clickTime
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(root, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(root, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateUI()
        return START_NOT_STICKY
    }

    private fun updateUI() {
        if (!::harmonyRow.isInitialized) return
        
        // Colores exactos sincronizados con la App (Gris Plata en modo Luz)
        val bgColor = if (isDarkMode) 0xFF262626.toInt() else 0xFFDDDDDD.toInt()
        val textColor = if (isDarkMode) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val borderColor = if (isDarkMode) 0xFF444444.toInt() else 0xFFCCCCCC.toInt()

        val background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dpToPx(16f).toFloat()
            setStroke(dpToPx(1.5f), borderColor)
        }
        root.background = background
        
        hexText.text = currentHex
        hexText.setTextColor(textColor)
        
        harmonyRow.removeAllViews()

        currentHarmony.forEachIndexed { index, colorHex ->
            val square = View(this).apply {
                val size = dpToPx(38f)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, 0, dpToPx(8f), 0)
                }
                
                val bg = GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(colorHex))
                    cornerRadius = dpToPx(6f).toFloat()
                    if (index == originalIndex) {
                        setStroke(dpToPx(2.5f), if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                    }
                }
                this.background = bg
                
                setOnClickListener {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("HEX", colorHex)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@FloatingService, "Copiado: $colorHex", Toast.LENGTH_SHORT).show()
                }
            }
            harmonyRow.addView(square)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::root.isInitialized) windowManager.removeView(root)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
