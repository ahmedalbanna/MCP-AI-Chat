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
 * Anthropic Messages API (`/v1/messages`) with tool use (tool_use /
 * tool_result content blocks).
 */
class AnthropicClient(private val client: OkHttpClient) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): GeminiResponse = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext GeminiResponse(text = null, error = API_KEY_MISSING)
        }

        try {
            val body = buildRequestJson(config, modelName, history, tools, systemInstruction)
            // Tolerate base URLs that already carry the /v1 suffix (a common
            // habit from OpenAI-style endpoints) instead of building /v1/v1/messages.
            val base = config.baseUrl.trimEnd('/')
            val endpoint = if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"
            val request = Request.Builder()
                .url(endpoint)
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .build()

            client.newCall(request).await().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Anthropic request failed (${response.code}): $responseBody")
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

    /**
     * SSE streaming via `/v1/messages` with `stream:true`. Text deltas are
     * forwarded through [onTextDelta]; `tool_use` content blocks stream their
     * name via `content_block_start` and arguments via incremental
     * `input_json_delta` fragments, which are assembled per block index into
     * the same shape [parseResponse] produces.
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
        if (config.apiKey.isBlank()) {
            return@withContext GeminiResponse(text = null, error = API_KEY_MISSING)
        }

        try {
            val body = buildRequestJson(config, modelName, history, tools, systemInstruction)
                .put("stream", true)
            val base = config.baseUrl.trimEnd('/')
            val endpoint = if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"
            val request = Request.Builder()
                .url(endpoint)
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .build()

            client.newCall(request).executeStreaming { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Anthropic stream failed (${response.code}): $errorBody")
                    return@executeStreaming errorResponse(response.code, errorBody)
                }

                val textBuilder = StringBuilder()
                val toolBlocks = LinkedHashMap<Int, ToolUseAccumulator>()
                var stopReason: String? = null
                var streamError: String? = null

                response.forEachSseData { data ->
                    try {
                        val event = JSONObject(data)
                        when (event.optString("type")) {
                            "error" -> {
                                if (streamError == null) {
                                    streamError = event.optJSONObject("error")
                                        ?.optString("message") ?: "stream error"
                                }
                            }

                            "content_block_start" -> {
                                val block = event.optJSONObject("content_block") ?: return@forEachSseData
                                if (block.optString("type") == "tool_use") {
                                    toolBlocks[event.optInt("index", 0)] = ToolUseAccumulator(block.optString("name"))
                                }
                            }

                            "content_block_delta" -> {
                                val delta = event.optJSONObject("delta") ?: return@forEachSseData
                                when (delta.optString("type")) {
                                    "text_delta" -> {
                                        val piece = delta.optString("text")
                                        if (piece.isNotEmpty()) {
                                            textBuilder.append(piece)
                                            onTextDelta(piece)
                                        }
                                    }

                                    "input_json_delta" ->
                                        toolBlocks[event.optInt("index", 0)]
                                            ?.args?.append(delta.optString("partial_json"))
                                }
                            }

                            "message_delta" ->
                                stopReason = event.optJSONObject("delta")?.optString("stop_reason")
                        }
                    } catch (_: Exception) {
                        // Malformed chunk — skip it; the stream continues.
                    }
                }

                streamError?.let {
                    return@executeStreaming GeminiResponse(text = null, rawJson = "", error = it)
                }
                if (stopReason == "max_tokens") {
                    textBuilder.append("\n\n[Output truncated: max_tokens reached]")
                }
                val functionCalls = toolBlocks.entries
                    .sortedBy { it.key }
                    .mapNotNull { it.value.build() }
                GeminiResponse(
                    text = textBuilder.toString().takeIf { it.isNotBlank() },
                    functionCalls = functionCalls,
                    rawJson = "",
                )
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

    /** Assembles one tool_use block from name + concatenated JSON fragments. */
    private class ToolUseAccumulator(val name: String) {
        val args = StringBuilder()

        fun build(): GeminiPart.FunctionCall? {
            if (name.isBlank()) return null
            val input = mutableMapOf<String, Any?>()
            runCatching {
                val obj = JSONObject(args.toString())
                obj.keys().forEach { key -> input[key] = obj.get(key) }
            }
            return GeminiPart.FunctionCall(name, input)
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "UnusedParameter")
    private fun buildRequestJson(
        @Suppress("UNUSED_PARAMETER") config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): JSONObject {
        val messages = JSONArray()

        fun appendContent(role: String, blocks: JSONArray) {
            if (blocks.length() == 0) return
            // Anthropic requires strictly alternating user/assistant turns;
            // merge into the previous message when the role repeats.
            val last = if (messages.length() > 0) messages.getJSONObject(messages.length() - 1) else null
            if (last != null && last.optString("role") == role) {
                val content = last.getJSONArray("content")
                for (i in 0 until blocks.length()) content.put(blocks.get(i))
            } else {
                messages.put(JSONObject().put("role", role).put("content", blocks))
            }
        }

        // Tool ids must be unique across ALL turns in one request: a request
        // containing several rounds of tool use would otherwise reuse
        // "toolu_call_0" for every round, and strict servers (Anthropic,
        // AgentRouter) reject or mispair them. A turn-scoped counter keeps
        // the nth-call ↔ nth-result pairing within a round while staying
        // unique across rounds.
        var turnIndex = 0
        history.forEach { message ->
            when (message.role) {
                "user" -> {
                    val blocks = JSONArray()
                    message.parts.filterIsInstance<GeminiPart.Text>().forEach { part ->
                        blocks.put(JSONObject().put("type", "text").put("text", part.text))
                    }
                    appendContent("user", blocks)
                }

                "model" -> {
                    val blocks = JSONArray()
                    var callsEmitted = 0
                    message.parts.forEach { part ->
                        when (part) {
                            is GeminiPart.Text ->
                                blocks.put(JSONObject().put("type", "text").put("text", part.text))

                            is GeminiPart.FunctionCall ->
                                blocks.put(
                                    JSONObject()
                                        .put("type", "tool_use")
                                        .put("id", syntheticToolUseId(turnIndex, callsEmitted++))
                                        .put("name", sanitizeName(part.name))
                                        .put("input", JSONObject(part.args)),
                                )

                            is GeminiPart.FunctionResponse -> Unit // handled below as tool_result
                        }
                    }
                    appendContent("assistant", blocks)
                }

                // Function responses arrive as user-role tool_result blocks.
                "tool" -> {
                    val blocks = JSONArray()
                    message.parts.filterIsInstance<GeminiPart.FunctionResponse>()
                        .forEachIndexed { index, part ->
                            blocks.put(
                                JSONObject()
                                    .put("type", "tool_result")
                                    .put("tool_use_id", syntheticToolUseId(turnIndex, index))
                                    .put("content", JSONObject(part.response).toString()),
                            )
                        }
                    appendContent("user", blocks)
                    turnIndex++
                }
            }
        }

        val requestJson = JSONObject()
            .put("model", modelName)
            .put("max_tokens", MAX_TOKENS)
            .put("messages", messages)

        if (systemInstruction.isNotBlank()) {
            requestJson.put("system", systemInstruction)
        }

        if (tools.isNotEmpty()) {
            val declarations = JSONArray()
            tools.forEach { tool ->
                declarations.put(
                    JSONObject()
                        .put("name", sanitizeName(tool.name))
                        .put("description", tool.description.take(1024))
                        .put(
                            "input_schema",
                            if (tool.inputSchemaJson.isNotBlank()) {
                                JSONObject(tool.inputSchemaJson)
                            } else {
                                JSONObject().put("type", "object").put("properties", JSONObject())
                            },
                        ),
                )
            }
            requestJson.put("tools", declarations)
        }
        return requestJson
    }

    @Suppress("NestedBlockDepth")
    private fun parseResponse(body: String): GeminiResponse {
        val json = JSONObject(body)
        val contentBlocks = json.optJSONArray("content")

        val textBuilder = StringBuilder()
        val functionCalls = mutableListOf<GeminiPart.FunctionCall>()

        if (contentBlocks != null) {
            for (i in 0 until contentBlocks.length()) {
                val block = contentBlocks.getJSONObject(i)
                when (block.optString("type")) {
                    "text" -> textBuilder.append(block.optString("text"))
                    "tool_use" -> {
                        val inputObj = block.optJSONObject("input") ?: JSONObject()
                        val args = mutableMapOf<String, Any?>()
                        inputObj.keys().forEach { key -> args[key] = inputObj.get(key) }
                        functionCalls.add(GeminiPart.FunctionCall(block.getString("name"), args))
                    }
                }
            }
        }

        val stopReason = json.optString("stop_reason")
        if (stopReason == "max_tokens") {
            textBuilder.append("\n\n[Output truncated: max_tokens reached]")
        }

        return GeminiResponse(
            text = textBuilder.toString().takeIf { it.isNotBlank() },
            functionCalls = functionCalls,
            rawJson = body,
        )
    }

    private fun errorResponse(code: Int, body: String): GeminiResponse {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(300)
        }.getOrDefault(body.take(300))

        val message = when (code) {
            401 -> "Anthropic rejected the API key (401). Check it in AI Provider settings."
            403 -> "Anthropic denied access (403). $detail"
            429 -> "Rate limit exceeded (429) at Anthropic. Please retry shortly. $detail"
            in 500..599 -> "Anthropic is temporarily unavailable ($code). Please retry shortly."
            else -> "Anthropic error ($code): $detail"
        }
        return GeminiResponse(text = null, rawJson = body, error = message)
    }

    private fun sanitizeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64)

    companion object {
        const val TAG = "AnthropicClient"
        const val API_KEY_MISSING = "API_KEY_MISSING"
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val MAX_TOKENS = 8192

        /**
         * Deterministic turn-scoped id: pairs the nth call with the nth result
         * of the SAME round while staying unique across rounds in one request.
         */
        fun syntheticToolUseId(turn: Int, index: Int): String = "toolu_t${turn}_c$index"
    }
}
