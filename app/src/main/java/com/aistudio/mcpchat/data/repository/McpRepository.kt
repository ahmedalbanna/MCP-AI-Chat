package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.McpLogEntity
import com.aistudio.mcpchat.data.local.McpServerEntity
import com.aistudio.mcpchat.data.local.McpToolEntity
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpResource
import com.aistudio.mcpchat.data.model.McpResourceContent
import com.aistudio.mcpchat.data.model.McpResourceNotification
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.network.McpHttpClient
import com.aistudio.mcpchat.data.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** One resource together with the server that exposes it. */
data class ResourceSummary(
    val serverId: String,
    val serverName: String,
    val resource: McpResource,
)

/** Push-driven resource lifecycle emitted through [McpRepository.resourceEvents]. */
sealed interface ResourceEvent {
    /** A subscribed resource changed; [content] is the fresh read (null if the re-read failed). */
    data class Updated(
        val serverId: String,
        val serverName: String,
        val uri: String,
        val content: McpResourceContent?,
    ) : ResourceEvent

    /** The server's resource catalog changed — cached listings are invalid. */
    data class ListChanged(val serverId: String, val serverName: String) : ResourceEvent
}

// Single facade over the MCP data domain (servers, tools, logs, resources);
// splitting it would scatter one DAO family's concerns across repositories.
@Suppress("TooManyFunctions")
class McpRepository(
    private val database: AppDatabase,
    private val mcpHttpClient: McpHttpClient = McpHttpClient(),
    // Auth headers are secrets; they are encrypted before touching Room.
    private val crypto: CryptoManager = CryptoManager(),
    // Owns the per-server SSE listening jobs; injectable for tests.
    private val resourceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val serverDao = database.mcpServerDao()
    private val toolDao = database.mcpToolDao()
    private val logDao = database.mcpLogDao()

    companion object {
        private val loopbackHosts = setOf("localhost", "127.0.0.1", "[::1]", "::1", "10.0.2.2")

        fun isInsecureEndpoint(endpointUrl: String): Boolean {
            return try {
                val url = URL(endpointUrl)
                url.protocol.equals("http", ignoreCase = true) &&
                    url.host.lowercase() !in loopbackHosts
            } catch (_: Exception) {
                false
            }
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

    suspend fun updateCustomServer(serverId: String, name: String, description: String, endpointUrl: String, authHeader: String = ""): McpServer =
        withContext(Dispatchers.IO) {
            validateEndpointUrl(endpointUrl)
            require(name.isNotBlank()) { "Server name cannot be empty" }

            val existing = serverDao.getServerById(serverId)
                ?: throw IllegalArgumentException("Server not found: $serverId")

            val endpointChanged = existing.endpointUrl != endpointUrl ||
                crypto.decrypt(existing.authHeader) != authHeader

            val updatedEntity = existing.copy(
                name = name,
                description = description,
                endpointUrl = endpointUrl,
                // Entities store the encrypted secret; the model below decrypts it.
                authHeader = crypto.encrypt(authHeader),
            )
            serverDao.updateServer(updatedEntity)

            val model = updatedEntity.toModel()
            if (endpointChanged) {
                // Cached tools belong to the old endpoint/credentials — drop them
                // and re-handshake so the tool list matches the new configuration.
                toolDao.deleteToolsForServer(serverId)
                syncServerTools(model)
            }
            model
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
        stopResourceListener(serverId)
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

    /** Why the most recent handshake for this server failed, if it did. */
    fun lastConnectionError(serverId: String): String? = mcpHttpClient.lastHandshakeError(serverId)

    suspend fun syncServerTools(server: McpServer): List<McpTool> = withContext(Dispatchers.IO) {
        val (status, latency) = mcpHttpClient.testConnection(server)
        // Only discover tools after a successful handshake; otherwise keep cached tools.
        val tools = if (status == ServerStatus.CONNECTED) {
            mcpHttpClient.discoverTools(server)
        } else {
            emptyList()
        }

        val updatedServer = if (status == ServerStatus.CONNECTED) {
            // Successful handshake — replace cached tools (even if zero, the server may legitimately have no tools).
            toolDao.deleteToolsForServer(server.id)
            if (tools.isNotEmpty()) {
                toolDao.insertTools(tools.map { it.toEntity() })
            }
            server.copy(
                status = status,
                latencyMs = latency,
                toolsCount = tools.size,
                lastSyncTime = System.currentTimeMillis(),
            )
        } else {
            // Transient failure — preserve cached tools and count, only update status/latency.
            server.copy(
                status = status,
                latencyMs = latency,
            )
        }
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

    // ---- MCP resources: listing, reading, subscriptions, push updates ----

    /**
     * Hot stream of resource lifecycle events. Emissions are best-effort —
     * collectors refresh caches/UI from the payload rather than relying on
     * event ordering.
     */
    private val _resourceEvents = MutableSharedFlow<ResourceEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val resourceEvents: SharedFlow<ResourceEvent> = _resourceEvents.asSharedFlow()

    /** serverId -> uris currently subscribed server-side. */
    private val _subscribedResources = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val subscribedResources: StateFlow<Map<String, Set<String>>> = _subscribedResources.asStateFlow()

    private val resourceListCache = ConcurrentHashMap<String, List<McpResource>>()
    private val resourceContentCache = ConcurrentHashMap<String, McpResourceContent>()
    private val listenerJobs = ConcurrentHashMap<String, Job>()

    private fun resourceKey(serverId: String, uri: String) = "$serverId|$uri"

    fun isSubscribed(serverId: String, uri: String): Boolean = uri in (_subscribedResources.value[serverId] ?: emptySet())

    suspend fun listResources(server: McpServer): List<McpResource> = withContext(Dispatchers.IO) {
        val resources = mcpHttpClient.listResources(server)
        resourceListCache[server.id] = resources
        resources
    }

    suspend fun readResource(server: McpServer, uri: String, forceRefresh: Boolean = false): McpResourceContent? = withContext(Dispatchers.IO) {
        val key = resourceKey(server.id, uri)
        if (!forceRefresh) resourceContentCache[key]?.let { return@withContext it }
        val content = mcpHttpClient.readResource(server, uri) ?: return@withContext resourceContentCache[key]
        resourceContentCache[key] = content
        content
    }

    /**
     * Registers a subscription server-side and (on success) ensures the
     * server's SSE listening channel is running so pushes arrive.
     */
    suspend fun subscribeToResource(server: McpServer, uri: String): Boolean = withContext(Dispatchers.IO) {
        val registered = mcpHttpClient.subscribeResource(server, uri)
        if (registered) {
            val current = _subscribedResources.value.toMutableMap()
            current[server.id] = (current[server.id] ?: emptySet()) + uri
            _subscribedResources.value = current
            ensureResourceListener(server)
        }
        registered
    }

    suspend fun unsubscribeFromResource(server: McpServer, uri: String) = withContext(Dispatchers.IO) {
        val current = _subscribedResources.value.toMutableMap()
        val remaining = (current[server.id] ?: emptySet()) - uri
        if (remaining.isEmpty()) current.remove(server.id) else current[server.id] = remaining
        _subscribedResources.value = current
        // Best-effort; servers drop subscriptions on session loss anyway.
        mcpHttpClient.unsubscribeResource(server, uri)
        if (remaining.isEmpty()) stopResourceListener(server.id)
        Unit
    }

    fun stopResourceListener(serverId: String) {
        listenerJobs.remove(serverId)?.cancel()
        resourceListCache.remove(serverId)
        resourceContentCache.keys.removeIf { it.startsWith("$serverId|") }
    }

    fun clearResourceListeners() {
        listenerJobs.keys.toList().forEach(::stopResourceListener)
    }

    private fun ensureResourceListener(server: McpServer) {
        listenerJobs.computeIfAbsent(server.id) {
            resourceScope.launch {
                mcpHttpClient.resourceChannelFlow(server).collect { notification ->
                    handleResourceNotification(server, notification)
                }
            }
        }
    }

    private suspend fun handleResourceNotification(server: McpServer, notification: McpResourceNotification) {
        when (notification) {
            is McpResourceNotification.Updated -> {
                if (notification.uri !in (_subscribedResources.value[server.id] ?: emptySet())) return
                // Subscribed resource changed — re-read immediately so the
                // cache and event carry the fresh content, not a stale copy.
                val content = mcpHttpClient.readResource(server, notification.uri)
                if (content != null) {
                    resourceContentCache[resourceKey(server.id, notification.uri)] = content
                }
                _resourceEvents.tryEmit(
                    ResourceEvent.Updated(server.id, server.name, notification.uri, content),
                )
            }
            is McpResourceNotification.ListChanged -> {
                resourceListCache.remove(server.id)
                _resourceEvents.tryEmit(ResourceEvent.ListChanged(server.id, server.name))
            }
        }
    }

    /** All resources across enabled servers — the AI-facing catalog. */
    suspend fun listAllResourceSummaries(): List<ResourceSummary> = withContext(Dispatchers.IO) {
        getEnabledServers().flatMap { server ->
            mcpHttpClient.listResources(server).map { ResourceSummary(server.id, server.name, it) }
        }
    }

    /**
     * Reads one resource for use in chat. [serverName] narrows the search
     * when known; without it every enabled server is tried until one owns
     * the uri.
     */
    suspend fun readResourceForChat(serverName: String?, uri: String): McpResourceContent? = withContext(Dispatchers.IO) {
        val servers = getEnabledServers()
        val candidates = if (serverName.isNullOrBlank()) {
            servers
        } else {
            servers.filter { it.name.equals(serverName.trim(), ignoreCase = true) }
        }
        for (server in candidates) {
            mcpHttpClient.readResource(server, uri)?.let { content ->
                resourceContentCache[resourceKey(server.id, uri)] = content
                return@withContext content
            }
        }
        null
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
        // Plain HTTP is allowed but flagged as insecure — callers must confirm
        // via isInsecureEndpoint() before persisting. Loopback / emulator hosts
        // (localhost, 127.0.0.1, ::1, 10.0.2.2) are exempt from the warning.
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
            sessionTtlMs = sessionTtlMs,
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
            sessionTtlMs = sessionTtlMs,
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
