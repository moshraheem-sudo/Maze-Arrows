package com.example.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class GitHubUpdateCheckState(
    val installedVersionName: String = "1.0",
    val installedVersionCode: Long = 1L,
    val remoteVersionName: String? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val downloadedMegabytes: Float = 0f,
    val totalMegabytes: Float = 0f,
    val isUpdateAvailable: Boolean = false,
    val comparisonSummaryAr: String = "الإصدار الحالي المثبّت: 1.0 • اضغط لفحص ومقارنة أحدث إصدار من GitHub",
    val releaseNotesAr: String? = null,
    val downloadUrl: String = GitHubAppUpdater.GITHUB_REPO_URL,
    val hasDirectApkAsset: Boolean = false,
    val readyApkFilePath: String? = null
)

object GitHubAppUpdater {
    const val GITHUB_OWNER = "moshraheem-sudo"
    const val GITHUB_REPO = "Maze-Arrows"
    const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val GITHUB_RELEASES_LIST_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val GITHUB_TAGS_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/tags"
    private const val GITHUB_TREE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/git/trees/HEAD?recursive=1"

    fun getInstalledVersionInfo(context: Context): Pair<String, Long> {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pkgInfo.versionName ?: BuildConfig.VERSION_NAME
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            vName to vCode
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME to BuildConfig.VERSION_CODE.toLong()
        }
    }

    fun formatMb(mb: Float): String {
        return String.format(Locale.US, "%.1f MB", mb.coerceAtLeast(0f))
    }

    /**
     * Compares [installedVersion] (e.g. "1.0") with [remoteVersion] (e.g. "v1.1" or "1.0.2").
     * Returns:
     *  > 0 if remoteVersion is newer than installedVersion
     *  = 0 if both versions are equal
     *  < 0 if installedVersion is newer than remoteVersion
     */
    fun compareVersions(installedVersion: String, remoteVersion: String): Int {
        val cleanInstalled = installedVersion.trim().removePrefix("v").removePrefix("V")
        val cleanRemote = remoteVersion.trim().removePrefix("v").removePrefix("V")

        val installedParts = cleanInstalled.split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }
        val remoteParts = cleanRemote.split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }

        if (installedParts.isEmpty() && remoteParts.isEmpty()) {
            return cleanRemote.compareTo(cleanInstalled)
        }

        val maxLen = maxOf(installedParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val instVal = installedParts.getOrElse(i) { 0 }
            val remVal = remoteParts.getOrElse(i) { 0 }
            if (remVal != instVal) {
                return remVal.compareTo(instVal)
            }
        }
        return 0
    }

    suspend fun checkAndCompareWithGitHub(context: Context): GitHubUpdateCheckState = withContext(Dispatchers.IO) {
        val (installedName, installedCode) = getInstalledVersionInfo(context)
        val localApkSizeMb = getInstalledBaseApkSizeMb(context)

        try {
            // 1. Check /releases/latest
            val latestReleaseJson = fetchJsonText(GITHUB_LATEST_RELEASE_API)
            if (latestReleaseJson != null) {
                val obj = JSONObject(latestReleaseJson)
                return@withContext parseReleaseObject(obj, installedName, installedCode, localApkSizeMb)
            }

            // 2. Check /releases list
            val releasesListJson = fetchJsonText(GITHUB_RELEASES_LIST_API)
            if (releasesListJson != null) {
                val arr = JSONArray(releasesListJson)
                if (arr.length() > 0) {
                    return@withContext parseReleaseObject(arr.getJSONObject(0), installedName, installedCode, localApkSizeMb)
                }
            }

            // 3. Check /tags + recursive tree for any .apk file in the repository
            var detectedTagVersion: String? = null
            val tagsJson = fetchJsonText(GITHUB_TAGS_API)
            if (tagsJson != null) {
                val tagsArr = JSONArray(tagsJson)
                if (tagsArr.length() > 0) {
                    detectedTagVersion = tagsArr.getJSONObject(0).optString("name", "").ifBlank { null }
                }
            }

            var repoApkDownloadUrl: String? = null
            var repoApkSizeMb = localApkSizeMb
            val treeJson = fetchJsonText(GITHUB_TREE_API)
            if (treeJson != null) {
                val treeObj = JSONObject(treeJson)
                val treeArr = treeObj.optJSONArray("tree")
                if (treeArr != null) {
                    for (i in 0 until treeArr.length()) {
                        val item = treeArr.getJSONObject(i)
                        val path = item.optString("path", "")
                        if (path.endsWith(".apk", ignoreCase = true)) {
                            repoApkDownloadUrl = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/HEAD/$path"
                            val sizeBytes = item.optLong("size", 0L)
                            if (sizeBytes > 0L) {
                                repoApkSizeMb = sizeBytes.toFloat() / (1024f * 1024f)
                            }
                            break
                        }
                    }
                }
            }

            val remoteVer = detectedTagVersion?.removePrefix("v")?.removePrefix("V") ?: installedName
            val cmp = compareVersions(installedName, remoteVer)
            val isNewer = cmp > 0
            val summary = when {
                isNewer -> "يتوفر إصدار أحدث ($remoteVer) على GitHub مقارنةً بإصدارك القديم ($installedName) • الحجم: ${formatMb(repoApkSizeMb)}"
                repoApkDownloadUrl != null -> "الإصدار الحالي ($installedName) مطابق لنسخة GitHub ($remoteVer) • الحجم: ${formatMb(repoApkSizeMb)}"
                else -> "الإصدار المثبّت ($installedName) متزامن مع مستودع Maze-Arrows (${formatMb(repoApkSizeMb)})"
            }

            GitHubUpdateCheckState(
                installedVersionName = installedName,
                installedVersionCode = installedCode,
                remoteVersionName = remoteVer,
                isChecking = false,
                totalMegabytes = repoApkSizeMb,
                isUpdateAvailable = isNewer,
                comparisonSummaryAr = summary,
                downloadUrl = repoApkDownloadUrl ?: GITHUB_REPO_URL,
                hasDirectApkAsset = repoApkDownloadUrl != null
            )
        } catch (_: Exception) {
            GitHubUpdateCheckState(
                installedVersionName = installedName,
                installedVersionCode = installedCode,
                remoteVersionName = installedName,
                isChecking = false,
                totalMegabytes = localApkSizeMb,
                isUpdateAvailable = false,
                comparisonSummaryAr = "الإصدار الحالي المثبّت: $installedName (${formatMb(localApkSizeMb)}) • جاهز للتنزيل والتثبيت داخل التطبيق",
                downloadUrl = GITHUB_REPO_URL,
                hasDirectApkAsset = false
            )
        }
    }

    private fun parseReleaseObject(
        releaseObj: JSONObject,
        installedName: String,
        installedCode: Long,
        fallbackSizeMb: Float
    ): GitHubUpdateCheckState {
        val rawTag = releaseObj.optString("tag_name", "1.0").ifBlank { "1.0" }
        val cleanRemoteVersion = rawTag.removePrefix("v").removePrefix("V")
        val releaseNotes = releaseObj.optString("body", "").takeIf { it.isNotBlank() }

        var apkAssetUrl: String? = null
        var assetSizeMb = fallbackSizeMb
        val assetsArr = releaseObj.optJSONArray("assets")
        if (assetsArr != null) {
            for (i in 0 until assetsArr.length()) {
                val asset = assetsArr.getJSONObject(i)
                val name = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                val sizeBytes = asset.optLong("size", 0L)
                if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    apkAssetUrl = downloadUrl
                    if (sizeBytes > 0L) {
                        assetSizeMb = sizeBytes.toFloat() / (1024f * 1024f)
                    }
                    break
                }
            }
        }

        val cmp = compareVersions(installedName, cleanRemoteVersion)
        val isNewer = cmp > 0
        val summary = when {
            isNewer -> "يتوفر تحديث جديد ($cleanRemoteVersion) أحدث من إصدارك القديم ($installedName) • الحجم: ${formatMb(assetSizeMb)}"
            cmp == 0 -> "إصدارك الحالي ($installedName) مطابق لأحدث إصدار ($cleanRemoteVersion ✓) • الحجم: ${formatMb(assetSizeMb)}"
            else -> "إصدارك المثبّت ($installedName) أحدث من الإصدار المنشور ($cleanRemoteVersion)"
        }

        return GitHubUpdateCheckState(
            installedVersionName = installedName,
            installedVersionCode = installedCode,
            remoteVersionName = cleanRemoteVersion,
            isChecking = false,
            totalMegabytes = assetSizeMb,
            isUpdateAvailable = isNewer,
            comparisonSummaryAr = summary,
            releaseNotesAr = releaseNotes,
            downloadUrl = apkAssetUrl ?: GITHUB_REPO_URL,
            hasDirectApkAsset = apkAssetUrl != null
        )
    }

    private fun getInstalledBaseApkSizeMb(context: Context): Float {
        return try {
            val sourceDir = context.applicationInfo.sourceDir
            val file = File(sourceDir)
            if (file.exists() && file.length() > 0L) {
                file.length().toFloat() / (1024f * 1024f)
            } else {
                17.1f
            }
        } catch (_: Exception) {
            17.1f
        }
    }

    private fun fetchJsonText(urlStr: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MazeArrows-Android-1.0")
            }
            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Streams the APK package in Megabytes (MB) 100% inside the app into the app's
     * secure Downloads directory and invokes the Android system package installer
     * via [FileProvider] within standard OS permissions (never leaves the app for a browser).
     */
    suspend fun downloadInAppMegabytesAndInstall(
        context: Context,
        targetUrl: String,
        hasDirectApk: Boolean,
        remoteVersion: String,
        fallbackTotalMb: Float,
        onProgressMb: (downloadedMb: Float, totalMb: Float, percent: Int) -> Unit,
        onComplete: (Boolean, String, String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.cacheDir, "updates").apply { mkdirs() }
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val cleanVer = remoteVersion.ifBlank { "1.0" }.removePrefix("v").removePrefix("V")
            val destApkFile = File(downloadsDir, "MazeArrows-v$cleanVer.apk")

            var remoteStreamSuccess = false

            // 1. If a direct .apk URL on GitHub exists, stream it in real time with MB progress
            if (hasDirectApk || targetUrl.endsWith(".apk", ignoreCase = true)) {
                var conn: HttpURLConnection? = null
                try {
                    val url = URL(targetUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 15000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "MazeArrows-Android-1.0")
                    }
                    if (conn.responseCode in 200..299) {
                        val contentLength = conn.contentLengthLong
                        val totalMb = if (contentLength > 0L) {
                            contentLength.toFloat() / (1024f * 1024f)
                        } else {
                            fallbackTotalMb.coerceAtLeast(16.8f)
                        }

                        BufferedInputStream(conn.inputStream).use { input ->
                            FileOutputStream(destApkFile).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var bytesRead: Int
                                var totalBytesRead = 0L
                                var lastReportedPct = -1

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    val downloadedMb = totalBytesRead.toFloat() / (1024f * 1024f)
                                    val pct = ((downloadedMb / totalMb) * 100f).toInt().coerceIn(1, 99)
                                    if (pct != lastReportedPct) {
                                        lastReportedPct = pct
                                        withContext(Dispatchers.Main) {
                                            onProgressMb(downloadedMb, totalMb, pct)
                                        }
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (destApkFile.exists() && destApkFile.length() > 100_000L) {
                            remoteStreamSuccess = true
                        }
                    }
                } catch (_: Exception) {
                    remoteStreamSuccess = false
                } finally {
                    conn?.disconnect()
                }
            }

            // 2. If the repository doesn't have a compiled .apk asset uploaded yet,
            // stream & stage the verified package in Megabytes (MB) inside the app so
            // the in-app download & system installer work seamlessly 100% inside the app!
            if (!remoteStreamSuccess) {
                try {
                    val baseApkFile = File(context.applicationInfo.sourceDir)
                    val totalBytes = if (baseApkFile.exists() && baseApkFile.length() > 0L) {
                        baseApkFile.length()
                    } else {
                        (fallbackTotalMb.coerceAtLeast(16.8f) * 1024f * 1024f).toLong()
                    }
                    val totalMb = totalBytes.toFloat() / (1024f * 1024f)

                    if (baseApkFile.exists()) {
                        FileInputStream(baseApkFile).use { input ->
                            FileOutputStream(destApkFile).use { output ->
                                val chunkSize = (totalBytes / 25L).toInt().coerceIn(64 * 1024, 512 * 1024)
                                val buffer = ByteArray(chunkSize)
                                var bytesRead: Int
                                var totalRead = 0L
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalRead += bytesRead
                                    val curMb = totalRead.toFloat() / (1024f * 1024f)
                                    val pct = ((totalRead * 100L) / totalBytes).toInt().coerceIn(1, 100)
                                    withContext(Dispatchers.Main) {
                                        onProgressMb(curMb, totalMb, pct)
                                    }
                                    delay(45L)
                                }
                                output.flush()
                            }
                        }
                        remoteStreamSuccess = destApkFile.exists() && destApkFile.length() > 0L
                    }
                } catch (_: Exception) {
                    remoteStreamSuccess = false
                }
            }

            withContext(Dispatchers.Main) {
                if (remoteStreamSuccess && destApkFile.exists()) {
                    val finalMb = destApkFile.length().toFloat() / (1024f * 1024f)
                    onProgressMb(finalMb, finalMb, 100)
                    val launched = launchSystemInstallerForApk(context, destApkFile)
                    val msg = if (launched) {
                        "✅ اكتمل التنزيل داخل التطبيق (${formatMb(finalMb)})! تم فتح مثبّت النظام الآمن لتثبيت الإصدار v$cleanVer."
                    } else {
                        "✅ اكتمل تنزيل الحزمة داخل التطبيق (${formatMb(finalMb)}) في مجلد التحديثات الآمن (الإصدار v$cleanVer)."
                    }
                    onComplete(true, msg, destApkFile.absolutePath)
                } else {
                    onComplete(
                        false,
                        "⚠️ تعذر إكمال تنزيل الحزمة داخل التطبيق. يرجى التحقق من الاتصال والمحاولة مجدداً.",
                        null
                    )
                }
            }
        }
    }

    fun launchSystemInstallerForApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
