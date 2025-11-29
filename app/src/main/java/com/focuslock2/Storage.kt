package com.focuslock2

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit // FIX: Import the edit extension function

object Storage {
    private const val PREFS_NAME = "focus_lock_prefs"
    private const val KEY_LOCKED_APPS = "locked_apps"
    private const val KEY_TASKS = "tasks"
    private const val KEY_LOGS = "completion_logs"
    private const val KEY_BLOCKED_UNTIL = "blocked_until"
    private const val KEY_REQUIRE_PIN = "require_pin"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_LOCKED_APPS, emptySet()) ?: emptySet()

    fun setLockedApps(context: Context, packages: Set<String>) {
        prefs(context).edit { // Use the imported 'edit' extension function
            putStringSet(KEY_LOCKED_APPS, packages)
        }
    }

    fun getTasks(context: Context): List<String> =
        prefs(context).getString(KEY_TASKS, "")
            ?.takeIf { it.isNotEmpty() }
            ?.split("\n")
            ?: emptyList()

    fun setTasks(context: Context, tasks: List<String>) {
        prefs(context).edit { // Use the imported 'edit' extension function
            putString(KEY_TASKS, tasks.joinToString("\n"))
        }
    }

    fun addLog(context: Context, message: String) {
        val existing = getLogs(context)
        val updated = existing + message
        prefs(context).edit { // Use the imported 'edit' extension function
            putString(KEY_LOGS, updated.joinToString("\n"))
        }
    }

    fun getLogs(context: Context): List<String> =
        prefs(context).getString(KEY_LOGS, "")
            ?.takeIf { it.isNotEmpty() }
            ?.split("\n")
            ?: emptyList()

    fun getBlockedUntil(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_BLOCKED_UNTIL, "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return raw.split("\n").mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) null else {
                val pkg = line.substring(0, idx)
                val ts = line.substring(idx + 1).toLongOrNull()
                if (ts == null) null else pkg to ts
            }
        }.toMap()
    }

    fun setBlockedUntil(context: Context, data: Map<String, Long>) {
        val raw = data.entries.joinToString("\n") { "${it.key}=${it.value}" }
        prefs(context).edit { // Use the imported 'edit' extension function
            putString(KEY_BLOCKED_UNTIL, raw)
        }
    }

    fun setBlockUntil(context: Context, pkg: String, untilMillis: Long) {
        val current = getBlockedUntil(context).toMutableMap()
        current[pkg] = untilMillis
        setBlockedUntil(context, current)
    }

    fun clearBlock(context: Context, pkg: String) {
        val current = getBlockedUntil(context).toMutableMap()
        current.remove(pkg)
        setBlockedUntil(context, current)
    }

    fun isBlocked(context: Context, pkg: String): Boolean {
        val until = getBlockedUntil(context)[pkg] ?: return false
        return System.currentTimeMillis() < until
    }

    fun getRequirePin(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REQUIRE_PIN, true)

    fun setRequirePin(context: Context, value: Boolean) {
        prefs(context).edit { // Use the imported 'edit' extension function
            putBoolean(KEY_REQUIRE_PIN, value)
        }
    }

    fun registerPrefsListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPrefsListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}
