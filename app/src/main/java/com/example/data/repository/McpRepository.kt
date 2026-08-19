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
            ensureErpNextPresetExists()
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

        val erpnext = McpServer(
            id = "preset_erpnext",
            name = "ERPNext Gateway",
            description = "Enterprise Resource Planning gateway for ERPNext, providing tools for document management, accounting, and business automation.",
            endpointUrl = "https://8033-01ky8gme6ek5wava50hemgxjk9.cloudspaces.litng.ai/mcp",
            authHeader = "gateway-dev-token",
            isPreset = false,
            isEnabled = true,
            status = ServerStatus.DISCONNECTED,
            latencyMs = 0,
            toolsCount = 0
        )

        serverDao.insertServer(erpnext.toEntity())
        val tools = mcpHttpClient.discoverTools(erpnext)
        if (tools.isNotEmpty()) {
            toolDao.insertTools(tools.map { it.toEntity() })
        }
    }

    private suspend fun ensureErpNextPresetExists() = withContext(Dispatchers.IO) {
        val serverId = "preset_erpnext"
        val existing = serverDao.getServerById(serverId)
        val erpnext = McpServer(
            id = serverId,
            name = "ERPNext Gateway",
            description = "Enterprise Resource Planning gateway for ERPNext, providing tools for document management, accounting, and business automation.",
            endpointUrl = "https://8033-01ky8gme6ek5wava50hemgxjk9.cloudspaces.litng.ai/mcp",
            authHeader = "gateway-dev-token",
            isPreset = false,
            isEnabled = true,
            status = ServerStatus.DISCONNECTED,
            latencyMs = 0,
            toolsCount = 0
        )

        if (existing == null) {
            serverDao.insertServer(erpnext.toEntity())
            syncServerTools(erpnext)
        } else {
            val existingModel = existing.toModel()
            if (existingModel.authHeader != erpnext.authHeader || 
                existingModel.endpointUrl != erpnext.endpointUrl || 
                existingModel.isPreset) {
                
                val updated = erpnext.copy(
                    status = ServerStatus.CONNECTING,
                    toolsCount = existingModel.toolsCount
                )
                serverDao.updateServer(updated.toEntity())
                syncServerTools(updated)
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
