package com.aistudio.mcpchat.data.network

import android.util.Log
import com.aistudio.mcpchat.core.endpoint.EndpointPolicy
import com.aistudio.mcpchat.core.network.CredentialSafeHttpClientFactory
import com.aistudio.mcpchat.data.model.McpHeaderCodec
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpResource
import com.aistudio.mcpchat.data.model.McpResourceContent
import com.aistudio.mcpchat.data.model.McpResourceNotification
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// One class per MCP server connection: session lifecycle, tool RPCs and
// resource RPCs share the session/circuit/retry machinery, so the size is
// accepted rather than splitting state across helpers.
@Suppress("TooManyFunctions")
class McpHttpClient(
    private val retryManager: RetryManager = RetryManager(),
    private val circuitBreaker: CircuitBreaker = CircuitBreaker(),
    private val client: OkHttpClient = CredentialSafeHttpClientFactory.create(
        connectTimeoutSeconds = 15,
        readTimeoutSeconds = 30,
        writeTimeoutSeconds = 30,
    ),
    // Long-lived SSE listening channel: read timeout is deliberately much
    // laxer than the RPC client so a quiet (but healthy) connection isn't
    // torn down between server keep-alives.
    private val streamingClient: OkHttpClient = CredentialSafeHttpClientFactory.create(
        connectTimeoutSeconds = 15,
        readTimeoutSeconds = 300,
        writeTimeoutSeconds = 30,
    ),
    // Exponential reconnect base for the SSE channel; injectable to keep
    // reconnect tests fast.
    private val reconnectBaseDelayMs: Long = 5_000,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** Exposes the default OkHttpClient for unit-test assertions on timeout contract. */
    internal val defaultClientForTesting: OkHttpClient get() = client

    private val sessions = ConcurrentHashMap<String, SessionEntry>()

    /**
     * Human-readable reason for the most recent handshake failure per server
     * (e.g. "HTTP 401", "Failed to connect to …"). Cleared on success. Used to
     * surface WHY a ping/sync failed instead of a bare ERROR status.
     */
    private val lastHandshakeErrors = ConcurrentHashMap<String, String>()

    fun lastHandshakeError(serverId: String): String? = lastHandshakeErrors[serverId]

    private val rpcIdCounter = AtomicLong(0)

    private fun Request.Builder.applyAuthHeaders(server: McpServer): Request.Builder {
        // Supports the JSON multi-header form and the legacy single-value form
        // ("Name: value" or a bare Authorization value).
        McpHeaderCodec.decode(server.authHeader).forEach { (name, value) ->
            header(name, value)
        }
        return this
    }

    private fun Request.Builder.applySessionHeader(sessionKey: String): Request.Builder {
        sessions[sessionKey]?.let { header(HEADER_SESSION_ID, it.sessionId) }
        return this
    }

    // Sessions must be scoped to endpoint AND credentials so two servers that
    // share a URL but not an auth header never exchange session ids.
    private fun sessionKey(server: McpServer): String = "${server.endpointUrl}|${server.authHeader}"

    private fun nextRpcId(): Long = rpcIdCounter.incrementAndGet()

    private fun buildJsonRpcRequest(method: String, params: JSONObject = JSONObject()): JSONObject {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", nextRpcId())
            put("method", method)
            put("params", params)
        }
    }

    private fun buildJsonRpcNotification(method: String, params: JSONObject = JSONObject()): JSONObject {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
        }
    }

    private fun endpointFailure(server: McpServer): String? {
        return EndpointPolicy.authorizationFailure(
            rawUrl = server.endpointUrl,
            allowPrivateHttp = server.allowInsecureHttp,
        )
    }

    private fun requireEndpointAllowed(server: McpServer) {
        endpointFailure(server)?.let { throw IllegalArgumentException(it) }
    }

    private fun buildPostRequest(server: McpServer, payload: JSONObject): Request {
        requireEndpointAllowed(server)
        return Request.Builder()
            .url(server.endpointUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .applyAuthHeaders(server)
            .build()
    }

    /** POST carrying the MCP session id for an already-initialized server. */
    private fun buildSessionPostRequest(server: McpServer, payload: JSONObject): Request {
        requireEndpointAllowed(server)
        val key = sessionKey(server)
        return Request.Builder()
            .url(server.endpointUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .applyAuthHeaders(server)
            .applySessionHeader(key)
            .build()
    }

    private suspend fun postWithSession(server: McpServer, payload: JSONObject): Pair<Int, String> = withContext(Dispatchers.IO) {
        client.newCall(buildSessionPostRequest(server, payload)).await().use { response ->
            Pair(response.code, response.body?.string() ?: "")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun performHandshake(server: McpServer): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            endpointFailure(server)?.let { failure ->
                lastHandshakeErrors[server.id] = failure
                return@withContext false
            }
            val initPayload = buildJsonRpcRequest(
                "initialize",
                JSONObject().apply {
                    put("protocolVersion", server.protocolVersion)
                    put(
                        "capabilities",
                        JSONObject().apply {
                            put("tools", JSONObject().apply { put("listChanged", true) })
                        },
                    )
                    put(
                        "clientInfo",
                        JSONObject().apply {
                            put("name", "Android-MCP-Client")
                            put("version", "1.0.0")
                        },
                    )
                },
            )

            val response = retryManager.executeWithRetry("initialize ${server.name}") {
                client.newCall(buildPostRequest(server, initPayload)).await()
            }
            val body = response.body?.string() ?: ""
            val statusCode = response.code
            val sessionIdHeader = response.header(HEADER_SESSION_ID)
            response.close()

            if (statusCode !in 200..299) {
                Log.e(TAG, "Initialize failed for ${server.name}: HTTP $statusCode")
                lastHandshakeErrors[server.id] = "HTTP $statusCode"
                return@withContext false
            }

            val json = extractJsonRpcBody(body)
            if (json == null || !json.has("result")) {
                Log.e(TAG, "Initialize returned invalid response for ${server.name}")
                lastHandshakeErrors[server.id] =
                    "HTTP $statusCode but response is not a JSON-RPC result"
                return@withContext false
            }

            if (!sessionIdHeader.isNullOrBlank()) {
                sessions[sessionKey(server)] = SessionEntry(sessionIdHeader)
            }

            val notification = buildJsonRpcNotification("notifications/initialized")
            try {
                // await() is cancellable — rethrow CE so Stop propagates
                // instead of being swallowed as a best-effort failure.
                client.newCall(buildPostRequest(server, notification)).await().close()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort notification — failures don't block the handshake.
            }

            Log.d(TAG, "Handshake completed for ${server.name} in ${System.currentTimeMillis() - startTime}ms")
            lastHandshakeErrors.remove(server.id)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Handshake failed for ${server.name}: ${e.message}")
            lastHandshakeErrors[server.id] = e.message ?: e.javaClass.simpleName
            false
        }
    }

    private suspend fun ensureSession(server: McpServer): Boolean {
        val entry = sessions[sessionKey(server)]
        if (entry != null && !entry.isExpired(server.sessionTtlMs)) return true
        if (entry != null) sessions.remove(sessionKey(server))
        return performHandshake(server)
    }

    private fun isSessionError(json: JSONObject?): Boolean {
        if (json == null || !json.has("error")) return false
        val error = json.optJSONObject("error") ?: return false
        val code = error.optInt("code", 0)
        val message = error.optString("message", "")
        return code == -32600 || message.contains("session", ignoreCase = true)
    }

    private suspend fun callWithSessionRetry(server: McpServer, requestJson: JSONObject, retriable: Boolean): JSONObject? =
        withContext(Dispatchers.IO) {
            if (!ensureSession(server)) return@withContext null

            val send: suspend () -> Pair<Int, String> = {
                try {
                    if (retriable) {
                        // Idempotent calls only (initialize, tools/list) — never
                        // tools/call, which may have side effects.
                        retryManager.executeWithRetry("POST ${server.name}") {
                            postWithSession(server, requestJson)
                        }
                    } else {
                        postWithSession(server, requestJson)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Request failed for ${server.name}: ${e.message}")
                    Pair(-1, "")
                }
            }

            var (code, body) = send()
            var json = if (code in 200..299) extractJsonRpcBody(body) else null

            if (isSessionError(json)) {
                Log.w(TAG, "Session expired for ${server.name}, re-initializing")
                sessions.remove(sessionKey(server))
                if (performHandshake(server)) {
                    val retry = send()
                    json = if (retry.first in 200..299) extractJsonRpcBody(retry.second) else null
                }
            }

            json
        }

    private fun shouldSkipDueToCircuit(server: McpServer): Boolean {
        if (!circuitBreaker.canExecute(server.id)) {
            Log.w(TAG, "Circuit open for ${server.name}, skipping call")
            return true
        }
        return false
    }

    /** GET carrying the MCP session id — the Streamable HTTP listening channel. */
    private fun buildSessionGetRequest(server: McpServer): Request {
        requireEndpointAllowed(server)
        val key = sessionKey(server)
        return Request.Builder()
            .url(server.endpointUrl)
            .get()
            .header("Accept", "text/event-stream")
            .applyAuthHeaders(server)
            .applySessionHeader(key)
            .build()
    }

    suspend fun testConnection(server: McpServer): Pair<ServerStatus, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        sessions.remove(sessionKey(server))
        val connected = performHandshake(server)
        val latency = System.currentTimeMillis() - startTime
        if (connected) {
            circuitBreaker.recordSuccess(server.id)
            Pair(ServerStatus.CONNECTED, latency)
        } else {
            circuitBreaker.recordFailure(server.id)
            Pair(ServerStatus.ERROR, latency)
        }
    }

    suspend fun discoverTools(server: McpServer): List<McpTool> = withContext(Dispatchers.IO) {
        if (shouldSkipDueToCircuit(server)) {
            lastHandshakeErrors[server.id] = "circuit breaker open — retry after a cool-down"
            return@withContext emptyList()
        }
        try {
            val requestPayload = buildJsonRpcRequest("tools/list")
            val json = callWithSessionRetry(server, requestPayload, retriable = true)

            if (json == null || json.has("error")) {
                Log.e(TAG, "Tool discovery failed on ${server.name}: ${json?.optJSONObject("error")?.optString("message")}")
                circuitBreaker.recordFailure(server.id)
                return@withContext emptyList()
            }

            circuitBreaker.recordSuccess(server.id)
            val result = json.optJSONObject("result") ?: JSONObject()
            val toolsArray = result.optJSONArray("tools") ?: JSONArray()
            val tools = mutableListOf<McpTool>()

            for (i in 0 until toolsArray.length()) {
                val toolObj = toolsArray.getJSONObject(i)
                val name = toolObj.optString("name")
                val desc = toolObj.optString("description", "No description provided.")
                val inputSchema = toolObj.optJSONObject("inputSchema")?.toString() ?: "{}"
                tools.add(
                    McpTool(
                        id = "${server.id}_$name",
                        serverId = server.id,
                        serverName = server.name,
                        name = name,
                        description = desc,
                        inputSchemaJson = inputSchema,
                        isEnabled = true,
                    ),
                )
            }
            tools
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tool discovery error on ${server.name}: ${e.message}")
            circuitBreaker.recordFailure(server.id)
            emptyList()
        }
    }

    private data class ToolCallResult(
        val text: String,
        val isSuccess: Boolean,
        val errorMessage: String?,
    )

    private fun errorResult(message: String): ToolCallResult = ToolCallResult(
        text = JSONObject().put("error", message).toString(),
        isSuccess = false,
        errorMessage = message,
    )

    private fun parseToolResponse(json: JSONObject?): ToolCallResult {
        return when {
            json == null -> errorResult("Failed to connect to remote MCP server")
            json.has("error") -> {
                val message = json.getJSONObject("error").optString("message", "Unknown MCP server error")
                errorResult(message)
            }
            else -> {
                val resultObj = json.optJSONObject("result")
                val isError = resultObj?.optBoolean("isError", false) ?: false
                val text = extractContentText(resultObj)
                if (isError) {
                    ToolCallResult(text, false, text.take(MAX_ERROR_MESSAGE_LENGTH))
                } else {
                    ToolCallResult(text, true, null)
                }
            }
        }
    }

    private fun extractContentText(resultObj: JSONObject?): String {
        val contentArray = resultObj?.optJSONArray("content")
        if (contentArray != null && contentArray.length() > 0) {
            val textBuilder = StringBuilder()
            for (i in 0 until contentArray.length()) {
                val item = contentArray.getJSONObject(i)
                if (item.optString("type") == "text") {
                    textBuilder.append(item.optString("text"))
                }
            }
            return textBuilder.toString()
        }
        return resultObj?.toString() ?: "{}"
    }

    suspend fun executeToolCall(server: McpServer, toolName: String, arguments: Map<String, Any?>): Pair<String, McpLogItem> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val requestJson = buildJsonRpcRequest(
                "tools/call",
                JSONObject().apply {
                    put("name", toolName)
                    put("arguments", JSONObject(arguments))
                },
            )

            val result = when {
                shouldSkipDueToCircuit(server) -> errorResult("Server ${server.name} is temporarily unavailable")
                else -> try {
                    parseToolResponse(callWithSessionRetry(server, requestJson, retriable = false))
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorResult(e.message ?: "Failed to connect to remote MCP server")
                }
            }

            if (result.isSuccess) {
                circuitBreaker.recordSuccess(server.id)
            } else {
                circuitBreaker.recordFailure(server.id)
            }

            val latency = System.currentTimeMillis() - startTime
            val logItem = McpLogItem(
                serverId = server.id,
                serverName = server.name,
                toolName = toolName,
                requestPayload = requestJson.toString(2),
                responsePayload = result.text,
                isSuccess = result.isSuccess,
                latencyMs = latency,
                errorMessage = result.errorMessage,
            )

            Pair(result.text, logItem)
        }

    // ---- MCP resources (list / read / subscribe) ----

    @Suppress("CyclomaticComplexMethod")
    suspend fun listResources(server: McpServer): List<McpResource> = withContext(Dispatchers.IO) {
        if (shouldSkipDueToCircuit(server)) return@withContext emptyList()
        try {
            val json = callWithSessionRetry(server, buildJsonRpcRequest("resources/list"), retriable = true)
            if (json == null || json.has("error")) {
                Log.e(TAG, "Resource listing failed on ${server.name}: ${json?.optJSONObject("error")?.optString("message")}")
                circuitBreaker.recordFailure(server.id)
                return@withContext emptyList()
            }
            circuitBreaker.recordSuccess(server.id)
            val result = json.optJSONObject("result") ?: JSONObject()
            val resourcesArray = result.optJSONArray("resources") ?: JSONArray()
            val resources = mutableListOf<McpResource>()
            for (i in 0 until resourcesArray.length()) {
                val resObj = resourcesArray.getJSONObject(i)
                val uri = resObj.optString("uri")
                if (uri.isBlank()) continue
                resources.add(
                    McpResource(
                        uri = uri,
                        name = resObj.optString("name", uri),
                        description = resObj.optString("description").takeIf { it.isNotBlank() },
                        mimeType = resObj.optString("mimeType").takeIf { it.isNotBlank() },
                    ),
                )
            }
            resources
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Resource listing error on ${server.name}: ${e.message}")
            circuitBreaker.recordFailure(server.id)
            emptyList()
        }
    }

    suspend fun readResource(server: McpServer, uri: String): McpResourceContent? = withContext(Dispatchers.IO) {
        try {
            val requestPayload = buildJsonRpcRequest(
                "resources/read",
                JSONObject().put("uri", uri),
            )
            val json = callWithSessionRetry(server, requestPayload, retriable = false)
            if (json == null || json.has("error")) {
                Log.e(TAG, "Resource read failed on ${server.name} ($uri): ${json?.optJSONObject("error")?.optString("message")}")
                return@withContext null
            }
            val contents = json.optJSONObject("result")?.optJSONArray("contents") ?: return@withContext null
            val content = contents.optJSONObject(0) ?: return@withContext null
            val contentUri = content.optString("uri", uri)
            val mimeType = content.optString("mimeType").takeIf { it.isNotBlank() }
            val text = if (content.has("text")) {
                content.optString("text")
            } else {
                val blobLength = content.optString("blob").length
                "(binary resource, ~${blobLength * 3 / 4} bytes)"
            }
            McpResourceContent(uri = contentUri, mimeType = mimeType, text = text)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Resource read error on ${server.name} ($uri): ${e.message}")
            null
        }
    }

    /**
     * Registers a resources/subscribe subscription server-side. Returns
     * false when the server rejects it (unknown method, unsupported
     * capability) — callers then treat the resource as poll-only.
     */
    suspend fun subscribeResource(server: McpServer, uri: String): Boolean {
        val json = callWithSessionRetry(
            server,
            buildJsonRpcRequest("resources/subscribe", JSONObject().put("uri", uri)),
            retriable = false,
        )
        return json != null && !json.has("error")
    }

    /** Best-effort: servers may drop subscriptions on disconnect anyway. */
    suspend fun unsubscribeResource(server: McpServer, uri: String): Boolean {
        val json = callWithSessionRetry(
            server,
            buildJsonRpcRequest("resources/unsubscribe", JSONObject().put("uri", uri)),
            retriable = false,
        )
        return json != null && !json.has("error")
    }

    /**
     * The server-push listening channel for one server: a GET SSE stream
     * (MCP Streamable HTTP) that delivers notifications/resources/updated
     * and notifications/resources/list_changed as they happen.
     *
     * - Transport hiccups (IOException, 5xx, 429) reconnect with capped
     *   exponential backoff.
     * - 404/405 or other 4xx mean the server offers no listening channel —
     *   the flow completes so callers stop instead of retry-storming.
     * - Collecting-scope cancellation tears the connection down immediately.
     */
    fun resourceChannelFlow(server: McpServer): Flow<McpResourceNotification> = flow {
        if (endpointFailure(server) != null) return@flow
        var delayMs = reconnectBaseDelayMs.coerceAtLeast(1)
        while (currentCoroutineContext().isActive) {
            val call = streamingClient.newCall(buildSessionGetRequest(server))
            var channelAvailable = true
            try {
                call.sseStreamFlow().collect { event ->
                    when (event) {
                        is SseStreamEvent.Data -> parseResourceNotification(event.payload)?.let { emit(it) }
                        is SseStreamEvent.PlainBody -> channelAvailable = false
                        is SseStreamEvent.StreamRejected -> channelAvailable = false
                        is SseStreamEvent.HttpError -> channelAvailable = event.code == 429 || event.code >= 500
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IOException) {
                // Connection dropped — reconnect below.
                Log.d(TAG, "Resource channel dropped for ${server.name}: ${e.message}")
            }
            if (!channelAvailable) {
                Log.d(TAG, "Server ${server.name} offers no SSE listening channel; stopping")
                return@flow
            }
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
    }

    private fun parseResourceNotification(payload: String): McpResourceNotification? {
        val json = try {
            JSONObject(payload)
        } catch (_: Exception) {
            return null
        }
        return when (json.optString("method")) {
            "notifications/resources/updated" -> json.optJSONObject("params")?.optString("uri")
                ?.takeIf { it.isNotBlank() }
                ?.let { McpResourceNotification.Updated(it) }
            "notifications/resources/list_changed" -> McpResourceNotification.ListChanged
            else -> null
        }
    }

    companion object {
        private const val TAG = "McpHttpClient"
        private const val HEADER_SESSION_ID = "Mcp-Session-Id"
        private const val MAX_ERROR_MESSAGE_LENGTH = 500
        private const val DEFAULT_SESSION_TTL_MS = 5 * 60_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L

        private class SessionEntry(
            var sessionId: String,
            var createdAt: Long = System.currentTimeMillis(),
        ) {
            fun isExpired(ttlMs: Long): Boolean {
                val effectiveTtl = if (ttlMs > 0) ttlMs else DEFAULT_SESSION_TTL_MS
                return System.currentTimeMillis() - createdAt > effectiveTtl
            }
        }

        internal fun extractJsonRpcBody(raw: String): JSONObject? {
            if (raw.isBlank()) return null
            val trimmed = raw.trim()

            if (trimmed.startsWith("{")) {
                return try {
                    JSONObject(trimmed)
                } catch (_: Exception) {
                    null
                }
            }

            return trimmed.lines()
                .filter { it.startsWith("data:") }
                .reversed()
                .firstNotNullOfOrNull { line ->
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) {
                        null
                    } else {
                        try {
                            JSONObject(payload)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
        }
    }
}
