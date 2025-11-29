package com.focuslock2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class BlockerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra("blocked_package") ?: ""
        setContent { BlockerScreen(pkg) }
        Handler(Looper.getMainLooper()).postDelayed({
            try { finish() } catch (_: Exception) {}
        }, 3000)
    }
}

@Composable
fun BlockerScreen(pkg: String) {
    val context = LocalContext.current
    val appLabel = try {
        val pm = context.packageManager
        val ai = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(ai).toString()
    } catch (_: Exception) { pkg }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = context.getString(R.string.access_denied), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(appLabel)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }, modifier = Modifier.fillMaxWidth()) {
            Text(context.getString(R.string.go_home))
        }
    }
}
