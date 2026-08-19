package com.aistudio.mcpchat

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.mcpchat.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNav_chatTabIsSelectedByDefault() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainApp()
            }
        }

        composeTestRule.onNodeWithText("Chat").assertExists()
        composeTestRule.onNodeWithContentDescription("Chat").assertIsSelected()
    }

    @Test
    fun bottomNav_navigateToServers() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainApp()
            }
        }

        composeTestRule.onNodeWithText("MCP Servers").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("MCP Servers").assertIsSelected()
    }

    @Test
    fun bottomNav_navigateToInspector() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainApp()
            }
        }

        composeTestRule.onNodeWithText("Inspector").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Inspector").assertIsSelected()
    }

    @Test
    fun bottomNav_navigateBackToChat() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainApp()
            }
        }

        composeTestRule.onNodeWithText("MCP Servers").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Chat").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Chat").assertIsSelected()
    }
}
