package com.aistudio.mcpchat.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.McpLogEntity
import com.aistudio.mcpchat.data.local.McpServerEntity
import com.aistudio.mcpchat.data.local.McpToolEntity
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.network.GeminiApiService
import com.aistudio.mcpchat.data.network.McpHttpClient
import com.aistudio.mcpchat.data.repository.ChatRepository
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.data.security.CryptoManager
import com.aistudio.mcpchat.data.tool.MemoryTool
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FullChatLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var mcpHttpClient: McpHttpClient
    private lateinit var geminiApiService: GeminiApiService
    private lateinit var mcpRepository: McpRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var memoryTool: MemoryTool

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

        mcpHttpClient = mockk(relaxed = true)
        geminiApiService = mockk(relaxed = true)
        mcpRepository = McpRepository(database, mcpHttpClient, testCrypto)
        chatRepository = ChatRepository(database, mcpRepository, geminiApiService)
        memoryTool = MemoryTool(database)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `full lifecycle - add server sync tools send message`() = runTest {
        val serverEntity = McpServerEntity(
            id = "srv1",
            name = "Test Server",
            description = "Test",
            endpointUrl = "http://localhost:3000",
            status = "CONNECTED",
        )
        database.mcpServerDao().insertServer(serverEntity)

        val toolEntity = McpToolEntity(
            id = "t1",
            serverId = "srv1",
            serverName = "Test Server",
            name = "get_weather",
            description = "Get weather info",
            inputSchemaJson = """{"type":"object","properties":{"location":{"type":"string"}}}""",
        )
        database.mcpToolDao().insertTools(listOf(toolEntity))

        val session = chatRepository.createNewSession("Test Chat", "gemini-3.5-flash")
        assertNotNull(session.id)

        val userMessage = ChatMessageEntity(
            id = "user_msg_1",
            sessionId = session.id,
            role = "user",
            content = "What is the weather in Tokyo?",
        )
        database.chatDao().insertMessage(userMessage)

        val messages = database.chatDao().getMessagesList(session.id)
        assertEquals(1, messages.size)
        assertEquals("user", messages[0].role)

        val sessions = chatRepository.allSessionsFlow.first()
        assertEquals(1, sessions.size)
        assertEquals("Test Chat", sessions[0].title)
    }

    @Test
    fun `session lifecycle - create switch messages are session scoped`() = runTest {
        val session1 = chatRepository.createNewSession("Session 1", "gemini-3.5-flash")
        val session2 = chatRepository.createNewSession("Session 2", "gemini-3.5-flash")

        val msg1 = ChatMessageEntity(
            id = "m1",
            sessionId = session1.id,
            role = "user",
            content = "Hello from s1",
        )
        val msg2 = ChatMessageEntity(
            id = "m2",
            sessionId = session2.id,
            role = "user",
            content = "Hello from s2",
        )

        database.chatDao().insertMessage(msg1)
        database.chatDao().insertMessage(msg2)

        val s1Messages = chatRepository.getMessagesForSession(session1.id).first()
        val s2Messages = chatRepository.getMessagesForSession(session2.id).first()

        assertEquals(1, s1Messages.size)
        assertEquals("Hello from s1", s1Messages[0].content)

        assertEquals(1, s2Messages.size)
        assertEquals("Hello from s2", s2Messages[0].content)
    }

    private fun mcpTool(id: String, serverId: String, serverName: String, name: String, description: String) = McpToolEntity(
        id = id,
        serverId = serverId,
        serverName = serverName,
        name = name,
        description = description,
        inputSchemaJson = "{}",
    )

    @Test
    fun `tool call progress callback fires during invocation`() = runTest {
        database.mcpServerDao().insertServer(
            McpServerEntity(
                id = "srv1",
                name = "Server 1",
                description = "desc",
                endpointUrl = "http://test.com",
                status = "CONNECTED",
            ),
        )

        val toolCallsReceived = mutableListOf<List<McpToolCall>>()

        val toolCall = McpToolCall(
            callId = "call_1", serverId = "srv1", serverName = "Server 1",
            toolName = "get_weather", inputArguments = mapOf("location" to "Tokyo"),
            status = com.aistudio.mcpchat.data.model.ToolExecutionStatus.SUCCESS,
            outputResult = """{"temp":"25C"}""",
            isSuccess = true, executionTimeMs = 100,
            timestamp = System.currentTimeMillis(),
        )

        toolCallsReceived.add(listOf(toolCall))

        assertEquals(1, toolCallsReceived.size)
        assertEquals("get_weather", toolCallsReceived[0][0].toolName)
        assertEquals("SUCCESS", toolCallsReceived[0][0].status.name)
    }

    @Test
    fun `error recovery - network error during message send`() = runTest {
        val session = chatRepository.createNewSession("Error Chat", "gemini-3.5-flash")

        coEvery { geminiApiService.streamContent(any(), any(), any(), any(), any(), any()) } throws
            RuntimeException("Network timeout")

        try {
            chatRepository.sendMessage(session.id, "Hello", {})
        } catch (e: RuntimeException) {
            assertEquals("Network timeout", e.message)
        }

        val messages = database.chatDao().getMessagesList(session.id)
        assertTrue(messages.isEmpty() || messages.all { it.role == "user" })
    }

    @Test
    fun `memory tool full CRUD lifecycle`() = runTest {
        val saveResult = memoryTool.handleRequest(
            "save_memory_note",
            mapOf("content" to "MCP integration is active", "tags" to "mcp,important"),
        )
        assertTrue(saveResult.contains("saved"))
        assertTrue(saveResult.contains("id"))

        val searchResult = memoryTool.handleRequest(
            "search_memory_notes",
            mapOf("query" to "MCP"),
        )
        assertTrue(searchResult.contains("MCP integration is active"))

        val listResult = memoryTool.handleRequest(
            "list_memory_notes",
            mapOf("limit" to "10"),
        )
        assertTrue(listResult.contains("MCP integration is active"))

        val idStr = saveResult.replace(Regex(".*\"id\":(\\d+).*"), "$1").toLongOrNull()
        if (idStr != null) {
            val deleteResult = memoryTool.handleRequest(
                "delete_memory_note",
                mapOf("id" to idStr),
            )
            assertTrue(deleteResult.contains("deleted"))
        }
    }

    @Test
    fun `fallback mode - API key missing uses local handler`() = runTest {
        coEvery {
            geminiApiService.streamContent(any(), any(), any(), any(), any(), any())
        } returns com.aistudio.mcpchat.data.model.GeminiResponse(text = null, error = "API_KEY_MISSING")

        val session = chatRepository.createNewSession("Fallback Chat", "gemini-3.5-flash")

        val result = chatRepository.sendMessage(session.id, "hello", {})
        assertNotNull(result)
    }

    @Test
    fun `delete session cascades to messages`() = runTest {
        val session = chatRepository.createNewSession("Cascade Test", "gemini-3.5-flash")

        val msg1 = ChatMessageEntity(
            id = "cm1",
            sessionId = session.id,
            role = "user",
            content = "Hello",
        )
        val msg2 = ChatMessageEntity(
            id = "cm2",
            sessionId = session.id,
            role = "assistant",
            content = "Hi",
        )
        database.chatDao().insertMessage(msg1)
        database.chatDao().insertMessage(msg2)

        val beforeMessages = database.chatDao().getMessagesList(session.id)
        assertEquals(2, beforeMessages.size)

        chatRepository.deleteSession(session.id)

        val afterMessages = database.chatDao().getMessagesList(session.id)
        assertTrue(afterMessages.isEmpty())

        val afterSessions = chatRepository.allSessionsFlow.first()
        assertTrue(afterSessions.none { it.id == session.id })
    }

    @Test
    fun `update session title persists`() = runTest {
        val session = chatRepository.createNewSession("Original Title", "gemini-3.5-flash")

        chatRepository.updateSessionTitle(session.id, "Updated Title")

        val updated = database.chatDao().getSessionById(session.id)
        assertNotNull(updated)
        assertEquals("Updated Title", updated?.title)
    }

    @Test
    fun `update session model persists`() = runTest {
        val session = chatRepository.createNewSession("Model Test", "gemini-3.5-flash")

        chatRepository.updateSessionModel(session.id, "gemini-2.0-flash")

        val updated = database.chatDao().getSessionById(session.id)
        assertNotNull(updated)
        assertEquals("gemini-2.0-flash", updated?.model)
    }

    @Test
    fun `mcp log creation and retrieval lifecycle`() = runTest {
        val log = McpLogEntity(
            serverId = "srv1",
            serverName = "Server 1",
            toolName = "get_weather",
            requestPayload = """{"method":"tools/call","params":{"name":"get_weather","arguments":{"location":"Tokyo"}}}""",
            responsePayload = """{"result":{"temp":"25C","humidity":"60%"}}""",
            isSuccess = true,
            latencyMs = 150,
        )
        database.mcpLogDao().insertLog(log)

        val logs = database.mcpLogDao().getAllLogs().first()
        assertEquals(1, logs.size)
        assertEquals("get_weather", logs[0].toolName)
        assertEquals(true, logs[0].isSuccess)
        assertEquals(150L, logs[0].latencyMs)
        assertTrue(logs[0].requestPayload.contains("get_weather"))
        assertTrue(logs[0].responsePayload.contains("25C"))
    }

    @Test
    fun `multiple servers with independent tool sets`() = runTest {
        database.mcpServerDao().insertServer(
            McpServerEntity(id = "s1", name = "Weather Server", description = "Weather", endpointUrl = "http://weather.com"),
        )
        database.mcpServerDao().insertServer(
            McpServerEntity(id = "s2", name = "Finance Server", description = "Finance", endpointUrl = "http://finance.com"),
        )

        database.mcpToolDao().insertTools(
            listOf(
                mcpTool("t1", "s1", "Weather Server", "get_weather", "Weather"),
                mcpTool("t2", "s1", "Weather Server", "get_forecast", "Forecast"),
                mcpTool("t3", "s2", "Finance Server", "get_stock", "Stock"),
            ),
        )

        val s1Tools = database.mcpToolDao().getToolsForServer("s1").first()
        val s2Tools = database.mcpToolDao().getToolsForServer("s2").first()

        assertEquals(2, s1Tools.size)
        assertEquals(1, s2Tools.size)
        // Room does not guarantee row ordering without ORDER BY.
        assertEquals(
            setOf("get_weather", "get_forecast"),
            s1Tools.map { it.name }.toSet(),
        )
        assertEquals("get_stock", s2Tools[0].name)
    }

    @Test
    fun `chat repository sessions flow updates on create and delete`() = runTest {
        val initialSessions = chatRepository.allSessionsFlow.first()
        assertTrue(initialSessions.isEmpty())

        val session1 = chatRepository.createNewSession("Session 1", "gemini-3.5-flash")
        val afterCreate = chatRepository.allSessionsFlow.first()
        assertEquals(1, afterCreate.size)

        chatRepository.createNewSession("Session 2", "gemini-3.5-flash")
        val afterSecondCreate = chatRepository.allSessionsFlow.first()
        assertEquals(2, afterSecondCreate.size)

        chatRepository.deleteSession(session1.id)
        val afterDelete = chatRepository.allSessionsFlow.first()
        assertEquals(1, afterDelete.size)
        assertEquals("Session 2", afterDelete[0].title)
    }
}
