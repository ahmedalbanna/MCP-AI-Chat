package com.example.data.network

import android.util.Log
import com.example.data.model.McpLogItem
import com.example.data.model.McpServer
import com.example.data.model.McpTool
import com.example.data.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class McpHttpClient {
    private val TAG = "McpHttpClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

    private fun buildJsonRpcRequest(server: McpServer, method: String, params: JSONObject = JSONObject()): JSONObject {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", (1..99999).random())
            put("method", method)
            put("params", params)
        }
    }

    suspend fun testConnection(server: McpServer): Pair<ServerStatus, Long> = withContext(Dispatchers.IO) {
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
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                Pair(ServerStatus.CONNECTED, latency)
            } else {
                Pair(ServerStatus.ERROR, latency)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed for ${server.name}: ${e.message}")
            Pair(ServerStatus.ERROR, System.currentTimeMillis() - startTime)
        }
    }

    suspend fun discoverTools(server: McpServer): List<McpTool> = withContext(Dispatchers.IO) {
        try {
            val requestPayload = buildJsonRpcRequest(server, "tools/list")

            val requestBuilder = Request.Builder()
                .url(server.endpointUrl)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .applyAuthHeaders(server)

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful || body.isBlank()) {
                return@withContext emptyList()
            }

            val json = JSONObject(body)
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
            val requestBuilder = Request.Builder()
                .url(server.endpointUrl)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .applyAuthHeaders(server)

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                if (json.has("error")) {
                    isSuccess = false
                    errorMessage = json.getJSONObject("error").optString("message", "Unknown MCP server error")
                    resultText = JSONObject().put("error", errorMessage).toString()
                } else {
                    val resultObj = json.optJSONObject("result")
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
                        resultText = resultObj?.toString() ?: body
                    }
                }
            } else {
                isSuccess = false
                errorMessage = "HTTP ${response.code}: ${response.message}"
                resultText = JSONObject().put("error", errorMessage).toString()
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
}
