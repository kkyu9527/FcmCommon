package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.core.content.edit
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalModuleConfigRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storageContext = appContext.createDeviceProtectedStorageContext()
    private val preferences: SharedPreferences =
        storageContext.getSharedPreferences(ConfigKeys.LocalPrefsName, Context.MODE_PRIVATE)
    @Volatile
    private var remotePreferences: SharedPreferences? = null

    private val mutableConfig = MutableStateFlow(loadConfig())
    val config: StateFlow<ModuleConfig> = mutableConfig.asStateFlow()

    init {
        runCatching {
            XposedServiceHelper.registerListener(
                object : XposedServiceHelper.OnServiceListener {
                    override fun onServiceBind(service: XposedService) {
                        remotePreferences = runCatching {
                            service.getRemotePreferences(ConfigKeys.ConfigGroup)
                        }.getOrNull()
                        val remote = remotePreferences
                        if (remote != null) {
                            if (hasStoredConfig(remote)) {
                                val synced = loadFrom(remote)
                                writeConfig(preferences, synced)
                                mutableConfig.value = synced
                            } else {
                                writeConfig(remote, mutableConfig.value)
                                notifyHookConfigChanged()
                            }
                        }
                    }

                    override fun onServiceDied(service: XposedService) {
                        if (remotePreferences != null) {
                            remotePreferences = null
                        }
                    }
                },
            )
        }
    }

    fun refresh() {
        mutableConfig.value = loadConfig()
    }

    fun primeRemoteSync(timeoutMs: Long = 8_000L) {
        refresh()
        val startedAt = SystemClock.elapsedRealtime()
        while (remotePreferences == null && SystemClock.elapsedRealtime() - startedAt < timeoutMs) {
            Thread.sleep(150)
        }
        remotePreferences?.let { remote ->
            writeConfig(remote, mutableConfig.value)
            notifyHookConfigChanged()
        }
    }

    fun setFeatureEnabled(featureKey: FeatureKey, enabled: Boolean) {
        updateConfig(mutableConfig.value.withFeature(featureKey, enabled))
    }

    fun setAppAllowed(packageName: String, allowed: Boolean) {
        updateConfig(mutableConfig.value.withAllowedPackage(packageName, allowed))
    }

    fun applyRecommendedFeatures() {
        updateConfig(mutableConfig.value.withEnabledFeatures(FeatureKey.defaultEnabledSet))
    }

    fun replaceAllowList(packageNames: Set<String>) {
        updateConfig(mutableConfig.value.withAllowList(packageNames))
    }

    fun clearAllowList() {
        updateConfig(mutableConfig.value.withAllowList(emptySet()))
    }

    private fun loadConfig(): ModuleConfig {
        val remote = remotePreferences
        val source = if (remote != null && hasStoredConfig(remote)) remote else preferences
        val config = loadFrom(source)
        if (source === remote) {
            writeConfig(preferences, config)
        }
        return config
    }

    private fun updateConfig(updated: ModuleConfig) {
        writeConfig(preferences, updated)
        remotePreferences?.let { remote ->
            writeConfig(remote, updated)
            notifyHookConfigChanged()
        }
        mutableConfig.value = updated
    }

    private fun loadFrom(source: SharedPreferences): ModuleConfig {
        val savedFeatures = source.getStringSet(ConfigKeys.KeyEnabledFeatures, null)
            ?.mapNotNull { raw ->
                FeatureKey.entries.firstOrNull { it.prefKey == raw || it.name == raw }
            }
            ?.toSet()
            ?: FeatureKey.defaultEnabledSet

        val savedAllowList = source.getStringSet(ConfigKeys.KeyAllowList, emptySet()).orEmpty()

        return ModuleConfig(
            enabledFeatures = savedFeatures,
            allowList = savedAllowList,
        )
    }

    private fun writeConfig(target: SharedPreferences, config: ModuleConfig) {
        target.edit(commit = true) {
            putBoolean(ConfigKeys.KeyInit, true)
            putStringSet(
                ConfigKeys.KeyEnabledFeatures,
                config.enabledFeatures.mapTo(linkedSetOf()) { it.prefKey },
            )
            putStringSet(ConfigKeys.KeyAllowList, config.allowList)
        }
    }

    private fun hasStoredConfig(source: SharedPreferences): Boolean =
        source.contains(ConfigKeys.KeyInit) ||
            source.contains(ConfigKeys.KeyEnabledFeatures) ||
            source.contains(ConfigKeys.KeyAllowList)

    private fun notifyHookConfigChanged() {
        appContext.sendBroadcast(Intent(ConfigKeys.UpdateConfigAction))
    }
}
