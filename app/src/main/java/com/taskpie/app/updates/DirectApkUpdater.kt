package com.taskpie.app.updates

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ApkRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String
)

object DirectApkUpdater {
    private const val REPOSITORY = "AkkarinRothen/TaskPie"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$REPOSITORY/releases/latest"
    private const val MANIFEST_ASSET_NAME = "update.json"
    private const val PREFERENCES_NAME = "direct_apk_updates"
    private const val CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
    private const val MAX_APK_BYTES = 200L * 1024 * 1024

    suspend fun checkForUpdate(activity: Activity): ApkRelease? = withContext(Dispatchers.IO) {
        val preferences = activity.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - preferences.getLong("last_check", 0L) < CHECK_INTERVAL_MILLIS) return@withContext null

        try {
            val latestRelease = getJson(LATEST_RELEASE_API)
            preferences.edit().putLong("last_check", now).apply()
            val assets = latestRelease.getAsJsonArray("assets") ?: return@withContext null
            val manifestAsset = assets.firstOrNull {
                it.asJsonObject.get("name")?.asString == MANIFEST_ASSET_NAME
            }?.asJsonObject ?: return@withContext null
            val manifestUrl = manifestAsset.get("browser_download_url")?.asString
                ?: return@withContext null
            val manifest = getJson(manifestUrl)
            val versionCode = manifest.get("versionCode")?.asInt ?: return@withContext null
            val versionName = manifest.get("versionName")?.asString ?: return@withContext null
            val apkAssetName = manifest.get("apkAsset")?.asString ?: return@withContext null
            val sha256 = manifest.get("sha256")?.asString?.lowercase() ?: return@withContext null
            val notes = manifest.get("releaseNotes")?.asString.orEmpty()

            require(versionCode > 0) { "Invalid versionCode" }
            require(sha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid SHA-256" }

            val apkAsset = assets.firstOrNull {
                it.asJsonObject.get("name")?.asString == apkAssetName
            }?.asJsonObject ?: return@withContext null
            val apkUrl = apkAsset.get("browser_download_url")?.asString
                ?: return@withContext null

            val installedVersionCode = activity.packageManager
                .getPackageInfo(activity.packageName, 0)
                .let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong() }

            if (versionCode <= installedVersionCode) null
            else ApkRelease(versionCode, versionName, apkUrl, sha256, notes)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadRelease(activity: Activity, release: ApkRelease): File = withContext(Dispatchers.IO) {
        val updatesDirectory = File(activity.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDirectory, "TaskPie-${release.versionCode}.apk")
        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "TaskPie-Android-Updater")
            connection.connect()
            check(connection.responseCode in 200..299) { "Download failed (${connection.responseCode})" }
            check(connection.contentLengthLong <= MAX_APK_BYTES || connection.contentLengthLong < 0) {
                "APK exceeds the allowed size"
            }

            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        check(totalBytes <= MAX_APK_BYTES) { "APK exceeds the allowed size" }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actualHash = apkFile.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }
        check(actualHash.equals(release.sha256, ignoreCase = true)) { "APK checksum does not match" }
        @Suppress("DEPRECATION")
        val archiveInfo = requireNotNull(activity.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)) {
            "APK could not be read"
        }
        check(archiveInfo.packageName == activity.packageName) { "APK package does not match" }
        val downloadedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }
        check(downloadedVersionCode == release.versionCode.toLong()) { "APK version does not match release metadata" }
        apkFile
    }

    fun canRequestInstall(activity: Activity): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    fun openInstaller(activity: Activity, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }

    private fun getJson(url: String) = (URL(url).openConnection() as HttpURLConnection).run {
        try {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "TaskPie-Android-Updater")
            inputStream.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
        } finally {
            disconnect()
        }
    }
}
