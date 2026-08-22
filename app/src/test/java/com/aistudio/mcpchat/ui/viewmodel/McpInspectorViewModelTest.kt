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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpInspectorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: McpRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: McpInspectorViewModel

    private val serversFlow = MutableStateFlow<List<McpServer>>(emptyList())
    private val toolsFlow = MutableStateFlow<List<McpTool>>(emptyList())
    private val logsFlow = MutableStateFlow<List<McpLogItem>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        every { repository.serversFlow } returns serversFlow
        every { repository.allToolsFlow } returns toolsFlow
        every { repository.logsFlow } returns logsFlow
    }

    private fun TestScope.createViewModel() {
        viewModel = McpInspectorViewModel(repository, savedStateHandle)
        // stateIn(WhileSubscribed) flows (uiState and the upstream
        // servers/tools/logs flows) only produce values once collected.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState has default values`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.servers.isEmpty())
        assertTrue(state.tools.isEmpty())
        assertTrue(state.logs.isEmpty())
        assertNull(state.selectedToolId)
        assertTrue(state.parameterInputs.isEmpty())
        assertFalse(state.isInvoking)
        assertNull(state.latestResult)
        assertNull(state.latestLatencyMs)
        assertNull(state.latestIsSuccess)
        assertNull(state.error)
    }

    @Test
    fun `selectTool sets selectedToolId and parses schema defaults`() = runTest {
        val tool = McpTool(
            id = "t1",
            serverId = "s1",
            serverName = "Server",
            name = "get_weather",
            description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string","default":"Tokyo"}}}""",
        )

        createViewModel()
        viewModel.selectTool(tool)
        advanceUntilIdle()

        assertEquals("t1", viewModel.selectedToolId.value)
        assertEquals("Tokyo", viewModel.parameterInputs.value["city"])
    }

    @Test
    fun `selectTool clears previous results`() = runTest {
        val tool = McpTool(
            id = "t1",
            serverId = "s1",
            serverName = "Server",
            name = "tool",
            description = "desc",
            inputSchemaJson = "{}",
        )

        createViewModel()
        viewModel.selectTool(tool)
        advanceUntilIdle()

        assertNull(viewModel.latestResult.value)
        assertNull(viewModel.latestLatencyMs.value)
        assertNull(viewModel.latestIsSuccess.value)
    }

    @Test
    fun `updateParameter updates parameterInputs`() = runTest {
        createViewModel()
        viewModel.updateParameter("city", "London")
        advanceUntilIdle()

        assertEquals("London", viewModel.parameterInputs.value["city"])
    }

    @Test
    fun `invokeSelectedTool calls repository and updates result`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Server",
            description = "desc",
            endpointUrl = "http://test.com",
            status = ServerStatus.CONNECTED,
        )
        val tool = McpTool(
            id = "t1",
            serverId = "s1",
            serverName = "Server",
            name = "get_weather",
            description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"}}}""",
        )
        val logItem = McpLogItem(
            id = 1,
            serverId = "s1",
            serverName = "Server",
            toolName = "get_weather",
            requestPayload = "{}",
            responsePayload = """{"temp":22}""",
            isSuccess = true,
            latencyMs = 150,
        )

        serversFlow.value = listOf(server)
        toolsFlow.value = listOf(tool)
        coEvery { repository.executeTool(any(), any(), any()) } returns Pair("""{"temp":22}""", logItem)

        createViewModel()
        viewModel.selectTool(tool)
        viewModel.updateParameter("city", "Tokyo")
        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        assertFalse(viewModel.isInvoking.value)
        assertEquals("""{"temp":22}""", viewModel.latestResult.value)
        assertEquals(150L, viewModel.latestLatencyMs.value)
        assertTrue(viewModel.latestIsSuccess.value!!)
    }

    @Test
    fun `invokeSelectedTool handles error`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Server",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        val tool = McpTool(
            id = "t1",
            serverId = "s1",
            serverName = "Server",
            name = "tool",
            description = "desc",
            inputSchemaJson = "{}",
        )

        serversFlow.value = listOf(server)
        toolsFlow.value = listOf(tool)
        coEvery { repository.executeTool(any(), any(), any()) } throws RuntimeException("Connection failed")

        createViewModel()
        viewModel.selectTool(tool)
        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        assertFalse(viewModel.isInvoking.value)
        assertFalse(viewModel.latestIsSuccess.value!!)
        assertTrue(viewModel.latestResult.value!!.contains("Error"))
    }

    @Test
    fun `invokeSelectedTool does nothing when no tool selected`() = runTest {
        createViewModel()
        viewModel.invokeSelectedTool()
        advanceUntilIdle()

        assertFalse(viewModel.isInvoking.value)
        coVerify(exactly = 0) { repository.executeTool(any(), any(), any()) }
    }

    @Test
    fun `clearLogs calls repository`() = runTest {
        createViewModel()
        viewModel.clearLogs()
        advanceUntilIdle()

        coVerify { repository.clearAllLogs() }
    }

    @Test
    fun `uiState reflects logs from repository`() = runTest {
        val logs = listOf(
            McpLogItem(
                id = 1,
                serverId = "s1",
                serverName = "Server",
                toolName = "tool1",
                requestPayload = "{}",
                responsePayload = "{}",
                isSuccess = true,
                latencyMs = 100,
            ),
        )
        logsFlow.value = logs

        createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.logs.size)
    }
}
