package com.aistudio.mcpchat.data.network

import android.util.Log
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class McpHttpClient {
    private val TAG = "McpHttpClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val sessions = ConcurrentHashMap<String, String>()

    private fun Request.Builder.applyAuthHeaders(server: McpServer): Request.Builder {
        if (server.authHeader.isNotBlank()) {
            val parts = server.authHeader.split(":", limit = 2)
            if (parts.size == 2) {
                header(parts[0].trim(), parts[1].trim())
            } else {
                header("Authorization", server.authHeader.trim())
            }
        }
        return this
    }

    private fun Request.Builder.applySessionHeader(endpointUrl: String): Request.Builder {
        sessions[endpointUrl]?.let { header(HEADER_SESSION_ID, it) }
        return this
    }

    private fun buildJsonRpcRequest(server: McpServer, method: String, params: JSONObject = JSONObject()): JSONObject {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", (1..99999).random())
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

    private suspend fun postJsonRpc(
        server: McpServer,
        payload: JSONObject,
        expectResponseBody: Boolean = true
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(server.endpointUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .applyAuthHeaders(server)
                .applySessionHeader(server.endpointUrl)

            val response = client.newCall(requestBuilder.build()).execute()
            val body = if (expectResponseBody) response.body?.string() ?: "" else ""
            Pair(response.code, body)
        } catch (e: Exception) {
            Log.e(TAG, "Request failed for ${server.name}: ${e.message}")
            Pair(-1, "")
        }
    }

    private suspend fun performHandshake(server: McpServer): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val initPayload = buildJsonRpcRequest(server, "initialize", JSONObject().apply {
                put("protocolVersion", server.protocolVersion)
                put("capabilities", JSONObject().apply {
                    put("tools", JSONObject().apply { put("listChanged", true) })
                })
                put("clientInfo", JSONObject().apply {
                    put("name", "Android-MCP-Client")
                    put("version", "1.0.0")
                })
            })

            val requestBuilder = Request.Builder()
                .url(server.endpointUrl)
                .post(initPayload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .applyAuthHeaders(server)

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Initialize failed for ${server.name}: HTTP ${response.code}")
                return@withContext false
            }

            val json = extractJsonRpcBody(body)
            if (json == null || !json.has("result")) {
                Log.e(TAG, "Initialize returned invalid response for ${server.name}")
                return@withContext false
            }

            val sessionId = response.header(HEADER_SESSION_ID)
            if (!sessionId.isNullOrBlank()) {
                sessions[server.endpointUrl] = sessionId
            }

            val notification = buildJsonRpcNotification("notifications/initialized")
            val notificationRequest = Request.Builder()
                .url(server.endpointUrl)
                .post(notification.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .applyAuthHeaders(server)
                .applySessionHeader(server.endpointUrl)

            runCatching {
                client.newCall(notificationRequest.build()).execute().use { it.close() }
            }

            Log.d(TAG, "Handshake completed for ${server.name} in ${System.currentTimeMillis() - startTime}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Handshake failed for ${server.name}: ${e.message}")
            false
        }
    }

    private suspend fun ensureSession(server: McpServer): Boolean {
        if (sessions.containsKey(server.endpointUrl)) return true
        return performHandshake(server)
    }

    private fun isSessionError(json: JSONObject?): Boolean {
        if (json == null || !json.has("error")) return false
        val error = json.optJSONObject("error") ?: return false
        val code = error.optInt("code", 0)
        val message = error.optString("message", "")
        return code == -32600 || message.contains("session", ignoreCase = true)
    }

    private suspend fun callWithSessionRetry(
        server: McpServer,
        requestJson: JSONObject
    ): JSONObject? = withContext(Dispatchers.IO) {
        ensureSession(server)

        var (code, body) = postJsonRpc(server, requestJson)
        var json = if (code in 200..299) extractJsonRpcBody(body) else null

        if (isSessionError(json)) {
            Log.w(TAG, "Session expired for ${server.name}, re-initializing")
            sessions.remove(server.endpointUrl)
            if (performHandshake(server)) {
                val retry = postJsonRpc(server, requestJson)
                json = if (retry.first in 200..299) extractJsonRpcBody(retry.second) else null
            }
        }

        json
    }

    suspend fun testConnection(server: McpServer): Pair<ServerStatus, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        sessions.remove(server.endpointUrl)
        val connected = performHandshake(server)
        val latency = System.currentTimeMillis() - startTime
        if (connected) Pair(ServerStatus.CONNECTED, latency) else Pair(ServerStatus.ERROR, latency)
    }

    suspend fun discoverTools(server: McpServer): List<McpTool> = withContext(Dispatchers.IO) {
        try {
            val requestPayload = buildJsonRpcRequest(server, "tools/list")
            val json = callWithSessionRetry(server, requestPayload)

            if (json == null || json.has("error")) {
                Log.e(TAG, "Tool discovery failed on ${server.name}: ${json?.optJSONObject("error")?.optString("message")}")
                return@withContext emptyList()
            }

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
                        isEnabled = true
                    )
                )
            }
            tools
        } catch (e: Exception) {
            Log.e(TAG, "Tool discovery error on ${server.name}: ${e.message}")
            emptyList()
        }
    }

    suspend fun executeToolCall(
        server: McpServer,
        toolName: String,
        arguments: Map<String, Any?>
    ): Pair<String, McpLogItem> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val requestJson = buildJsonRpcRequest(server, "tools/call", JSONObject().apply {
            put("name", toolName)
            put("arguments", JSONObject(arguments))
        })

        var isSuccess = true
        var resultText = ""
        var errorMessage: String? = null

        try {
            val json = callWithSessionRetry(server, requestJson)
            val latency = System.currentTimeMillis() - startTime

            when {
                json == null -> {
                    isSuccess = false
                    errorMessage = "Failed to connect to remote MCP server"
                    resultText = JSONObject().put("error", errorMessage).toString()
                }
                json.has("error") -> {
                    isSuccess = false
                    errorMessage = json.getJSONObject("error").optString("message", "Unknown MCP server error")
                    resultText = JSONObject().put("error", errorMessage).toString()
                }
                else -> {
                    val resultObj = json.optJSONObject("result")
                    val isError = resultObj?.optBoolean("isError", false) ?: false
                    val contentArray = resultObj?.optJSONArray("content")
                    if (contentArray != null && contentArray.length() > 0) {
                        val textBuilder = StringBuilder()
                        for (i in 0 until contentArray.length()) {
                            val item = contentArray.getJSONObject(i)
                            if (item.optString("type") == "text") {
                                textBuilder.append(item.optString("text"))
                            }
                        }
                        resultText = textBuilder.toString()
                    } else {
                        resultText = resultObj?.toString() ?: "{}"
                    }
                    if (isError) {
                        isSuccess = false
                        errorMessage = resultText.take(MAX_ERROR_MESSAGE_LENGTH)
                    }
                }
            }
        } catch (e: Exception) {
            isSuccess = false
            errorMessage = e.message ?: "Failed to connect to remote MCP server"
            resultText = JSONObject().put("error", errorMessage).toString()
        }

        val latency = System.currentTimeMillis() - startTime
        val logItem = McpLogItem(
            serverId = server.id,
            serverName = server.name,
            toolName = toolName,
            requestPayload = requestJson.toString(2),
            responsePayload = resultText,
            isSuccess = isSuccess,
            latencyMs = latency,
            errorMessage = errorMessage
        )

        Pair(resultText, logItem)
    }

    companion object {
        private const val HEADER_SESSION_ID = "Mcp-Session-Id"
        private const val MAX_ERROR_MESSAGE_LENGTH = 500

        internal fun extractJsonRpcBody(raw: String): JSONObject? {
            if (raw.isBlank()) return null
            val trimmed = raw.trim()

            if (trimmed.startsWith("{")) {
                return try {
                    JSONObject(trimmed)
                } catch (e: Exception) {
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
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
        }
    }
}
