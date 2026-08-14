package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.model.McpServer
import com.example.data.model.McpTool
import com.example.data.model.McpToolCall
import com.example.data.repository.ChatRepository
import com.example.data.repository.McpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentSession: ChatSessionEntity? = null,
    val sessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val activeServers: List<McpServer> = emptyList(),
    val allTools: List<McpTool> = emptyList(),
    val isGenerating: Boolean = false,
    val inProgressToolCalls: List<McpToolCall> = emptyList(),
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val mcpRepository = McpRepository(database)
    val chatRepository = ChatRepository(database, mcpRepository)

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _inProgressToolCalls = MutableStateFlow<List<McpToolCall>>(emptyList())
    val inProgressToolCalls: StateFlow<List<McpToolCall>> = _inProgressToolCalls.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = chatRepository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val servers: StateFlow<List<McpServer>> = mcpRepository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tools: StateFlow<List<McpTool>> = mcpRepository.allToolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) chatRepository.getMessagesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Auto-select latest session or create one
            sessions.collect { list ->
                if (_currentSessionId.value == null) {
                    if (list.isNotEmpty()) {
                        _currentSessionId.value = list.first().id
                    } else {
                        createNewChat()
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _inProgressToolCalls.value = emptyList()
        _errorMessage.value = null
    }

    fun createNewChat(model: String = "gemini-3.5-flash") {
        viewModelScope.launch {
            val session = chatRepository.createNewSession(
                title = "New AI Chat",
                model = model
            )
            _currentSessionId.value = session.id
            _inProgressToolCalls.value = emptyList()
            _errorMessage.value = null
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().id
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

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _inProgressToolCalls.value = emptyList()

            try {
                chatRepository.sendMessage(
                    sessionId = sessionId,
                    userPrompt = prompt.trim(),
                    onToolCallUpdate = { toolCalls ->
                        _inProgressToolCalls.value = toolCalls
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred during generation."
            } finally {
                _isGenerating.value = false
                _inProgressToolCalls.value = emptyList()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
