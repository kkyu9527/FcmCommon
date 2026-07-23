package com.kixyu9527.fcmcommon

import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.data.ModuleConfig
import com.kixyu9527.fcmcommon.data.UiSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleConfigTest {
    @Test
    fun defaultConfig_enablesCoreHyperOsFeatures() {
        val config = ModuleConfig()

        assertTrue(config.isEnabled(FeatureKey.HyperOsBroadcastShield))
        assertTrue(config.isEnabled(FeatureKey.WakeStoppedApps))
        assertTrue(config.isEnabled(FeatureKey.PowerKeeperBypass))
        assertTrue(config.isEnabled(FeatureKey.GmsReconnectTuning))
        assertFalse(config.isEnabled(FeatureKey.KeepNotifications))
    }

    @Test
    fun defaultUiSettings_showAllApps() {
        assertFalse(UiSettings().onlyShowPushApps)
    }
}
