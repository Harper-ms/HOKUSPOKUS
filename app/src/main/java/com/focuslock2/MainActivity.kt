package com.focuslock2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.focuslock2.ui.theme.FocusLock2Theme
import android.content.Intent
import android.content.SharedPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusLock2Theme {
                ChildHome()
            }
        }
    }
}

@Composable
fun ChildHome() {
    val context = LocalContext.current
    val pm = context.packageManager
    val tasksState = remember { mutableStateOf(Storage.getTasks(context)) }
    val lockedAppsState = remember { mutableStateOf(Storage.getLockedApps(context)) }
    val completedState = remember { mutableStateOf(Storage.getLogs(context)) }
    val requirePinState = remember { mutableStateOf(Storage.getRequirePin(context)) }
    androidx.compose.runtime.DisposableEffect(Unit) {
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
            context.startActivity(Intent(context, VerifyAccessActivity::class.java))
        }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }


    }
}
