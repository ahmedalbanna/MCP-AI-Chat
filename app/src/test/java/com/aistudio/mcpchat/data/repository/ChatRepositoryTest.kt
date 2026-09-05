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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
        coEvery { chatDao.getMessagesList("s1") } returnsMany listOf(initial, initial.filter { it.id != "m2" })
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
        coEvery { chatDao.getMessagesList("s1") } returns listOf(
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
        coEvery { chatDao.getMessagesList("s1") } returns listOf(
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
}
