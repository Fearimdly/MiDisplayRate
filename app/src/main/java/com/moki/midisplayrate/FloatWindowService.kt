package com.moki.midisplayrate

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import kotlinx.coroutines.*
import java.lang.reflect.Field

@SuppressLint("ClickableViewAccessibility")
class FloatWindowService : Service() {
    private val windowManager by lazy {
        application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val floatWindow by lazy {
        View(this).apply {
            setBackgroundColor(Color.BLUE)
            setOnTouchListener { _, _ ->
                setHighRate()
                startLowRateTimer()
                true
            }
        }
    }

    private val floatLayoutParams by lazy {
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            format = PixelFormat.TRANSPARENT
            width = 30
            height = MATCH_PARENT
            gravity = Gravity.START
        }
    }

    private var lowRateModeTimerJob: Job? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFloatWindow()
        startLowRateTimer()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun isFloatWindowShown(): Boolean {
        return floatWindow.windowToken != null
    }

    private fun showFloatWindow() {
        if (isFloatWindowShown()) return
        windowManager.addView(floatWindow, floatLayoutParams)
    }

    private fun hideFloatWindow() {
        if (!isFloatWindowShown()) return
        windowManager.removeView(floatWindow)
    }

    private fun setRate(rate: Float) {
        if (!isFloatWindowShown()) return
        val fieldMeta = Class::class.java.getDeclaredMethod("getDeclaredField", String::class.java)
        fieldMeta.invoke(WindowManager.LayoutParams::class.java, "preferredMaxDisplayRefreshRate").run {
            this as Field
        }.set(floatLayoutParams, rate)
        windowManager.updateViewLayout(floatWindow, floatLayoutParams)
    }

    private fun setHighRate() {
        setRate(120f)
    }

    private fun setLowRate() {
        setRate(60f)
    }

    private fun startLowRateTimer() {
        if (lowRateModeTimerJob?.isActive == true) {
            lowRateModeTimerJob?.cancel()
            lowRateModeTimerJob = null
        }
        lowRateModeTimerJob = CoroutineScope(Dispatchers.Default).launch {
            delay(1500)
            withContext(Dispatchers.Main) {
                setLowRate()
            }
        }
    }
}