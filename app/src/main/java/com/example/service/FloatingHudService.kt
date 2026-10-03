package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloatingHudService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.HUD_START"
        const val ACTION_STOP = "com.example.service.HUD_STOP"

        private val _isShowing = MutableStateFlow(false)
        val isShowing: StateFlow<Boolean> = _isShowing.asStateFlow()

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FloatingHudService::class.java).apply {
                    action = ACTION_START
                }
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var hudView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var tvPing: TextView
    private lateinit var tvGps: TextView
    private lateinit var tvSpeed: TextView

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> showHud()
            ACTION_STOP -> hideHud()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showHud() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        if (hudView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 150
        }

        // Programmatically build sleek cyber HUD layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#E60A0E17")) // Translucent Deep Navy
                setStroke(dpToPx(1), Color.parseColor("#00E676")) // Neon Green Border
            }
            elevation = dpToPx(8).toFloat()
        }

        // 1. Ping Text
        tvPing = TextView(this).apply {
            setTextColor(Color.parseColor("#00E676"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            text = "⚡ 0ms"
            setPadding(0, 0, dpToPx(8), 0)
        }
        rootLayout.addView(tvPing)

        // Divider
        rootLayout.addView(createDivider())

        // 2. GPS Accuracy Text
        tvGps = TextView(this).apply {
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            text = "📍 ±0.0m"
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
        }
        rootLayout.addView(tvGps)

        // Divider
        rootLayout.addView(createDivider())

        // 3. Speed Text
        tvSpeed = TextView(this).apply {
            setTextColor(Color.parseColor("#FFB300"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            text = "🏎️ 0 km/h"
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
        }
        rootLayout.addView(tvSpeed)

        // 4. Close Button (X)
        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#90A4C4"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener {
                hideHud()
            }
        }
        rootLayout.addView(closeBtn)

        // Setup Drag Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        rootLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(rootLayout, params)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(rootLayout, params)
            hudView = rootLayout
            _isShowing.value = true
        } catch (_: Exception) {
            stopSelf()
            return
        }

        // Subscribe to live telemetry from PulseForegroundService
        serviceScope.launch {
            PulseForegroundService.networkTelemetry.collect { net ->
                tvPing.text = "⚡ ${net.currentPingMs}ms"
                if (net.currentPingMs > 120) {
                    tvPing.setTextColor(Color.parseColor("#FF5252"))
                } else if (net.currentPingMs > 60) {
                    tvPing.setTextColor(Color.parseColor("#FFB300"))
                } else {
                    tvPing.setTextColor(Color.parseColor("#00E676"))
                }
            }
        }

        serviceScope.launch {
            PulseForegroundService.gpsTelemetry.collect { gps ->
                if (gps.hasLocation) {
                    tvGps.text = "📍 ±${String.format("%.1f", gps.accuracyMeters)}m"
                    tvSpeed.text = "🏎️ ${String.format("%.0f", gps.speedKmh)} km/h"
                } else {
                    tvGps.text = "📍 Mencari GPS"
                    tvSpeed.text = "🏎️ 0 km/h"
                }
            }
        }
    }

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(16))
            setBackgroundColor(Color.parseColor("#263550"))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun hideHud() {
        if (hudView != null) {
            try {
                windowManager?.removeView(hudView)
            } catch (_: Exception) {}
            hudView = null
        }
        _isShowing.value = false
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHud()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
