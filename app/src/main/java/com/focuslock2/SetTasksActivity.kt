package com.focuslock2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SetTasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var taskText by remember { mutableStateOf("") }
            val tasks = remember { mutableStateListOf<String>() }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("SET PARENT TASKS", fontSize = 20.sp)
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    if (taskText.isNotBlank()) {
                        tasks.add(taskText)
                        Storage.setTasks(this@SetTasksActivity, tasks.toList())
                        val ts = System.currentTimeMillis()
                        val formatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
                        Storage.addLog(this@SetTasksActivity, "$formatted: $taskText — Incomplete")
                        Toast.makeText(this@SetTasksActivity, "Task Added", Toast.LENGTH_SHORT).show()
                        taskText = ""
                        val i = android.content.Intent(this@SetTasksActivity, MainActivity::class.java)
                        i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(i)
                    }
                }) { Text("+ ADD TASK") }

                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(tasks) { task ->
                        Text("• $task", modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}
