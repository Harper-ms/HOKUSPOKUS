package com.focuslock2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

class CompletionLogsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val entries = remember { Storage.getLogs(context) }
            Column(modifier = Modifier.padding(16.dp)) {
                Text("COMPLETION LOGS", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(entries) { log -> Text(log, modifier = Modifier.padding(4.dp)) }
                }
            }
        }
    }
}
