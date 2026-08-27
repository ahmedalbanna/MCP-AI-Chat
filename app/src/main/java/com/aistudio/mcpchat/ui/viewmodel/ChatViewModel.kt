package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.ChatException
import com.aistudio.mcpchat.data.model.DEFAULT_MODEL
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.repository.ChatRepository
import com.aistudio.mcpchat.data.repository.McpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ChatUiState(
    val currentSessionId: String? = null,
    val currentSession: ChatSessionEntity? = null,
    val sessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val servers: List<McpServer> = emptyList(),
    val tools: List<McpTool> = emptyList(),
    val isGenerating: Boolean = false,
    val inProgressToolCalls: List<McpToolCall> = emptyList(),
    val error: UiError? = null,
)

sealed class UiError(val message: String) {
    data class Network(val detail: String) : UiError(detail)
    data class Api(val detail: String) : UiError(detail)
    data class Unknown(val detail: String) : UiError(detail)
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val mcpRepository: McpRepository,
    val chatRepository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _currentSessionId = savedStateHandle.getStateFlow<String?>(KEY_SESSION_ID, null)
    val currentSessionId: StateFlow<String?> = _currentSessionId

    private fun updateCurrentSessionId(id: String?) {
        savedStateHandle[KEY_SESSION_ID] = id
    }

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _inProgressToolCalls = MutableStateFlow<List<McpToolCall>>(emptyList())
    val inProgressToolCalls: StateFlow<List<McpToolCall>> = _inProgressToolCalls.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _uiError = MutableStateFlow<UiError?>(null)
    val uiError: StateFlow<UiError?> = _uiError.asStateFlow()

    private var generateJob: Job? = null

    val sessions: StateFlow<List<ChatSessionEntity>> = chatRepository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val servers: StateFlow<List<McpServer>> = mcpRepository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tools: StateFlow<List<McpTool>> = mcpRepository.allToolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) chatRepository.getMessagesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ChatUiState> = combine(
        _currentSessionId,
        sessions,
        messages,
        servers,
        tools,
        _isGenerating,
        _inProgressToolCalls,
        _uiError,
    ) { results ->
        @Suppress("UNCHECKED_CAST")
        val sessionId = results[0] as? String
        val sessionList = results[1] as List<ChatSessionEntity>
        val msgList = results[2] as List<ChatMessageEntity>
        val serverList = results[3] as List<McpServer>
        val toolList = results[4] as List<McpTool>
        val generating = results[5] as Boolean
        val toolCalls = results[6] as List<McpToolCall>
        val error = results[7] as? UiError

        ChatUiState(
            currentSessionId = sessionId,
            currentSession = sessionList.find { it.id == sessionId },
            sessions = sessionList,
            messages = msgList,
            servers = serverList,
            tools = toolList,
            isGenerating = generating,
            inProgressToolCalls = toolCalls,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    init {
        // Observe the repository flow directly: the `sessions` StateFlow seeds
        // an artificial emptyList before the DB query lands, which would
        // trigger spurious session creation on every cold start.
        viewModelScope.launch {
            chatRepository.allSessionsFlow.collect { list ->
                val current = _currentSessionId.value
                if (current.isNullOrBlank()) {
                    when {
                        list.isNotEmpty() -> {
                            val nextId = list.firstNotNullOfOrNull { it.id.takeIf(String::isNotBlank) }
                            if (nextId != null) {
                                updateCurrentSessionId(nextId)
                            } else {
                                // All ids blank (DB corruption) — recover by creating a fresh session
                                createNewChat()
                            }
                        }
                        else -> createNewChat()
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        updateCurrentSessionId(sessionId)
        _inProgressToolCalls.value = emptyList()
        _errorMessage.value = null
        _uiError.value = null
    }

    fun createNewChat(model: String = DEFAULT_MODEL) {
        viewModelScope.launch {
            val session = chatRepository.createNewSession(
                title = "New AI Chat",
                model = model,
            )
            updateCurrentSessionId(session.id)
            _inProgressToolCalls.value = emptyList()
            _errorMessage.value = null
            _uiError.value = null
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    updateCurrentSessionId(remaining.first().id)
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun updateSessionModel(model: String) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            chatRepository.updateSessionModel(sessionId, model)
        }
    }

    fun toggleServer(serverId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            mcpRepository.toggleServer(serverId, isEnabled)
        }
    }

    fun sendMessage(prompt: String) {
        val sessionId = _currentSessionId.value ?: return
        if (prompt.isBlank() || _isGenerating.value) return

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _uiError.value = null
            _inProgressToolCalls.value = emptyList()

            try {
                chatRepository.sendMessage(
                    sessionId = sessionId,
                    userPrompt = prompt.trim(),
                    onToolCallUpdate = { toolCalls ->
                        _inProgressToolCalls.value = toolCalls
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ChatException) {
                val error = when (e) {
                    is ChatException.ApiKeyMissing ->
                        UiError.Api("Gemini API key is missing. Configure it in the Secrets panel.")
                    is ChatException.RateLimit ->
                        UiError.Api("Rate limit exceeded. Please wait a moment and try again.")
                    is ChatException.Network ->
                        UiError.Network("Server unreachable. Check your network connection.")
                    is ChatException.GeminiApi ->
                        UiError.Api(e.message ?: "Gemini request failed.")
                }
                _errorMessage.value = error.message
                _uiError.value = error
            } catch (_: IOException) {
                val error = UiError.Network("Network error. Check your internet connection.")
                _errorMessage.value = error.message
                _uiError.value = error
            } catch (e: Exception) {
                val error = UiError.Unknown(e.message ?: "An error occurred during generation.")
                _errorMessage.value = error.message
                _uiError.value = error
            } finally {
                _isGenerating.value = false
                _inProgressToolCalls.value = emptyList()
            }
        }
    }

    fun cancelGeneration() {
        generateJob?.cancel()
        generateJob = null
        _isGenerating.value = false
        _inProgressToolCalls.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
        _uiError.value = null
    }

    companion object {
        private const val KEY_SESSION_ID = "current_session_id"
    }
}
