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

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "OpenAI-compatible request failed (${response.code}): $responseBody")
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

    /** Some local runtimes (Ollama, OpenCode) work without an API key. */
    private fun requiresApiKey(config: AiProviderConfig): Boolean = config.baseUrl.contains("api.openai.com")

    private fun buildRequestJson(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): JSONObject {
        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        }

        history.forEach { message ->
            when (message.role) {
                "user" -> messages.put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            message.parts.filterIsInstance<GeminiPart.Text>()
                                .joinToString("\n") { it.text },
                        ),
                )

                "model" -> messages.put(buildAssistantMessage(message))

                // Gemini-style function responses -> one role:"tool" per call.
                "tool" -> message.parts.filterIsInstance<GeminiPart.FunctionResponse>()
                    .forEachIndexed { index, part ->
                        messages.put(
                            JSONObject()
                                .put("role", "tool")
                                .put("tool_call_id", toolCallIdFor(index))
                                .put("content", JSONObject(part.response).toString()),
                        )
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

    private fun buildAssistantMessage(message: GeminiMessage): JSONObject {
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
                        .put("id", toolCallIdFor(index))
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
                val args = mutableMapOf<String, Any?>()
                call.optString("arguments", "{}").let { rawArgs ->
                    runCatching {
                        val obj = JSONObject(rawArgs)
                        obj.keys().forEach { key -> args[key] = obj.get(key) }
                    }
                }
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

        fun toolCallIdFor(index: Int): String = "call_$index"
    }
}
