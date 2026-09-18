package com.aistudio.mcpchat

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.data.local.ThemePreferences
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.ui.screens.ChatScreen
import com.aistudio.mcpchat.ui.screens.EmptyChatView
import com.aistudio.mcpchat.ui.screens.ExecutionResultCard
import com.aistudio.mcpchat.ui.screens.InspectorScreen
import com.aistudio.mcpchat.ui.screens.LogItemCard
import com.aistudio.mcpchat.ui.screens.ServerCardItem
import com.aistudio.mcpchat.ui.screens.ServerStatusBadge
import com.aistudio.mcpchat.ui.screens.ServersScreen
import com.aistudio.mcpchat.ui.screens.SettingsScreenContent
import com.aistudio.mcpchat.ui.screens.ToolCallCard
import com.aistudio.mcpchat.ui.screens.TypingIndicator
import com.aistudio.mcpchat.ui.theme.MyApplicationTheme
import com.aistudio.mcpchat.ui.viewmodel.AiProviderSettingsViewModel
import com.aistudio.mcpchat.ui.viewmodel.AiProviderUiState
import com.aistudio.mcpchat.ui.viewmodel.ChatViewModel
import com.aistudio.mcpchat.ui.viewmodel.McpInspectorViewModel
import com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
        options = RoborazziRule.Options(),
    )

    private fun capture(name: String) {
        composeTestRule.onRoot().captureRoboImage("src/test/snapshots/$name.png")
    }

    companion object {
        // LogItemCard renders the timestamp, so snapshots need a fixed clock.
        private const val FIXED_LOG_TIMESTAMP = 1_700_000_000_000L
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
            timestamp = FIXED_LOG_TIMESTAMP,
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
            timestamp = FIXED_LOG_TIMESTAMP,
            errorMessage = "Connection refused",
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                LogItemCard(log)
            }
        }
        capture("log_item_error")
    }

    @Test
    fun servers_populated() {
        val server = McpServer(
            id = "srv_1",
            name = "Weather MCP",
            description = "Provides weather data",
            endpointUrl = "https://weather.example.com/mcp",
            isEnabled = true,
            status = ServerStatus.CONNECTED,
            latencyMs = 85,
        )
        val tool = McpTool(
            id = "srv_1_get_weather",
            serverId = "srv_1",
            serverName = "Weather MCP",
            name = "get_weather",
            description = "Current weather for a city",
            inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"}}}""",
        )
        val viewModel = mockk<McpServersViewModel>(relaxed = true)
        every { viewModel.servers } returns MutableStateFlow(listOf(server))
        every { viewModel.tools } returns MutableStateFlow(listOf(tool))
        every { viewModel.isTesting } returns MutableStateFlow(null)
        every { viewModel.feedbackMessage } returns MutableStateFlow(null)
        composeTestRule.setContent {
            MyApplicationTheme {
                ServersScreen(viewModel = viewModel, onAddServer = {}, onEditServer = {})
            }
        }
        capture("servers_populated")
    }

    @Test
    fun servers_empty() {
        val viewModel = mockk<McpServersViewModel>(relaxed = true)
        every { viewModel.servers } returns MutableStateFlow(emptyList())
        every { viewModel.tools } returns MutableStateFlow(emptyList())
        every { viewModel.isTesting } returns MutableStateFlow(null)
        every { viewModel.feedbackMessage } returns MutableStateFlow(null)
        composeTestRule.setContent {
            MyApplicationTheme {
                ServersScreen(viewModel = viewModel, onAddServer = {}, onEditServer = {})
            }
        }
        capture("servers_empty")
    }

    @Test
    fun inspector_with_result() {
        val server = McpServer(
            id = "srv_1",
            name = "Weather MCP",
            description = "Provides weather data",
            endpointUrl = "https://weather.example.com/mcp",
            isEnabled = true,
            status = ServerStatus.CONNECTED,
            latencyMs = 85,
        )
        val tool = McpTool(
            id = "srv_1_get_weather",
            serverId = "srv_1",
            serverName = "Weather MCP",
            name = "get_weather",
            description = "Current weather for a city",
            inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"}}}""",
        )
        val log = McpLogItem(
            serverId = "srv_1",
            serverName = "Weather MCP",
            toolName = "get_weather",
            requestPayload = """{"name":"get_weather","arguments":{"city":"Tokyo"}}""",
            responsePayload = """{"temp":22,"condition":"sunny"}""",
            isSuccess = true,
            latencyMs = 150,
            timestamp = FIXED_LOG_TIMESTAMP,
        )
        val viewModel = mockk<McpInspectorViewModel>(relaxed = true)
        every { viewModel.servers } returns MutableStateFlow(listOf(server))
        every { viewModel.tools } returns MutableStateFlow(listOf(tool))
        every { viewModel.logs } returns MutableStateFlow(listOf(log))
        every { viewModel.selectedToolId } returns MutableStateFlow("srv_1_get_weather")
        every { viewModel.parameterInputs } returns MutableStateFlow(mapOf("city" to "Tokyo"))
        every { viewModel.isInvoking } returns MutableStateFlow(false)
        every { viewModel.latestResult } returns MutableStateFlow("""{"temp":22,"condition":"sunny"}""")
        every { viewModel.latestLatencyMs } returns MutableStateFlow(150L)
        every { viewModel.latestIsSuccess } returns MutableStateFlow(true)
        every { viewModel.resourceSummaries } returns MutableStateFlow(emptyList())
        every { viewModel.selectedResourceServerId } returns MutableStateFlow(null)
        every { viewModel.viewedResource } returns MutableStateFlow(null)
        every { viewModel.viewedResourceContent } returns MutableStateFlow(null)
        every { viewModel.isLoadingResources } returns MutableStateFlow(false)
        every { viewModel.subscribedResources } returns MutableStateFlow(emptyMap())
        composeTestRule.setContent {
            MyApplicationTheme {
                InspectorScreen(viewModel = viewModel)
            }
        }
        capture("inspector_with_result")
    }

    @Test
    fun chat_with_history() {
        val session = ChatSessionEntity(
            id = "s1",
            title = "Tokyo weather",
            createdAt = FIXED_LOG_TIMESTAMP,
            updatedAt = FIXED_LOG_TIMESTAMP,
        )
        val messages = listOf(
            ChatMessageEntity(
                id = "m1",
                sessionId = "s1",
                role = "user",
                content = "What is the weather in Tokyo?",
                timestamp = FIXED_LOG_TIMESTAMP,
            ),
            ChatMessageEntity(
                id = "m2",
                sessionId = "s1",
                role = "assistant",
                content = "It is 22°C and sunny in Tokyo.",
                latencyMs = 150,
                timestamp = FIXED_LOG_TIMESTAMP,
            ),
        )
        val server = McpServer(
            id = "srv_1",
            name = "Weather MCP",
            description = "Provides weather data",
            endpointUrl = "https://weather.example.com/mcp",
            isEnabled = true,
            status = ServerStatus.CONNECTED,
            latencyMs = 85,
        )
        val viewModel = mockk<ChatViewModel>(relaxed = true)
        every { viewModel.sessions } returns MutableStateFlow(listOf(session))
        every { viewModel.currentSessionId } returns MutableStateFlow("s1")
        every { viewModel.messages } returns MutableStateFlow(messages)
        every { viewModel.servers } returns MutableStateFlow(listOf(server))
        every { viewModel.isGenerating } returns MutableStateFlow(false)
        every { viewModel.inProgressToolCalls } returns MutableStateFlow(emptyList())
        every { viewModel.streamingText } returns MutableStateFlow(null)
        every { viewModel.streamProgress } returns MutableStateFlow(null)
        every { viewModel.pendingAttachments } returns MutableStateFlow(emptyList())
        every { viewModel.messageSearchResults } returns MutableStateFlow(emptyList())
        every { viewModel.errorMessage } returns MutableStateFlow(null)
        every { viewModel.hasPendingUserTurn } returns MutableStateFlow(false)
        val providerViewModel = mockk<AiProviderSettingsViewModel>(relaxed = true)
        every { providerViewModel.uiState } returns MutableStateFlow(
            AiProviderUiState(
                configs = listOf(AiProviderConfig.google()),
                activeId = "preset_google",
            ),
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ChatScreen(
                    viewModel = viewModel,
                    providerViewModel = providerViewModel,
                    onNavigateToServers = {},
                    onNavigateToProviders = {},
                )
            }
        }
        capture("chat_with_history")
    }
}
