package com.amk.app.model

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File

enum class ClickMode {
    TAP_ONLY,
    BUTTONS_ONLY,
    BOTH
}

data class AppSettings(
    val clickMode: ClickMode = ClickMode.BOTH,
    val pointerSpeed: Float = 1.2f,
    val accelerationEnabled: Boolean = true,
    val invertScroll: Boolean = false,
    val hapticEnabled: Boolean = true,
    val includeNightly: Boolean = true
) {
    fun toJsonString(): String {
        return "{\"clickMode\":\"${clickMode.name}\",\"pointerSpeed\":$pointerSpeed,\"accelerationEnabled\":$accelerationEnabled,\"invertScroll\":$invertScroll,\"hapticEnabled\":$hapticEnabled,\"includeNightly\":$includeNightly}"
    }

    companion object {
        private const val PREFS_NAME = "amk_settings_prefs"
        private const val KEY_SETTINGS_JSON = "key_settings_json"

        fun fromJsonString(str: String): AppSettings {
            var mode = ClickMode.BOTH
            var speed = 1.2f
            var accel = true
            var invert = false
            var haptic = true
            var nightly = true

            val clean = str.trim().removeSurrounding("{", "}").split(",")
            for (part in clean) {
                val kv = part.split(":")
                if (kv.size == 2) {
                    val key = kv[0].trim().replace("\"", "")
                    val value = kv[1].trim().replace("\"", "")
                    when (key) {
                        "clickMode" -> mode = try { ClickMode.valueOf(value) } catch (e: Exception) { ClickMode.BOTH }
                        "pointerSpeed" -> speed = value.toFloatOrNull() ?: 1.2f
                        "accelerationEnabled" -> accel = value.toBoolean()
                        "invertScroll" -> invert = value.toBoolean()
                        "hapticEnabled" -> haptic = value.toBoolean()
                        "includeNightly" -> nightly = value.toBoolean()
                    }
                }
            }
            return AppSettings(
                clickMode = mode,
                pointerSpeed = speed,
                accelerationEnabled = accel,
                invertScroll = invert,
                hapticEnabled = haptic,
                includeNightly = nightly
            )
        }

        /**
         * Resolves persistent backup file in public Documents or Downloads directory
         * which survives app uninstallation and subsequent reinstallation.
         */
        fun getPersistentBackupFile(createDir: Boolean = false): File? {
            try {
                // Target 1: Public Documents directory (/sdcard/Documents/AMK/amk_settings.json)
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (docsDir != null) {
                    val appDir = File(docsDir, "AMK")
                    if (createDir && !appDir.exists()) {
                        appDir.mkdirs()
                    }
                    if (appDir.exists() || createDir) {
                        return File(appDir, "amk_settings.json")
                    }
                }

                // Target 2: Public Downloads directory (/sdcard/Download/AMK/amk_settings.json)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null) {
                    val appDir = File(downloadDir, "AMK")
                    if (createDir && !appDir.exists()) {
                        appDir.mkdirs()
                    }
                    if (appDir.exists() || createDir) {
                        return File(appDir, "amk_settings.json")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun load(context: Context): AppSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedJson = prefs.getString(KEY_SETTINGS_JSON, null)

            if (!savedJson.isNullOrEmpty()) {
                try {
                    return fromJsonString(savedJson)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fresh install or after uninstall: Check persistent public storage
            val backupFile = getPersistentBackupFile(false)
            if (backupFile != null && backupFile.exists() && backupFile.length() > 0) {
                try {
                    val content = backupFile.readText(Charsets.UTF_8)
                    if (content.isNotEmpty()) {
                        val restored = fromJsonString(content)
                        prefs.edit().putString(KEY_SETTINGS_JSON, content).apply()
                        return restored
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return AppSettings()
        }

        fun save(context: Context, settings: AppSettings) {
            val jsonStr = settings.toJsonString()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SETTINGS_JSON, jsonStr).apply()

            try {
                val backupFile = getPersistentBackupFile(true)
                if (backupFile != null) {
                    backupFile.writeText(jsonStr, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
