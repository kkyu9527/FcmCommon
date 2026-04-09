package com.kixyu9527.fcmcommon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("fcmcommon.config", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun homeScreen_exposesRuntimeAndDiagnostics() {
        composeRule.onNodeWithTag(TestTags.HomeList).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.ConnectionHeadline).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.DiagnosticsCard).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun keepNotifications_togglePersistsAcrossRecreate() {
        val toggleTag = TestTags.featureToggle(FeatureKey.KeepNotifications)

        composeRule.onNodeWithTag(toggleTag).performScrollTo().assertIsOff()
        composeRule.onNodeWithTag(toggleTag).performClick()
        composeRule.onNodeWithTag(toggleTag).assertIsOn()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithTag(toggleTag).performScrollTo().assertIsOn()
    }
}
