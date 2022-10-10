package com.moki.midisplayrate

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.*
import android.view.Display.DEFAULT_DISPLAY
import android.view.MotionEvent.*
import android.view.View.OnTouchListener
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import java.lang.reflect.Field

@SuppressLint("ClickableViewAccessibility")
class FloatWindowService : AccessibilityService() {

    companion object {
        private const val SHOW_WINDOW = "com.moki.midisplayrate.showWindow"
        private const val FLOAT_INIT_X = 0
        private const val FLOAT_INIT_Y = 400

        fun showFloatWindow(context: Context) {
            context.startService(
                Intent(context, FloatWindowService::class.java).apply {
                    action = SHOW_WINDOW
                }
            )
        }
    }

    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val floatWindow by lazy {
        View(this).apply {
            alpha = 0.2f
        }
    }

    private val floatLayoutParams by lazy {
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            format = PixelFormat.TRANSPARENT
            width = 30.dp()
            height = 30.dp()
            gravity = Gravity.START or Gravity.TOP
            x = FLOAT_INIT_X
            y = FLOAT_INIT_Y
        }
    }

    private var isSpeedMode: Boolean = false
    private var lowRateModeTimerJob: Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when(event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isSpeedMode) {
                    setHighRate()
                    startLowRateTimer(500)
                }
            }
        }
    }

    override fun onInterrupt() {

    }

    override fun onCreate() {
        super.onCreate()
        setFloatViewBySpeedMode()
        contentResolver.registerContentObserver(
            Settings.System.getUriFor("speed_mode"),
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    setFloatViewBySpeedMode()
                }
            }
        )
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when(intent?.action) {
                        Intent.ACTION_USER_PRESENT -> {
                            if (isSpeedMode) {
                                setHighRate()
                                startLowRateTimer()
                            }
                        }
                    }
                }
            },
            IntentFilter(Intent.ACTION_USER_PRESENT)
        )
    }

    private fun speedMode(): Boolean {
        return try {
            Settings.System.getInt(contentResolver, "speed_mode") > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun setSpeedMode(isSpeedMode: Boolean) {
        Settings.System.putInt(contentResolver, "speed_mode", if (isSpeedMode) 1 else 0)
    }

    private fun setFloatViewBySpeedMode() {
        isSpeedMode = speedMode()
        floatWindow.apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setSize(1000, 1000)
                setColor(
                    if (isSpeedMode) {
                        Color.GREEN
                    } else {
                        Color.RED
                    }
                )
            }

            setOnTouchListener(
                object : OnTouchListener {
                    private var initX = FLOAT_INIT_X.toFloat()
                    private var initY = FLOAT_INIT_Y.toFloat()
                    private var timeDown = System.currentTimeMillis()
                    private var isLongClick = false

                    override fun onTouch(v: View?, event: MotionEvent): Boolean {
                        when(event.action) {
                            ACTION_OUTSIDE -> {
                                if (isSpeedMode) {
                                    setHighRate()
                                    startLowRateTimer()
                                }
                            }

                            ACTION_DOWN -> {
                                initX = event.x
                                initY = event.y
                                timeDown = System.currentTimeMillis()
                            }

                            ACTION_MOVE -> {
                                if (System.currentTimeMillis() - timeDown > ViewConfiguration.getLongPressTimeout()) {
                                    isLongClick = true
                                }
                            }

                            ACTION_UP -> {
                                if (!isLongClick) {
                                    if (isSpeedMode) {
                                        setHighRate()
                                        startLowRateTimer()
                                        setSpeedMode(false)
                                    } else {
                                        setSpeedMode(true)
                                    }
                                }
                            }
                        }
                        return true
                    }
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == SHOW_WINDOW) {
            showFloatWindow()
        }
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
        val field = fieldMeta.invoke(WindowManager.LayoutParams::class.java, "preferredMaxDisplayRefreshRate").run {
            this as Field
        }
        if (field.get(floatLayoutParams) != rate) {
            field.set(floatLayoutParams, rate)
        }
        windowManager.updateViewLayout(floatWindow, floatLayoutParams)
    }

    private fun setHighRate() {
        setRate(120f)
    }

    private fun setLowRate() {
        setRate(60f)
    }

    private fun startLowRateTimer(delay: Long = 1500) {
        if (lowRateModeTimerJob?.isActive == true) {
            lowRateModeTimerJob?.cancel()
            lowRateModeTimerJob = null
        }
        lowRateModeTimerJob = CoroutineScope(Dispatchers.Default).launch {
            delay(delay)
            withContext(Dispatchers.Main) {
                if (isSpeedMode) {
                    setLowRate()
                }
            }
        }
    }
}