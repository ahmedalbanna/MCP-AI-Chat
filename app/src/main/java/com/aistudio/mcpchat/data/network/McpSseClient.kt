package com.aistudio.mcpchat.data.network

import android.util.Log
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.TimeUnit

class McpSseClient : Closeable {
    private val TAG = "McpSseClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // SSE connections stay open
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var activeEventSource: EventSource? = null

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

    suspend fun testSseConnection(server: McpServer): Pair<ServerStatus, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(server.endpointUrl)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .applyAuthHeaders(server)
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("text/event-stream")) {
                    Pair(ServerStatus.CONNECTED, latency)
                } else {
                    Pair(ServerStatus.ERROR, latency)
                }
            } else {
                Pair(ServerStatus.ERROR, latency)
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSE connection test failed for ${server.name}: ${e.message}")
            Pair(ServerStatus.ERROR, System.currentTimeMillis() - startTime)
        }
    }

    fun listenForEvents(server: McpServer): Flow<SseEvent> = callbackFlow {
        val request = Request.Builder()
            .url(server.endpointUrl)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .applyAuthHeaders(server)
            .build()

        val factory = EventSources.createFactory(client)
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                trySend(SseEvent(
                    eventType = type ?: "message",
                    data = data,
                    id = id
                ))
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE connection failed: ${t?.message ?: response?.message}")
                trySend(SseEvent(
                    eventType = "error",
                    data = t?.message ?: "Connection lost"
                ))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        activeEventSource = factory.newEventSource(request, listener)

        awaitClose {
            activeEventSource?.cancel()
            activeEventSource = null
        }
    }

    suspend fun sendJsonRpcRequest(
        server: McpServer,
        method: String,
        params: JSONObject = JSONObject()
    ): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", (1..99999).random())
                put("method", method)
                put("params", params)
            }

            val request = Request.Builder()
                .url(server.endpointUrl)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .applyAuthHeaders(server)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotBlank()) {
                JSONObject(body)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON-RPC request failed: ${e.message}")
            null
        }
    }

    fun cancelConnection() {
        activeEventSource?.cancel()
        activeEventSource = null
    }

    override fun close() {
        cancelConnection()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

data class SseEvent(
    val eventType: String,
    val data: String,
    val id: String? = null
)
