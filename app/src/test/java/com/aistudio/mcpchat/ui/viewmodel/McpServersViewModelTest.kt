package com.aistudio.mcpchat.ui.viewmodel

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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class McpServersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: McpRepository
    private lateinit var viewModel: McpServersViewModel

    private val serversFlow = MutableStateFlow<List<McpServer>>(emptyList())
    private val toolsFlow = MutableStateFlow<List<McpTool>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.serversFlow } returns serversFlow
        every { repository.allToolsFlow } returns toolsFlow
    }

    private fun createViewModel() {
        viewModel = McpServersViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState has empty values`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.servers.isEmpty())
        assertTrue(state.tools.isEmpty())
        assertNull(state.isTestingServerId)
        assertNull(state.feedback)
    }

    @Test
    fun `toggleServer calls repository`() = runTest {
        createViewModel()
        viewModel.toggleServer("server_1", true)
        advanceUntilIdle()

        coVerify { repository.toggleServer("server_1", true) }
    }

    @Test
    fun `toggleTool calls repository`() = runTest {
        createViewModel()
        viewModel.toggleTool("tool_1", false)
        advanceUntilIdle()

        coVerify { repository.toggleTool("tool_1", false) }
    }

    @Test
    fun `pingServer sets isTesting and shows success feedback`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Test Server",
            description = "desc",
            endpointUrl = "http://test.com",
            status = ServerStatus.CONNECTED,
            latencyMs = 42,
        )
        coEvery { repository.pingServer(any()) } returns server

        createViewModel()
        viewModel.pingServer(server)
        advanceUntilIdle()

        assertNull(viewModel.isTesting.value)
        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("Test Server"))
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("CONNECTED"))
    }

    @Test
    fun `pingServer shows error feedback on exception`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Test Server",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        coEvery { repository.pingServer(any()) } throws RuntimeException("Connection refused")

        createViewModel()
        viewModel.pingServer(server)
        advanceUntilIdle()

        assertNull(viewModel.isTesting.value)
        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.isError)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("Connection refused"))
    }

    @Test
    fun `pingServer reports handshake failure reason and error state`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Test Server",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        coEvery { repository.pingServer(any()) } returns server.copy(
            status = ServerStatus.ERROR,
            latencyMs = 99,
        )
        every { repository.lastConnectionError("s1") } returns "HTTP 401"

        createViewModel()
        viewModel.pingServer(server)
        advanceUntilIdle()

        assertNull(viewModel.isTesting.value)
        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.isError)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("HTTP 401"))
    }

    @Test
    fun `syncTools sets isTesting and shows success feedback`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Test Server",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        val tools = listOf(
            McpTool(
                id = "t1",
                serverId = "s1",
                serverName = "Test Server",
                name = "tool1",
                description = "A tool",
                isEnabled = true,
            ),
        )
        coEvery { repository.syncServerTools(any()) } returns tools

        createViewModel()
        viewModel.syncTools(server)
        advanceUntilIdle()

        assertNull(viewModel.isTesting.value)
        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("1 tools"))
    }

    @Test
    fun `syncTools shows error feedback on exception`() = runTest {
        val server = McpServer(
            id = "s1",
            name = "Test Server",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        coEvery { repository.syncServerTools(any()) } throws RuntimeException("Timeout")

        createViewModel()
        viewModel.syncTools(server)
        advanceUntilIdle()

        assertNull(viewModel.isTesting.value)
        assertTrue(viewModel.feedbackMessage.value!!.isError)
    }

    @Test
    fun `addCustomServer calls repository and shows feedback`() = runTest {
        val server = McpServer(
            id = "custom_1",
            name = "My Server",
            description = "Custom",
            endpointUrl = "http://custom.com/mcp",
            isPreset = false,
        )
        coEvery { repository.addCustomServer(any(), any(), any(), any(), any()) } returns server

        createViewModel()
        viewModel.addCustomServer("My Server", "Custom", "http://custom.com/mcp", "")
        advanceUntilIdle()

        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("My Server"))
        assertFalse(viewModel.feedbackMessage.value!!.isError)
    }

    @Test
    fun `addCustomServer shows error for invalid input`() = runTest {
        coEvery { repository.addCustomServer(any(), any(), any(), any(), any()) } throws
            IllegalArgumentException("Server name cannot be empty")

        createViewModel()
        viewModel.addCustomServer("", "", "http://test.com", "")
        advanceUntilIdle()

        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.isError)
    }

    @Test
    fun `updateServer calls repository with trimmed values`() = runTest {
        val server = McpServer(id = "srv1", name = "Old", description = "", endpointUrl = "https://x.com")
        coEvery { repository.updateCustomServer(any(), any(), any(), any(), any(), any()) } returns server

        createViewModel()
        viewModel.updateServer(server, "  New  ", " desc ", " https://x.com ", " Bearer tok ")
        advanceUntilIdle()

        coVerify {
            repository.updateCustomServer("srv1", "New", "desc", "https://x.com", "Bearer tok", false)
        }
        assertNotNull(viewModel.feedbackMessage.value)
        assertFalse(viewModel.feedbackMessage.value!!.isError)
    }

    @Test
    fun `updateServer shows error feedback for invalid input`() = runTest {
        val server = McpServer(id = "srv1", name = "Old", description = "", endpointUrl = "https://x.com")
        coEvery { repository.updateCustomServer(any(), any(), any(), any(), any(), any()) } throws
            IllegalArgumentException("URL must use http or https scheme")

        createViewModel()
        viewModel.updateServer(server, "Name", "d", "ftp://bad", "")
        advanceUntilIdle()

        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.isError)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("http or https"))
    }

    @Test
    fun `deleteServer calls repository and shows feedback`() = runTest {
        createViewModel()
        viewModel.deleteServer("server_1")
        advanceUntilIdle()

        coVerify { repository.deleteServer("server_1") }
        assertNotNull(viewModel.feedbackMessage.value)
        assertTrue(viewModel.feedbackMessage.value!!.text.contains("removed"))
    }

    @Test
    fun `clearFeedback sets feedback to null`() = runTest {
        createViewModel()
        viewModel.clearFeedback()
        advanceUntilIdle()

        assertNull(viewModel.feedbackMessage.value)
    }

    @Test
    fun `uiState reflects servers from repository`() = runTest {
        val servers = listOf(
            McpServer(id = "s1", name = "Server 1", description = "desc", endpointUrl = "http://s1.com"),
            McpServer(id = "s2", name = "Server 2", description = "desc", endpointUrl = "http://s2.com"),
        )
        serversFlow.value = servers

        createViewModel()
        // stateIn(WhileSubscribed) only produces values once collected.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.servers.size)
    }
}
