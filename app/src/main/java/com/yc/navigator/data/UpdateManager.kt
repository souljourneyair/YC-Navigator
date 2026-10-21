package com.yc.navigator.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String
)

class UpdateManager(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatest(): UpdateInfo = withContext(Dispatchers.IO) {
        val url = "$VERSION_URL?t=${System.currentTimeMillis()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "YC-Navigator-Updater")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Update check failed: HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val info = json.decodeFromString<UpdateInfo>(body)
            require(info.versionCode > 0) { "Invalid version code" }
            require(info.versionName.isNotBlank()) { "Invalid version name" }
            require(info.apkUrl.startsWith("https://")) { "Invalid APK URL" }
            info
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(info: UpdateInfo): File = withContext(Dispatchers.IO) {
        val dir = File(appContext.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        val safeVersion = info.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(dir, "YC-navigator-$safeVersion.apk")
        val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(60).toInt()
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "YC-Navigator-Updater")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Download failed: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }

        require(file.length() > 1024) { "Downloaded file is too small" }
        val firstBytes = ByteArray(2)
        file.inputStream().use { it.read(firstBytes) }
        require(firstBytes[0] == 'P'.code.toByte() && firstBytes[1] == 'K'.code.toByte()) {
            "Downloaded file is not an APK"
        }
        file
    }

    fun canInstallPackages(): Boolean =
        appContext.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    companion object {
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/souljourneyair/YC-Navigator/main/version.json"
    }
}
