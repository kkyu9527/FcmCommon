package com.kixyu9527.fcmcommon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.TestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetLocalDraft() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            .createDeviceProtectedStorageContext()
        context.getSharedPreferences("fcmcommon.config", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("fcmcommon.ui", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun navigation_switchesBetweenPagesAndDiagnosticsDetail() {
        composeRule.onNodeWithTag(TestTags.Header).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BottomBar).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.PageOverview).assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.navItem("apps")).performClick()
        composeRule.onNodeWithTag(TestTags.PageApps).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.AppsSearchField).assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.navItem("settings")).performClick()
        composeRule.onNodeWithTag(TestTags.PageSettings).assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.SettingsDiagnosticsEntry).performClick()
        composeRule.onNodeWithTag(TestTags.PageDiagnosticsDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.DiagnosticsCard).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.TopBarBack).performClick()

        composeRule.onNodeWithTag(TestTags.PageSettings).assertIsDisplayed()
    }

    @Test
    fun keepNotifications_togglePersistsAcrossRecreate() {
        val toggleTag = TestTags.featureToggle(FeatureKey.KeepNotifications)

        composeRule.onNodeWithTag(TestTags.navItem("settings")).performClick()
        composeRule.onNodeWithTag(TestTags.SettingsFeaturesEntry).performClick()
        composeRule.onNodeWithTag(TestTags.PageFeatureSettings).assertIsDisplayed()
        composeRule.onNodeWithTag(toggleTag).performScrollTo().assertIsOff()
        composeRule.onNodeWithTag(toggleTag).performClick()
        composeRule.onNodeWithTag(toggleTag).assertIsOn()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithTag(TestTags.navItem("settings")).performClick()
        composeRule.onNodeWithTag(TestTags.SettingsFeaturesEntry).performClick()
        composeRule.onNodeWithTag(toggleTag).performScrollTo().assertIsOn()
    }

    @Test
    fun recommendedPreset_restoresDefaultFeatureState() {
        val toggleTag = TestTags.featureToggle(FeatureKey.PowerKeeperBypass)

        composeRule.onNodeWithTag(TestTags.navItem("settings")).performClick()
        composeRule.onNodeWithTag(TestTags.SettingsFeaturesEntry).performClick()
        composeRule.onNodeWithTag(toggleTag).performScrollTo().assertIsOn()
        composeRule.onNodeWithTag(toggleTag).performClick()
        composeRule.onNodeWithTag(toggleTag).assertIsOff()

        composeRule.onNodeWithTag(TestTags.ActionApplyRecommended).performScrollTo().performClick()
        composeRule.onNodeWithTag(toggleTag).performScrollTo().assertIsOn()
    }

    @Test
    fun themeMode_dropdownPersistsAcrossRecreate() {
        composeRule.onNodeWithTag(TestTags.navItem("settings")).performClick()
        composeRule.onNodeWithTag(TestTags.ThemeModeDropdown).performClick()
        composeRule.onNodeWithTag(TestTags.themeMode(AppThemeMode.Dark)).performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithTag(TestTags.navItem("settings")).performClick()
        composeRule.onNodeWithText(AppThemeMode.Dark.title).assertIsDisplayed()
    }
}
