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
    val includeNightly: Boolean = true,
    val lastConnectedDeviceAddress: String? = null,
    val lastConnectedDeviceName: String? = null
) {
    fun toJsonString(pretty: Boolean = false): String {
        val obj = JSONObject()
        obj.put("clickMode", clickMode.name)
        obj.put("pointerSpeed", pointerSpeed.toDouble())
        obj.put("accelerationEnabled", accelerationEnabled)
        obj.put("invertScroll", invertScroll)
        obj.put("hapticEnabled", hapticEnabled)
        obj.put("includeNightly", includeNightly)
        obj.put("lastConnectedDeviceAddress", lastConnectedDeviceAddress ?: "")
        obj.put("lastConnectedDeviceName", lastConnectedDeviceName ?: "")
        return if (pretty) obj.toString(2) else obj.toString()
    }

    companion object {
        private const val PREFS_NAME = "amk_settings_prefs"
        private const val KEY_SETTINGS_JSON = "key_settings_json"

        fun fromJsonString(str: String): AppSettings {
            return try {
                val obj = JSONObject(str)
                val mode = try {
                    ClickMode.valueOf(obj.optString("clickMode", ClickMode.BOTH.name))
                } catch (e: Exception) {
                    ClickMode.BOTH
                }
                val speed = obj.optDouble("pointerSpeed", 1.2).toFloat()
                val accel = obj.optBoolean("accelerationEnabled", true)
                val invert = obj.optBoolean("invertScroll", false)
                val haptic = obj.optBoolean("hapticEnabled", true)
                val nightly = obj.optBoolean("includeNightly", true)
                val lastAddr = obj.optString("lastConnectedDeviceAddress").ifEmpty { null }
                val lastName = obj.optString("lastConnectedDeviceName").ifEmpty { null }

                AppSettings(
                    clickMode = mode,
                    pointerSpeed = speed,
                    accelerationEnabled = accel,
                    invertScroll = invert,
                    hapticEnabled = haptic,
                    includeNightly = nightly,
                    lastConnectedDeviceAddress = lastAddr,
                    lastConnectedDeviceName = lastName
                )
            } catch (e: Exception) {
                e.printStackTrace()
                AppSettings()
            }
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
