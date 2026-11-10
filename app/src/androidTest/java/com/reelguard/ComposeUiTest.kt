package com.reelguard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.Settings
import com.reelguard.domain.EvaluateLimit
import com.reelguard.ui.dashboard.DashboardContent
import com.reelguard.ui.dashboard.DashboardState
import com.reelguard.ui.onboarding.OnboardingContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun onboardingShowsDisclosureBeforeAskingForAccessibility() {
        rule.setContent { MaterialTheme { OnboardingContent(isServiceEnabled = { false }, onFinish = {}) } }
        rule.onNodeWithTag("next").performClick()
        rule.onNodeWithTag("disclosure_title").assertIsDisplayed()
        rule.onNodeWithText("Open accessibility settings").assertDoesNotExist()
        rule.onNodeWithTag("agree").performClick()
        rule.onNodeWithText("Open accessibility settings").assertIsDisplayed()
    }

    private fun state(
        vararg counts: Pair<String, Int>,
        limit: Int = 50,
    ): DashboardState {
        val s = Settings(combinedLimit = limit)
        return DashboardState(EvaluateLimit.evaluate(counts.toMap(), s), s, emptySet(), serviceEnabled = true, showWidgetPrompt = false)
    }

    @Test
    fun dashboardShowsProgressAndRemaining() {
        rule.setContent { MaterialTheme { DashboardContent(state(Apps.YOUTUBE to 30, Apps.INSTAGRAM to 7), onOpenHealth = {}) } }
        rule.onNodeWithTag("count").assertIsDisplayed()
        rule.onNodeWithText("37").assertIsDisplayed()
        rule.onNodeWithText("of 50").assertIsDisplayed()
        rule.onNodeWithText("13").assertIsDisplayed()
    }

    @Test
    fun dashboardShowsLimitReached() {
        rule.setContent { MaterialTheme { DashboardContent(state(Apps.YOUTUBE to 50), onOpenHealth = {}) } }
        rule.onNodeWithText("Limit reached").assertIsDisplayed()
    }
}
