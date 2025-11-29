package com.focuslock2

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Existing data class (unchanged)
data class AppTimeout(val app: String, val timeout: String)

// New: Data class for app info (from the snippet)
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)

// New: Function to get installed apps (from the snippet, with minor tweaks for Compose)
fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    return pm.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { appInfo ->
            // Exclude system apps
            appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0
        }
        .map { appInfo ->
            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                appInfo.packageName  // Fallback
            }
            val icon = try {
                appInfo.loadIcon(pm)
            } catch (e: Exception) {
                ColorDrawable(Color.GRAY)  // Fallback icon
            }
            AppInfo(appInfo.packageName, appName, icon)
        }
        .sortedBy { it.appName }  // Sort for better UX
}

class SetTimeoutAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SetTimeoutAppsScreen() }
    }
}

@Composable
fun SetTimeoutAppsScreen() {
    val context = LocalContext.current
    var apps by remember {
        mutableStateOf(
            try {
                // Fetch real installed apps and map to AppTimeout with default timeouts
                getInstalledApps(context).map { appInfo ->
                    AppTimeout(appInfo.appName, "00:00")  // Default timeout; customize as needed
                }
            } catch (e: Exception) {
                // Fallback to empty list or error message
                emptyList<AppTimeout>()
            }
        )
    }

    // If no apps, show a message
    if (apps.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("No apps found or error loading apps.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SET APPLICATIONS FOR TIMEOUT", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(apps) { index, app ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${app.app} — ", modifier = Modifier.weight(1f))
                    // Editable timeout field (basic; expand with time picker if needed)
                    var timeout by remember { mutableStateOf(app.timeout) }
                    OutlinedTextField(
                        value = timeout,
                        onValueChange = {
                            timeout = it
                            // Update the list (in a real app, save to Storage or SharedPreferences)
                            apps = apps.toMutableList().apply { this[index] = app.copy(timeout = it) }
                        },
                        label = { Text("Timeout (MM:SS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // Add a placeholder app (e.g., for custom entry)
            apps = apps + AppTimeout("Custom App", "00:10")
        }, modifier = Modifier.fillMaxWidth()) {
            Text("+ ADD APP")
        }
    }
}
