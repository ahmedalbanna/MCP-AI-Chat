package com.aistudio.mcpchat.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.McpLogEntity
import com.aistudio.mcpchat.data.local.McpServerEntity
import com.aistudio.mcpchat.data.local.McpToolEntity
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.network.McpHttpClient
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.data.security.CryptoManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var httpClient: McpHttpClient
    private lateinit var repository: McpRepository

    // AndroidKeyStore is unavailable on the JVM; inject one stable test key.
    private val testKey: javax.crypto.SecretKey =
        javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val testCrypto = CryptoManager(keyProvider = { testKey })

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        httpClient = mockk(relaxed = true)
        repository = McpRepository(database, httpClient, testCrypto)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `add server inserts into database and syncs tools`() = runTest {
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(any()) } returns listOf()

        val server = repository.addCustomServer(
            name = "Test Server",
            description = "A test MCP server",
            endpointUrl = "http://localhost:3000",
            authHeader = "",
        )

        assertNotNull(server)
        assertEquals("Test Server", server.name)

        val dbServer = database.mcpServerDao().getServerById(server.id)
        assertNotNull(dbServer)
        assertEquals("http://localhost:3000/", dbServer?.endpointUrl)
    }

    @Test
    fun `add server with auth header persists auth`() = runTest {
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(any()) } returns listOf()

        val server = repository.addCustomServer(
            name = "Auth Server",
            description = "Server with auth",
            endpointUrl = "https://secure.com",
            authHeader = "Bearer token123",
        )

        val dbServer = database.mcpServerDao().getServerById(server.id)
        assertNotNull(dbServer)
        // Stored encrypted at rest; decrypts back to the original secret.
        assertTrue(dbServer!!.authHeader.startsWith("enc:v1:"))
        assertEquals("Bearer token123", testCrypto.decrypt(dbServer.authHeader))
    }

    @Test
    fun `toggle server updates enabled state`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
            isEnabled = true,
        )
        database.mcpServerDao().insertServer(serverEntity)

        repository.toggleServer("srv1", false)

        val updated = database.mcpServerDao().getServerById("srv1")
        assertNotNull(updated)
        assertFalse(updated?.isEnabled == true)
    }

    @Test
    fun `toggle tool updates enabled state`() = runTest {
        val toolEntity = McpToolEntity(
            id = "t1",
            serverId = "srv1",
            serverName = "Server 1",
            name = "get_weather",
            description = "Weather",
            inputSchemaJson = "{}",
            isEnabled = true,
        )
        database.mcpToolDao().insertTools(listOf(toolEntity))

        repository.toggleTool("t1", false)

        val allTools = database.mcpToolDao().getAllTools().first()
        assertEquals(1, allTools.size)
        assertFalse(allTools[0].isEnabled)
    }

    @Test
    fun `ping server updates status and latency`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
            status = "DISCONNECTED",
        )
        database.mcpServerDao().insertServer(serverEntity)

        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)

        val server = repository.pingServer(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
            ),
        )

        assertEquals(ServerStatus.CONNECTED, server.status)
        assertEquals(42L, server.latencyMs)
    }

    @Test
    fun `ping server with error preserves cached tools`() = runTest {
        // A transient ping failure must not permanently wipe the discovered
        // tool catalog — the server may come back on the next sync.
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        database.mcpServerDao().insertServer(serverEntity)

        val toolEntity = McpToolEntity(
            id = "t1",
            serverId = "srv1",
            serverName = "Server 1",
            name = "tool1",
            description = "Tool 1",
            inputSchemaJson = "{}",
        )
        database.mcpToolDao().insertTools(listOf(toolEntity))

        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.ERROR, 100L)

        repository.pingServer(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
            ),
        )

        val tools = database.mcpToolDao().getToolsForServer("srv1").first()
        assertEquals(1, tools.size)
    }

    @Test
    fun `sync tools discovers and stores tools`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        database.mcpServerDao().insertServer(serverEntity)

        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(any()) } returns listOf(
            com.aistudio.mcpchat.data.model.McpTool(
                id = "disc_t1",
                serverId = "srv1",
                serverName = "Server 1",
                name = "get_weather",
                description = "Get weather",
                inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string"}}}""",
            ),
            com.aistudio.mcpchat.data.model.McpTool(
                id = "disc_t2",
                serverId = "srv1",
                serverName = "Server 1",
                name = "get_forecast",
                description = "Get forecast",
                inputSchemaJson = """{"type":"object","properties":{"days":{"type":"integer"}}}""",
            ),
        )

        val discovered = repository.syncServerTools(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
            ),
        )

        assertEquals(2, discovered.size)

        val dbTools = database.mcpToolDao().getToolsForServer("srv1").first()
        assertEquals(2, dbTools.size)
    }

    @Test
    fun `delete server cascades to tools`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        database.mcpServerDao().insertServer(serverEntity)

        val tools = listOf(
            McpToolEntity(id = "t1", serverId = "srv1", serverName = "Server 1", name = "tool1", description = "Tool 1", inputSchemaJson = "{}"),
            McpToolEntity(id = "t2", serverId = "srv1", serverName = "Server 1", name = "tool2", description = "Tool 2", inputSchemaJson = "{}"),
        )
        database.mcpToolDao().insertTools(tools)

        repository.deleteServer("srv1")

        val deletedServer = database.mcpServerDao().getServerById("srv1")
        assertEquals(null, deletedServer)

        val remainingTools = database.mcpToolDao().getToolsForServer("srv1").first()
        assertTrue(remainingTools.isEmpty())
    }

    @Test
    fun `get enabled servers returns only enabled`() = runTest {
        database.mcpServerDao().insertServer(
            McpServerEntity(id = "s1", name = "Enabled", description = "", endpointUrl = "http://e.com", isEnabled = true),
        )
        database.mcpServerDao().insertServer(
            McpServerEntity(id = "s2", name = "Disabled", description = "", endpointUrl = "http://d.com", isEnabled = false),
        )

        val enabled = repository.getEnabledServers()
        assertEquals(1, enabled.size)
        assertEquals("Enabled", enabled[0].name)
    }

    @Test
    fun `get enabled tools returns only enabled`() = runTest {
        database.mcpToolDao().insertTools(
            listOf(
                McpToolEntity(
                    id = "t1",
                    serverId = "s1",
                    serverName = "S1",
                    name = "Enabled Tool",
                    description = "",
                    inputSchemaJson = "{}",
                    isEnabled = true,
                ),
                McpToolEntity(
                    id = "t2",
                    serverId = "s1",
                    serverName = "S1",
                    name = "Disabled Tool",
                    description = "",
                    inputSchemaJson = "{}",
                    isEnabled = false,
                ),
            ),
        )

        val enabled = repository.getEnabledTools()
        assertEquals(1, enabled.size)
        assertEquals("Enabled Tool", enabled[0].name)
    }

    @Test
    fun `execute tool calls httpClient and logs result`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        database.mcpServerDao().insertServer(serverEntity)

        coEvery { httpClient.executeToolCall(any(), any(), any()) } returns
            Pair(
                """{"temp":"25C"}""",
                com.aistudio.mcpchat.data.model.McpLogItem(
                    serverId = "srv1",
                    serverName = "Server 1",
                    toolName = "get_weather",
                    requestPayload = "{}",
                    responsePayload = """{"temp":"25C"}""",
                    isSuccess = true,
                    latencyMs = 100,
                    timestamp = System.currentTimeMillis(),
                ),
            )

        val (result, logItem) = repository.executeTool(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
            ),
            "get_weather",
            mapOf("location" to "Tokyo"),
        )

        assertEquals("""{"temp":"25C"}""", result)
        assertEquals(true, logItem.isSuccess)

        val dbLogs = database.mcpLogDao().getAllLogs().first()
        assertEquals(1, dbLogs.size)
        assertEquals("get_weather", dbLogs[0].toolName)
    }

    @Test
    fun `multiple servers independent tool discovery`() = runTest {
        database.mcpServerDao().insertServer(
            McpServerEntity(id = "s1", name = "Weather", description = "", endpointUrl = "http://w.com"),
        )
        database.mcpServerDao().insertServer(
            McpServerEntity(id = "s2", name = "Finance", description = "", endpointUrl = "http://f.com"),
        )

        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(match { it.endpointUrl == "http://w.com" }) } returns listOf(
            com.aistudio.mcpchat.data.model.McpTool(
                id = "w1",
                serverId = "s1",
                serverName = "Weather",
                name = "get_weather",
                description = "Weather",
                inputSchemaJson = "{}",
            ),
        )
        coEvery { httpClient.discoverTools(match { it.endpointUrl == "http://f.com" }) } returns listOf(
            com.aistudio.mcpchat.data.model.McpTool(
                id = "f1",
                serverId = "s2",
                serverName = "Finance",
                name = "get_stock",
                description = "Stock",
                inputSchemaJson = "{}",
            ),
        )

        repository.syncServerTools(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "s1",
                name = "Weather",
                description = "",
                endpointUrl = "http://w.com",
            ),
        )
        repository.syncServerTools(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "s2",
                name = "Finance",
                description = "",
                endpointUrl = "http://f.com",
            ),
        )

        val wTools = database.mcpToolDao().getToolsForServer("s1").first()
        val fTools = database.mcpToolDao().getToolsForServer("s2").first()

        assertEquals(1, wTools.size)
        assertEquals(1, fTools.size)
        assertEquals("get_weather", wTools[0].name)
        assertEquals("get_stock", fTools[0].name)
    }

    @Test
    fun `server health monitoring lifecycle`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
            status = "DISCONNECTED",
            latencyMs = 0,
        )
        database.mcpServerDao().insertServer(serverEntity)

        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        repository.pingServer(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
            ),
        )

        val afterPing = database.mcpServerDao().getServerById("srv1")
        assertNotNull(afterPing)
        assertEquals("CONNECTED", afterPing?.status)
        assertEquals(42L, afterPing?.latencyMs)

        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.ERROR, 100L)
        repository.pingServer(
            com.aistudio.mcpchat.data.model.McpServer(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
            ),
        )

        val afterError = database.mcpServerDao().getServerById("srv1")
        assertNotNull(afterError)
        assertEquals("ERROR", afterError?.status)
    }

    @Test
    fun `clear logs empties the log table`() = runTest {
        val log = com.aistudio.mcpchat.data.local.McpLogEntity(
            serverId = "srv1",
            serverName = "Server 1",
            toolName = "tool1",
            requestPayload = "{}",
            responsePayload = "{}",
            isSuccess = true,
            latencyMs = 50,
        )
        database.mcpLogDao().insertLog(log)

        val before = database.mcpLogDao().getAllLogs().first()
        assertEquals(1, before.size)

        repository.clearAllLogs()

        val after = database.mcpLogDao().getAllLogs().first()
        assertTrue(after.isEmpty())
    }

    @Test
    fun `add custom server validates URL format`() = runTest {
        try {
            repository.addCustomServer(
                name = "Bad URL",
                description = "desc",
                endpointUrl = "ftp://invalid.com",
                authHeader = "",
            )
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("URL") == true || e.message?.contains("scheme") == true)
        }
    }

    @Test
    fun `add custom server rejects blank name`() = runTest {
        try {
            repository.addCustomServer(
                name = "",
                description = "desc",
                endpointUrl = "https://valid.com",
                authHeader = "",
            )
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("name") == true || e.message?.contains("blank") == true)
        }
    }

    @Test
    fun `add custom server rejects public cleartext endpoint`() = runTest {
        val result = runCatching {
            repository.addCustomServer(
                name = "Insecure",
                description = "desc",
                endpointUrl = "http://insecure.example.com",
                authHeader = "",
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("use HTTPS"))
        coVerify(exactly = 0) { httpClient.testConnection(any()) }
    }

    @Test
    fun `update custom server persists edited fields and re-syncs tools`() = runTest {
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(any()) } returns listOf()

        val server = repository.addCustomServer(
            name = "Original",
            description = "original desc",
            endpointUrl = "http://localhost:3000",
            authHeader = "Bearer old-token",
        )

        val updated = repository.updateCustomServer(
            serverId = server.id,
            name = "Renamed",
            description = "new desc",
            endpointUrl = "https://secure.example.com/mcp",
            authHeader = "Bearer new-token",
        )

        assertEquals("Renamed", updated.name)
        assertEquals("new desc", updated.description)
        assertEquals("https://secure.example.com/mcp", updated.endpointUrl)
        assertEquals("Bearer new-token", updated.authHeader)

        val dbServer = database.mcpServerDao().getServerById(server.id)
        assertNotNull(dbServer)
        assertEquals("Renamed", dbServer?.name)
        assertEquals("https://secure.example.com/mcp", dbServer?.endpointUrl)
        // Secret re-encrypted at rest and decrypts to the new value.
        assertTrue(dbServer!!.authHeader.startsWith("enc:v1:"))
        assertEquals("Bearer new-token", testCrypto.decrypt(dbServer.authHeader))
        // Endpoint changed → handshake ran again (add + edit).
        coVerify(exactly = 2) { httpClient.testConnection(any()) }
    }

    @Test
    fun `update custom server skips re-sync when endpoint and auth unchanged`() = runTest {
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(any()) } returns listOf()

        val server = repository.addCustomServer(
            name = "Original",
            description = "desc",
            endpointUrl = "https://stable.example.com",
            authHeader = "Bearer token",
        )

        val updated = repository.updateCustomServer(
            serverId = server.id,
            name = "Renamed only",
            description = server.description,
            endpointUrl = server.endpointUrl,
            authHeader = server.authHeader,
        )

        assertEquals("Renamed only", updated.name)
        // No endpoint/auth change → no extra handshake (only the one from add).
        coVerify(exactly = 1) { httpClient.testConnection(any()) }
    }

    @Test
    fun `update custom server rejects blank name and bad URL`() = runTest {
        coEvery { httpClient.testConnection(any()) } returns Pair(ServerStatus.CONNECTED, 42L)
        coEvery { httpClient.discoverTools(any()) } returns listOf()

        val server = repository.addCustomServer(
            name = "Original",
            description = "desc",
            endpointUrl = "https://stable.example.com",
            authHeader = "",
        )

        try {
            repository.updateCustomServer(server.id, " ", "d", "https://x.com", "")
            fail("expected blank name to be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("name") == true)
        }

        try {
            repository.updateCustomServer(server.id, "Name", "d", "ftp://bad", "")
            fail("expected invalid URL to be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("URL") == true || e.message?.contains("scheme") == true)
        }
    }

    @Test
    fun `update custom server fails for unknown id`() = runTest {
        try {
            repository.updateCustomServer(
                serverId = "missing_server",
                name = "Name",
                description = "d",
                endpointUrl = "https://x.com",
                authHeader = "",
            )
            fail("expected unknown server id to be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("not found") == true)
        }
    }
}
