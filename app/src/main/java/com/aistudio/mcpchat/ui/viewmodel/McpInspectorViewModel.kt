package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpResource
import com.aistudio.mcpchat.data.model.McpResourceContent
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.data.repository.ResourceEvent
import com.aistudio.mcpchat.data.repository.ResourceSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class InspectorUiState(
    val servers: List<McpServer> = emptyList(),
    val tools: List<McpTool> = emptyList(),
    val logs: List<McpLogItem> = emptyList(),
    val selectedToolId: String? = null,
    val parameterInputs: Map<String, String> = emptyMap(),
    val isInvoking: Boolean = false,
    val latestResult: String? = null,
    val latestLatencyMs: Long? = null,
    val latestIsSuccess: Boolean? = null,
    val error: UiError? = null,
)

@HiltViewModel
class McpInspectorViewModel @Inject constructor(
    private val repository: McpRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val servers: StateFlow<List<McpServer>> = repository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tools: StateFlow<List<McpTool>> = repository.allToolsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<McpLogItem>> = repository.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedToolId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED_TOOL_ID, null)
    val selectedToolId: StateFlow<String?> = _selectedToolId

    private fun updateSelectedToolId(id: String?) {
        savedStateHandle[KEY_SELECTED_TOOL_ID] = id
    }

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

    private val _error = MutableStateFlow<UiError?>(null)
    val error: StateFlow<UiError?> = _error.asStateFlow()

    // ---- Resources tab ----

    private val _resourceSummaries = MutableStateFlow<List<ResourceSummary>>(emptyList())
    val resourceSummaries: StateFlow<List<ResourceSummary>> = _resourceSummaries.asStateFlow()

    private val _selectedResourceServerId = MutableStateFlow<String?>(null)
    val selectedResourceServerId: StateFlow<String?> = _selectedResourceServerId.asStateFlow()

    private val _viewedResource = MutableStateFlow<McpResource?>(null)
    val viewedResource: StateFlow<McpResource?> = _viewedResource.asStateFlow()

    private val _viewedResourceContent = MutableStateFlow<McpResourceContent?>(null)
    val viewedResourceContent: StateFlow<McpResourceContent?> = _viewedResourceContent.asStateFlow()

    private val _isLoadingResources = MutableStateFlow(false)
    val isLoadingResources: StateFlow<Boolean> = _isLoadingResources.asStateFlow()

    val subscribedResources: StateFlow<Map<String, Set<String>>> = repository.subscribedResources

    init {
        // Restore parameter defaults after process death: SavedStateHandle
        // brings back the tool id, but the schema-derived inputs are lost.
        viewModelScope.launch {
            tools.collect { list ->
                val id = _selectedToolId.value
                if (id != null && _parameterInputs.value.isEmpty()) {
                    list.find { it.id == id }?.let { tool ->
                        _parameterInputs.value = buildInitialParams(tool)
                    }
                }
            }
        }
        // Live resource view: a push for the resource currently on screen
        // swaps in the fresh content; catalog changes reload the listing.
        viewModelScope.launch {
            repository.resourceEvents.collect { event ->
                when (event) {
                    is ResourceEvent.Updated -> {
                        val viewing = _viewedResource.value
                        if (viewing != null && event.serverId == _selectedResourceServerId.value && event.uri == viewing.uri) {
                            _viewedResourceContent.value = event.content
                        }
                    }
                    is ResourceEvent.ListChanged -> {
                        if (event.serverId == _selectedResourceServerId.value) {
                            loadResources()
                        }
                    }
                }
            }
        }
    }

    fun selectResourceServer(serverId: String?) {
        _selectedResourceServerId.value = serverId
        _viewedResource.value = null
        _viewedResourceContent.value = null
        loadResources()
    }

    fun loadResources() {
        val serverId = _selectedResourceServerId.value ?: return
        viewModelScope.launch {
            val server = servers.value.find { it.id == serverId } ?: return@launch
            _isLoadingResources.value = true
            try {
                _resourceSummaries.value = repository.listResources(server)
                    .map { ResourceSummary(server.id, server.name, it) }
            } finally {
                _isLoadingResources.value = false
            }
        }
    }

    fun openResource(resource: McpResource) {
        val serverId = _selectedResourceServerId.value ?: return
        _viewedResource.value = resource
        _viewedResourceContent.value = null
        viewModelScope.launch {
            val server = servers.value.find { it.id == serverId } ?: return@launch
            _viewedResourceContent.value = repository.readResource(server, resource.uri)
        }
    }

    fun toggleSubscription(resource: McpResource) {
        val serverId = _selectedResourceServerId.value ?: return
        viewModelScope.launch {
            val server = servers.value.find { it.id == serverId } ?: return@launch
            if (repository.isSubscribed(serverId, resource.uri)) {
                repository.unsubscribeFromResource(server, resource.uri)
            } else {
                repository.subscribeToResource(server, resource.uri)
            }
        }
    }

    val uiState: StateFlow<InspectorUiState> = combine(
        servers,
        tools,
        logs,
        _selectedToolId,
        _parameterInputs,
        _isInvoking,
        _latestResult,
        _latestLatencyMs,
        _latestIsSuccess,
        _error,
    ) { results ->
        @Suppress("UNCHECKED_CAST")
        InspectorUiState(
            servers = results[0] as List<McpServer>,
            tools = results[1] as List<McpTool>,
            logs = results[2] as List<McpLogItem>,
            selectedToolId = results[3] as? String,
            parameterInputs = results[4] as Map<String, String>,
            isInvoking = results[5] as Boolean,
            latestResult = results[6] as? String,
            latestLatencyMs = results[7] as? Long,
            latestIsSuccess = results[8] as? Boolean,
            error = results[9] as? UiError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InspectorUiState())

    fun selectTool(tool: McpTool) {
        updateSelectedToolId(tool.id)
        _latestResult.value = null
        _latestLatencyMs.value = null
        _latestIsSuccess.value = null
        _parameterInputs.value = buildInitialParams(tool)
    }

    private fun buildInitialParams(tool: McpTool): Map<String, String> {
        val initialParams = mutableMapOf<String, String>()
        try {
            val schema = JSONObject(tool.inputSchemaJson)
            val properties = schema.optJSONObject("properties")
            if (properties != null) {
                val keys = properties.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val prop = properties.optJSONObject(k)
                    // Prefer an explicit default declared by the tool's schema,
                    // then fall back to sensible curated examples.
                    val schemaDefault = prop?.optString("default", "").orEmpty()
                    initialParams[k] = schemaDefault.ifBlank { curatedParamDefaults[k] ?: "" }
                }
            }
        } catch (_: Exception) { }
        return initialParams
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
            _error.value = null

            try {
                val argsMap = coerceArguments(tool, _parameterInputs.value)

                val (res, logItem) = repository.executeTool(server, tool.name, argsMap)
                _latestResult.value = res
                _latestLatencyMs.value = logItem.latencyMs
                _latestIsSuccess.value = logItem.isSuccess
            } catch (e: Exception) {
                _latestResult.value = "Error: ${e.message}"
                _latestIsSuccess.value = false
                _error.value = UiError.Unknown(e.message ?: "Tool invocation failed")
            } finally {
                _isInvoking.value = false
            }
        }
    }

    fun reExecuteTool(toolName: String, serverId: String, args: Map<String, Any?>) {
        val server = servers.value.find { it.id == serverId } ?: return
        val tool = tools.value.find { it.name == toolName && it.serverId == serverId } ?: return

        updateSelectedToolId(tool.id)
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

    private fun coerceArguments(tool: McpTool, params: Map<String, String>): Map<String, Any?> {
        // Coerce string inputs to the types declared in the tool's input
        // schema so MCP servers receive proper JSON types.
        val propTypes = try {
            val props = JSONObject(tool.inputSchemaJson).optJSONObject("properties")
            buildMap {
                props?.let { p ->
                    val keys = p.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        put(k, p.optJSONObject(k)?.optString("type")?.lowercase())
                    }
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }

        return params.mapValues { (k, v) ->
            when (propTypes[k]) {
                "integer" -> v.toIntOrNull() ?: v.toDoubleOrNull()?.toInt() ?: v
                "number" -> v.toDoubleOrNull() ?: v
                "boolean" -> v.toBooleanStrictOrNull() ?: v
                else -> v
            }
        }
    }

    companion object {
        private const val KEY_SELECTED_TOOL_ID = "selected_tool_id"

        private val curatedParamDefaults = mapOf(
            "location" to "Tokyo",
            "days" to "3",
            "query" to "Jetpack Compose",
            "topic" to "Artificial Intelligence",
            "coin_id" to "bitcoin",
            "vs_currency" to "usd",
            "from_currency" to "USD",
            "to_currency" to "EUR",
            "amount" to "500",
            "expression" to "sqrt(144) + 50 * 2",
            "value" to "100",
            "from_unit" to "km",
            "to_unit" to "miles",
            "location_or_tz" to "Europe/London",
            "city_a" to "San Francisco",
            "city_b" to "Paris",
            "key" to "project_brief",
            "content" to "MCP remote integration server protocol active",
        )
    }
}
