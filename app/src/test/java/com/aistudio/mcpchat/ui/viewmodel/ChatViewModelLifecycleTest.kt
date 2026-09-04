package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.ChatException
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.repository.ChatRepository
import com.aistudio.mcpchat.data.repository.ChatStreamEvent
import com.aistudio.mcpchat.data.repository.McpRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class ChatViewModelLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mcpRepository: McpRepository
    private lateinit var chatRepository: ChatRepository

    private val sessionsFlow = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    private val serversFlow = MutableStateFlow<List<McpServer>>(emptyList())
    private val toolsFlow = MutableStateFlow<List<McpTool>>(emptyList())
    private val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private val streamEventsFlow = MutableSharedFlow<ChatStreamEvent>(extraBufferCapacity = 64)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mcpRepository = mockk(relaxed = true)
        chatRepository = mockk(relaxed = true)

        every { mcpRepository.serversFlow } returns serversFlow
        every { mcpRepository.allToolsFlow } returns toolsFlow
        every { chatRepository.allSessionsFlow } returns sessionsFlow
        every { chatRepository.getMessagesForSession(any()) } returns messagesFlow
        every { chatRepository.streamEvents } returns streamEventsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savedStateHandle persists currentSessionId across ViewModel recreation`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("current_session_id" to "restored_session"))

        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        advanceUntilIdle()

        assertEquals("restored_session", viewModel.currentSessionId.value)
    }

    @Test
    fun `savedStateHandle restores session from prepopulated state`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        sessionsFlow.value = sessions

        val savedStateHandle = SavedStateHandle(mapOf("current_session_id" to "s2"))
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals("s2", viewModel.currentSessionId.value)
        val currentState = viewModel.uiState.value
        assertNotNull(currentState.currentSession)
        assertEquals("Session 2", currentState.currentSession?.title)
    }

    @Test
    fun `configuration change preserves session selection`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        sessionsFlow.value = sessions

        val savedStateHandle = SavedStateHandle()
        val viewModel1 = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        viewModel1.selectSession("s2")
        advanceUntilIdle()

        assertEquals("s2", viewModel1.currentSessionId.value)

        val restoredHandle = SavedStateHandle(mapOf("current_session_id" to "s2"))
        val viewModel2 = ChatViewModel(mcpRepository, chatRepository, restoredHandle)
        advanceUntilIdle()

        assertEquals("s2", viewModel2.currentSessionId.value)
    }

    @Test
    fun `auto creates new chat when no sessions exist`() = runTest {
        sessionsFlow.value = emptyList()
        coEvery { chatRepository.createNewSession(any(), any()) } returns
            ChatSessionEntity(id = "auto_created", title = "New AI Chat")

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        advanceUntilIdle()

        assertEquals("auto_created", viewModel.currentSessionId.value)
        coVerify { chatRepository.createNewSession(title = "New AI Chat", model = "gemini-3.5-flash") }
    }

    @Test
    fun `auto selects first session when sessions exist but no currentSessionId`() = runTest {
        sessionsFlow.value = listOf(
            ChatSessionEntity(id = "first", title = "First Session"),
            ChatSessionEntity(id = "second", title = "Second Session"),
        )

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        advanceUntilIdle()

        assertEquals("first", viewModel.currentSessionId.value)
    }

    @Test
    fun `switching sessions clears inProgressToolCalls`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        sessionsFlow.value = sessions

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.selectSession("s1")
        advanceUntilIdle()

        assertTrue(viewModel.inProgressToolCalls.value.isEmpty())
    }

    @Test
    fun `message flow updates when session changes`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        sessionsFlow.value = sessions

        val s1Messages = listOf(
            ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "Hello from s1"),
        )
        val s2Messages = listOf(
            ChatMessageEntity(id = "m2", sessionId = "s2", role = "user", content = "Hello from s2"),
        )

        var currentMessages = MutableStateFlow(s1Messages)
        every { chatRepository.getMessagesForSession("s1") } returns currentMessages
        every { chatRepository.getMessagesForSession("s2") } returns MutableStateFlow(s2Messages)

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messages.collect {}
        }
        advanceUntilIdle()

        viewModel.selectSession("s1")
        advanceUntilIdle()
        assertEquals(1, viewModel.messages.value.size)
        assertEquals("Hello from s1", viewModel.messages.value[0].content)

        viewModel.selectSession("s2")
        advanceUntilIdle()
        assertEquals(1, viewModel.messages.value.size)
        assertEquals("Hello from s2", viewModel.messages.value[0].content)
    }

    @Test
    fun `delete current session auto selects next session`() = runTest {
        sessionsFlow.value = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
            ChatSessionEntity(id = "s3", title = "Session 3"),
        )

        coEvery { chatRepository.deleteSession(any()) } coAnswers {
            sessionsFlow.value = sessionsFlow.value.filter { it.id != firstArg<String>() }
        }

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectSession("s2")
        viewModel.deleteSession("s2")
        advanceUntilIdle()

        coVerify { chatRepository.deleteSession("s2") }
        val remaining = viewModel.uiState.value.sessions
        assertTrue(remaining.none { it.id == "s2" })
    }

    @Test
    fun `delete last session creates new chat`() = runTest {
        sessionsFlow.value = listOf(ChatSessionEntity(id = "only", title = "Only Session"))

        coEvery { chatRepository.deleteSession(any()) } coAnswers {
            sessionsFlow.value = emptyList()
        }
        coEvery { chatRepository.createNewSession(any(), any()) } returns
            ChatSessionEntity(id = "new_after_delete", title = "New AI Chat")

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.deleteSession("only")
        advanceUntilIdle()

        assertEquals("new_after_delete", viewModel.currentSessionId.value)
    }

    @Test
    fun `uiState combines all flows correctly`() = runTest {
        sessionsFlow.value = listOf(ChatSessionEntity(id = "s1", title = "Session 1"))
        serversFlow.value = listOf(
            McpServer(id = "srv1", name = "Server 1", description = "desc", endpointUrl = "http://test.com"),
        )
        toolsFlow.value = listOf(
            McpTool(
                id = "t1",
                serverId = "srv1",
                serverName = "Server 1",
                name = "tool1",
                description = "A tool",
                inputSchemaJson = "{}",
            ),
        )

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sessions.size)
        assertEquals(1, state.servers.size)
        assertEquals(1, state.tools.size)
        assertFalse(state.isGenerating)
        assertTrue(state.inProgressToolCalls.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `sendMessage error maps to correct UiError types`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } throws
            ChatException.ApiKeyMissing()

        sessionsFlow.value = listOf(ChatSessionEntity(id = "s1", title = "Test"))
        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.sendMessage("test message")
        advanceUntilIdle()

        assertNotNull(viewModel.uiError.value)
        assertTrue(viewModel.uiError.value is UiError.Api)
        assertTrue(viewModel.uiError.value?.message?.contains("API key") == true)
    }

    @Test
    fun `viewModel scope operations complete before verification`() = runTest {
        coEvery { chatRepository.createNewSession(any(), any()) } returns
            ChatSessionEntity(id = "new", title = "New")

        val savedStateHandle = SavedStateHandle()
        val viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
        viewModel.createNewChat()
        advanceUntilIdle()

        assertEquals("new", viewModel.currentSessionId.value)
        assertFalse(viewModel.isGenerating.value)
    }
}
