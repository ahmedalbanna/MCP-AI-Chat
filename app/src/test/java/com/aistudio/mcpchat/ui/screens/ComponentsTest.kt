package com.aistudio.mcpchat.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun serverStatusBadge_connected_showsOnline() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.CONNECTED, latencyMs = 42)
            }
        }
        composeTestRule.onNodeWithText("42ms").assertIsDisplayed()
    }

    @Test
    fun serverStatusBadge_disconnected_showsDisabled() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.DISCONNECTED)
            }
        }
        composeTestRule.onNodeWithText("Disabled").assertIsDisplayed()
    }

    @Test
    fun serverStatusBadge_connecting_showsConnecting() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.CONNECTING)
            }
        }
        composeTestRule.onNodeWithText("Connecting...").assertIsDisplayed()
    }

    @Test
    fun serverStatusBadge_error_showsOffline() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.ERROR)
            }
        }
        composeTestRule.onNodeWithText("Offline").assertIsDisplayed()
    }

    @Test
    fun serverStatusBadge_connected_noLatency_showsOnline() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.CONNECTED, latencyMs = 0)
            }
        }
        composeTestRule.onNodeWithText("Online").assertIsDisplayed()
    }

    @Test
    fun toolCallCard_success_showsToolName() {
        val toolCall = McpToolCall(
            callId = "call_1",
            serverId = "server_1",
            serverName = "Test Server",
            toolName = "get_weather",
            inputArguments = mapOf("city" to "Tokyo"),
            outputResult = "{\"temp\": 22}",
            isSuccess = true,
            executionTimeMs = 150,
            status = ToolExecutionStatus.SUCCESS
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ToolCallCard(toolCall = toolCall)
            }
        }
        composeTestRule.onNodeWithText("get_weather").assertIsDisplayed()
    }

    @Test
    fun toolCallCard_executing_showsExecutingStatus() {
        val toolCall = McpToolCall(
            callId = "call_1",
            serverId = "server_1",
            serverName = "Test Server",
            toolName = "fetch_data",
            inputArguments = emptyMap(),
            status = ToolExecutionStatus.EXECUTING
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ToolCallCard(toolCall = toolCall)
            }
        }
        composeTestRule.onNodeWithText("fetch_data").assertIsDisplayed()
    }

    @Test
    fun codeBlock_displaysCode() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CodeBlock(code = """{"key": "value"}""", language = "json")
            }
        }
        composeTestRule.onNodeWithText("JSON").assertIsDisplayed()
    }
}
