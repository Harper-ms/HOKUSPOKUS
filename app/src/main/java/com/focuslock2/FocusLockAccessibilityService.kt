package com.focuslock2

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusLockAccessibilityService : AccessibilityService() {
    private var lastPackage: String? = null
    private var lastLaunchAt: Long = 0L
    private var overlayShownAt: Long = 0L
    private var stopHandler: android.os.Handler? = null
    private var stopRunnable: Runnable? = null
    private var showHandler: android.os.Handler? = null
    private var showRunnable: Runnable? = null
    private val SHOW_DELAY_MS = 400L
    private val MIN_HOLD_MS = 10_000L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val type = event?.eventType ?: return
            if (
                type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            ) return
            val pkg = event.packageName?.toString() ?: return
            if (pkg == packageName) return
            val now = System.currentTimeMillis()
            if (pkg == lastPackage && now - lastLaunchAt < 1500) return

            // Check if app is blocked (with logging for debugging)
            val locked = try {
                Storage.getLockedApps(applicationContext)
            } catch (e: Exception) {
                Log.e("FocusLockAccessibilityService", "Error getting locked apps", e)
                emptySet<String>()
            }
            val isTimeBlocked = try {
                Storage.isBlocked(applicationContext, pkg)
            } catch (e: Exception) {
                Log.e("FocusLockAccessibilityService", "Error checking if blocked", e)
                false
            }

            if (locked.contains(pkg) || isTimeBlocked) {
                Log.d("FocusLockAccessibilityService", "Blocking app: $pkg")
                lastPackage = pkg
                lastLaunchAt = now

                // Immediately navigate to home to prevent access
                performGlobalAction(GLOBAL_ACTION_HOME)

                showHandler = showHandler ?: android.os.Handler(android.os.Looper.getMainLooper())
                showRunnable?.let { showHandler?.removeCallbacks(it) }
                showRunnable = Runnable {
                    overlayShownAt = System.currentTimeMillis()
                    try {
                        val overlay = Intent(applicationContext, AppBlockerOverlayService::class.java)
                        overlay.putExtra("blocked_package", pkg)
                        startService(overlay)
                    } catch (e: Exception) {
                        Log.e("FocusLockAccessibilityService", "Error starting overlay", e)
                    }
                }
                showHandler?.postDelayed(showRunnable!!, SHOW_DELAY_MS)
            } else {
                val elapsed = if (overlayShownAt == 0L) Long.MAX_VALUE else now - overlayShownAt
                val delay = if (elapsed >= MIN_HOLD_MS) 0L else MIN_HOLD_MS - elapsed
                stopHandler = stopHandler ?: android.os.Handler(android.os.Looper.getMainLooper())
                stopRunnable?.let { stopHandler?.removeCallbacks(it) }
                stopRunnable = Runnable {
                    try {
                        stopService(Intent(applicationContext, AppBlockerOverlayService::class.java))
                        Log.d("FocusLockAccessibilityService", "Stopped overlay for $pkg")
                    } catch (e: Exception) {
                        Log.e("FocusLockAccessibilityService", "Error stopping overlay", e)
                    }
                    overlayShownAt = 0L
                }
                if (delay <= 0L) {
                    stopRunnable?.run()
                } else {
                    stopHandler?.postDelayed(stopRunnable!!, delay)
                }
            }
        } catch (e: Exception) {
            Log.e("FocusLockAccessibilityService", "Error in onAccessibilityEvent", e)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        try {
            showRunnable?.let { showHandler?.removeCallbacks(it) }
            stopRunnable?.let { stopHandler?.removeCallbacks(it) }
        } catch (e: Exception) {
            Log.e("FocusLockAccessibilityService", "Error in onDestroy", e)
        }
        showRunnable = null
        stopRunnable = null
        super.onDestroy()
    }
}