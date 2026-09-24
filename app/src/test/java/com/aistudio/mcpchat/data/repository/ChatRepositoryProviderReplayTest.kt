package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatDao
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderType
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.GeminiResponse
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.network.GeminiApiService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryProviderReplayTest {

    @Test
    fun `legacy persisted call identity migrates only through the active server tool`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val database = mockk<AppDatabase>(relaxed = true)
            val chatDao = mockk<ChatDao>(relaxed = true)
            val mcpRepository = mockk<McpRepository>(relaxed = true)
            val service = mockk<GeminiApiService>(relaxed = true)
            every { database.chatDao() } returns chatDao
            val server = McpServer("srv", "Search Server", "", "https://mcp.example.com")
            val tool = McpTool("custom-tool-id", server.id, server.name, "search", "Search")
            coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
            coEvery { mcpRepository.getEnabledTools() } returns listOf(tool)
            coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity("s1", "Replay")
            coEvery { chatDao.getMessagesList(any()) } returns emptyList()
            coEvery { chatDao.getRecentMessages("s1", any()) } returns listOf(
                ChatMessageEntity("u1", "s1", "user", "Use search"),
                ChatMessageEntity(
                    id = "a1",
                    sessionId = "s1",
                    role = "assistant",
                    content = "done",
                    toolCallsJson = """[{"serverId":"srv","toolName":"search","inputArguments":{}}]""",
                ),
            )
            val historySlot = slot<List<com.aistudio.mcpchat.data.model.GeminiMessage>>()
            coEvery { service.streamContent(any(), any(), capture(historySlot), any(), any(), any()) } returns
                GeminiResponse(text = "ok")

            val repository = ChatRepository(database, mcpRepository, service)
            assertTrue(repository.sendMessage("s1", "Continue").isSuccess)
            advanceUntilIdle()

            val modelCall = historySlot.captured.single { it.role == "model" }
                .parts.filterIsInstance<GeminiPart.FunctionCall>().single()
            val toolResponse = historySlot.captured.single { it.role == "tool" }
                .parts.filterIsInstance<GeminiPart.FunctionResponse>().single()
            assertEquals(tool.id, modelCall.toolId)
            assertEquals(tool.id, toolResponse.toolId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `persisted tool exchange replays through the real Gemini wire contract`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        val networkServer = MockWebServer()
        try {
            networkServer.start()
            val database = mockk<AppDatabase>(relaxed = true)
            val chatDao = mockk<ChatDao>(relaxed = true)
            val mcpRepository = mockk<McpRepository>(relaxed = true)
            val firstService = mockk<GeminiApiService>(relaxed = true)
            every { database.chatDao() } returns chatDao

            val server = McpServer(
                id = "srv",
                name = "Search Server",
                description = "",
                endpointUrl = "https://mcp.example.com",
            )
            val tool = McpTool(
                id = "srv_search",
                serverId = server.id,
                serverName = server.name,
                name = "reports.export",
                description = "Search",
            )
            coEvery { mcpRepository.getEnabledServers() } returns listOf(server)
            coEvery { mcpRepository.getEnabledTools() } returns listOf(tool)
            coEvery { mcpRepository.executeTool(server, tool.name, any()) } returns Pair(
                "result",
                McpLogItem(
                    serverId = server.id,
                    serverName = server.name,
                    toolName = tool.name,
                    requestPayload = "",
                    responsePayload = "result",
                    isSuccess = true,
                    latencyMs = 1,
                ),
            )
            coEvery { chatDao.getSessionById("s1") } returns ChatSessionEntity(
                id = "s1",
                title = "Replay",
                model = "gemini-test",
            )
            coEvery { chatDao.getMessagesList(any()) } returns emptyList()
            coEvery { chatDao.getRecentMessages("s1", any()) } returns emptyList()
            val persisted = mutableListOf<ChatMessageEntity>()
            coEvery { chatDao.insertMessage(any()) } coAnswers {
                persisted += firstArg<ChatMessageEntity>()
            }
            coEvery { firstService.streamContent(any(), any(), any(), any(), any(), any()) } returnsMany listOf(
                GeminiResponse(
                    text = null,
                    functionCalls = listOf(GeminiPart.FunctionCall(tool.name, emptyMap(), toolId = tool.id)),
                ),
                GeminiResponse(text = "done"),
            )

            val firstRepository = ChatRepository(database, mcpRepository, firstService)
            assertTrue(firstRepository.sendMessage("s1", "Use search").isSuccess)
            advanceUntilIdle()

            val conversation = persisted.toList()
            val assistant = conversation.single { it.role == "assistant" }
            assertEquals(tool.id, firstRepository.parseToolCallsJson(assistant.toolCallsJson!!).single().toolId)
            val replayHistory = conversation + ChatMessageEntity("current-user", "s1", "user", "Continue")
            coEvery { chatDao.getMessagesList(any()) } returns conversation
            coEvery { chatDao.getRecentMessages("s1", any()) } returns replayHistory

            networkServer.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("""data: {"candidates":[{"content":{"parts":[{"text":"replayed"}]}}]}"""),
            )
            val config = AiProviderConfig(
                name = "Test Gemini",
                type = AiProviderType.GOOGLE,
                baseUrl = networkServer.url("/v1beta").toString().trimEnd('/'),
                apiKey = "test-key",
                model = "gemini-test",
            )
            val replayRepository = ChatRepository(
                database = database,
                mcpRepository = mcpRepository,
                aiService = GeminiApiService(OkHttpClient()),
                activeProvider = { config },
            )

            assertTrue(replayRepository.sendMessage("s1", "Continue").isSuccess)
            advanceUntilIdle()

            val request = JSONObject(networkServer.takeRequest(10, TimeUnit.SECONDS)!!.body.readUtf8())
            val contents = request.getJSONArray("contents")
            assertEquals(listOf("user", "model", "user"), (0 until contents.length()).map { contents.getJSONObject(it).getString("role") })
            val modelCall = contents.getJSONObject(1).getJSONArray("parts").getJSONObject(0)
            val finalUserParts = contents.getJSONObject(2).getJSONArray("parts")
            val toolResponse = finalUserParts.getJSONObject(0)
            assertEquals("reports_export", modelCall.getJSONObject("functionCall").getString("name"))
            assertEquals("reports_export", toolResponse.getJSONObject("functionResponse").getString("name"))
            assertEquals("Continue", finalUserParts.getJSONObject(1).getString("text"))
            val declarations = request.getJSONArray("tools").getJSONObject(0).getJSONArray("functionDeclarations")
            val declaredNames = (0 until declarations.length()).map { declarations.getJSONObject(it).getString("name") }
            assertTrue(declaredNames.contains("reports_export"))
            val systemInstruction = request.getJSONObject("systemInstruction").getJSONArray("parts")
                .getJSONObject(0).getString("text")
            assertFalse(systemInstruction.contains(tool.name))
        } finally {
            networkServer.shutdown()
            Dispatchers.resetMain()
        }
    }
}
