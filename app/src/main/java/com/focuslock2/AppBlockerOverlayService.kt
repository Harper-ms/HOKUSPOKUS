package com.focuslock2

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.focuslock2.R

class AppBlockerOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var blockedPackage: String? = null
    private var countdownView: TextView? = null
    private var handler: android.os.Handler? = null
    private var updateRunnable: Runnable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val newBlockedPackage = intent?.getStringExtra("blocked_package")
        if (!Settings.canDrawOverlays(this)) {
            Log.e("AppBlockerOverlayService", "Overlay permission not granted")
            return START_NOT_STICKY
        }
        // If the package changed, update the overlay
        if (blockedPackage != newBlockedPackage) {
            blockedPackage = newBlockedPackage
            hideOverlay()  // Remove old overlay to refresh
        }
        showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return  // Already showing; no need to re-add
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xCC000000.toInt())
            setPadding(48, 48, 48, 48)
            val title = TextView(context).apply {
                text = getString(R.string.access_denied)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 22f
            }
            val pkgText = TextView(context).apply {
                val label = try {
                    val pm = packageManager
                    val ai = pm.getApplicationInfo(blockedPackage ?: "", 0)
                    pm.getApplicationLabel(ai).toString()
                } catch (e: Exception) {
                    Log.e("AppBlockerOverlayService", "Error getting app label", e)
                    blockedPackage ?: ""
                }
                text = label
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
            }
            countdownView = TextView(context).apply {
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 18f
            }
            val button = Button(context).apply {
                text = getString(R.string.go_home)
                setOnClickListener {
                    val i = Intent(context, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(i)
                    stopSelf()
                }
            }
            addView(title)
            addView(pkgText)
            addView(countdownView)
            addView(button)
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT  // Safer fallback than TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        overlayView = layout
        try {
            windowManager?.addView(layout, params)
            layout.alpha = 0f
            layout.animate().alpha(1f).setDuration(250).start()
            startCountdown()
        } catch (e: Exception) {
            Log.e("AppBlockerOverlayService", "Failed to add overlay view", e)
            stopSelf()
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("AppBlockerOverlayService", "Failed to remove overlay view", e)
            }
        }
        overlayView = null
        stopCountdown()
    }

    private fun startCountdown() {
        val pkg = blockedPackage
        handler = android.os.Handler(android.os.Looper.getMainLooper())
        updateRunnable = Runnable {
            val until: Long? = try {
                if (pkg == null) null else Storage.getBlockedUntil(this@AppBlockerOverlayService)[pkg]
            } catch (e: Exception) {
                Log.e("AppBlockerOverlayService", "Error accessing Storage", e)
                null
            }
            val now = System.currentTimeMillis()
            val remaining = if (until != null) until - now else Long.MAX_VALUE
            if (until != null && remaining <= 0L) {
                stopSelf()
                return@Runnable
            }
            val text = if (until != null) {
                val h = remaining / 3600000
                val m = (remaining % 3600000) / 60000
                val s = (remaining % 60000) / 1000
                getString(R.string.time_remaining_format, h, m, s)
            } else {
                getString(R.string.locked)
            }
            countdownView?.text = text
            handler?.postDelayed(updateRunnable!!, 1000)
        }
        handler?.post(updateRunnable!!)
        // Removed the 10-second auto-stop to avoid conflicts with the timer
    }

    private fun stopCountdown() {
        updateRunnable?.let { handler?.removeCallbacks(it) }
        updateRunnable = null
        handler = null
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
