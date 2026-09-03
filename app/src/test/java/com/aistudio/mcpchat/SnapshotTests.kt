package com.aistudio.mcpchat

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.data.local.ThemePreferences
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.ui.screens.EmptyChatView
import com.aistudio.mcpchat.ui.screens.ExecutionResultCard
import com.aistudio.mcpchat.ui.screens.LogItemCard
import com.aistudio.mcpchat.ui.screens.ServerCardItem
import com.aistudio.mcpchat.ui.screens.ServerStatusBadge
import com.aistudio.mcpchat.ui.screens.SettingsScreenContent
import com.aistudio.mcpchat.ui.screens.ToolCallCard
import com.aistudio.mcpchat.ui.screens.TypingIndicator
import com.aistudio.mcpchat.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SnapshotTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(name: String) {
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/$name.png")
    }

    @Test
    fun serverStatusBadge_connected() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.CONNECTED, latencyMs = 42)
            }
        }
        capture("server_status_connected")
    }

    @Test
    fun serverStatusBadge_disconnected() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.DISCONNECTED)
            }
        }
        capture("server_status_disconnected")
    }

    @Test
    fun serverStatusBadge_error() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerStatusBadge(status = ServerStatus.ERROR)
            }
        }
        capture("server_status_error")
    }

    @Test
    fun toolCallCard_success() {
        val toolCall = McpToolCall(
            callId = "call_1",
            serverId = "server_1",
            serverName = "Test Server",
            toolName = "get_weather",
            inputArguments = mapOf("city" to "Tokyo", "units" to "metric"),
            outputResult = "{\"temp\": 22, \"condition\": \"sunny\"}",
            isSuccess = true,
            executionTimeMs = 150,
            status = ToolExecutionStatus.SUCCESS,
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ToolCallCard(toolCall = toolCall)
            }
        }
        capture("tool_call_card_success")
    }

    @Test
    fun toolCallCard_executing() {
        val toolCall = McpToolCall(
            callId = "call_2",
            serverId = "server_1",
            serverName = "Test Server",
            toolName = "fetch_data",
            inputArguments = mapOf("query" to "SELECT * FROM users"),
            status = ToolExecutionStatus.EXECUTING,
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ToolCallCard(toolCall = toolCall)
            }
        }
        capture("tool_call_card_executing")
    }

    @Test
    fun toolCallCard_error() {
        val toolCall = McpToolCall(
            callId = "call_3",
            serverId = "server_1",
            serverName = "Test Server",
            toolName = "process_payment",
            inputArguments = mapOf("amount" to 100, "currency" to "USD"),
            outputResult = "{\"error\": \"Insufficient funds\"}",
            isSuccess = false,
            executionTimeMs = 500,
            status = ToolExecutionStatus.ERROR,
            errorMessage = "Payment processing failed",
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ToolCallCard(toolCall = toolCall)
            }
        }
        capture("tool_call_card_error")
    }

    @Test
    fun settings_light() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                SettingsScreenContent(
                    prefs = ThemePreferences(ThemeMode.LIGHT, useAmoledDark = false, useDynamicColor = false),
                    onSelectMode = {},
                    onAmoledChange = {},
                    onDynamicChange = {},
                )
            }
        }
        capture("settings_light")
    }

    @Test
    fun settings_dark() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                SettingsScreenContent(
                    prefs = ThemePreferences(ThemeMode.DARK, useAmoledDark = false, useDynamicColor = false),
                    onSelectMode = {},
                    onAmoledChange = {},
                    onDynamicChange = {},
                )
            }
        }
        capture("settings_dark")
    }

    @Test
    fun settings_amoled() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true, useAmoledDark = true) {
                SettingsScreenContent(
                    prefs = ThemePreferences(ThemeMode.DARK, useAmoledDark = true, useDynamicColor = false),
                    onSelectMode = {},
                    onAmoledChange = {},
                    onDynamicChange = {},
                )
            }
        }
        capture("settings_amoled")
    }

    @Test
    fun settings_system() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                SettingsScreenContent(
                    prefs = ThemePreferences(ThemeMode.SYSTEM, useAmoledDark = false, useDynamicColor = false),
                    onSelectMode = {},
                    onAmoledChange = {},
                    onDynamicChange = {},
                )
            }
        }
        capture("settings_system")
    }

    @Test
    fun typingIndicator_bubble() {
        composeTestRule.setContent {
            MyApplicationTheme {
                TypingIndicator()
            }
        }
        capture("typing_indicator")
    }

    @Test
    fun emptyChatView_welcome() {
        composeTestRule.setContent {
            MyApplicationTheme {
                EmptyChatView(onPromptSelected = {})
            }
        }
        capture("empty_chat_view")
    }

    @Test
    fun serverCard_connected() {
        val server = McpServer(
            id = "srv_1",
            name = "Weather MCP",
            description = "Provides weather data",
            endpointUrl = "https://weather.example.com/mcp",
            isEnabled = true,
            status = ServerStatus.CONNECTED,
            latencyMs = 85,
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerCardItem(
                    server = server,
                    tools = emptyList(),
                    isTesting = false,
                    onToggleEnabled = {},
                    onToggleTool = { _, _ -> },
                    onPing = {},
                    onSync = {},
                    onEdit = {},
                    onDelete = {},
                    modifier = Modifier,
                )
            }
        }
        capture("server_card_connected")
    }

    @Test
    fun serverCard_error() {
        val server = McpServer(
            id = "srv_2",
            name = "Broken MCP",
            description = "Failing server",
            endpointUrl = "https://broken.example.com/mcp",
            isEnabled = true,
            status = ServerStatus.ERROR,
            latencyMs = 5000,
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerCardItem(
                    server = server,
                    tools = emptyList(),
                    isTesting = false,
                    onToggleEnabled = {},
                    onToggleTool = { _, _ -> },
                    onPing = {},
                    onSync = {},
                    onEdit = {},
                    onDelete = {},
                    modifier = Modifier,
                )
            }
        }
        capture("server_card_error")
    }

    @Test
    fun serverCard_insecure() {
        val server = McpServer(
            id = "srv_3",
            name = "Insecure MCP",
            description = "Plain HTTP server",
            endpointUrl = "http://insecure.example.com/mcp",
            isEnabled = true,
            status = ServerStatus.CONNECTED,
            latencyMs = 42,
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerCardItem(
                    server = server,
                    tools = emptyList(),
                    isTesting = false,
                    onToggleEnabled = {},
                    onToggleTool = { _, _ -> },
                    onPing = {},
                    onSync = {},
                    onEdit = {},
                    onDelete = {},
                    modifier = Modifier,
                )
            }
        }
        capture("server_card_insecure")
    }

    @Test
    fun executionResult_success() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ExecutionResultCard(result = """{"temp": 22, "condition": "sunny"}""", latencyMs = 150, isSuccess = true)
            }
        }
        capture("execution_result_success")
    }

    @Test
    fun executionResult_error() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ExecutionResultCard(result = "Connection refused", latencyMs = 5000, isSuccess = false)
            }
        }
        capture("execution_result_error")
    }

    @Test
    fun logItem_success() {
        val log = McpLogItem(
            serverId = "srv_1",
            serverName = "Weather MCP",
            toolName = "get_weather",
            requestPayload = """{"name":"get_weather","arguments":{"city":"Tokyo"}}""",
            responsePayload = """{"temp":22,"condition":"sunny"}""",
            isSuccess = true,
            latencyMs = 150,
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                LogItemCard(log)
            }
        }
        capture("log_item_success")
    }

    @Test
    fun logItem_error() {
        val log = McpLogItem(
            serverId = "srv_2",
            serverName = "Broken MCP",
            toolName = "fetch_data",
            requestPayload = """{"name":"fetch_data","arguments":{"query":"SELECT*"}}""",
            responsePayload = """{"error":"Connection refused"}""",
            isSuccess = false,
            latencyMs = 5000,
            errorMessage = "Connection refused",
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                LogItemCard(log)
            }
        }
        capture("log_item_error")
    }
}
