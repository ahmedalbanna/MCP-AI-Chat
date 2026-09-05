package com.aistudio.mcpchat.data.network

import android.util.Log
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.GeminiResponse
import com.aistudio.mcpchat.data.model.McpTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Talks to any OpenAI Chat Completions-compatible endpoint: OpenAI/ChatGPT,
 * OpenCode, Ollama, vLLM, Groq, OpenRouter, LM Studio, …
 *
 * Supports function calling (`tools` / `tool_calls` / `role:"tool"`).
 */
class OpenAiCompatibleClient(private val client: OkHttpClient) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): GeminiResponse = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank() && requiresApiKey(config)) {
            return@withContext GeminiResponse(text = null, error = API_KEY_MISSING)
        }

        try {
            val body = buildRequestJson(config, modelName, history, tools, systemInstruction)
            val request = Request.Builder()
                .url(config.baseUrl.trimEnd('/') + "/chat/completions")
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .apply {
                    if (config.apiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${config.apiKey}")
                    }
                }
                .build()

            client.newCall(request).await().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "OpenAI-compatible request failed (${response.code}): $responseBody")
                    return@use errorResponse(response.code, responseBody)
                }
                parseResponse(responseBody)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network error contacting ${config.name}", e)
            GeminiResponse(text = null, error = "unreachable: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error building/parsing request for ${config.name}", e)
            GeminiResponse(text = null, error = e.message ?: "Unknown provider error")
        }
    }

    /** Some local runtimes (Ollama, OpenCode) work without an API key. */
    private fun requiresApiKey(config: AiProviderConfig): Boolean = config.baseUrl.contains("api.openai.com")

    /**
     * SSE streaming via `chat/completions` with `stream:true`. Text deltas are
     * forwarded through [onTextDelta]; fragmented `tool_calls` deltas (name in
     * the first chunk, `arguments` JSON string split across later chunks) are
     * assembled by chunk index, mirroring the non-streaming [parseResponse]
     * shape.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    suspend fun streamContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
        onTextDelta: (String) -> Unit,
    ): GeminiResponse = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank() && requiresApiKey(config)) {
            return@withContext GeminiResponse(text = null, error = API_KEY_MISSING)
        }

        try {
            val body = buildRequestJson(config, modelName, history, tools, systemInstruction)
                .put("stream", true)
            val request = Request.Builder()
                .url(config.baseUrl.trimEnd('/') + "/chat/completions")
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .apply {
                    if (config.apiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${config.apiKey}")
                    }
                }
                .build()

            val textBuilder = StringBuilder()
            val toolAccumulators = LinkedHashMap<Int, ToolCallAccumulator>()
            var streamError: String? = null
            var streamRejected = false
            var terminalResponse: GeminiResponse? = null

            client.newCall(request).sseStreamFlow().collect { event ->
                when (event) {
                    is SseStreamEvent.Data -> {
                        if (event.payload == DONE_SENTINEL) return@collect
                        try {
                            val chunk = JSONObject(event.payload)
                            chunk.optJSONObject("error")?.let { err ->
                                if (streamError == null) {
                                    streamError = err.optString("message", "stream error")
                                }
                                return@collect
                            }
                            val delta = chunk.optJSONArray("choices")?.optJSONObject(0)
                                ?.optJSONObject("delta") ?: return@collect

                            val piece = delta.optString("content")
                            if (piece.isNotEmpty()) {
                                textBuilder.append(piece)
                                onTextDelta(piece)
                            }

                            delta.optJSONArray("tool_calls")?.let { toolCallDeltas ->
                                for (i in 0 until toolCallDeltas.length()) {
                                    val tc = toolCallDeltas.getJSONObject(i)
                                    val index = tc.optInt("index", 0)
                                    val acc = toolAccumulators.getOrPut(index) { ToolCallAccumulator() }
                                    tc.optJSONObject("function")?.let { fn ->
                                        val name = fn.optString("name")
                                        if (name.isNotEmpty()) acc.name = name
                                        acc.args.append(fn.optString("arguments"))
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Malformed chunk — skip it; the stream continues.
                        }
                    }

                    is SseStreamEvent.PlainBody ->
                        terminalResponse =
                            parseResponse(event.body).also { parsed -> parsed.text?.let(onTextDelta) }

                    is SseStreamEvent.StreamRejected -> {
                        Log.w(TAG, "Provider rejected stream mode; retrying without it")
                        streamRejected = true
                    }

                    is SseStreamEvent.HttpError -> {
                        Log.e(TAG, "OpenAI-compatible stream failed (${event.code}): ${event.errorBody}")
                        terminalResponse = errorResponse(event.code, event.errorBody)
                    }
                }
            }

            val error = streamError
            val terminal = terminalResponse
            when {
                streamRejected ->
                    generateContent(config, modelName, history, tools, systemInstruction)
                        .also { fallback -> fallback.text?.let(onTextDelta) }

                error != null -> GeminiResponse(text = null, rawJson = "", error = error)

                terminal != null -> terminal

                else -> {
                    val functionCalls = toolAccumulators.entries
                        .sortedBy { it.key }
                        .mapNotNull { it.value.build() }
                    GeminiResponse(
                        text = textBuilder.toString().takeIf { it.isNotBlank() },
                        functionCalls = functionCalls,
                        rawJson = "",
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network error streaming from ${config.name}", e)
            GeminiResponse(text = null, error = "unreachable: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error building/parsing stream for ${config.name}", e)
            GeminiResponse(text = null, error = e.message ?: "Unknown provider error")
        }
    }

    /** Assembles one tool call from name + concatenated argument fragments. */
    private class ToolCallAccumulator {
        var name: String = ""
        val args = StringBuilder()

        fun build(): GeminiPart.FunctionCall? {
            if (name.isBlank()) return null
            return GeminiPart.FunctionCall(name, parseArgsMap(args.toString()))
        }
    }

    @Suppress("UnusedParameter")
    private fun buildRequestJson(
        @Suppress("UNUSED_PARAMETER") config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): JSONObject {
        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        }

        // Tool-call ids must be unique across ALL rounds in one request; a
        // per-round counter would reuse "call_0" for every round and strict
        // OpenAI-compatible servers reject or mispair them. Incrementing the
        // round counter on each role:"tool" message keeps the nth-call ↔
        // nth-result pairing while staying unique.
        var turnIndex = 0
        history.forEach { message ->
            when (message.role) {
                "user" -> messages.put(buildUserMessage(message))

                "model" -> messages.put(buildAssistantMessage(message, turnIndex))

                // Gemini-style function responses -> one role:"tool" per call.
                "tool" -> {
                    message.parts.filterIsInstance<GeminiPart.FunctionResponse>()
                        .forEachIndexed { index, part ->
                            messages.put(
                                JSONObject()
                                    .put("role", "tool")
                                    .put("tool_call_id", toolCallIdFor(turnIndex, index))
                                    .put("content", JSONObject(part.response).toString()),
                            )
                        }
                    turnIndex++
                }
            }
        }

        val requestJson = JSONObject()
            .put("model", modelName)
            .put("messages", messages)

        if (tools.isNotEmpty()) {
            val declarations = JSONArray()
            tools.forEach { tool ->
                declarations.put(
                    JSONObject().put(
                        "function",
                        JSONObject()
                            .put("name", sanitizeName(tool.name))
                            .put("description", tool.description.take(1024))
                            .put(
                                "parameters",
                                if (tool.inputSchemaJson.isNotBlank()) {
                                    JSONObject(tool.inputSchemaJson)
                                } else {
                                    JSONObject().put("type", "object").put("properties", JSONObject())
                                },
                            ),
                    ),
                )
            }
            requestJson.put("tools", declarations)
            requestJson.put("tool_choice", "auto")
        }
        return requestJson
    }

    /**
     * Plain text turns stay a simple string; turns with inline images use the
     * vision content form — an array of typed blocks where each image becomes
     * a data: URI the provider decodes server-side.
     */
    private fun buildUserMessage(message: GeminiMessage): JSONObject {
        val inlineParts = message.parts.filterIsInstance<GeminiPart.InlineData>()
        if (inlineParts.isEmpty()) {
            return JSONObject()
                .put("role", "user")
                .put(
                    "content",
                    message.parts.filterIsInstance<GeminiPart.Text>()
                        .joinToString("\n") { it.text },
                )
        }

        val contentArray = JSONArray()
        message.parts.filterIsInstance<GeminiPart.Text>()
            .filter { it.text.isNotBlank() }
            .forEach { part ->
                contentArray.put(JSONObject().put("type", "text").put("text", part.text))
            }
        inlineParts.forEach { part ->
            contentArray.put(
                JSONObject().put(
                    "image_url",
                    JSONObject().put("url", "data:${part.mimeType};base64,${part.base64Data}"),
                ),
            )
        }
        return JSONObject().put("role", "user").put("content", contentArray)
    }

    private fun buildAssistantMessage(message: GeminiMessage, turnIndex: Int): JSONObject {
        val text = message.parts.filterIsInstance<GeminiPart.Text>().joinToString("\n") { it.text }
        val calls = message.parts.filterIsInstance<GeminiPart.FunctionCall>()

        val assistant = JSONObject().put("role", "assistant")
        if (calls.isEmpty()) {
            assistant.put("content", text)
        } else {
            // OpenAI allows content:null alongside tool_calls.
            assistant.put("content", text.ifBlank { null } ?: JSONObject.NULL)
            val toolCalls = JSONArray()
            calls.forEachIndexed { index, call ->
                toolCalls.put(
                    JSONObject()
                        .put("id", toolCallIdFor(turnIndex, index))
                        .put("type", "function")
                        .put(
                            "function",
                            JSONObject()
                                .put("name", sanitizeName(call.name))
                                .put("arguments", JSONObject(call.args).toString()),
                        ),
                )
            }
            assistant.put("tool_calls", toolCalls)
        }
        return assistant
    }

    private fun parseResponse(body: String): GeminiResponse {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            return GeminiResponse(text = "No response candidates returned.", rawJson = body)
        }

        val message = choices.getJSONObject(0).optJSONObject("message") ?: JSONObject()
        val functionCalls = mutableListOf<GeminiPart.FunctionCall>()

        message.optJSONArray("tool_calls")?.let { toolCalls ->
            for (i in 0 until toolCalls.length()) {
                val call = toolCalls.getJSONObject(i).getJSONObject("function")
                val args = parseArgsMap(call.optString("arguments", "{}"))
                functionCalls.add(GeminiPart.FunctionCall(call.getString("name"), args))
            }
        }

        val text = message.optString("content", "").takeIf { it.isNotBlank() }
        return GeminiResponse(text = text, functionCalls = functionCalls, rawJson = body)
    }

    private fun errorResponse(code: Int, body: String): GeminiResponse {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(300)
        }.getOrDefault(body.take(300))

        val message = when (code) {
            401 -> "Provider rejected the API key (401). Check it in AI Provider settings. Detail: $detail"
            403 -> "Provider denied access (403). $detail"
            429 -> "Rate limit exceeded (429) at the provider. Please retry shortly. $detail"
            in 500..599 -> "Provider is temporarily unavailable ($code). Please retry shortly."
            else -> "Provider error ($code): $detail"
        }
        return GeminiResponse(text = null, rawJson = body, error = message)
    }

    private fun sanitizeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64)

    companion object {
        const val TAG = "OpenAiCompatClient"
        const val API_KEY_MISSING = "API_KEY_MISSING"

        /** OpenAI-style SSE terminator: `data: [DONE]`. */
        private const val DONE_SENTINEL = "[DONE]"

        /** Deterministic turn-scoped id: pairs the nth call with the nth result of the same round. */
        fun toolCallIdFor(turn: Int, index: Int): String = "call_t${turn}_c$index"

        private fun parseArgsMap(raw: String): MutableMap<String, Any?> {
            val args = mutableMapOf<String, Any?>()
            runCatching {
                val obj = JSONObject(raw)
                obj.keys().forEach { key -> args[key] = obj.get(key) }
            }
            return args
        }
    }
}
