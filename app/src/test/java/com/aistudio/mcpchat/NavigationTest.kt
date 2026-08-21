package com.aistudio.mcpchat

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.mcpchat.ui.screens.markOnboardingSeen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@HiltAndroidTest
@Config(sdk = [36], application = HiltTestApplication::class)
class NavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun skipOnboarding() {
        // The activity launched during rule setup; seed the flag and recreate
        // so composition starts on the main app surface, not the overlay.
        markOnboardingSeen(RuntimeEnvironment.getApplication())
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomNav_chatTabIsSelectedByDefault() {
        composeTestRule.onNodeWithText("Chat").assertExists()
        composeTestRule.onNodeWithTag("nav_tab_chat").assertIsSelected()
    }

    @Test
    fun bottomNav_navigateToServers() {
        composeTestRule.onNodeWithTag("nav_tab_servers").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_servers").assertIsSelected()
    }

    @Test
    fun bottomNav_navigateToInspector() {
        composeTestRule.onNodeWithTag("nav_tab_inspector").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_inspector").assertIsSelected()
    }

    @Test
    fun bottomNav_navigateBackToChat() {
        composeTestRule.onNodeWithTag("nav_tab_servers").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_chat").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_chat").assertIsSelected()
    }
}
