package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatDao
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.network.GeminiApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ChatRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var geminiService: GeminiApiService
    private lateinit var repository: ChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = mockk(relaxed = true)
        chatDao = mockk(relaxed = true)
        geminiService = mockk(relaxed = true)
        every { database.chatDao() } returns chatDao
    }

    private fun createRepository() {
        repository = ChatRepository(database, mockk(relaxed = true), geminiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage persists image attachments on the user turn`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "ok")

        createRepository()
        repository.sendMessage(
            sessionId = "s1",
            userPrompt = "look",
            attachments = listOf(com.aistudio.mcpchat.data.model.ImageAttachment("content://media/9", "image/jpeg")),
        )
        advanceUntilIdle()

        coVerify {
            chatDao.insertMessage(
                match {
                    it.role == "user" &&
                        it.attachmentsJson!!.contains("content://media/9") &&
                        it.attachmentsJson.contains("image/jpeg")
                },
            )
        }
    }

    @Test
    fun `history rebuild includes inline image parts for persisted attachments`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns listOf(
            ChatMessageEntity(
                id = "m1",
                sessionId = "s1",
                role = "user",
                content = "what is this?",
                attachmentsJson = """[{"uri":"content://media/1","mimeType":"image/png"}]""",
            ),
        )
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "a cat")

        repository = ChatRepository(
            database,
            mockk(relaxed = true),
            geminiService,
            attachmentSource = AttachmentContentSource { LoadedAttachment("image/png", "aGVsbG8=") },
        )
        repository.sendMessage(sessionId = "s1", userPrompt = "describe")
        advanceUntilIdle()

        val userMsg = historySlot.captured.first { it.role == "user" && it.parts.any { it is GeminiPart.InlineData } }
        val inline = userMsg.parts.filterIsInstance<GeminiPart.InlineData>().single()
        assertEquals("image/png", inline.mimeType)
        assertEquals("aGVsbG8=", inline.base64Data)
        assertTrue(userMsg.parts.any { it is GeminiPart.Text && it.text == "what is this?" })
    }

    @Test
    fun `history rebuild drops attachments whose URI is unreadable`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns listOf(
            ChatMessageEntity(
                id = "m1",
                sessionId = "s1",
                role = "user",
                content = "what is this?",
                attachmentsJson = """[{"uri":"content://revoked","mimeType":"image/png"}]""",
            ),
        )
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "no image")

        repository = ChatRepository(
            database,
            mockk(relaxed = true),
            geminiService,
            attachmentSource = AttachmentContentSource { null },
        )
        repository.sendMessage(sessionId = "s1", userPrompt = "describe")
        advanceUntilIdle()

        val userMsg = historySlot.captured.first { it.role == "user" }
        assertTrue(userMsg.parts.none { it is GeminiPart.InlineData })
        assertTrue(userMsg.parts.any { it is GeminiPart.Text && it.text == "what is this?" })
    }

    @Test
    fun `tool calls run concurrently and responses keep request order`() = runTest {
        val server = com.aistudio.mcpchat.data.model.McpServer(
            id = "srv",
            name = "Test Server",
            description = "",
            endpointUrl = "http://mcp.test",
        )
        val toolA = com.aistudio.mcpchat.data.model.McpTool(
            id = "srv_tool_a",
            serverId = "srv",
            serverName = "Test Server",
            name = "tool_a",
            description = "",
        )
        val toolB = toolA.copy(id = "srv_tool_b", name = "tool_b")
        val mcpRepository = mockk<com.aistudio.mcpchat.data.repository.McpRepository>(relaxed = true)
        coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
        coEvery { mcpRepository.getEnabledTools() } returns listOf(toolA, toolB)
        fun log(toolName: String, latencyMs: Long) = com.aistudio.mcpchat.data.model.McpLogItem(
            serverId = "srv",
            serverName = "Test Server",
            toolName = toolName,
            requestPayload = "",
            responsePayload = "",
            isSuccess = true,
            latencyMs = latencyMs,
        )
        // B finishes first (A is slower) — completion order differs from
        // request order, exercising the order-preserving aggregation.
        coEvery { mcpRepository.executeTool(server, "tool_a", any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Pair("a done", log("tool_a", 100))
        }
        coEvery { mcpRepository.executeTool(server, "tool_b", any()) } returns Pair("b done", log("tool_b", 5))

        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returnsMany listOf(
            com.aistudio.mcpchat.data.model.GeminiResponse(
                text = null,
                functionCalls = listOf(
                    GeminiPart.FunctionCall("tool_a", emptyMap()),
                    GeminiPart.FunctionCall("tool_b", emptyMap()),
                ),
            ),
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "all done"),
        )

        repository = ChatRepository(database, mcpRepository, geminiService)
        val result = repository.sendMessage(sessionId = "s1", userPrompt = "run tools")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val toolMessage = historySlot.captured.last { it.role == "tool" }
        val responses = toolMessage.parts.filterIsInstance<GeminiPart.FunctionResponse>()
        assertEquals(listOf("tool_a", "tool_b"), responses.map { it.name })
        assertEquals(listOf("a done", "b done"), responses.map { it.response["result"] })
    }

    @Test
    fun `one tool failing does not cancel sibling tool calls`() = runTest {
        val server = com.aistudio.mcpchat.data.model.McpServer(
            id = "srv",
            name = "Test Server",
            description = "",
            endpointUrl = "http://mcp.test",
        )
        val toolA = com.aistudio.mcpchat.data.model.McpTool(
            id = "srv_tool_a",
            serverId = "srv",
            serverName = "Test Server",
            name = "tool_a",
            description = "",
        )
        val toolB = toolA.copy(id = "srv_tool_b", name = "tool_b")
        val mcpRepository = mockk<com.aistudio.mcpchat.data.repository.McpRepository>(relaxed = true)
        coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
        coEvery { mcpRepository.getEnabledTools() } returns listOf(toolA, toolB)
        coEvery { mcpRepository.executeTool(server, "tool_a", any()) } throws RuntimeException("boom")
        coEvery { mcpRepository.executeTool(server, "tool_b", any()) } returns Pair(
            "b done",
            com.aistudio.mcpchat.data.model.McpLogItem(
                serverId = "srv",
                serverName = "Test Server",
                toolName = "tool_b",
                requestPayload = "",
                responsePayload = "",
                isSuccess = true,
                latencyMs = 5,
            ),
        )

        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returnsMany listOf(
            com.aistudio.mcpchat.data.model.GeminiResponse(
                text = null,
                functionCalls = listOf(
                    GeminiPart.FunctionCall("tool_a", emptyMap()),
                    GeminiPart.FunctionCall("tool_b", emptyMap()),
                ),
            ),
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "all done"),
        )

        repository = ChatRepository(database, mcpRepository, geminiService)
        val result = repository.sendMessage(sessionId = "s1", userPrompt = "run tools")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val toolMessage = historySlot.captured.last { it.role == "tool" }
        val responses = toolMessage.parts.filterIsInstance<GeminiPart.FunctionResponse>()
        assertEquals(listOf("tool_a", "tool_b"), responses.map { it.name })
        assertEquals("Tool execution failed: boom", responses[0].response["error"])
        assertEquals("b done", responses[1].response["result"])
    }

    @Test
    fun `builtin resource tools are advertised when servers are enabled`() = runTest {
        val server = com.aistudio.mcpchat.data.model.McpServer(
            id = "srv",
            name = "Test Server",
            description = "",
            endpointUrl = "http://mcp.test",
        )
        val mcpRepository = mockk<com.aistudio.mcpchat.data.repository.McpRepository>(relaxed = true)
        coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
        coEvery { mcpRepository.getEnabledTools() } returns emptyList()
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns emptyList()
        val toolsSlot = slot<List<com.aistudio.mcpchat.data.model.McpTool>>()
        coEvery {
            geminiService.streamContent(any(), any(), any(), capture(toolsSlot), any(), any())
        } returns com.aistudio.mcpchat.data.model.GeminiResponse(text = "ok")

        repository = ChatRepository(database, mcpRepository, geminiService)
        repository.sendMessage(sessionId = "s1", userPrompt = "hi")
        advanceUntilIdle()

        val names = toolsSlot.captured.map { it.name }
        assertTrue(names.contains("list_mcp_resources"))
        assertTrue(names.contains("read_mcp_resource"))
    }

    @Test
    fun `builtin resource tools are omitted without enabled servers`() = runTest {
        val mcpRepository = mockk<com.aistudio.mcpchat.data.repository.McpRepository>(relaxed = true)
        coEvery { mcpRepository.getEnabledServers() } returns emptyList()
        coEvery { mcpRepository.getEnabledTools() } returns emptyList()
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns emptyList()
        val toolsSlot = slot<List<com.aistudio.mcpchat.data.model.McpTool>>()
        coEvery {
            geminiService.streamContent(any(), any(), any(), capture(toolsSlot), any(), any())
        } returns com.aistudio.mcpchat.data.model.GeminiResponse(text = "ok")

        repository = ChatRepository(database, mcpRepository, geminiService)
        repository.sendMessage(sessionId = "s1", userPrompt = "hi")
        advanceUntilIdle()

        assertTrue(toolsSlot.captured.none { it.serverId == com.aistudio.mcpchat.data.tool.BuiltinResourceTools.SERVER_ID })
    }

    @Test
    fun `read_mcp_resource executes locally and returns content as the tool result`() = runTest {
        val server = com.aistudio.mcpchat.data.model.McpServer(
            id = "srv",
            name = "Test Server",
            description = "",
            endpointUrl = "http://mcp.test",
        )
        val mcpRepository = mockk<com.aistudio.mcpchat.data.repository.McpRepository>(relaxed = true)
        coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
        coEvery { mcpRepository.getEnabledTools() } returns emptyList()
        coEvery { mcpRepository.readResourceForChat(null, "file://notes.txt") } returns
            com.aistudio.mcpchat.data.model.McpResourceContent("file://notes.txt", "text/plain", "the file body")
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns emptyList()
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returnsMany listOf(
            com.aistudio.mcpchat.data.model.GeminiResponse(
                text = null,
                functionCalls = listOf(
                    GeminiPart.FunctionCall("read_mcp_resource", mapOf("uri" to "file://notes.txt")),
                ),
            ),
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "summarized the file"),
        )

        repository = ChatRepository(database, mcpRepository, geminiService)
        val result = repository.sendMessage(sessionId = "s1", userPrompt = "read the notes file")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val toolMessage = historySlot.captured.last { it.role == "tool" }
        val response = toolMessage.parts.filterIsInstance<GeminiPart.FunctionResponse>().single()
        assertEquals("read_mcp_resource", response.name)
        assertEquals("the file body", response.response["content"])
        // The builtin ran locally — no MCP server round trip for execution.
        coVerify(exactly = 0) { mcpRepository.executeTool(any(), any(), any()) }
    }

    @Test
    fun `list_mcp_resources returns the catalog across servers`() = runTest {
        val server = com.aistudio.mcpchat.data.model.McpServer(
            id = "srv",
            name = "Test Server",
            description = "",
            endpointUrl = "http://mcp.test",
        )
        val mcpRepository = mockk<com.aistudio.mcpchat.data.repository.McpRepository>(relaxed = true)
        coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
        coEvery { mcpRepository.getEnabledTools() } returns emptyList()
        coEvery { mcpRepository.listAllResourceSummaries() } returns listOf(
            com.aistudio.mcpchat.data.repository.ResourceSummary(
                "srv",
                "Test Server",
                com.aistudio.mcpchat.data.model.McpResource("file://notes.txt", "notes", "daily notes", "text/plain"),
            ),
        )
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns emptyList()
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returnsMany listOf(
            com.aistudio.mcpchat.data.model.GeminiResponse(
                text = null,
                functionCalls = listOf(
                    GeminiPart.FunctionCall("list_mcp_resources", emptyMap()),
                ),
            ),
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "found one resource"),
        )

        repository = ChatRepository(database, mcpRepository, geminiService)
        val result = repository.sendMessage(sessionId = "s1", userPrompt = "what resources exist")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val toolMessage = historySlot.captured.last { it.role == "tool" }
        val response = toolMessage.parts.filterIsInstance<GeminiPart.FunctionResponse>().single()
        val resources = response.response["resources"]
        assertTrue(resources is org.json.JSONArray)
        val first = (resources as org.json.JSONArray).getJSONObject(0)
        assertEquals("file://notes.txt", first.getString("uri"))
        assertEquals("Test Server", first.getString("server"))
    }

    @Test
    fun `history is capped at MAX_HISTORY_MESSAGES and trimmed forward to a user turn`() = runTest {
        // 202 messages: a leading assistant pair (orphaned by truncation)
        // followed by 100 user/assistant turns, then the new prompt.
        val seeded = buildList {
            add(ChatMessageEntity(id = "old_a", sessionId = "s1", role = "assistant", content = "orphan tail"))
            repeat(100) { i ->
                add(ChatMessageEntity(id = "u$i", sessionId = "s1", role = "user", content = "turn $i"))
                add(ChatMessageEntity(id = "a$i", sessionId = "s1", role = "assistant", content = "reply $i"))
            }
            add(ChatMessageEntity(id = "last_user", sessionId = "s1", role = "user", content = "final question"))
        }
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        // The capped DAO read returns only the LAST 200 messages.
        coEvery { chatDao.getRecentMessages("s1", any()) } returns seeded.takeLast(200)
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "ok")

        createRepository()
        repository.sendMessage(sessionId = "s1", userPrompt = "final question")
        advanceUntilIdle()

        val history = historySlot.captured
        // 200 capped + 1 newly persisted prompt; leading orphan trimmed.
        assertTrue(history.size <= 201)
        assertEquals("user", history.first().role)
        coVerify { chatDao.getRecentMessages("s1", ChatRepository.MAX_HISTORY_MESSAGES) }
    }

    @Test
    fun `searchMessages delegates the sanitized FTS expression to chatDao`() = runTest {
        val expected = flowOf(
            listOf(ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "quarterly report")),
        )
        coEvery { chatDao.searchMessages("quarterly* report*") } returns expected

        createRepository()
        val results = repository.searchMessages("  quarterly (report) ").first()

        assertEquals(1, results.size)
        assertEquals("m1", results[0].id)
    }

    @Test
    fun `searchMessages with blank or operator-only query never touches the FTS index`() = runTest {
        createRepository()

        assertTrue(repository.searchMessages("   ").first().isEmpty())
        assertTrue(repository.searchMessages("***\"-()").first().isEmpty())
        coVerify(exactly = 0) { chatDao.searchMessages(any()) }
    }

    @Test
    fun `buildFtsQuery sanitizes operators, dedupes and caps tokens`() {
        assertEquals("quarterly*", ChatRepository.buildFtsQuery("quarterly"))
        assertEquals("bit* coin*", ChatRepository.buildFtsQuery("  bit   coin "))
        // Operator characters are stripped so MATCH can never syntax-error;
        // a no-whitespace input collapses to a single prefix token.
        assertEquals("bitcoin*", ChatRepository.buildFtsQuery("\"bit\"-(coin)*"))
        assertNull(ChatRepository.buildFtsQuery("  "))
        assertNull(ChatRepository.buildFtsQuery("- * \"'"))
        assertEquals(8, Regex("[a-z0-9]+\\*").findAll(ChatRepository.buildFtsQuery((1..20).joinToString(" ") { "w$it" })!!).count())
    }

    @Test
    fun `allSessionsFlow delegates to chatDao`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        every { chatDao.getAllSessions() } returns flowOf(sessions)

        createRepository()
        val result = repository.allSessionsFlow
        assertNotNull(result)
    }

    @Test
    fun `getMessagesForSession delegates to chatDao`() = runTest {
        val messages = listOf(
            ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "Hi"),
        )
        every { chatDao.getMessagesForSession("s1") } returns flowOf(messages)

        createRepository()
        val flow = repository.getMessagesForSession("s1")
        assertNotNull(flow)
    }

    @Test
    fun `createNewSession inserts session into database`() = runTest {
        createRepository()
        val session = repository.createNewSession(title = "Test Chat", model = "gemini-2.0-flash")

        assertNotNull(session)
        assertEquals("Test Chat", session.title)
        assertEquals("gemini-2.0-flash", session.model)
        coVerify { chatDao.insertSession(any()) }
    }

    @Test
    fun `updateSessionTitle updates session title`() = runTest {
        val existing = ChatSessionEntity(id = "s1", title = "Old Title")
        coEvery { chatDao.getSessionById("s1") } returns existing

        createRepository()
        repository.updateSessionTitle("s1", "New Title")
        advanceUntilIdle()

        coVerify { chatDao.updateSession(match { it.title == "New Title" }) }
    }

    @Test
    fun `updateSessionModel updates session model`() = runTest {
        val existing = ChatSessionEntity(id = "s1", title = "Test", model = "gemini-3.5-flash")
        coEvery { chatDao.getSessionById("s1") } returns existing

        createRepository()
        repository.updateSessionModel("s1", "gemini-2.0-flash")
        advanceUntilIdle()

        coVerify { chatDao.updateSession(match { it.model == "gemini-2.0-flash" }) }
    }

    @Test
    fun `updateSessionTitle does nothing when session not found`() = runTest {
        coEvery { chatDao.getSessionById("nonexistent") } returns null

        createRepository()
        repository.updateSessionTitle("nonexistent", "New Title")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatDao.updateSession(any()) }
    }

    @Test
    fun `deleteSession deletes messages and session`() = runTest {
        createRepository()
        repository.deleteSession("s1")
        advanceUntilIdle()

        coVerify { chatDao.deleteMessagesForSession("s1") }
        coVerify { chatDao.deleteSessionById("s1") }
    }

    @Test
    fun `parseToolCallsJson parses valid JSON`() {
        createRepository()
        val json = """[
            {
                "callId": "call_1",
                "serverId": "server_1",
                "serverName": "Test Server",
                "toolName": "get_weather",
                "inputArguments": {"city": "Tokyo"},
                "outputResult": "{\"temp\":22}",
                "isSuccess": true,
                "executionTimeMs": 150,
                "status": "SUCCESS",
                "timestamp": 1234567890
            }
        ]"""

        val result = repository.parseToolCallsJson(json)
        assertEquals(1, result.size)
        assertEquals("get_weather", result[0].toolName)
        assertEquals("server_1", result[0].serverId)
        assertEquals("Tokyo", result[0].inputArguments["city"])
    }

    @Test
    fun `parseToolCallsJson returns empty list for invalid JSON`() {
        createRepository()
        val result = repository.parseToolCallsJson("not valid json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseToolCallsJson handles empty array`() {
        createRepository()
        val result = repository.parseToolCallsJson("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseToolCallsJson handles multiple tool calls`() {
        createRepository()
        val json = """[
            {"callId":"c1","toolName":"tool1","inputArguments":{},"status":"SUCCESS","timestamp":1},
            {"callId":"c2","toolName":"tool2","inputArguments":{"key":"val"},"status":"ERROR","timestamp":2}
        ]"""

        val result = repository.parseToolCallsJson(json)
        assertEquals(2, result.size)
        assertEquals("tool1", result[0].toolName)
        assertEquals("tool2", result[1].toolName)
    }

    @Test
    fun `sendMessage with API_KEY_MISSING uses fallback handler`() = runTest {
        coEvery { chatDao.getSessionById(any()) } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = null, error = "API_KEY_MISSING")

        createRepository()
        val result = repository.sendMessage(sessionId = "s1", userPrompt = "Hello")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `sendMessage with unknown sessionId persists turns under the fallback session`() = runTest {
        coEvery { chatDao.getSessionById("stale") } returns null
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = null, error = "API_KEY_MISSING")

        createRepository()
        val result = repository.sendMessage(sessionId = "stale", userPrompt = "Hello")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        // A fallback session was created…
        coVerify { chatDao.insertSession(any()) }
        // …and both turns (user + assistant) were persisted into it — never
        // under the stale id, which would create orphaned rows.
        coVerify(exactly = 0) { chatDao.insertMessage(match { it.sessionId == "stale" }) }
        coVerify(exactly = 2) { chatDao.insertMessage(any()) }
    }

    @Test
    fun `sendMessage streams token deltas through streamEvents`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val onDelta = args[5] as (String) -> Unit
            onDelta("Hel")
            onDelta("lo!")
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "Hello!")
        }

        createRepository()
        val events = mutableListOf<ChatStreamEvent>()
        val collectJob = launch { repository.streamEvents.collect { events.add(it) } }
        advanceUntilIdle()

        val result = repository.sendMessage(sessionId = "s1", userPrompt = "Hi")
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue(result.isSuccess)
        val sessionEvents = events.filter { it.sessionId == "s1" }
        assertTrue(sessionEvents.isNotEmpty())
        assertTrue(sessionEvents.first() is ChatStreamEvent.StreamStarted)
        assertTrue(sessionEvents.any { it is ChatStreamEvent.TokenDelta && it.delta == "Hel" })
        assertTrue(sessionEvents.any { it is ChatStreamEvent.TokenDelta && it.delta == "lo!" })
        assertTrue(sessionEvents.last() is ChatStreamEvent.StreamFinished)
    }

    @Test
    fun `sendMessage cancelled mid-stream persists partial text with stop marker`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val onDelta = args[5] as (String) -> Unit
            onDelta("partial answer")
            throw CancellationException("stopped by user")
        }

        createRepository()
        val outcome = runCatching { repository.sendMessage(sessionId = "s1", userPrompt = "Hello") }
        advanceUntilIdle()

        assertTrue(outcome.isFailure)
        // The partial reply survives in history instead of vanishing.
        coVerify {
            chatDao.insertMessage(
                match {
                    it.role == "assistant" &&
                        it.content.contains("partial answer") &&
                        it.content.contains("[Generation stopped by user]")
                },
            )
        }
    }

    @Test
    fun `sendMessage cancelled before any token persists nothing`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } throws
            CancellationException("stopped by user")

        createRepository()
        val outcome = runCatching { repository.sendMessage(sessionId = "s1", userPrompt = "Hello") }
        advanceUntilIdle()

        assertTrue(outcome.isFailure)
        // Only the user turn is stored; no empty assistant placeholder.
        coVerify(exactly = 1) { chatDao.insertMessage(match { it.role == "user" }) }
    }

    @Test
    fun `regenerateLastMessage drops trailing assistant and regenerates`() = runTest {
        val initial = listOf(
            ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "Hello"),
            ChatMessageEntity(id = "m2", sessionId = "s1", role = "assistant", content = "Old answer"),
        )
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        // First call (trailing-message detection) sees the assistant message;
        // the second (history build) runs after its deletion.
        coEvery { chatDao.getRecentMessages("s1", any()) } returnsMany listOf(initial, initial.filter { it.id != "m2" })
        val historySlot = slot<List<GeminiMessage>>()
        coEvery {
            geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any())
        } returns com.aistudio.mcpchat.data.model.GeminiResponse(text = "New answer")

        createRepository()
        val result = repository.regenerateLastMessage("s1")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        coVerify { chatDao.deleteMessageById("m2") }
        assertEquals(listOf("user"), historySlot.captured.map { it.role })
        coVerify { chatDao.insertMessage(match { it.role == "assistant" && it.content == "New answer" }) }
    }

    @Test
    fun `regenerateLastMessage retries as-is when last message is a user turn`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns listOf(
            ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "Hello"),
        )
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "Fresh answer")

        createRepository()
        val result = repository.regenerateLastMessage("s1")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { chatDao.deleteMessageById(any()) }
        coVerify { chatDao.insertMessage(match { it.role == "assistant" && it.content == "Fresh answer" }) }
    }

    @Test
    fun `regenerateLastMessage fails without persisting when session is gone`() = runTest {
        coEvery { chatDao.getSessionById("gone") } returns null

        createRepository()
        val result = repository.regenerateLastMessage("gone")
        advanceUntilIdle()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { chatDao.insertMessage(any()) }
    }

    @Test
    fun `history rebuild pairs every persisted tool call with a result even when output blank`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getRecentMessages("s1", any()) } returns listOf(
            ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "weather?"),
            ChatMessageEntity(
                id = "m2",
                sessionId = "s1",
                role = "assistant",
                content = "",
                toolCallsJson = """[
                    {"callId":"c1","toolName":"get_weather","inputArguments":{"city":"Tokyo"},
                     "outputResult":"","status":"SUCCESS","timestamp":1},
                    {"callId":"c2","toolName":"get_forecast","inputArguments":{},
                     "outputResult":"","status":"SUCCESS","timestamp":2}
                ]""",
            ),
        )
        val historySlot = slot<List<GeminiMessage>>()
        coEvery { geminiService.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "done")

        createRepository()
        repository.sendMessage(sessionId = "s1", userPrompt = "thanks")
        advanceUntilIdle()

        val history = historySlot.captured
        val modelMsg = history.lastOrNull { it.role == "model" && it.parts.any { p -> p is GeminiPart.FunctionCall } }
        val toolMsg = history.lastOrNull { it.role == "tool" }
        assertNotNull("expected a rebuilt model message with function calls", modelMsg)
        assertNotNull("expected a rebuilt tool message with function responses", toolMsg)
        val calls = modelMsg!!.parts.filterIsInstance<GeminiPart.FunctionCall>()
        val responses = toolMsg!!.parts.filterIsInstance<GeminiPart.FunctionResponse>()
        // Strict providers (Anthropic, AgentRouter) reject a request where a
        // tool_use has no matching tool_result — blank outputs must still pair.
        assertEquals(calls.size, responses.size)
        assertEquals(2, responses.size)
        assertTrue(responses.all { it.response["result"] == "Empty response" })
    }

    @Test
    fun `sendMessage strips suggestions marker and emits parsed suggestions`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(
                text = "Sunny in Tokyo.\n\n---SUGGESTIONS---\n1. Show the extended forecast\n2. Compare with London",
            )

        createRepository()
        val events = mutableListOf<ChatStreamEvent>()
        val collectJob = launch { repository.streamEvents.collect { events.add(it) } }
        advanceUntilIdle()

        val result = repository.sendMessage(sessionId = "s1", userPrompt = "Weather?")
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue(result.isSuccess)
        coVerify { chatDao.insertMessage(match { it.role == "assistant" && it.content == "Sunny in Tokyo." }) }
        val finished = events.filterIsInstance<ChatStreamEvent.StreamFinished>().lastOrNull()
        assertNotNull(finished)
        assertEquals(listOf("Show the extended forecast", "Compare with London"), finished!!.suggestions)
    }

    @Test
    fun `sendMessage falls back to generic suggestions without marker`() = runTest {
        coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(id = "s1", title = "Test")
        coEvery { chatDao.getMessagesList(any()) } returns emptyList()
        coEvery { geminiService.streamContent(any(), any(), any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = "Plain answer")

        createRepository()
        val events = mutableListOf<ChatStreamEvent>()
        val collectJob = launch { repository.streamEvents.collect { events.add(it) } }
        advanceUntilIdle()

        repository.sendMessage(sessionId = "s1", userPrompt = "Hi")
        advanceUntilIdle()
        collectJob.cancel()

        val finished = events.filterIsInstance<ChatStreamEvent.StreamFinished>().lastOrNull()
        assertNotNull(finished)
        assertEquals(3, finished!!.suggestions.size)
    }
}
