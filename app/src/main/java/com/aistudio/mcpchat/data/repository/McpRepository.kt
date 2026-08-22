package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.McpLogEntity
import com.aistudio.mcpchat.data.local.McpServerEntity
import com.aistudio.mcpchat.data.local.McpToolEntity
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.network.McpHttpClient
import com.aistudio.mcpchat.data.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.URL

class McpRepository(
    private val database: AppDatabase,
    private val mcpHttpClient: McpHttpClient = McpHttpClient(),
    // Auth headers are secrets; they are encrypted before touching Room.
    private val crypto: CryptoManager = CryptoManager(),
) {
    private val serverDao = database.mcpServerDao()
    private val toolDao = database.mcpToolDao()
    private val logDao = database.mcpLogDao()

    companion object {
        private val loopbackHosts = setOf("localhost", "127.0.0.1", "[::1]", "::1")
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

    suspend fun addCustomServer(name: String, description: String, endpointUrl: String, authHeader: String = ""): McpServer =
        withContext(Dispatchers.IO) {
            validateEndpointUrl(endpointUrl)

            require(name.isNotBlank()) { "Server name cannot be empty" }

            val id = "custom_${System.currentTimeMillis()}"
            val server = McpServer(
                id = id,
                name = name,
                description = description,
                endpointUrl = endpointUrl,
                authHeader = authHeader,
                isEnabled = true,
                isPreset = false,
                status = ServerStatus.CONNECTING,
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
            lastSyncTime = System.currentTimeMillis(),
        )
        serverDao.updateServer(updatedServer.toEntity())
        tools
    }

    suspend fun executeTool(server: McpServer, toolName: String, arguments: Map<String, Any?>): Pair<String, McpLogItem> =
        withContext(Dispatchers.IO) {
            val (result, logItem) = mcpHttpClient.executeToolCall(server, toolName, arguments)
            logDao.insertLog(logItem.toEntity())
            Pair(result, logItem)
        }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        logDao.clearLogs()
    }

    private fun validateEndpointUrl(endpointUrl: String) {
        val url = try {
            URL(endpointUrl)
        } catch (e: MalformedURLException) {
            throw IllegalArgumentException("Invalid URL format: ${e.message}")
        }
        require(url.protocol == "http" || url.protocol == "https") {
            "URL must use http or https scheme"
        }
        require(!url.host.isNullOrBlank()) { "URL must have a valid host" }
        // Credentials sent over plain HTTP are exposed in transit; only allow
        // loopback endpoints (local development servers) to opt out of TLS.
        if (url.protocol == "http") {
            require(url.host.lowercase() in loopbackHosts) {
                "URL must use https (http is only allowed for localhost)"
            }
        }
    }

    private fun McpServerEntity.toModel(): McpServer {
        return McpServer(
            id = id,
            name = name,
            description = description,
            endpointUrl = endpointUrl,
            // Decrypt at read; legacy plaintext values pass through untouched.
            authHeader = crypto.decrypt(authHeader),
            isEnabled = isEnabled,
            isPreset = isPreset,
            status = try {
                ServerStatus.valueOf(status)
            } catch (_: Exception) {
                ServerStatus.DISCONNECTED
            },
            latencyMs = latencyMs,
            protocolVersion = protocolVersion,
            toolsCount = toolsCount,
            lastSyncTime = lastSyncTime,
        )
    }

    private fun McpServer.toEntity(): McpServerEntity {
        return McpServerEntity(
            id = id,
            name = name,
            description = description,
            endpointUrl = endpointUrl,
            // Encrypt at rest; already-encrypted values are left as-is.
            authHeader = crypto.encrypt(authHeader),
            isEnabled = isEnabled,
            isPreset = isPreset,
            status = status.name,
            latencyMs = latencyMs,
            protocolVersion = protocolVersion,
            toolsCount = toolsCount,
            lastSyncTime = lastSyncTime,
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
            isEnabled = isEnabled,
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
            isEnabled = isEnabled,
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
            errorMessage = errorMessage,
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
            errorMessage = errorMessage,
        )
    }
}
