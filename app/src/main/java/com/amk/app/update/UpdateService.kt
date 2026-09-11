package com.amk.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.amk.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateService {
    private const val TAG = "AMK_Update"
    private const val GITHUB_REPO = "jefrimustapa/amk"

    suspend fun checkForUpdates(includeNightly: Boolean): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = BuildConfig.APP_VERSION_NAME
        val currentBuildNumber = BuildConfig.APP_BUILD_NUMBER

        try {
            val endpoint = "https://api.github.com/repos/$GITHUB_REPO/releases?per_page=10"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "AMK-Android-App")
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateInfo(
                    hasUpdate = false,
                    currentVersion = currentVersion,
                    latestVersion = currentVersion,
                    releaseName = "Up to date",
                    releaseNotes = "No updates available or repo not yet populated.",
                    apkUrl = null,
                    apkName = null,
                    apkSizeMb = null,
                    isNightly = false
                )
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val releasesArray = JSONArray(responseBody)

            for (i in 0 until releasesArray.length()) {
                val rel = releasesArray.getJSONObject(i)
                if (rel.optBoolean("draft", false)) continue

                val isPrerelease = rel.optBoolean("prerelease", false)
                val tagName = rel.optString("tag_name", "")
                val isNightly = isPrerelease || tagName.contains("nightly", ignoreCase = true)

                if (!includeNightly && isNightly) continue

                // Find APK in assets
                val assetsArray = rel.optJSONArray("assets") ?: JSONArray()
                var apkUrl: String? = null
                var apkName: String? = null
                var apkSize: Long = 0

                for (j in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(j)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        apkSize = asset.optLong("size", 0)
                        break
                    }
                }

                val cleanTag = tagName.removePrefix("v")
                var hasUpdate = false

                if (isNightly) {
                    val tagDigits = tagName.filter { it.isDigit() }
                    val currentDigits = currentBuildNumber.filter { it.isDigit() }
                    if (tagDigits.length >= 8 && currentDigits.length >= 8) {
                        val remoteDate = tagDigits.take(8).toIntOrNull() ?: 0
                        val localDate = currentDigits.take(8).toIntOrNull() ?: 0
                        if (remoteDate > localDate) hasUpdate = true
                    } else if (cleanTag != currentVersion) {
                        hasUpdate = true
                    }
                } else {
                    if (cleanTag != currentVersion) {
                        hasUpdate = true
                    }
                }

                val apkSizeMb = if (apkSize > 0) String.format("%.1f MB", apkSize / (1024.0 * 1024.0)) else null

                return@withContext UpdateInfo(
                    hasUpdate = hasUpdate,
                    currentVersion = currentVersion,
                    latestVersion = cleanTag,
                    releaseName = rel.optString("name", tagName),
                    releaseNotes = rel.optString("body", "Bug fixes and performance enhancements."),
                    apkUrl = apkUrl,
                    apkName = apkName ?: "amk-update.apk",
                    apkSizeMb = apkSizeMb,
                    isNightly = isNightly
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
        }

        UpdateInfo(
            hasUpdate = false,
            currentVersion = currentVersion,
            latestVersion = currentVersion,
            releaseName = "Up to date",
            releaseNotes = "You are running the latest build.",
            apkUrl = null,
            apkName = null,
            apkSizeMb = null,
            isNightly = false
        )
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        apkFileName: String,
        onProgress: (Int, String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            var currentUrl = downloadUrl
            var connection = URL(currentUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "AMK-Android-App")
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            // Handle HTTP redirects (GitHub to AWS S3)
            var responseCode = connection.responseCode
            var redirects = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) && redirects < 6
            ) {
                val newUrl = connection.getHeaderField("Location") ?: break
                connection.disconnect()
                currentUrl = newUrl
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "AMK-Android-App")
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()
                responseCode = connection.responseCode
                redirects++
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP $responseCode")
            }

            val fileLength = connection.contentLength
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val outputFile = File(downloadDir, apkFileName)
            if (outputFile.exists()) outputFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    var lastUpdate = 0L

                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250) {
                            lastUpdate = now
                            val progress = if (fileLength > 0) ((total * 100) / fileLength).toInt() else 50
                            val status = String.format(
                                "%.1f MB / %.1f MB",
                                total / (1024.0 * 1024.0),
                                if (fileLength > 0) fileLength / (1024.0 * 1024.0) else total / (1024.0 * 1024.0)
                            )
                            withContext(Dispatchers.Main) {
                                onProgress(progress, status)
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(100, "Download complete!")
                promptInstallApk(context, outputFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Download failed")
            }
        }
    }

    private fun promptInstallApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer: ${e.message}", e)
        }
    }
}
