package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.repository.McpRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpInspectorViewModelLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: McpRepository

    private val serversFlow = MutableStateFlow<List<McpServer>>(emptyList())
    private val toolsFlow = MutableStateFlow<List<McpTool>>(emptyList())
    private val logsFlow = MutableStateFlow<List<McpLogItem>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        every { repository.serversFlow } returns serversFlow
        every { repository.allToolsFlow } returns toolsFlow
        every { repository.logsFlow } returns logsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savedStateHandle persists selectedToolId across ViewModel recreation`() = runTest {
        val tool = McpTool(
            id = "tool_1", serverId = "srv1", serverName = "Server 1",
            name = "get_weather", description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string"}}}"""
        )
        toolsFlow.value = listOf(tool)

        val savedStateHandle = SavedStateHandle(mapOf("selected_tool_id" to "tool_1"))
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        assertEquals("tool_1", viewModel.selectedToolId.value)
    }

    @Test
    fun `savedStateHandle restores tool selection from prepopulated state`() = runTest {
        val tool = McpTool(
            id = "tool_1", serverId = "srv1", serverName = "Server 1",
            name = "get_weather", description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string","default":"Tokyo"}}}"""
        )
        toolsFlow.value = listOf(tool)

        val savedStateHandle = SavedStateHandle(mapOf("selected_tool_id" to "tool_1"))
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        assertEquals("tool_1", viewModel.selectedToolId.value)
        assertEquals("Tokyo", viewModel.parameterInputs.value["location"])
    }

    @Test
    fun `parameter inputs reset when switching tools`() = runTest {
        val tool1 = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "tool1", description = "Tool 1",
            inputSchemaJson = """{"type":"object","properties":{"query":{"type":"string"}}}"""
        )
        val tool2 = McpTool(
            id = "t2", serverId = "srv1", serverName = "Server 1",
            name = "tool2", description = "Tool 2",
            inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string"}}}"""
        )
        toolsFlow.value = listOf(tool1, tool2)

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)

        viewModel.selectTool(tool1)
        advanceUntilIdle()
        assertTrue(viewModel.parameterInputs.value.containsKey("query"))

        viewModel.selectTool(tool2)
        advanceUntilIdle()
        assertTrue(viewModel.parameterInputs.value.containsKey("location"))
        assertFalse(viewModel.parameterInputs.value.containsKey("query"))
    }

    @Test
    fun `invocation state lifecycle tracks isInvoking correctly`() = runTest {
        val server = McpServer(
            id = "srv1", name = "Server 1", description = "desc",
            endpointUrl = "http://test.com", status = ServerStatus.CONNECTED
        )
        val tool = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "get_weather", description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string"}}}"""
        )
        serversFlow.value = listOf(server)
        toolsFlow.value = listOf(tool)

        val logItem = McpLogItem(
            serverId = "srv1", serverName = "Server 1", toolName = "get_weather",
            requestPayload = "{}", responsePayload = """{"temp":"25C"}""",
            isSuccess = true, latencyMs = 150, timestamp = System.currentTimeMillis()
        )
        coEvery { repository.executeTool(any(), any(), any()) } returns Pair("""{"temp":"25C"}""", logItem)

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)
        viewModel.selectTool(tool)
        advanceUntilIdle()

        assertFalse(viewModel.isInvoking.value)

        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        assertFalse(viewModel.isInvoking.value)
        assertEquals("""{"temp":"25C"}""", viewModel.latestResult.value)
        assertEquals(150L, viewModel.latestLatencyMs.value)
        assertEquals(true, viewModel.latestIsSuccess.value)
    }

    @Test
    fun `invocation error sets error state and resets isInvoking`() = runTest {
        val server = McpServer(
            id = "srv1", name = "Server 1", description = "desc",
            endpointUrl = "http://test.com", status = ServerStatus.CONNECTED
        )
        val tool = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "failing_tool", description = "Fails",
            inputSchemaJson = """{"type":"object","properties":{}}"""
        )
        serversFlow.value = listOf(server)
        toolsFlow.value = listOf(tool)

        coEvery { repository.executeTool(any(), any(), any()) } throws RuntimeException("Connection failed")

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)
        viewModel.selectTool(tool)
        advanceUntilIdle()

        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        assertFalse(viewModel.isInvoking.value)
        assertNotNull(viewModel.latestResult.value)
        assertTrue(viewModel.latestResult.value?.contains("Error") == true)
        assertEquals(false, viewModel.latestIsSuccess.value)
    }

    @Test
    fun `selecting new tool clears previous results`() = runTest {
        val server = McpServer(
            id = "srv1", name = "Server 1", description = "desc",
            endpointUrl = "http://test.com", status = ServerStatus.CONNECTED
        )
        val tool = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "tool1", description = "Tool 1",
            inputSchemaJson = """{"type":"object","properties":{}}"""
        )
        serversFlow.value = listOf(server)
        toolsFlow.value = listOf(tool)

        val logItem = McpLogItem(
            serverId = "srv1", serverName = "Server 1", toolName = "tool1",
            requestPayload = "{}", responsePayload = """{"result":"ok"}""",
            isSuccess = true, latencyMs = 100, timestamp = System.currentTimeMillis()
        )
        coEvery { repository.executeTool(any(), any(), any()) } returns Pair("""{"result":"ok"}""", logItem)

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)

        viewModel.selectTool(tool)
        advanceUntilIdle()

        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        assertNotNull(viewModel.latestResult.value)
        assertNotNull(viewModel.latestLatencyMs.value)

        viewModel.selectTool(tool)
        advanceUntilIdle()

        assertNull(viewModel.latestResult.value)
        assertNull(viewModel.latestLatencyMs.value)
        assertNull(viewModel.latestIsSuccess.value)
    }

    @Test
    fun `updateParameter modifies specific parameter without affecting others`() = runTest {
        val tool = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "tool1", description = "Tool 1",
            inputSchemaJson = """{"type":"object","properties":{"a":{"type":"string"},"b":{"type":"string"}}}"""
        )
        toolsFlow.value = listOf(tool)

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)

        viewModel.selectTool(tool)
        advanceUntilIdle()

        viewModel.updateParameter("a", "value_a")
        viewModel.updateParameter("b", "value_b")
        advanceUntilIdle()

        assertEquals("value_a", viewModel.parameterInputs.value["a"])
        assertEquals("value_b", viewModel.parameterInputs.value["b"])

        viewModel.updateParameter("a", "updated_a")
        advanceUntilIdle()

        assertEquals("updated_a", viewModel.parameterInputs.value["a"])
        assertEquals("value_b", viewModel.parameterInputs.value["b"])
    }

    @Test
    fun `clearLogs delegates to repository`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)

        viewModel.clearLogs()
        advanceUntilIdle()

        coVerify { repository.clearAllLogs() }
    }

    @Test
    fun `reExecuteTool sets tool and parameters then invokes`() = runTest {
        val server = McpServer(
            id = "srv1", name = "Server 1", description = "desc",
            endpointUrl = "http://test.com", status = ServerStatus.CONNECTED
        )
        val tool = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "get_weather", description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string"}}}"""
        )
        serversFlow.value = listOf(server)
        toolsFlow.value = listOf(tool)

        val logItem = McpLogItem(
            serverId = "srv1", serverName = "Server 1", toolName = "get_weather",
            requestPayload = "{}", responsePayload = """{"result":"ok"}""",
            isSuccess = true, latencyMs = 100, timestamp = System.currentTimeMillis()
        )
        coEvery { repository.executeTool(any(), any(), any()) } returns Pair("""{"result":"ok"}""", logItem)

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        viewModel.reExecuteTool("get_weather", "srv1", mapOf("location" to "London"))
        advanceUntilIdle()

        assertEquals("t1", viewModel.selectedToolId.value)
        assertEquals("London", viewModel.parameterInputs.value["location"])
        assertEquals("""{"result":"ok"}""", viewModel.latestResult.value)
    }

    @Test
    fun `numeric parameter coercion for days amount value`() = runTest {
        val tool = McpTool(
            id = "t1", serverId = "srv1", serverName = "Server 1",
            name = "tool1", description = "Tool 1",
            inputSchemaJson = """{"type":"object","properties":{"days":{"type":"integer"},"amount":{"type":"number"},"value":{"type":"integer"}}}"""
        )
        toolsFlow.value = listOf(tool)

        val server = McpServer(
            id = "srv1", name = "Server 1", description = "desc",
            endpointUrl = "http://test.com", status = ServerStatus.CONNECTED
        )
        serversFlow.value = listOf(server)

        val logItem = McpLogItem(
            serverId = "srv1", serverName = "Server 1", toolName = "tool1",
            requestPayload = "{}", responsePayload = "{}",
            isSuccess = true, latencyMs = 50, timestamp = System.currentTimeMillis()
        )
        coEvery { repository.executeTool(any(), any(), any()) } returns Pair("{}", logItem)

        val savedStateHandle = SavedStateHandle()
        val viewModel = McpInspectorViewModel(repository, savedStateHandle)
        viewModel.selectTool(tool)
        advanceUntilIdle()

        viewModel.updateParameter("days", "5")
        viewModel.updateParameter("amount", "100")
        viewModel.updateParameter("value", "42")
        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        coVerify {
            repository.executeTool(
                any(),
                any(),
                match { args ->
                    args["days"] == 5 && args["amount"] == 100.0 && args["value"] == 42
                }
            )
        }
    }
}
