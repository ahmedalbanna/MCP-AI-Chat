package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.ImageAttachment
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.repository.ChatRepository
import com.aistudio.mcpchat.data.repository.ChatStreamEvent
import com.aistudio.mcpchat.data.repository.McpRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mcpRepository: McpRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ChatViewModel

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
        savedStateHandle = SavedStateHandle()

        every { mcpRepository.serversFlow } returns serversFlow
        every { mcpRepository.allToolsFlow } returns toolsFlow
        every { chatRepository.allSessionsFlow } returns sessionsFlow
        every { chatRepository.getMessagesWindow(any(), any()) } returns messagesFlow
        every { chatRepository.streamEvents } returns streamEventsFlow
    }

    private fun createViewModel() {
        viewModel = ChatViewModel(mcpRepository, chatRepository, savedStateHandle)
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
        assertNull(state.currentSessionId)
        assertTrue(state.sessions.isEmpty())
        assertTrue(state.messages.isEmpty())
        assertTrue(state.servers.isEmpty())
        assertTrue(state.tools.isEmpty())
        assertFalse(state.isGenerating)
        assertTrue(state.inProgressToolCalls.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `message window grows only while the visible list fills it`() = runTest {
        // Exactly one window's worth of messages: the list is "full", so a
        // load request expands the window.
        messagesFlow.value = List(ChatViewModel.MESSAGE_WINDOW_INITIAL) { i ->
            ChatMessageEntity(id = "m$i", sessionId = "s1", role = "user", content = "msg $i")
        }
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect {} }
        viewModel.selectSession("s1")
        advanceUntilIdle()

        assertEquals(ChatViewModel.MESSAGE_WINDOW_INITIAL, viewModel.messageWindowLimit.value)

        viewModel.loadEarlierMessages()
        advanceUntilIdle()
        assertEquals(ChatViewModel.MESSAGE_WINDOW_INITIAL + ChatViewModel.MESSAGE_WINDOW_STEP, viewModel.messageWindowLimit.value)

        // The flow now emits a short list (< window): all history is
        // materialized, so further requests are no-ops.
        messagesFlow.value = messagesFlow.value.take(10)
        advanceUntilIdle()

        viewModel.loadEarlierMessages()
        advanceUntilIdle()
        assertEquals(ChatViewModel.MESSAGE_WINDOW_INITIAL + ChatViewModel.MESSAGE_WINDOW_STEP, viewModel.messageWindowLimit.value)
    }

    @Test
    fun `message window resets when switching sessions`() = runTest {
        messagesFlow.value = List(ChatViewModel.MESSAGE_WINDOW_INITIAL) { i ->
            ChatMessageEntity(id = "m$i", sessionId = "s1", role = "user", content = "msg $i")
        }
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect {} }
        viewModel.selectSession("s1")
        advanceUntilIdle()
        viewModel.loadEarlierMessages()
        advanceUntilIdle()
        assertTrue(viewModel.messageWindowLimit.value > ChatViewModel.MESSAGE_WINDOW_INITIAL)

        viewModel.selectSession("s2")
        advanceUntilIdle()

        assertEquals(ChatViewModel.MESSAGE_WINDOW_INITIAL, viewModel.messageWindowLimit.value)
    }

    @Test
    fun `search query drives messageSearchResults and blank query clears them`() = runTest {
        val results = listOf(ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "hit"))
        every { chatRepository.searchMessages("quarterly") } returns MutableStateFlow(results)
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messageSearchResults.collect {} }

        viewModel.updateMessageSearchQuery("quarterly")
        advanceUntilIdle()
        assertEquals(results, viewModel.messageSearchResults.value)

        viewModel.updateMessageSearchQuery("")
        advanceUntilIdle()
        assertTrue(viewModel.messageSearchResults.value.isEmpty())
    }

    @Test
    fun `streamingText accumulates token deltas for the current session`() = runTest {
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        streamEventsFlow.tryEmit(ChatStreamEvent.TokenDelta("s1", "Hel"))
        streamEventsFlow.tryEmit(ChatStreamEvent.TokenDelta("s1", "lo"))
        advanceUntilIdle()
        assertEquals("Hello", viewModel.streamingText.value)

        // Tokens from a chat the user navigated away from never bleed in.
        streamEventsFlow.tryEmit(ChatStreamEvent.TokenDelta("other", "XXX"))
        advanceUntilIdle()
        assertEquals("Hello", viewModel.streamingText.value)

        // A new hop resets the preview; finishing clears it entirely.
        streamEventsFlow.tryEmit(ChatStreamEvent.StreamStarted("s1"))
        advanceUntilIdle()
        assertNull(viewModel.streamingText.value)

        streamEventsFlow.tryEmit(ChatStreamEvent.TokenDelta("s1", "again"))
        streamEventsFlow.tryEmit(ChatStreamEvent.StreamFinished("s1"))
        advanceUntilIdle()
        assertNull(viewModel.streamingText.value)
    }

    @Test
    fun `regenerate delegates to repository for current session`() = runTest {
        createViewModel()
        viewModel.selectSession("s1")
        // messages is a WhileSubscribed stateIn flow: collect it so the
        // guard sees the seeded message.
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messages.collect {}
        }
        messagesFlow.value = listOf(
            ChatMessageEntity(id = "m1", sessionId = "s1", role = "user", content = "hi"),
            ChatMessageEntity(id = "m2", sessionId = "s1", role = "assistant", content = "old"),
        )
        advanceUntilIdle()
        coEvery { chatRepository.regenerateLastMessage("s1", any()) } returns Result.success(
            ChatMessageEntity(id = "m3", sessionId = "s1", role = "assistant", content = "new"),
        )

        viewModel.regenerate()
        advanceUntilIdle()

        coVerify { chatRepository.regenerateLastMessage("s1", any()) }
        assertFalse(viewModel.isGenerating.value)
        collector.cancel()
    }

    @Test
    fun `regenerate is skipped without messages`() = runTest {
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.regenerate()
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.regenerateLastMessage(any(), any()) }
    }

    @Test
    fun `streamProgress tracks deltas and clears on finish`() = runTest {
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        streamEventsFlow.tryEmit(ChatStreamEvent.TokenDelta("s1", "Hello"))
        advanceUntilIdle()
        val progress = viewModel.streamProgress.value
        assertNotNull(progress)
        assertEquals(5, progress!!.charsStreamed)

        streamEventsFlow.tryEmit(ChatStreamEvent.StreamFinished("s1"))
        advanceUntilIdle()
        assertNull(viewModel.streamProgress.value)
    }

    @Test
    fun `selectSession updates currentSessionId`() = runTest {
        createViewModel()

        viewModel.selectSession("session_123")
        advanceUntilIdle()
        assertEquals("session_123", viewModel.currentSessionId.value)
    }

    @Test
    fun `selectSession clears error state`() = runTest {
        createViewModel()

        viewModel.selectSession("session_123")
        advanceUntilIdle()

        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.uiError.value)
    }

    @Test
    fun `createNewChat calls chatRepository and sets sessionId`() = runTest {
        val session = ChatSessionEntity(id = "new_session", title = "New AI Chat")
        coEvery { chatRepository.createNewSession(any(), any()) } returns session

        createViewModel()
        viewModel.createNewChat()
        advanceUntilIdle()

        assertEquals("new_session", viewModel.currentSessionId.value)
        coVerify { chatRepository.createNewSession(title = "New AI Chat", model = "gemini-3.5-flash") }
    }

    @Test
    fun `createNewChat with custom model passes model parameter`() = runTest {
        val session = ChatSessionEntity(id = "new_session", title = "New AI Chat", model = "gemini-2.0-flash")
        coEvery { chatRepository.createNewSession(any(), any()) } returns session

        createViewModel()
        viewModel.createNewChat(model = "gemini-2.0-flash")
        advanceUntilIdle()

        coVerify { chatRepository.createNewSession(title = "New AI Chat", model = "gemini-2.0-flash") }
    }

    @Test
    fun `deleteSession calls chatRepository and updates sessionId`() = runTest {
        sessionsFlow.value = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        coEvery { chatRepository.deleteSession(any()) } coAnswers {
            sessionsFlow.value = sessionsFlow.value.filter { it.id != firstArg<String>() }
        }

        createViewModel()
        advanceUntilIdle()

        viewModel.deleteSession("s1")
        advanceUntilIdle()

        coVerify { chatRepository.deleteSession("s1") }
    }

    @Test
    fun `toggleServer calls mcpRepository`() = runTest {
        createViewModel()
        viewModel.toggleServer("server_1", true)
        advanceUntilIdle()

        coVerify { mcpRepository.toggleServer("server_1", true) }
    }

    @Test
    fun `updateSessionModel calls chatRepository`() = runTest {
        val session = ChatSessionEntity(id = "s1", title = "Test")
        sessionsFlow.value = listOf(session)
        coEvery { chatRepository.createNewSession(any(), any()) } returns session

        createViewModel()
        viewModel.selectSession("s1")
        viewModel.updateSessionModel("gemini-2.0-flash")
        advanceUntilIdle()

        coVerify { chatRepository.updateSessionModel("s1", "gemini-2.0-flash") }
    }

    @Test
    fun `updateSessionModel does nothing when no session selected`() = runTest {
        createViewModel()
        viewModel.updateSessionModel("gemini-2.0-flash")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.updateSessionModel(any(), any()) }
    }

    @Test
    fun `sendMessage sets isGenerating true and calls repository`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } returns Result.success(
            ChatMessageEntity(id = "msg_1", sessionId = "s1", role = "assistant", content = "Hello!"),
        )

        sessionsFlow.value = listOf(ChatSessionEntity(id = "s1", title = "Test"))
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.sendMessage("Hi there")
        advanceUntilIdle()

        assertFalse(viewModel.isGenerating.value)
        coVerify { chatRepository.sendMessage("s1", "Hi there", any()) }
    }

    @Test
    fun `sendMessage with blank prompt does nothing`() = runTest {
        createViewModel()
        viewModel.sendMessage("")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `pending attachments build up and clear on send`() = runTest {
        val attachment = ImageAttachment("content://media/1", "image/png")
        val attachmentSlot = slot<List<ImageAttachment>>()
        coEvery { chatRepository.sendMessage(any(), any(), any(), capture(attachmentSlot)) } returns Result.success(
            ChatMessageEntity(id = "msg_1", sessionId = "s1", role = "assistant", content = "Nice picture!"),
        )

        sessionsFlow.value = listOf(ChatSessionEntity(id = "s1", title = "Test"))
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.addPendingAttachment("content://media/1", "image/png")
        assertEquals(listOf(attachment), viewModel.pendingAttachments.value)

        // A blank prompt with an attachment still sends.
        viewModel.sendMessage("")
        advanceUntilIdle()

        assertEquals(emptyList<ImageAttachment>(), viewModel.pendingAttachments.value)
        assertEquals(listOf(attachment), attachmentSlot.captured)
    }

    @Test
    fun `removePendingAttachment drops the staged image`() = runTest {
        createViewModel()
        val first = ImageAttachment("content://media/1", "image/png")
        val second = ImageAttachment("content://media/2", "image/jpeg")

        viewModel.addPendingAttachment(first.uri, first.mimeType)
        viewModel.addPendingAttachment(second.uri, second.mimeType)
        viewModel.removePendingAttachment(first)

        assertEquals(listOf(second), viewModel.pendingAttachments.value)
    }

    @Test
    fun `sendMessage with whitespace-only prompt does nothing`() = runTest {
        createViewModel()
        viewModel.sendMessage("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `sendMessage when already generating does nothing`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(ChatMessageEntity(id = "msg_1", sessionId = "s1", role = "assistant", content = "Hi"))
        }

        sessionsFlow.value = listOf(ChatSessionEntity(id = "s1", title = "Test"))
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.sendMessage("First message")
        // Pump the scheduler until the first send suspends inside its work —
        // only then is _isGenerating true.
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.isGenerating.value)

        viewModel.sendMessage("Second message")
        advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.sendMessage(any(), any(), any()) }
        assertFalse(viewModel.isGenerating.value)
    }

    @Test
    fun `clearError clears both error and uiError`() = runTest {
        createViewModel()
        viewModel.clearError()
        advanceUntilIdle()

        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.uiError.value)
    }

    @Test
    fun `uiState reflects sessions from repository`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        sessionsFlow.value = sessions

        createViewModel()
        // stateIn(WhileSubscribed) only produces values once collected.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.sessions.size)
    }

    @Test
    fun `uiState reflects servers from repository`() = runTest {
        val servers = listOf(
            McpServer(id = "s1", name = "Server 1", description = "desc", endpointUrl = "http://test.com"),
        )
        serversFlow.value = servers

        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.servers.size)
        assertEquals("Server 1", viewModel.uiState.value.servers[0].name)
    }

    @Test
    fun `sendMessage sets hasPendingUserTurn true during generation and clears on completion`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(ChatMessageEntity(id = "msg_1", sessionId = "s1", role = "assistant", content = "Hello!"))
        }
        sessionsFlow.value = listOf(ChatSessionEntity(id = "s1", title = "Test"))
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.sendMessage("Hi")
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.hasPendingUserTurn.value)

        advanceUntilIdle()
        assertFalse(viewModel.hasPendingUserTurn.value)
    }

    @Test
    fun `followPersistedSession follows fallback session when user stays on original`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } returns Result.success(
            ChatMessageEntity(id = "msg_1", sessionId = "s_fallback", role = "assistant", content = "Hello!"),
        )
        sessionsFlow.value = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s_fallback", title = "Fallback"),
        )
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        assertEquals("s_fallback", viewModel.currentSessionId.value)
        assertFalse(viewModel.isGenerating.value)
    }

    @Test
    fun `followPersistedSession does not snap back when user switched sessions mid-request`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(ChatMessageEntity(id = "msg_1", sessionId = "s_fallback", role = "assistant", content = "Hello!"))
        }
        sessionsFlow.value = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1"),
            ChatSessionEntity(id = "s2", title = "Session 2"),
        )
        createViewModel()
        viewModel.selectSession("s1")
        advanceUntilIdle()

        viewModel.sendMessage("Hi")
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.isGenerating.value)

        viewModel.selectSession("s2")
        advanceUntilIdle()

        advanceUntilIdle()

        assertEquals("s2", viewModel.currentSessionId.value)
        assertFalse(viewModel.isGenerating.value)
    }
}
