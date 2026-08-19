package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.McpServer
import com.example.data.model.McpTool
import com.example.data.repository.McpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class McpServersViewModel @Inject constructor(
    private val repository: McpRepository
) : ViewModel() {

    val servers: StateFlow<List<McpServer>> = repository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tools: StateFlow<List<McpTool>> = repository.allToolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTesting = MutableStateFlow<String?>(null)
    val isTesting: StateFlow<String?> = _isTesting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

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

    fun pingServer(server: McpServer) {
        viewModelScope.launch {
            _isTesting.value = server.id
            try {
                val updated = repository.pingServer(server)
                _feedbackMessage.value = "Ping to ${server.name}: ${updated.status} (${updated.latencyMs}ms)"
            } catch (e: Exception) {
                _feedbackMessage.value = "Failed to ping ${server.name}: ${e.message}"
            } finally {
                _isTesting.value = null
            }
        }
    }

    fun syncTools(server: McpServer) {
        viewModelScope.launch {
            _isTesting.value = server.id
            try {
                val discovered = repository.syncServerTools(server)
                _feedbackMessage.value = "Discovered ${discovered.size} tools from ${server.name}"
            } catch (e: Exception) {
                _feedbackMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isTesting.value = null
            }
        }
    }

    fun addCustomServer(
        name: String,
        description: String,
        endpointUrl: String,
        authHeader: String
    ) {
        viewModelScope.launch {
            try {
                val server = repository.addCustomServer(
                    name = name.trim(),
                    description = description.trim(),
                    endpointUrl = endpointUrl.trim(),
                    authHeader = authHeader.trim()
                )
                _feedbackMessage.value = "Added MCP Server: ${server.name}"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error adding server: ${e.message}"
            }
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            repository.deleteServer(serverId)
            _feedbackMessage.value = "Server removed."
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
