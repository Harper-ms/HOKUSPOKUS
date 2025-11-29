package com.focuslock2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.focuslock2.ui.theme.FocusLock2Theme
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.net.Uri
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context

// --- Utility Functions for Permission Checks ---

/**
 * Checks if the Accessibility Service (FocusLockAccessibilityService) is currently enabled by the user.
 */
fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

    for (enabledService in enabledServices) {
        if (enabledService.resolveInfo.serviceInfo.packageName == context.packageName &&
            enabledService.resolveInfo.serviceInfo.name == serviceClass.name) {
            return true
        }
    }
    return false
}

/**
 * Launches the Intent to request the "Display over other apps" (Overlay) permission.
 */
fun requestOverlayPermission(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * Launches the Intent to guide the user to the Accessibility Settings screen.
 */
fun requestAccessibilityPermission(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

// --- Main Activity and Composable UI ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusLock2Theme {
                // Ensure permissions are checked on app start
                ChildHome()
            }
        }
    }
}

@Composable
fun PermissionStatusSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // State to track permission status
    // Use key to trigger re-read of system settings on composition
    val overlayGranted = remember(Settings.canDrawOverlays(context)) { mutableStateOf(Settings.canDrawOverlays(context)) }
    val accessibilityGranted = remember(isAccessibilityServiceEnabled(context, FocusLockAccessibilityService::class.java)) {
        mutableStateOf(isAccessibilityServiceEnabled(context, FocusLockAccessibilityService::class.java))
    }

    val allGranted = overlayGranted.value && accessibilityGranted.value

    // Effect to check permissions when the Activity resumes (e.g., returning from Settings)
    DisposableEffect(context) {
        // Since we are using remember(key), this initial check is less critical,
        // but still good practice to capture the initial state.
        val checkPermissions = {
            overlayGranted.value = Settings.canDrawOverlays(context)
            accessibilityGranted.value = isAccessibilityServiceEnabled(context, FocusLockAccessibilityService::class.java)
        }
        checkPermissions() // Initial check

        // Using remember(key) is a common way to trigger re-composition on system changes in Compose

        onDispose { /* Cleanup if any listeners were attached */ }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Permissions Check",
                style = MaterialTheme.typography.titleMedium,
                color = if (allGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))

            if (allGranted) {
                Text("All required permissions are granted. Service is active.", style = MaterialTheme.typography.bodyMedium)
            } else {
                if (!overlayGranted.value) {
                    Text("1. Overlay Permission: Denied", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { requestOverlayPermission(context) }) {
                        Text("Grant Overlay (Display Over Other Apps)")
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Text("1. Overlay Permission: Granted", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                }

                if (!accessibilityGranted.value) {
                    Text("2. Accessibility Service: Disabled", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { requestAccessibilityPermission(context) }) {
                        Text("Enable Accessibility Service")
                    }
                } else {
                    Text("2. Accessibility Service: Enabled", color = MaterialTheme.colorScheme.onSurface)
                }

                // NEW INSTRUCTION FOR THE USER
                Spacer(Modifier.height(16.dp))
                Text(
                    "Note: Tap the buttons above, grant the permission in Settings, and then press the BACK button to return to this screen to verify the status.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}


@Composable
fun ChildHome() {
    val context = LocalContext.current
    val pm = context.packageManager

    // NOTE: Relying on the existence of the Storage object provided by you.
    val tasksState = remember { mutableStateOf(Storage.getTasks(context)) }
    val lockedAppsState = remember { mutableStateOf(Storage.getLockedApps(context)) }
    val completedState = remember { mutableStateOf(Storage.getLogs(context)) }
    val requirePinState = remember { mutableStateOf(Storage.getRequirePin(context)) }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "tasks" -> tasksState.value = Storage.getTasks(context)
                "locked_apps" -> lockedAppsState.value = Storage.getLockedApps(context)
                "completion_logs" -> completedState.value = Storage.getLogs(context)
                "require_pin" -> requirePinState.value = Storage.getRequirePin(context)
            }
        }
        Storage.registerPrefsListener(context, listener)
        onDispose { Storage.unregisterPrefsListener(context, listener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // NEW: Permission Check UI
        PermissionStatusSection()

        Text("Tasks", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        tasksState.value.forEach { task ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(task, modifier = Modifier.weight(1f))
                Button(onClick = {
                    if (requirePinState.value) {
                        val i = Intent(context, VerifyAccessActivity::class.java)
                        i.putExtra("mode", "complete_task")
                        i.putExtra("task_name", task)
                        context.startActivity(i)
                    } else {
                        val updated = tasksState.value.toMutableList()
                        updated.remove(task)
                        tasksState.value = updated
                        Storage.setTasks(context, updated)
                        val timestamp = System.currentTimeMillis()
                        val formatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                        Storage.addLog(context, "$formatted: $task — Completed")
                        completedState.value = Storage.getLogs(context)
                    }
                }) { Text(if (requirePinState.value) "Complete (PIN)" else "Complete") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Locked Apps", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if(lockedAppsState.value.isEmpty()) {
            Text("None")
        } else {
            lockedAppsState.value.forEach { pkg ->
                val label = try {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(ai).toString()
                } catch (_: Exception) { pkg }
                Text(label)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Completed", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (completedState.value.isEmpty()) {
            Text("No completed tasks yet")
        } else {
            completedState.value.forEach { log -> Text(log) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            // Assuming VerifyAccessActivity is used to gate access to the main settings area
            val settingsIntent = Intent(context, VerifyAccessActivity::class.java)
            settingsIntent.putExtra("mode", "settings") // Optional: Pass mode to verify access
            context.startActivity(settingsIntent)
        }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
    }
}