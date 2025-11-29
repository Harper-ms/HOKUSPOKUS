package com.focuslock2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import android.content.Intent

class TasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TasksScreen() }
    }
}

@Composable
fun TasksScreen() {
    val context = LocalContext.current
    val tasks = remember { Storage.getTasks(context).toMutableList() }
    val requirePin = remember { Storage.getRequirePin(context) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tasks", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tasks) { task ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(text = task, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        if (requirePin) {
                            val i = Intent(context, VerifyAccessActivity::class.java)
                            i.putExtra("mode", "complete_task")
                            i.putExtra("task_name", task)
                            context.startActivity(i)
                        } else {
                            tasks.remove(task)
                            Storage.setTasks(context, tasks)
                            val timestamp = System.currentTimeMillis()
                            Storage.addLog(context, "$timestamp: $task — Completed")
                        }
                    }) { Text(if (requirePin) "Complete (PIN)" else "Complete") }
                }
            }
        }
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("View Completion Logs") }
    }
}