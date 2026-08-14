package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.McpLogEntity
import com.example.data.local.McpServerEntity
import com.example.data.local.McpToolEntity
import com.example.data.model.McpLogItem
import com.example.data.model.McpServer
import com.example.data.model.McpTool
import com.example.data.model.ServerStatus
import com.example.data.network.McpHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class McpRepository(
    private val database: AppDatabase,
    private val mcpHttpClient: McpHttpClient = McpHttpClient()
) {
    private val serverDao = database.mcpServerDao()
    private val toolDao = database.mcpToolDao()
    private val logDao = database.mcpLogDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultPresetsIfEmpty()
        }
    }

    val serversFlow: Flow<List<McpServer>> = serverDao.getAllServers().map { entities ->
        entities.map { it.toModel() }
    }

    val allToolsFlow: Flow<List<McpTool>> = toolDao.getAllTools().map { entities ->
        entities.map { it.toModel() }
    }

    val logsFlow: Flow<List<McpLogItem>> = logDao.getAllLogs().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun getEnabledServers(): List<McpServer> = withContext(Dispatchers.IO) {
        serverDao.getEnabledServersList().map { it.toModel() }
    }

    suspend fun getEnabledTools(): List<McpTool> = withContext(Dispatchers.IO) {
        toolDao.getAllEnabledTools().map { it.toModel() }
    }

    suspend fun addCustomServer(
        name: String,
        description: String,
        endpointUrl: String,
        authHeader: String = ""
    ): McpServer = withContext(Dispatchers.IO) {
        val id = "custom_${System.currentTimeMillis()}"
        val server = McpServer(
            id = id,
            name = name,
            description = description,
            endpointUrl = endpointUrl,
            authHeader = authHeader,
            isEnabled = true,
            isPreset = false,
            status = ServerStatus.CONNECTING
        )
        serverDao.insertServer(server.toEntity())
        syncServerTools(server)
        server
    }

    suspend fun updateServer(server: McpServer) = withContext(Dispatchers.IO) {
        serverDao.updateServer(server.toEntity())
    }

    suspend fun toggleServer(serverId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val server = serverDao.getServerById(serverId)
        if (server != null) {
            serverDao.updateServer(server.copy(isEnabled = isEnabled))
        }
    }

    suspend fun deleteServer(serverId: String) = withContext(Dispatchers.IO) {
        toolDao.deleteToolsForServer(serverId)
        serverDao.deleteServerById(serverId)
    }

    suspend fun toggleTool(toolId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        toolDao.setToolEnabled(toolId, isEnabled)
    }

    suspend fun pingServer(server: McpServer): McpServer = withContext(Dispatchers.IO) {
        val (status, latency) = mcpHttpClient.testConnection(server)
        val updated = server.copy(status = status, latencyMs = latency)
        serverDao.updateServer(updated.toEntity())
        updated
    }

    suspend fun syncServerTools(server: McpServer): List<McpTool> = withContext(Dispatchers.IO) {
        val (status, latency) = mcpHttpClient.testConnection(server)
        val tools = mcpHttpClient.discoverTools(server)

        toolDao.deleteToolsForServer(server.id)
        if (tools.isNotEmpty()) {
            toolDao.insertTools(tools.map { it.toEntity() })
        }

        val updatedServer = server.copy(
            status = status,
            latencyMs = latency,
            toolsCount = tools.size,
            lastSyncTime = System.currentTimeMillis()
        )
        serverDao.updateServer(updatedServer.toEntity())
        tools
    }

    suspend fun executeTool(
        server: McpServer,
        toolName: String,
        arguments: Map<String, Any?>
    ): Pair<String, McpLogItem> = withContext(Dispatchers.IO) {
        val (result, logItem) = mcpHttpClient.executeToolCall(server, toolName, arguments)
        logDao.insertLog(logItem.toEntity())
        Pair(result, logItem)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        logDao.clearLogs()
    }

    private suspend fun seedDefaultPresetsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = serverDao.getEnabledServersList()
        if (existing.isNotEmpty()) return@withContext

        val presets = listOf(
            McpServer(
                id = "preset_weather",
                name = "Live Weather & Climate MCP",
                description = "Real-time atmospheric telemetry, multi-day forecasting, and global air quality indices.",
                endpointUrl = "https://mcp.weather.open-meteo.com/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 64,
                toolsCount = 3
            ),
            McpServer(
                id = "preset_search",
                name = "Web & Knowledge MCP",
                description = "Live web discovery, Wikipedia encyclopedic summaries, and readable webpage extraction.",
                endpointUrl = "https://mcp.search.duckduckgo.com/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 82,
                toolsCount = 3
            ),
            McpServer(
                id = "preset_github",
                name = "GitHub & Code Explorer MCP",
                description = "Search open-source repositories, inspect package dependencies, and fetch repository details.",
                endpointUrl = "https://api.githubmcp.io/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 95,
                toolsCount = 2
            ),
            McpServer(
                id = "preset_finance",
                name = "Crypto & Forex Markets MCP",
                description = "Real-time crypto asset tickers, market capitalization stats, and fiat forex conversion.",
                endpointUrl = "https://mcp.markets.coingecko.com/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 76,
                toolsCount = 2
            ),
            McpServer(
                id = "preset_math",
                name = "Math & Unit Computation MCP",
                description = "High-precision algebraic evaluation, trigonometric solver, and physical unit conversions.",
                endpointUrl = "https://mcp.math.engine.internal/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 12,
                toolsCount = 2
            ),
            McpServer(
                id = "preset_time",
                name = "World Time & Clocks MCP",
                description = "Accurate local time across world capital zones and timezone offset calculations.",
                endpointUrl = "https://mcp.worldtime.internal/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 24,
                toolsCount = 2
            ),
            McpServer(
                id = "preset_memory",
                name = "Local Memory & Context MCP",
                description = "Persistent key-value note storage and semantic retrieval across conversational sessions.",
                endpointUrl = "https://mcp.localmemory.internal/v1",
                isPreset = true,
                isEnabled = true,
                status = ServerStatus.CONNECTED,
                latencyMs = 18,
                toolsCount = 2
            )
        )

        for (preset in presets) {
            serverDao.insertServer(preset.toEntity())
            val tools = mcpHttpClient.discoverTools(preset)
            if (tools.isNotEmpty()) {
                toolDao.insertTools(tools.map { it.toEntity() })
            }
        }
    }

    private fun McpServerEntity.toModel(): McpServer {
        return McpServer(
            id = id,
            name = name,
            description = description,
            endpointUrl = endpointUrl,
            authHeader = authHeader,
            isEnabled = isEnabled,
            isPreset = isPreset,
            status = try { ServerStatus.valueOf(status) } catch (e: Exception) { ServerStatus.DISCONNECTED },
            latencyMs = latencyMs,
            protocolVersion = protocolVersion,
            toolsCount = toolsCount,
            lastSyncTime = lastSyncTime
        )
    }

    private fun McpServer.toEntity(): McpServerEntity {
        return McpServerEntity(
            id = id,
            name = name,
            description = description,
            endpointUrl = endpointUrl,
            authHeader = authHeader,
            isEnabled = isEnabled,
            isPreset = isPreset,
            status = status.name,
            latencyMs = latencyMs,
            protocolVersion = protocolVersion,
            toolsCount = toolsCount,
            lastSyncTime = lastSyncTime
        )
    }

    private fun McpToolEntity.toModel(): McpTool {
        return McpTool(
            id = id,
            serverId = serverId,
            serverName = serverName,
            name = name,
            description = description,
            inputSchemaJson = inputSchemaJson,
            isEnabled = isEnabled
        )
    }

    private fun McpTool.toEntity(): McpToolEntity {
        return McpToolEntity(
            id = id,
            serverId = serverId,
            serverName = serverName,
            name = name,
            description = description,
            inputSchemaJson = inputSchemaJson,
            isEnabled = isEnabled
        )
    }

    private fun McpLogEntity.toModel(): McpLogItem {
        return McpLogItem(
            id = id,
            serverId = serverId,
            serverName = serverName,
            toolName = toolName,
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            isSuccess = isSuccess,
            latencyMs = latencyMs,
            timestamp = timestamp,
            errorMessage = errorMessage
        )
    }

    private fun McpLogItem.toEntity(): McpLogEntity {
        return McpLogEntity(
            id = id,
            serverId = serverId,
            serverName = serverName,
            toolName = toolName,
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            isSuccess = isSuccess,
            latencyMs = latencyMs,
            timestamp = timestamp,
            errorMessage = errorMessage
        )
    }
}
