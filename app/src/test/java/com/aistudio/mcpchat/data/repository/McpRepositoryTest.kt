package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.McpLogDao
import com.aistudio.mcpchat.data.local.McpServerDao
import com.aistudio.mcpchat.data.local.McpServerEntity
import com.aistudio.mcpchat.data.local.McpToolDao
import com.aistudio.mcpchat.data.local.McpToolEntity
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.network.McpHttpClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var serverDao: McpServerDao
    private lateinit var toolDao: McpToolDao
    private lateinit var logDao: McpLogDao
    private lateinit var httpClient: McpHttpClient
    private lateinit var repository: McpRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = mockk(relaxed = true)
        serverDao = mockk(relaxed = true)
        toolDao = mockk(relaxed = true)
        logDao = mockk(relaxed = true)
        httpClient = mockk(relaxed = true)
        every { database.mcpServerDao() } returns serverDao
        every { database.mcpToolDao() } returns toolDao
        every { database.mcpLogDao() } returns logDao
    }

    private fun createRepository() {
        repository = McpRepository(database, httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addCustomServer validates URL format`() = runTest {
        createRepository()
        try {
            repository.addCustomServer("Test", "desc", "not-a-url")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid URL"))
        }
    }

    @Test
    fun `addCustomServer rejects blank name`() = runTest {
        createRepository()
        try {
            repository.addCustomServer("", "desc", "https://test.com")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("empty"))
        }
    }

    @Test
    fun `addCustomServer rejects non-http scheme`() = runTest {
        createRepository()
        try {
            repository.addCustomServer("Test", "desc", "ftp://test.com")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("http or https"))
        }
    }

    @Test
    fun `addCustomServer rejects plain http for non-loopback host`() = runTest {
        createRepository()
        try {
            repository.addCustomServer("Test", "desc", "http://test.com/mcp")
            throw AssertionError("Expected IllegalArgumentException for plain http")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("https"))
        }
    }

    @Test
    fun `addCustomServer allows http for localhost`() = runTest {
        coEvery { httpClient.discoverTools(any()) } returns emptyList()
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 100)

        createRepository()
        val server = repository.addCustomServer("Local Server", "desc", "http://localhost:3000/mcp")

        assertNotNull(server)
        assertEquals("Local Server", server.name)
        coVerify { serverDao.insertServer(any()) }
    }

    @Test
    fun `addCustomServer inserts server and syncs tools`() = runTest {
        coEvery { httpClient.discoverTools(any()) } returns emptyList()
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 100)

        createRepository()
        val server = repository.addCustomServer("My Server", "desc", "https://test.com/mcp")

        assertNotNull(server)
        assertEquals("My Server", server.name)
        assertEquals(true, server.isEnabled)
        coVerify { serverDao.insertServer(any()) }
    }

    @Test
    fun `toggleServer updates server enabled state`() = runTest {
        val entity = McpServerEntity(
            id = "s1", name = "Server", description = "desc",
            endpointUrl = "http://test.com", isEnabled = true
        )
        coEvery { serverDao.getServerById("s1") } returns entity

        createRepository()
        repository.toggleServer("s1", false)
        advanceUntilIdle()

        coVerify { serverDao.updateServer(match { !it.isEnabled }) }
    }

    @Test
    fun `toggleServer does nothing for nonexistent server`() = runTest {
        coEvery { serverDao.getServerById("nonexistent") } returns null

        createRepository()
        repository.toggleServer("nonexistent", true)
        advanceUntilIdle()

        coVerify(exactly = 0) { serverDao.updateServer(any()) }
    }

    @Test
    fun `deleteServer removes server and tools`() = runTest {
        createRepository()
        repository.deleteServer("s1")
        advanceUntilIdle()

        coVerify { toolDao.deleteToolsForServer("s1") }
        coVerify { serverDao.deleteServerById("s1") }
    }

    @Test
    fun `toggleTool calls toolDao`() = runTest {
        createRepository()
        repository.toggleTool("t1", false)
        advanceUntilIdle()

        coVerify { toolDao.setToolEnabled("t1", false) }
    }

    @Test
    fun `pingServer tests connection and updates server`() = runTest {
        val server = McpServer(
            id = "s1", name = "Server", description = "desc",
            endpointUrl = "http://test.com"
        )
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42)
        every { toolDao.getToolsForServer("s1") } returns flowOf(emptyList())

        createRepository()
        val result = repository.pingServer(server)
        advanceUntilIdle()

        assertEquals(ServerStatus.CONNECTED, result.status)
        assertEquals(42L, result.latencyMs)
        coVerify { serverDao.updateServer(any()) }
    }

    @Test
    fun `pingServer with ERROR status preserves cached tools`() = runTest {
        // Transient failures must not wipe the discovered tool catalog.
        val server = McpServer(
            id = "s1", name = "Server", description = "desc",
            endpointUrl = "http://test.com"
        )
        val tools = listOf(McpToolEntity(
            id = "t1", serverId = "s1", serverName = "Server",
            name = "tool1", description = "desc", inputSchemaJson = "{}"
        ))
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.ERROR, 100)
        every { toolDao.getToolsForServer("s1") } returns flowOf(tools)

        createRepository()
        repository.pingServer(server)
        advanceUntilIdle()

        coVerify(exactly = 0) { toolDao.deleteToolsForServer("s1") }
    }

    @Test
    fun `syncServerTools discovers and stores tools`() = runTest {
        val server = McpServer(
            id = "s1", name = "Server", description = "desc",
            endpointUrl = "http://test.com"
        )
        val tools = listOf(
            McpTool(
                id = "t1", serverId = "s1", serverName = "Server",
                name = "tool1", description = "A tool", isEnabled = true
            )
        )
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 50)
        coEvery { httpClient.discoverTools(any()) } returns tools

        createRepository()
        val result = repository.syncServerTools(server)
        advanceUntilIdle()

        assertEquals(1, result.size)
        assertEquals("tool1", result[0].name)
        coVerify { toolDao.deleteToolsForServer("s1") }
        coVerify { toolDao.insertTools(any()) }
        coVerify { serverDao.updateServer(any()) }
    }

    @Test
    fun `executeTool calls httpClient and logs result`() = runTest {
        val server = McpServer(
            id = "s1", name = "Server", description = "desc",
            endpointUrl = "http://test.com"
        )
        val logItem = McpLogItem(
            id = 1, serverId = "s1", serverName = "Server", toolName = "tool1",
            requestPayload = "{}", responsePayload = "{}",
            isSuccess = true, latencyMs = 100
        )
        coEvery { httpClient.executeToolCall(any(), any(), any()) } returns Pair("""{"result":"ok}""", logItem)

        createRepository()
        val (result, item) = repository.executeTool(server, "tool1", mapOf("key" to "value"))
        advanceUntilIdle()

        assertEquals("""{"result":"ok}""", result)
        coVerify { logDao.insertLog(any()) }
    }

    @Test
    fun `clearAllLogs calls logDao`() = runTest {
        createRepository()
        repository.clearAllLogs()
        advanceUntilIdle()

        coVerify { logDao.clearLogs() }
    }

    @Test
    fun `getEnabledServers returns only enabled servers`() = runTest {
        val entities = listOf(
            McpServerEntity(id = "s1", name = "Enabled", description = "desc", endpointUrl = "http://a.com", isEnabled = true),
            McpServerEntity(id = "s2", name = "Disabled", description = "desc", endpointUrl = "http://b.com", isEnabled = false)
        )
        coEvery { serverDao.getEnabledServersList() } returns entities.filter { it.isEnabled }

        createRepository()
        val result = repository.getEnabledServers()
        advanceUntilIdle()

        assertEquals(1, result.size)
        assertEquals("Enabled", result[0].name)
    }

    @Test
    fun `getEnabledTools returns only enabled tools`() = runTest {
        val entities = listOf(
            McpToolEntity(id = "t1", serverId = "s1", serverName = "S", name = "tool1", description = "d", inputSchemaJson = "{}", isEnabled = true)
        )
        coEvery { toolDao.getAllEnabledTools() } returns entities

        createRepository()
        val result = repository.getEnabledTools()
        advanceUntilIdle()

        assertEquals(1, result.size)
    }
}
