package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.repository.McpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class McpInspectorViewModel @Inject constructor(
    private val repository: McpRepository
) : ViewModel() {

    val servers: StateFlow<List<McpServer>> = repository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tools: StateFlow<List<McpTool>> = repository.allToolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<McpLogItem>> = repository.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedToolId = MutableStateFlow<String?>(null)
    val selectedToolId: StateFlow<String?> = _selectedToolId.asStateFlow()

    private val _parameterInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val parameterInputs: StateFlow<Map<String, String>> = _parameterInputs.asStateFlow()

    private val _isInvoking = MutableStateFlow(false)
    val isInvoking: StateFlow<Boolean> = _isInvoking.asStateFlow()

    private val _latestResult = MutableStateFlow<String?>(null)
    val latestResult: StateFlow<String?> = _latestResult.asStateFlow()

    private val _latestLatencyMs = MutableStateFlow<Long?>(null)
    val latestLatencyMs: StateFlow<Long?> = _latestLatencyMs.asStateFlow()

    private val _latestIsSuccess = MutableStateFlow<Boolean?>(null)
    val latestIsSuccess: StateFlow<Boolean?> = _latestIsSuccess.asStateFlow()

    fun selectTool(tool: McpTool) {
        _selectedToolId.value = tool.id
        _latestResult.value = null
        _latestLatencyMs.value = null
        _latestIsSuccess.value = null

        // Prepopulate default parameters from schema
        val initialParams = mutableMapOf<String, String>()
        try {
            val schema = JSONObject(tool.inputSchemaJson)
            val properties = schema.optJSONObject("properties")
            if (properties != null) {
                val keys = properties.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val prop = properties.optJSONObject(k)
                    val defaultVal = when (k) {
                        "location" -> "Tokyo"
                        "days" -> "3"
                        "query" -> "Jetpack Compose"
                        "topic" -> "Artificial Intelligence"
                        "coin_id" -> "bitcoin"
                        "vs_currency" -> "usd"
                        "from_currency" -> "USD"
                        "to_currency" -> "EUR"
                        "amount" -> "500"
                        "expression" -> "sqrt(144) + 50 * 2"
                        "value" -> "100"
                        "from_unit" -> "km"
                        "to_unit" -> "miles"
                        "location_or_tz" -> "Europe/London"
                        "city_a" -> "San Francisco"
                        "city_b" -> "Paris"
                        "key" -> "project_brief"
                        "content" -> "MCP remote integration server protocol active"
                        else -> prop?.optString("default", "") ?: ""
                    }
                    initialParams[k] = defaultVal
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        _parameterInputs.value = initialParams
    }

    fun updateParameter(key: String, value: String) {
        val current = _parameterInputs.value.toMutableMap()
        current[key] = value
        _parameterInputs.value = current
    }

    fun invokeSelectedTool() {
        val toolId = _selectedToolId.value ?: return
        val tool = tools.value.find { it.id == toolId } ?: return
        val server = servers.value.find { it.id == tool.serverId } ?: return

        viewModelScope.launch {
            _isInvoking.value = true
            _latestResult.value = null
            _latestLatencyMs.value = null
            _latestIsSuccess.value = null

            try {
                val argsMap = mutableMapOf<String, Any?>()
                for ((k, v) in _parameterInputs.value) {
                    val num = v.toDoubleOrNull()
                    if (num != null && (k == "days" || k == "amount" || k == "value")) {
                        if (num % 1.0 == 0.0) {
                            argsMap[k] = num.toInt()
                        } else {
                            argsMap[k] = num
                        }
                    } else {
                        argsMap[k] = v
                    }
                }

                val (res, logItem) = repository.executeTool(server, tool.name, argsMap)
                _latestResult.value = res
                _latestLatencyMs.value = logItem.latencyMs
                _latestIsSuccess.value = logItem.isSuccess
            } catch (e: Exception) {
                _latestResult.value = "Error: ${e.message}"
                _latestIsSuccess.value = false
            } finally {
                _isInvoking.value = false
            }
        }
    }

    fun reExecuteTool(toolName: String, serverId: String, args: Map<String, Any?>) {
        val server = servers.value.find { it.id == serverId } ?: return
        val tool = tools.value.find { it.name == toolName && it.serverId == serverId } ?: return

        _selectedToolId.value = tool.id
        val paramMap = mutableMapOf<String, String>()
        args.forEach { (k, v) -> paramMap[k] = v?.toString() ?: "" }
        _parameterInputs.value = paramMap

        viewModelScope.launch {
            _isInvoking.value = true
            _latestResult.value = null
            _latestLatencyMs.value = null
            _latestIsSuccess.value = null

            try {
                val (res, logItem) = repository.executeTool(server, tool.name, args)
                _latestResult.value = res
                _latestLatencyMs.value = logItem.latencyMs
                _latestIsSuccess.value = logItem.isSuccess
            } catch (e: Exception) {
                _latestResult.value = "Error: ${e.message}"
                _latestIsSuccess.value = false
            } finally {
                _isInvoking.value = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }
}
