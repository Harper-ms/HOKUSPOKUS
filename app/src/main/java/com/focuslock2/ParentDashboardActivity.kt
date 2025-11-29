package com.focuslock2

import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

class ParentDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParentDashboardScreen()
        }
    }

    @Composable
    fun ParentDashboardScreen() {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Set Apps", "Set Tasks")
        val ctx = LocalContext.current
        var requirePin by remember { mutableStateOf(Storage.getRequirePin(ctx)) }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    ctx.startActivity(Intent(ctx, CompletionLogsActivity::class.java))
                }) { Text("View Logs") }
                Button(onClick = {
                    ctx.startActivity(Intent(ctx, OptionsActivity::class.java))
                }) { Text("Options") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Enable App Lock") }
                Button(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }) { Text("Enable Overlay") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Require PIN to complete tasks", modifier = Modifier.weight(1f))
                Switch(checked = requirePin, onCheckedChange = {
                    requirePin = it
                    Storage.setRequirePin(ctx, it)
                })
            }
            Spacer(modifier = Modifier.height(8.dp))
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                        Text(text = title, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedTab) {
                0 -> AppTimeoutManager()
                1 -> TaskManagerUI()
            }
        }
    }

    @Composable
    fun AppTimeoutManager() {
        val context = LocalContext.current
        val pm = context.packageManager
        val apps = remember {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcherIntent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .sortedBy { it.loadLabel(pm).toString() }
        }

        var selectedApps by remember { mutableStateOf(Storage.getLockedApps(context)) }
        var blockedUntil by remember { mutableStateOf(Storage.getBlockedUntil(context)) }
        var query by remember { mutableStateOf("") }
        var manualPkg by remember { mutableStateOf("") }
        var showLockedDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search apps") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = manualPkg,
                    onValueChange = { manualPkg = it },
                    label = { Text("App name or package") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (manualPkg.isNotBlank()) {
                        val match = apps.firstOrNull { it.loadLabel(pm).toString().equals(manualPkg, true) }
                            ?: apps.firstOrNull { it.loadLabel(pm).toString().contains(manualPkg, true) }
                        val toAdd = match?.activityInfo?.packageName ?: manualPkg
                        selectedApps = selectedApps + toAdd
                        Storage.setLockedApps(context, selectedApps)
                        manualPkg = ""
                    }
                    showLockedDialog = true
                }) { Text("Lock App") }
            }
            if (showLockedDialog) {
                AlertDialog(
                    onDismissRequest = { showLockedDialog = false },
                    confirmButton = {
                        Button(onClick = { showLockedDialog = false }) { Text(stringResource(R.string.done)) }
                    },
                    title = { Text(stringResource(R.string.apps_locked_title)) },
                    text = {
                        val names = selectedApps.sorted().map { pkg ->
                            try {
                                val ai = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(ai).toString()
                            } catch (_: Exception) { pkg }
                        }
                        Text(names.joinToString("\n"))
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(apps) { app ->
                    val appName = app.loadLabel(pm).toString()
                    val pkgName = app.activityInfo.packageName
                    val checked = selectedApps.contains(pkgName)
                    val matches = query.isBlank() || appName.contains(
                        query,
                        ignoreCase = true
                    ) || pkgName.contains(query, ignoreCase = true)
                    if (!matches) return@items
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = { isChecked ->
                                selectedApps =
                                    if (isChecked) selectedApps + pkgName else selectedApps - pkgName
                                Storage.setLockedApps(context, selectedApps)
                            })
                            Text(
                                text = appName,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (checked) {
                            var hours by remember { mutableIntStateOf(0) }
                            var minutes by remember { mutableIntStateOf(0) }
                            var seconds by remember { mutableIntStateOf(0) }
                            var showConfirm by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .padding(start = 32.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ScrollNumberPicker(
                                    value = hours,
                                    range = 0..23,
                                    label = "H",
                                    onValueChange = { hours = it },
                                    modifier = Modifier.weight(1f)
                                )
                                ScrollNumberPicker(
                                    value = minutes,
                                    range = 0..59,
                                    label = "M",
                                    onValueChange = { minutes = it },
                                    modifier = Modifier.weight(1f)
                                )
                                ScrollNumberPicker(
                                    value = seconds,
                                    range = 0..59,
                                    label = "S",
                                    onValueChange = { seconds = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .padding(start = 32.dp, top = 8.dp)
                                    .fillMaxWidth()
                            ) {
                                Button(onClick = { showConfirm = true }) { Text("Set Timeout") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    Storage.clearBlock(context, pkgName)
                                    blockedUntil = Storage.getBlockedUntil(context)
                                }) { Text("Clear") }
                            }

                            if (showConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showConfirm = false },
                                    confirmButton = {
                                        Button(onClick = {
                                            showConfirm = false
                                            val totalMs =
                                                ((hours * 3600) + (minutes * 60) + seconds) * 1000L
                                            if (totalMs > 0L) {
                                                val until = System.currentTimeMillis() + totalMs
                                                Storage.setBlockUntil(context, pkgName, until)
                                                blockedUntil = Storage.getBlockedUntil(context)
                                            }
                                        }) { Text("Confirm") }
                                    },
                                    dismissButton = {
                                        Button(onClick = { showConfirm = false }) { Text("Cancel") }
                                    },
                                    title = { Text("Confirm Timeout") },
                                    text = { Text("Apply timeout ${hours}h ${minutes}m ${seconds}s to ${appName}?") }
                                )
                            }
                        }
                        val until = blockedUntil[pkgName]
                        if (until != null) {
                            val remaining = until - System.currentTimeMillis()
                            if (remaining > 0) {
                                val h = (remaining / 3600000)
                                val m = ((remaining % 3600000) / 60000)
                                val s = ((remaining % 60000) / 1000)
                                Text(
                                    "Remaining: ${h}h ${m}m ${s}s",
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ScrollNumberPicker(
        value: Int,
        range: IntRange,
        label: String,
        onValueChange: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label)
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                items(range.count()) { idx ->
                    val num = range.first + idx
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .width(36.dp)
                            .height(36.dp), contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = { onValueChange(num) }) { Text("$num") }
                    }
                }
            }
            Text("$value")
        }
    }

    @Composable
    fun TaskManagerUI() {
        val context = LocalContext.current
        val tasks =
            remember { mutableStateListOf<String>().apply { addAll(Storage.getTasks(context)) } }
        var newTask by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tasks) { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        Text(task, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            tasks.remove(task)
                            Storage.setTasks(context, tasks.toList())
                        }) { Text("Remove") }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    label = { Text("New Task") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (newTask.isNotBlank()) {
                        tasks.add(newTask)
                        Storage.setTasks(context, tasks.toList())
                        val ts = System.currentTimeMillis()
                        val formatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
                        Storage.addLog(context, "$formatted: $newTask — Incomplete")
                        newTask = ""
                        val i = Intent(context, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                    }
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Add")
                }
            }
        }
    }
}
