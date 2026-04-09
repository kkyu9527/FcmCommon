package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val hasPushSupport: Boolean,
)

class InstalledAppsRepository(
    private val context: Context,
) {
    suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val pushPackages = detectPushPackages(packageManager)

        getInstalledApplications(packageManager)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(packageManager)?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: appInfo.packageName,
                    icon = appInfo.loadIcon(packageManager),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    hasPushSupport = appInfo.packageName in pushPackages,
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
