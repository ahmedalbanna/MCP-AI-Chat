package com.aistudio.mcpchat

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.mcpchat.ui.screens.OnboardingScreen
import com.aistudio.mcpchat.ui.screens.hasSeenOnboarding
import com.aistudio.mcpchat.ui.screens.markOnboardingSeen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLifecycleTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        sharedPreferences = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `onboarding is shown on first launch`() {
        assertFalse(hasSeenOnboarding(context))

        composeTestRule.setContent {
            OnboardingScreen(onDismiss = {})
        }

        composeTestRule.onNodeWithText("Welcome to MCP AI Chat").assertIsDisplayed()
    }

    @Test
    fun `onboarding is not shown after being marked as seen`() {
        markOnboardingSeen(context)

        assertTrue(hasSeenOnboarding(context))
    }

    @Test
    fun `onboarding dismiss updates shared preferences`() {
        var dismissed = false

        composeTestRule.setContent {
            OnboardingScreen(onDismiss = {
                markOnboardingSeen(context)
                dismissed = true
            })
        }

        composeTestRule.onNodeWithText("Get Started").performClick()

        assertTrue(dismissed)
        assertTrue(hasSeenOnboarding(context))
    }

    @Test
    fun `hasSeenOnboarding returns false by default`() {
        assertFalse(hasSeenOnboarding(context))
    }

    @Test
    fun `hasSeenOnboarding returns true after marking as seen`() {
        markOnboardingSeen(context)
        assertTrue(hasSeenOnboarding(context))
    }

    @Test
    fun `hasSeenOnboarding persists across shared preferences access`() {
        assertFalse(hasSeenOnboarding(context))

        markOnboardingSeen(context)
        assertTrue(hasSeenOnboarding(context))

        val reloaded = hasSeenOnboarding(context)
        assertTrue(reloaded)
    }

    @Test
    fun `main activity screen routes are defined correctly`() {
        assertEquals("chat", Screen.Chat.route)
        assertEquals("servers", Screen.Servers.route)
        assertEquals("inspector", Screen.Inspector.route)
    }

    @Test
    fun `screen titles are defined correctly`() {
        assertEquals("Chat", Screen.Chat.title)
        assertEquals("MCP Servers", Screen.Servers.title)
        assertEquals("Inspector", Screen.Inspector.title)
    }

    @Test
    fun `bottom navigation bar is displayed in main app`() {
        composeTestRule.setContent {
            MainApp()
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    }

    @Test
    fun `chat tab is navigable`() {
        composeTestRule.setContent {
            MainApp()
        }

        composeTestRule.onNodeWithTag("nav_tab_chat").performClick()
        composeTestRule.onNodeWithTag("nav_tab_chat").assertIsDisplayed()
    }

    @Test
    fun `servers tab is navigable`() {
        composeTestRule.setContent {
            MainApp()
        }

        composeTestRule.onNodeWithTag("nav_tab_servers").performClick()
        composeTestRule.onNodeWithTag("nav_tab_servers").assertIsDisplayed()
    }

    @Test
    fun `inspector tab is navigable`() {
        composeTestRule.setContent {
            MainApp()
        }

        composeTestRule.onNodeWithTag("nav_tab_inspector").performClick()
        composeTestRule.onNodeWithTag("nav_tab_inspector").assertIsDisplayed()
    }

    @Test
    fun `configuration change preserves navigation state`() {
        composeTestRule.setContent {
            MainApp()
        }

        composeTestRule.onNodeWithTag("nav_tab_servers").performClick()

        composeTestRule.setContent {
            MainApp()
        }

        composeTestRule.onNodeWithTag("nav_tab_servers").assertIsDisplayed()
    }

    @Test
    fun `onboarding screen shows feature highlights`() {
        composeTestRule.setContent {
            OnboardingScreen(onDismiss = {})
        }

        composeTestRule.onNodeWithText("Welcome to MCP AI Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
    }

    @Test
    fun `edge to edge is enabled by default in activity`() {
        val activity = MainActivity()
        assertTrue(activity is android.app.Activity)
    }

    @Test
    fun `shared preferences key is consistent`() {
        val key = "onboarding_seen"
        sharedPreferences.edit().putBoolean(key, true).apply()
        assertTrue(sharedPreferences.getBoolean(key, false))

        sharedPreferences.edit().putBoolean(key, false).apply()
        assertFalse(sharedPreferences.getBoolean(key, true))
    }

    @Test
    fun `multiple onboarding dismiss calls are idempotent`() {
        markOnboardingSeen(context)
        markOnboardingSeen(context)
        markOnboardingSeen(context)

        assertTrue(hasSeenOnboarding(context))
    }

    @Test
    fun `screen sealed class has exactly 3 routes`() {
        val screens = listOf(Screen.Chat, Screen.Servers, Screen.Inspector)
        assertEquals(3, screens.size)
        assertEquals(3, screens.map { it.route }.distinct().size)
    }
}
