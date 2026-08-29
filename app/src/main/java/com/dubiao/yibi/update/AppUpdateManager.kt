package com.dubiao.yibi.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dubiao.yibi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String,
    val forceUpdate: Boolean,
)

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data object UpToDate : AppUpdateState
    data class Available(val info: UpdateInfo) : AppUpdateState
    data class Downloading(val info: UpdateInfo, val percent: Int?) : AppUpdateState
    data class Ready(val info: UpdateInfo, val apk: File) : AppUpdateState
    data class Failed(val message: String) : AppUpdateState
}

enum class InstallResult {
    STARTED,
    PERMISSION_REQUIRED,
}

class AppUpdateManager(private val context: Context) {
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection = openConnection(BuildConfig.UPDATE_MANIFEST_URL)
        try {
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw IllegalStateException("GitHub 上还没有可用的 Release")
            }
            if (status !in 200..299) {
                throw IllegalStateException("更新服务返回 HTTP $status")
            }
            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText().also {
                    require(it.length <= MAX_MANIFEST_CHARS) { "更新信息文件过大" }
                }
            }
            parseManifest(json).takeIf { isNewerVersion(it.versionCode, BuildConfig.VERSION_CODE) }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(
        info: UpdateInfo,
        onProgress: (Int?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val updateDirectory = File(context.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
        updateDirectory.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        val temporaryFile = File(updateDirectory, "yibi-${info.versionName}.apk.part")
        val targetFile = File(updateDirectory, "yibi-${info.versionName}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = openConnection(info.apkUrl)
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("APK 下载失败：HTTP $status")
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
            var downloadedBytes = 0L
            var lastPercent: Int? = null
            connection.inputStream.buffered().use { input ->
                temporaryFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloadedBytes += count
                        val percent = totalBytes?.let { ((downloadedBytes * 100L) / it).toInt().coerceIn(0, 100) }
                        if (percent != lastPercent) {
                            lastPercent = percent
                            withContext(Dispatchers.Main.immediate) { onProgress(percent) }
                        }
                    }
                }
            }
            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val expectedSha256 = normalizeSha256(info.sha256)
            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                temporaryFile.delete()
                throw IllegalStateException("APK 校验失败，请重新下载")
            }
            check(temporaryFile.renameTo(targetFile)) { "无法保存下载的 APK" }
            withContext(Dispatchers.Main.immediate) { onProgress(100) }
            targetFile
        } catch (error: Throwable) {
            temporaryFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun install(apk: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permissionIntent)
            return InstallResult.PERMISSION_REQUIRED
        }
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
        return InstallResult.STARTED
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, application/octet-stream")
            setRequestProperty("User-Agent", "YiBi/${BuildConfig.VERSION_NAME} (Android)")
        }

    companion object {
        private const val UPDATE_DIRECTORY = "updates"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val MAX_MANIFEST_CHARS = 64 * 1024
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 30_000
        internal fun parseManifest(rawJson: String): UpdateInfo {
            val json = JSONObject(rawJson)
            val info = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName").trim(),
                apkUrl = json.getString("apkUrl").trim(),
                sha256 = json.getString("sha256").trim(),
                releaseNotes = json.optString("releaseNotes").trim(),
                forceUpdate = json.optBoolean("forceUpdate", false),
            )
            require(info.versionCode > 0) { "versionCode 必须大于 0" }
            require(info.versionName.isNotBlank()) { "versionName 不能为空" }
            require(info.apkUrl.startsWith("https://")) { "APK 下载地址必须使用 HTTPS" }
            require(normalizeSha256(info.sha256).matches(Regex("[0-9a-fA-F]{64}"))) { "SHA-256 格式无效" }
            return info
        }

        internal fun normalizeSha256(value: String): String = value
            .trim()
            .removePrefix("sha256:")
            .removePrefix("SHA256:")
            .trim()

        internal fun isNewerVersion(availableVersionCode: Int, currentVersionCode: Int): Boolean =
            availableVersionCode > currentVersionCode
    }
}
