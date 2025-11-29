package com.focuslock2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class VerifyAccessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("mode") ?: "parent_dashboard"
        val taskName = intent.getStringExtra("task_name")
        setContent { VerifyAccessScreen(mode, taskName) }
    }
}

@Composable
fun VerifyAccessScreen(mode: String, taskName: String?) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    var requirePin by remember { mutableStateOf(Storage.getRequirePin(context)) }

    if (!verified) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Enter PIN (0000)", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (pin == "0000") {
                    if (mode == "complete_task" && !taskName.isNullOrBlank()) {
                        val tasks = Storage.getTasks(context).toMutableList()
                        tasks.remove(taskName)
                        Storage.setTasks(context, tasks)
                        val timestamp = System.currentTimeMillis()
                        Storage.addLog(context, "$timestamp: $taskName — Completed")
                        val i = Intent(context, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                    } else {
                        verified = true
                    }
                } else {
                    Toast.makeText(context, "Invalid PIN", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Verify") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { (context as? ComponentActivity)?.finish() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text("Parent Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Require PIN to complete tasks", modifier = Modifier.weight(1f))
                Switch(checked = requirePin, onCheckedChange = {
                    requirePin = it
                    Storage.setRequirePin(context, it)
                })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { context.startActivity(Intent(context, ParentDashboardActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Open Dashboard") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, CompletionLogsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("View Logs") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, OptionsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Options") }
        }
    }
}