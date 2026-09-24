package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.repository.McpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServersUiState(
    val servers: List<McpServer> = emptyList(),
    val tools: List<McpTool> = emptyList(),
    val isTestingServerId: String? = null,
    val feedback: FeedbackMessage? = null,
)

data class FeedbackMessage(
    val text: String,
    val isError: Boolean = false,
)

@HiltViewModel
class McpServersViewModel @Inject constructor(
    private val repository: McpRepository,
) : ViewModel() {

    val servers: StateFlow<List<McpServer>> = repository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tools: StateFlow<List<McpTool>> = repository.allToolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTesting = MutableStateFlow<String?>(null)
    val isTesting: StateFlow<String?> = _isTesting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<FeedbackMessage?>(null)
    val feedbackMessage: StateFlow<FeedbackMessage?> = _feedbackMessage.asStateFlow()

    val uiState: StateFlow<ServersUiState> = combine(
        servers,
        tools,
        _isTesting,
        _feedbackMessage,
    ) { serverList, toolList, testingId, feedback ->
        ServersUiState(
            servers = serverList,
            tools = toolList,
            isTestingServerId = testingId,
            feedback = feedback,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServersUiState())

    fun toggleServer(serverId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleServer(serverId, isEnabled)
        }
    }

    fun toggleTool(toolId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleTool(toolId, isEnabled)
        }
    }

    suspend fun pingServerSuspend(server: McpServer) {
        _isTesting.value = server.id
        try {
            val updated = repository.pingServer(server)
            val reason = repository.lastConnectionError(server.id)
            _feedbackMessage.value = FeedbackMessage(
                "Ping to ${server.name}: ${updated.status}" +
                    (reason?.let { " — $it" } ?: "") +
                    " (${updated.latencyMs}ms)",
                isError = updated.status != ServerStatus.CONNECTED,
            )
        } catch (e: Exception) {
            _feedbackMessage.value = FeedbackMessage(
                "Failed to ping ${server.name}: ${e.message}",
                isError = true,
            )
        } finally {
            _isTesting.value = null
        }
    }

    fun pingServer(server: McpServer) {
        viewModelScope.launch { pingServerSuspend(server) }
    }

    suspend fun syncToolsSuspend(server: McpServer) {
        _isTesting.value = server.id
        try {
            val discovered = repository.syncServerTools(server)
            val reason = repository.lastConnectionError(server.id)
            _feedbackMessage.value = if (discovered.isEmpty() && reason != null) {
                FeedbackMessage(
                    "Sync failed for ${server.name}: $reason",
                    isError = true,
                )
            } else {
                FeedbackMessage(
                    "Discovered ${discovered.size} tools from ${server.name}",
                )
            }
        } catch (e: Exception) {
            _feedbackMessage.value = FeedbackMessage(
                "Sync failed: ${e.message}",
                isError = true,
            )
        } finally {
            _isTesting.value = null
        }
    }

    fun syncTools(server: McpServer) {
        viewModelScope.launch { syncToolsSuspend(server) }
    }

    fun addCustomServer(name: String, description: String, endpointUrl: String, authHeader: String, allowInsecureHttp: Boolean = false) {
        viewModelScope.launch {
            try {
                val server = repository.addCustomServer(
                    name = name.trim(),
                    description = description.trim(),
                    endpointUrl = endpointUrl.trim(),
                    authHeader = authHeader.trim(),
                    allowInsecureHttp = allowInsecureHttp,
                )
                _feedbackMessage.value = FeedbackMessage("Added MCP Server: ${server.name}")
            } catch (e: IllegalArgumentException) {
                _feedbackMessage.value = FeedbackMessage(
                    "Error: ${e.message}",
                    isError = true,
                )
            } catch (e: Exception) {
                _feedbackMessage.value = FeedbackMessage(
                    "Error adding server: ${e.message}",
                    isError = true,
                )
            }
        }
    }

    fun updateServer(
        server: McpServer,
        name: String,
        description: String,
        endpointUrl: String,
        authHeader: String,
        allowInsecureHttp: Boolean = false,
    ) {
        viewModelScope.launch {
            try {
                repository.updateCustomServer(
                    serverId = server.id,
                    name = name.trim(),
                    description = description.trim(),
                    endpointUrl = endpointUrl.trim(),
                    authHeader = authHeader.trim(),
                    allowInsecureHttp = allowInsecureHttp,
                )
                _feedbackMessage.value = FeedbackMessage("Updated MCP Server: ${name.trim()}")
            } catch (e: IllegalArgumentException) {
                _feedbackMessage.value = FeedbackMessage(
                    "Error: ${e.message}",
                    isError = true,
                )
            } catch (e: Exception) {
                _feedbackMessage.value = FeedbackMessage(
                    "Error updating server: ${e.message}",
                    isError = true,
                )
            }
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            repository.deleteServer(serverId)
            _feedbackMessage.value = FeedbackMessage("Server removed.")
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
