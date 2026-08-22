package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatDao
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.network.GeminiApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        coEvery { geminiService.generateContent(any(), any(), any(), any()) } returns
            com.aistudio.mcpchat.data.model.GeminiResponse(text = null, error = "API_KEY_MISSING")

        createRepository()
        val result = repository.sendMessage(sessionId = "s1", userPrompt = "Hello")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }
}
