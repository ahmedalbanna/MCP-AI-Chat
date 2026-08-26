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
            val request = Request.Builder()
                .url(config.baseUrl.trimEnd('/') + "/v1/messages")
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Anthropic request failed (${response.code}): $responseBody")
                    return@use errorResponse(response.code, responseBody)
                }
                parseResponse(responseBody)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error contacting ${config.name}", e)
            GeminiResponse(text = null, error = "unreachable: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error building/parsing request for ${config.name}", e)
            GeminiResponse(text = null, error = e.message ?: "Unknown provider error")
        }
    }

    private fun buildRequestJson(
        config: AiProviderConfig,
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
                                        .put("id", syntheticToolUseId(callsEmitted++))
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
                                    .put("tool_use_id", syntheticToolUseId(index))
                                    .put("content", JSONObject(part.response).toString()),
                            )
                        }
                    appendContent("user", blocks)
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

        /** Deterministic positional id: pairs nth call with nth result per turn. */
        fun syntheticToolUseId(index: Int): String = "toolu_call_$index"
    }
}
