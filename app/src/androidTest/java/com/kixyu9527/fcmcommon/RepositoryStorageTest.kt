package com.kixyu9527.fcmcommon

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kixyu9527.fcmcommon.data.ConfigKeys
import com.kixyu9527.fcmcommon.data.ConnectionEventRepository
import com.kixyu9527.fcmcommon.data.ConnectionEventType
import com.kixyu9527.fcmcommon.data.FcmDiagnosticsStateRepository
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.data.InstalledAppsRepository
import com.kixyu9527.fcmcommon.data.LocalModuleConfigRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryStorageTest {
    private lateinit var storageContext: Context

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        storageContext = appContext.createDeviceProtectedStorageContext()

        listOf(
            ConfigKeys.LocalPrefsName,
            ConfigKeys.UiPrefsName,
            ConfigKeys.InstalledAppsCachePrefsName,
            ConfigKeys.ConnectionPrefsName,
        ).forEach { prefsName ->
            storageContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }

        storageContext.filesDir.resolve("installed_app_icons")
            .deleteRecursively()
    }

    @Test
    fun installedAppsSnapshot_returnsEmptyWhenCacheMissing() {
        val snapshot = InstalledAppsRepository(storageContext).loadCachedInstalledAppsSnapshot()

        assertFalse(snapshot.hasSnapshot)
        assertTrue(snapshot.apps.isEmpty())
    }

    @Test
    fun installedAppsSnapshot_returnsEmptyWhenCacheJsonMalformed() {
        storageContext.getSharedPreferences(
            ConfigKeys.InstalledAppsCachePrefsName,
            Context.MODE_PRIVATE,
        ).edit(commit = true) {
            putString(ConfigKeys.KeyInstalledAppsCache, "{not-json")
        }

        val snapshot = InstalledAppsRepository(storageContext).loadCachedInstalledAppsSnapshot()

        assertFalse(snapshot.hasSnapshot)
        assertTrue(snapshot.apps.isEmpty())
    }

    @Test
    fun installedAppsSnapshot_normalizesLegacyCachedFields() {
        val payload = JSONArray().put(
            JSONObject().apply {
                put("packageName", "com.example.push")
                put("label", "")
                put("isSystemApp", false)
                put("hasPushSupport", true)
                put("versionName", "")
                put("versionCode", 42L)
                put("targetSdkVersion", 35)
                put("minSdkVersion", JSONObject.NULL)
                put("firstInstallTime", 1000L)
                put("lastUpdateTime", 2000L)
                put("installerPackageName", "")
                put("processName", JSONObject.NULL)
                put("uid", 10234)
                put("isEnabled", false)
            },
        )
        storageContext.getSharedPreferences(
            ConfigKeys.InstalledAppsCachePrefsName,
            Context.MODE_PRIVATE,
        ).edit(commit = true) {
            putString(ConfigKeys.KeyInstalledAppsCache, payload.toString())
        }

        val app = InstalledAppsRepository(storageContext)
            .loadCachedInstalledAppsSnapshot()
            .apps
            .single()

        assertEquals("com.example.push", app.label)
        assertEquals("-", app.versionName)
        assertNull(app.minSdkVersion)
        assertEquals("-", app.installerPackageName)
        assertNull(app.processName)
        assertFalse(app.isEnabled)
    }

    @Test
    fun installedAppsRepository_usesInjectedAccessChecker() {
        val repository = InstalledAppsRepository(storageContext) { false }

        assertFalse(repository.canReadInstalledApps())
    }

    @Test
    fun localModuleConfigRepository_readsLegacyFeatureNamesAndAllowList() {
        storageContext.getSharedPreferences(
            ConfigKeys.LocalPrefsName,
            Context.MODE_PRIVATE,
        ).edit(commit = true) {
            putBoolean(ConfigKeys.KeyInit, true)
            putStringSet(
                ConfigKeys.KeyEnabledFeatures,
                setOf(
                    FeatureKey.KeepNotifications.name,
                    FeatureKey.PowerKeeperBypass.prefKey,
                    "missing_feature",
                ),
            )
            putStringSet(ConfigKeys.KeyAllowList, setOf("com.example.push"))
        }

        val config = LocalModuleConfigRepository(storageContext).config.value

        assertEquals(
            setOf(FeatureKey.KeepNotifications, FeatureKey.PowerKeeperBypass),
            config.enabledFeatures,
        )
        assertEquals(setOf("com.example.push"), config.allowList)
    }

    @Test
    fun connectionAndDiagnosticsRepositories_handleCompatibilityFallbacks() {
        val eventsPayload = JSONArray()
            .put(
                JSONObject().apply {
                    put("recordedAtMillis", 123L)
                    put("type", "DeprecatedType")
                    put("detail", "should_skip")
                },
            )
            .put(
                JSONObject().apply {
                    put("recordedAtMillis", 456L)
                    put("type", ConnectionEventType.Connected.name)
                    put("detail", "bridge_ready")
                },
            )
        storageContext.getSharedPreferences(
            ConfigKeys.ConnectionPrefsName,
            Context.MODE_PRIVATE,
        ).edit(commit = true) {
            putString(ConfigKeys.KeyConnectionEvents, eventsPayload.toString())
            putBoolean(ConfigKeys.KeyFcmDiagnosticsConnected, true)
            putLong(ConfigKeys.KeyFcmDiagnosticsConnectedSince, 0L)
            putLong(ConfigKeys.KeyFcmDiagnosticsLastEventAt, 0L)
            putString(ConfigKeys.KeyFcmDiagnosticsLastEventTitle, "")
            putString(ConfigKeys.KeyFcmDiagnosticsLastEventDetail, "")
        }

        val events = ConnectionEventRepository(storageContext).loadEvents()
        val diagnostics = FcmDiagnosticsStateRepository(storageContext).loadState()

        assertEquals(1, events.size)
        assertEquals(ConnectionEventType.Connected, events.single().type)
        assertEquals("bridge_ready", events.single().detail)
        assertTrue(diagnostics.isConnected)
        assertNull(diagnostics.connectedSinceMillis)
        assertNull(diagnostics.lastEventAtMillis)
        assertEquals("暂无记录", diagnostics.lastEventTitle)
        assertEquals("等待 Google Play 服务上报连接状态。", diagnostics.lastEventDetail)
    }
}
