package com.example.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.MainActivity
import kotlinx.coroutines.Dispatchers
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
    val installedVersionName: String = "1.70.0",
    val installedVersionCode: Long = 1700L,
    val remoteVersionName: String? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val downloadedMegabytes: Float = 0f,
    val totalMegabytes: Float = 0f,
    val isUpdateAvailable: Boolean = false,
    val comparisonSummaryAr: String = "الإصدار الحالي المثبّت: v1.70.0 • اضغط «فحص الإصدار» للبحث في رابط GitHub عن تحديث أحدث",
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
    private const val GITHUB_RAW_GRADLE_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/HEAD/app/build.gradle.kts"

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
     *  > 0 if remoteVersion is strictly newer than installedVersion
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

    /**
     * Checks the official GitHub repository (`https://github.com/moshraheem-sudo/Maze-Arrows`)
     * based on the currently installed version (`installedName`).
     * Searches:
     * 1. GitHub Releases (`/releases/latest` and `/releases`) for a newer version tag and `.apk` asset
     * 2. GitHub Tags (`/tags`) + Repository Tree (`/git/trees/HEAD?recursive=1`) for any `.apk` file
     *    (`MazeArrows.apk` / `AmazeArrows.apk`) and `app/build.gradle.kts` `versionName`.
     */
    suspend fun checkAndCompareWithGitHub(context: Context): GitHubUpdateCheckState = withContext(Dispatchers.IO) {
        val (installedName, installedCode) = getInstalledVersionInfo(context)

        try {
            // 1. Check /releases/latest
            val latestReleaseJson = fetchTextFromUrl(GITHUB_LATEST_RELEASE_API, isGitHubApi = true)
            if (latestReleaseJson != null) {
                val obj = JSONObject(latestReleaseJson)
                val parsed = parseReleaseObject(obj, installedName, installedCode)
                if (parsed.hasDirectApkAsset) {
                    return@withContext parsed
                }
            }

            // 2. Check /releases list
            val releasesListJson = fetchTextFromUrl(GITHUB_RELEASES_LIST_API, isGitHubApi = true)
            if (releasesListJson != null) {
                val arr = JSONArray(releasesListJson)
                for (i in 0 until arr.length()) {
                    val parsed = parseReleaseObject(arr.getJSONObject(i), installedName, installedCode)
                    if (parsed.hasDirectApkAsset || i == 0) {
                        return@withContext parsed
                    }
                }
            }

            // 3. Check /tags + repository tree for MazeArrows.apk and remote build.gradle.kts versionName
            var detectedRemoteVersion: String? = null
            val tagsJson = fetchTextFromUrl(GITHUB_TAGS_API, isGitHubApi = true)
            if (tagsJson != null) {
                val tagsArr = JSONArray(tagsJson)
                if (tagsArr.length() > 0) {
                    detectedRemoteVersion = tagsArr.getJSONObject(0).optString("name", "").ifBlank { null }
                }
            }

            if (detectedRemoteVersion == null) {
                val gradleText = fetchTextFromUrl(GITHUB_RAW_GRADLE_URL, isGitHubApi = false)
                if (gradleText != null) {
                    val regex = Regex("""versionName\s*=\s*"([^"]+)"""")
                    val match = regex.find(gradleText)
                    if (match != null) {
                        detectedRemoteVersion = match.groupValues[1]
                    }
                }
            }

            var repoApkDownloadUrl: String? = null
            var repoApkSizeMb = 0f
            val treeJson = fetchTextFromUrl(GITHUB_TREE_API, isGitHubApi = true)
            if (treeJson != null) {
                val treeObj = JSONObject(treeJson)
                val treeArr = treeObj.optJSONArray("tree")
                if (treeArr != null) {
                    // Prefer MazeArrows.apk in root, then any .apk file
                    var fallbackApkPath: String? = null
                    var fallbackApkSize = 0L
                    for (i in 0 until treeArr.length()) {
                        val item = treeArr.getJSONObject(i)
                        val path = item.optString("path", "")
                        val sizeBytes = item.optLong("size", 0L)
                        if (path.equals("MazeArrows.apk", ignoreCase = true)) {
                            fallbackApkPath = path
                            fallbackApkSize = sizeBytes
                            break
                        } else if (path.endsWith(".apk", ignoreCase = true) && fallbackApkPath == null) {
                            fallbackApkPath = path
                            fallbackApkSize = sizeBytes
                        }
                    }
                    if (fallbackApkPath != null) {
                        repoApkDownloadUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/raw/HEAD/$fallbackApkPath"
                        if (fallbackApkSize > 0L) {
                            repoApkSizeMb = fallbackApkSize.toFloat() / (1024f * 1024f)
                        }
                    }
                }
            }

            val cleanRemoteVer = detectedRemoteVersion?.removePrefix("v")?.removePrefix("V")
            if (cleanRemoteVer != null) {
                val cmp = compareVersions(installedName, cleanRemoteVer)
                val isNewer = cmp > 0 && repoApkDownloadUrl != null
                val summary = when {
                    cmp > 0 && repoApkDownloadUrl != null ->
                        "🎉 تم العثور على إصدار أحدث على GitHub (v$cleanRemoteVer) مقارنةً بإصدارك الحالي (v$installedName) • الحجم: ${formatMb(repoApkSizeMb)}"
                    cmp > 0 && repoApkDownloadUrl == null ->
                        "⚠️ يوجد وسم إصدار (v$cleanRemoteVer) على GitHub ولكن لم يتم إرفاق ملف APK بعد في المستودع."
                    cmp == 0 ->
                        "✅ تم الفحص من GitHub: إصدارك الحالي (v$installedName) هو أحدث إصدار متوفر ولا يحتاج إلى تحديث."
                    else ->
                        "✅ إصدارك المثبّت حالياً (v$installedName) أحدث من النسخة المنشورة على GitHub (v$cleanRemoteVer)."
                }

                return@withContext GitHubUpdateCheckState(
                    installedVersionName = installedName,
                    installedVersionCode = installedCode,
                    remoteVersionName = cleanRemoteVer,
                    isChecking = false,
                    totalMegabytes = repoApkSizeMb,
                    isUpdateAvailable = isNewer,
                    comparisonSummaryAr = summary,
                    downloadUrl = repoApkDownloadUrl ?: GITHUB_REPO_URL,
                    hasDirectApkAsset = repoApkDownloadUrl != null
                )
            }

            // Repository is empty or has no version tags/files yet
            GitHubUpdateCheckState(
                installedVersionName = installedName,
                installedVersionCode = installedCode,
                remoteVersionName = null,
                isChecking = false,
                totalMegabytes = 0f,
                isUpdateAvailable = false,
                comparisonSummaryAr = "🔍 تم فحص مستودع GitHub بناءً على إصدارك الحالي (v$installedName): لم يتم رفع إصدار أحدث من v$installedName على الرابط بعد.",
                downloadUrl = GITHUB_REPO_URL,
                hasDirectApkAsset = false
            )
        } catch (_: Exception) {
            GitHubUpdateCheckState(
                installedVersionName = installedName,
                installedVersionCode = installedCode,
                remoteVersionName = null,
                isChecking = false,
                totalMegabytes = 0f,
                isUpdateAvailable = false,
                comparisonSummaryAr = "⚠️ تعذر الوصول إلى مستودع GitHub حالياً. الإصدار المثبّت لديك هو v$installedName.",
                downloadUrl = GITHUB_REPO_URL,
                hasDirectApkAsset = false
            )
        }
    }

    private fun parseReleaseObject(
        releaseObj: JSONObject,
        installedName: String,
        installedCode: Long
    ): GitHubUpdateCheckState {
        val rawTag = releaseObj.optString("tag_name", installedName).ifBlank { installedName }
        val cleanRemoteVersion = rawTag.removePrefix("v").removePrefix("V")
        val releaseNotes = releaseObj.optString("body", "").takeIf { it.isNotBlank() }

        var apkAssetUrl: String? = null
        var assetSizeMb = 0f
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
        val isNewer = cmp > 0 && apkAssetUrl != null
        val summary = when {
            cmp > 0 && apkAssetUrl != null ->
                "🎉 يتوفر تحديث جديد على GitHub (v$cleanRemoteVersion) أحدث من إصدارك الحالي (v$installedName) • الحجم: ${formatMb(assetSizeMb)}"
            cmp > 0 && apkAssetUrl == null ->
                "⚠️ تم العثور على الإصدار v$cleanRemoteVersion على GitHub ولكن لا يوجد ملف APK مرفق في هذا الإصدار بعد."
            cmp == 0 ->
                "✅ تم التحقق من GitHub: أنت تستخدم أحدث إصدار حالياً (v$installedName) ولا يوجد تحديث جديد."
            else ->
                "✅ إصدارك المثبّت (v$installedName) أحدث من الإصدار المنشور على GitHub (v$cleanRemoteVersion)."
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

    private fun fetchTextFromUrl(urlStr: String, isGitHubApi: Boolean): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                if (isGitHubApi) {
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                setRequestProperty("User-Agent", "MazeArrows-Android-Updater")
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
     * Streams a REAL updated APK file from GitHub in Megabytes (MB) inside the app
     * and launches the Android system package installer within system permissions.
     * NEVER performs a fake/local-copy download if no real update exists on GitHub!
     */
    suspend fun downloadRealGitHubUpdateAndInstall(
        context: Context,
        targetUrl: String,
        hasDirectApk: Boolean,
        installedVersion: String,
        remoteVersion: String?,
        expectedTotalMb: Float,
        onProgressMb: (downloadedMb: Float, totalMb: Float, percent: Int) -> Unit,
        onComplete: (Boolean, String, String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            // 1. Verify that a real newer version & real APK URL exist on GitHub
            if (remoteVersion.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    onComplete(
                        false,
                        "🔍 تم فحص رابط GitHub بناءً على إصدارك الحالي (v$installedVersion): لا يوجد إصدار مرفوع على المستودع حالياً لتنزيله.",
                        null
                    )
                }
                return@withContext
            }

            val cmp = compareVersions(installedVersion, remoteVersion)
            if (cmp <= 0) {
                withContext(Dispatchers.Main) {
                    onComplete(
                        false,
                        "✅ تطبيقك محدّث بالفعل لأحدث إصدار (v$installedVersion). لا يوجد إصدار أحدث على GitHub لتنزيله.",
                        null
                    )
                }
                return@withContext
            }

            if (!hasDirectApk || !targetUrl.startsWith("https://")) {
                withContext(Dispatchers.Main) {
                    onComplete(
                        false,
                        "⚠️ الإصدار v$remoteVersion على GitHub لا يحتوي على ملف APK مرفق للتنزيل المباشر بعد.",
                        null
                    )
                }
                return@withContext
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.cacheDir, "updates").apply { mkdirs() }
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val cleanVer = remoteVersion.removePrefix("v").removePrefix("V")
            val destApkFile = File(downloadsDir, "MazeArrows-v$cleanVer.apk")

            var conn: HttpURLConnection? = null
            var downloadSuccess = false
            try {
                val url = URL(targetUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                    setRequestProperty("User-Agent", "MazeArrows-Android-Updater")
                }

                if (conn.responseCode in 200..299) {
                    val contentLength = conn.contentLengthLong
                    val totalMb = if (contentLength > 0L) {
                        contentLength.toFloat() / (1024f * 1024f)
                    } else if (expectedTotalMb > 0f) {
                        expectedTotalMb
                    } else {
                        17.0f
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

                    // Verify the downloaded file is a genuine APK archive (starts with 'PK' ZIP header and > 500 KB)
                    if (isValidApkArchive(destApkFile)) {
                        downloadSuccess = true
                    } else {
                        destApkFile.delete()
                    }
                }
            } catch (_: Exception) {
                downloadSuccess = false
            } finally {
                conn?.disconnect()
            }

            withContext(Dispatchers.Main) {
                if (downloadSuccess && destApkFile.exists()) {
                    val finalMb = destApkFile.length().toFloat() / (1024f * 1024f)
                    onProgressMb(finalMb, finalMb, 100)
                    val installOutcome = launchSystemInstallerWithPermissions(context, destApkFile)
                    val msg = when (installOutcome) {
                        InstallLaunchOutcome.LAUNCHED_INSTALLER ->
                            "✅ اكتمل تنزيل التحديث الحقيقي v$cleanVer (${formatMb(finalMb)}) من GitHub! تم فتح مثبّت النظام لتثبيت التحديث."
                        InstallLaunchOutcome.REQUESTED_PERMISSION ->
                            "🔐 اكتمل تنزيل التحديث v$cleanVer (${formatMb(finalMb)})! يرجى السماح بصلاحية تثبيت التحديث من شاشة النظام ثم اضغط «تثبيت التحديث الآن»."
                        InstallLaunchOutcome.FAILED ->
                            "⚠️ تم تنزيل ملف التحديث v$cleanVer (${formatMb(finalMb)}) ولكن تعذر فتح المثبّت تلقائياً."
                    }
                    onComplete(true, msg, destApkFile.absolutePath)
                } else {
                    onComplete(
                        false,
                        "⚠️ تعذر تنزيل ملف APK صالح من GitHub. تأكد من رفع ملف MazeArrows.apk ضمن إصدار أحدث من v$installedVersion.",
                        null
                    )
                }
            }
        }
    }

    private fun isValidApkArchive(file: File): Boolean {
        if (!file.exists() || file.length() < 500_000L) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                // Standard APK / ZIP magic number: 0x50 ('P'), 0x4B ('K'), 0x03, 0x04
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    enum class InstallLaunchOutcome {
        LAUNCHED_INSTALLER,
        REQUESTED_PERMISSION,
        FAILED
    }

    /**
     * Launches the Android system package installer using FileProvider within system permissions.
     * Handles permissions, explicit URI grants, intent fallbacks, and multi-OEM compatibility.
     */
    fun launchSystemInstallerWithPermissions(context: Context, apkFile: File): InstallLaunchOutcome {
        if (!apkFile.exists()) return InstallLaunchOutcome.FAILED
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = try {
                    context.packageManager.canRequestPackageInstalls()
                } catch (_: Exception) {
                    true
                }
                if (!canInstall) {
                    var openedSettings = false
                    try {
                        val permIntent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(permIntent)
                        openedSettings = true
                    } catch (_: Exception) {
                        try {
                            val secIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(secIntent)
                            openedSettings = true
                        } catch (_: Exception) {
                        }
                    }
                    if (openedSettings) {
                        return InstallLaunchOutcome.REQUESTED_PERMISSION
                    }
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            // Primary: ACTION_VIEW with package-archive
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Grant read permission to all candidate package installers
            try {
                val resInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
                for (resolveInfo in resInfoList) {
                    val pkg = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (_: Exception) {
            }

            var launched = false
            try {
                context.startActivity(installIntent)
                launched = true
            } catch (_: Exception) {
                // Secondary fallback: ACTION_INSTALL_PACKAGE
                try {
                    val legacyIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                        data = contentUri
                        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(legacyIntent)
                    launched = true
                } catch (_: Exception) {
                    // Tertiary fallback: Intent Chooser
                    try {
                        val chooser = Intent.createChooser(installIntent, "تثبيت التحديث بواسطة").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                        launched = true
                    } catch (_: Exception) {
                    }
                }
            }

            if (launched) {
                InstallLaunchOutcome.LAUNCHED_INSTALLER
            } else {
                InstallLaunchOutcome.FAILED
            }
        } catch (_: Exception) {
            InstallLaunchOutcome.FAILED
        }
    }

    /**
     * Opens direct download or GitHub releases page in external browser (e.g. Chrome)
     * so user can download & install directly with 1 tap if in-app installer encounters OEM hurdles.
     */
    fun openDirectDownloadInBrowser(context: Context, url: String) {
        try {
            val target = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                GITHUB_REPO_URL
            }
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {
        }
    }

    /**
     * Shares or opens the downloaded APK via any installed File Manager or app on device.
     */
    fun openApkInSystemFiles(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "فتح أو مشاركة ملف التحديث (APK)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    const val CHANNEL_ID = "maze_arrows_updates"
    const val NOTIFICATION_ID = 1301

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تحديثات Maze Arrows"
            val descriptionText = "إشعارات توفر الإصدارات والتحديثات الجديدة للعبة"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postUpdateNotification(
        context: Context,
        versionName: String = "1.40",
        summaryText: String = "يتوفر الإصدار الأحدث 1.40 مع تحسينات مميزة للواجهة واللعب! اضغط لتثبيت التحديث الآن."
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_UPDATER", true)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("🎯 يتوفر تحديث جديد للتطبيق (الإصدار $versionName)")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {
        }
    }
}
