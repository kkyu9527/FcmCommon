package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val hasPushSupport: Boolean,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installerPackageName: String,
    val processName: String?,
    val uid: Int,
    val isEnabled: Boolean,
)

data class InstalledAppsCacheSnapshot(
    val apps: List<InstalledAppInfo>,
    val hasSnapshot: Boolean,
)

private data class CachedInstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val hasPushSupport: Boolean,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installerPackageName: String,
    val processName: String?,
    val uid: Int,
    val isEnabled: Boolean,
)

class InstalledAppsRepository(
    context: Context,
    private val installedAppsAccessChecker: () -> Boolean = { true },
) {
    private val appContext = context.applicationContext
    private val storageContext = appContext.createDeviceProtectedStorageContext()
    private val iconCacheDir = File(storageContext.filesDir, "installed_app_icons")
    private val preferences: SharedPreferences =
        storageContext.getSharedPreferences(
            ConfigKeys.InstalledAppsCachePrefsName,
            Context.MODE_PRIVATE,
        )
    private val iconMemoryCache = LruCache<String, Drawable>(120)

    fun canReadInstalledApps(): Boolean = installedAppsAccessChecker()

    fun loadCachedInstalledAppsSnapshot(): InstalledAppsCacheSnapshot {
        val rawCache = preferences.getString(ConfigKeys.KeyInstalledAppsCache, null)
            ?: return InstalledAppsCacheSnapshot(
                apps = emptyList(),
                hasSnapshot = false,
            )

        return runCatching {
            InstalledAppsCacheSnapshot(
                apps = decodeCachedApps(rawCache).map { cached ->
                    cached.toInstalledAppInfo()
                },
                hasSnapshot = true,
            )
        }.getOrElse {
            InstalledAppsCacheSnapshot(
                apps = emptyList(),
                hasSnapshot = false,
            )
        }
    }

    suspend fun refreshInstalledAppsCache(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val scannedApps = scanInstalledApps()
        persistInstalledAppsCache(scannedApps)
        persistIconCache(scannedApps)
        scannedApps
    }

    suspend fun hydrateCachedInstalledAppsIcons(
        apps: List<InstalledAppInfo>,
    ): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        apps.map { app ->
            if (app.icon != null) {
                app
            } else {
                app.copy(icon = loadCachedIcon(app.packageName))
            }
        }
    }

    private fun persistInstalledAppsCache(apps: List<InstalledAppInfo>) {
        val payload = JSONArray().apply {
            apps.forEach { app ->
                put(
                    JSONObject().apply {
                        put("packageName", app.packageName)
                        put("label", app.label)
                        put("isSystemApp", app.isSystemApp)
                        put("hasPushSupport", app.hasPushSupport)
                        put("versionName", app.versionName)
                        put("versionCode", app.versionCode)
                        put("targetSdkVersion", app.targetSdkVersion)
                        put("minSdkVersion", app.minSdkVersion ?: JSONObject.NULL)
                        put("firstInstallTime", app.firstInstallTime)
                        put("lastUpdateTime", app.lastUpdateTime)
                        put("installerPackageName", app.installerPackageName)
                        put("processName", app.processName ?: JSONObject.NULL)
                        put("uid", app.uid)
                        put("isEnabled", app.isEnabled)
                    },
                )
            }
        }.toString()

        preferences.edit(commit = true) {
            putString(ConfigKeys.KeyInstalledAppsCache, payload)
        }
    }

    private fun decodeCachedApps(rawCache: String): List<CachedInstalledAppInfo> {
        val array = JSONArray(rawCache)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    CachedInstalledAppInfo(
                        packageName = item.optString("packageName"),
                        label = item.optString("label"),
                        isSystemApp = item.optBoolean("isSystemApp"),
                        hasPushSupport = item.optBoolean("hasPushSupport"),
                        versionName = item.optString("versionName", "-"),
                        versionCode = item.optLong("versionCode"),
                        targetSdkVersion = item.optInt("targetSdkVersion"),
                        minSdkVersion = item.optNullableInt("minSdkVersion"),
                        firstInstallTime = item.optLong("firstInstallTime"),
                        lastUpdateTime = item.optLong("lastUpdateTime"),
                        installerPackageName = item.optString("installerPackageName", "-"),
                        processName = item.optNullableString("processName"),
                        uid = item.optInt("uid"),
                        isEnabled = item.optBoolean("isEnabled", true),
                    ),
                )
            }
        }
    }

    private fun persistIconCache(apps: List<InstalledAppInfo>) {
        if (!iconCacheDir.exists()) {
            iconCacheDir.mkdirs()
        }

        val activePackages = apps.mapTo(linkedSetOf()) { it.packageName }
        iconCacheDir.listFiles()
            ?.filter { cachedFile -> cachedFile.nameWithoutExtension !in activePackages }
            ?.forEach(File::delete)

        apps.forEach { app ->
            val icon = app.icon ?: return@forEach
            runCatching {
                val bitmap = icon.toBitmap(
                    width = IconCacheSizePx,
                    height = IconCacheSizePx,
                    config = Bitmap.Config.ARGB_8888,
                )
                FileOutputStream(iconFile(app.packageName)).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                iconMemoryCache.put(
                    app.packageName,
                    BitmapDrawable(storageContext.resources, bitmap),
                )
            }
        }
    }

    private fun loadCachedIcon(packageName: String): Drawable? {
        iconMemoryCache.get(packageName)?.let { cached ->
            return cached.constantState?.newDrawable()?.mutate() ?: cached
        }

        val iconFile = iconFile(packageName)
        if (!iconFile.exists()) return null

        val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath) ?: return null
        return BitmapDrawable(storageContext.resources, bitmap).also { drawable ->
            iconMemoryCache.put(packageName, drawable)
        }
    }

    private fun iconFile(packageName: String): File = File(iconCacheDir, "$packageName.png")

    private suspend fun scanInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val packageManager = appContext.packageManager
        val pushPackages = detectPushPackages(packageManager)

        getInstalledApplications(packageManager)
            .asSequence()
            .filter { it.packageName != appContext.packageName }
            .map { appInfo ->
                val packageInfo = getPackageInfo(packageManager, appInfo.packageName)
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(packageManager)?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: appInfo.packageName,
                    icon = appInfo.loadIcon(packageManager),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    hasPushSupport = appInfo.packageName in pushPackages,
                    versionName = packageInfo?.versionName.orEmpty().ifBlank { "-" },
                    versionCode = packageInfo?.toVersionCode() ?: 0L,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    minSdkVersion = appInfo.minSdkVersion,
                    firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
                    installerPackageName = resolveInstallerPackageName(
                        packageManager = packageManager,
                        packageName = appInfo.packageName,
                    ),
                    processName = appInfo.processName,
                    uid = appInfo.uid,
                    isEnabled = appInfo.enabled,
                )
            }
            .toList()
    }

    @Suppress("DEPRECATION")
    private fun getInstalledApplications(packageManager: PackageManager): List<ApplicationInfo> {
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(flags),
            )
        } else {
            packageManager.getInstalledApplications(flags.toInt())
        }
    }

    @Suppress("DEPRECATION")
    private fun getPackageInfo(
        packageManager: PackageManager,
        packageName: String,
    ): PackageInfo? {
        val flags = PackageManager.GET_PERMISSIONS.toLong()
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(flags),
                )
            } else {
                packageManager.getPackageInfo(packageName, flags.toInt())
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun resolveInstallerPackageName(
        packageManager: PackageManager,
        packageName: String,
    ): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val installSourceInfo: InstallSourceInfo =
                    packageManager.getInstallSourceInfo(packageName)
                installSourceInfo.installingPackageName
                    ?: installSourceInfo.initiatingPackageName
                    ?: "-"
            } else {
                packageManager.getInstallerPackageName(packageName) ?: "-"
            }
        }.getOrDefault("-")
    }

    private fun detectPushPackages(packageManager: PackageManager): Set<String> {
        val packages = linkedSetOf<String>()

        listOf(
            "com.google.firebase.MESSAGING_EVENT",
            "com.google.firebase.INSTANCE_ID_EVENT",
        ).forEach { action ->
            queryIntentServices(packageManager, Intent(action))
                .mapNotNullTo(packages) { it.serviceInfo?.packageName }
        }

        queryBroadcastReceivers(
            packageManager,
            Intent("com.google.android.c2dm.intent.RECEIVE"),
        ).mapNotNullTo(packages) { it.activityInfo?.packageName }

        return packages
    }

    @Suppress("DEPRECATION")
    private fun queryIntentServices(
        packageManager: PackageManager,
        intent: Intent,
    ): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()),
            )
        } else {
            packageManager.queryIntentServices(intent, PackageManager.MATCH_DISABLED_COMPONENTS)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryBroadcastReceivers(
        packageManager: PackageManager,
        intent: Intent,
    ): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryBroadcastReceivers(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()),
            )
        } else {
            packageManager.queryBroadcastReceivers(intent, PackageManager.MATCH_DISABLED_COMPONENTS)
        }
    }
}

private const val IconCacheSizePx = 128

private fun CachedInstalledAppInfo.toInstalledAppInfo(): InstalledAppInfo = InstalledAppInfo(
    packageName = packageName,
    label = label.ifBlank { packageName },
    icon = null,
    isSystemApp = isSystemApp,
    hasPushSupport = hasPushSupport,
    versionName = versionName.ifBlank { "-" },
    versionCode = versionCode,
    targetSdkVersion = targetSdkVersion,
    minSdkVersion = minSdkVersion,
    firstInstallTime = firstInstallTime,
    lastUpdateTime = lastUpdateTime,
    installerPackageName = installerPackageName.ifBlank { "-" },
    processName = processName,
    uid = uid,
    isEnabled = isEnabled,
)

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun PackageInfo.toVersionCode(): Long {
    @Suppress("DEPRECATION")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        versionCode.toLong()
    }
}
