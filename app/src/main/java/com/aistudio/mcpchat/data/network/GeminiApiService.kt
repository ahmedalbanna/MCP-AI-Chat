package com.aistudio.mcpchat.data.network

import android.util.Log
import com.aistudio.mcpchat.core.network.CredentialSafeHttpClientFactory
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

class GeminiApiService(
    private val client: OkHttpClient = CredentialSafeHttpClientFactory.create(
        connectTimeoutSeconds = 60,
        readTimeoutSeconds = 60,
        writeTimeoutSeconds = 60,
    ),
) : AiChatService {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun generateContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or is placeholder.")
            return@withContext GeminiResponse(
                text = null,
                error = "API_KEY_MISSING",
            )
        }

        try {
            val toolCatalog = ProviderToolCatalog.create(tools, ProviderToolNamePolicy.GEMINI)
            val requestJson = buildRequestBody(history, systemInstruction, toolCatalog)
            val targetModel = if (modelName.contains("/")) modelName else "models/$modelName"
            val baseUrl = config.baseUrl.trimEnd('/')
                .ifBlank { "https://generativelanguage.googleapis.com/v1beta" }
            val url = "$baseUrl/$targetModel:generateContent"

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                // Header instead of ?key= query param so the key never lands
                // in server/redirect access logs.
                .header("x-goog-api-key", apiKey)
                .build()

            val response = client.newCall(request).await()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext errorResponse(modelName, response.code, responseBody)
            }

            parseGeminiResponse(responseBody, toolCatalog)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini generateContent", e)
            GeminiResponse(
                text = null,
                error = e.message ?: "Unknown error calling Gemini API",
            )
        }
    }

    private fun errorResponse(modelName: String, code: Int, responseBody: String): GeminiResponse {
        Log.e(TAG, "Gemini API failed with code $code: $responseBody")
        val errorMsg = when (code) {
            429 -> "Rate limit exceeded. Please wait a moment and try again."
            403 -> "Gemini API access denied. Check your API key permissions."
            404 -> "Model '$modelName' not found. Try a different model."
            500, 502, 503 -> "Gemini API is temporarily unavailable. Please try again later."
            else -> "Gemini API Error ($code): ${parseErrorMessage(responseBody)}"
        }
        return GeminiResponse(text = null, rawJson = responseBody, error = errorMsg)
    }

    /**
     * SSE streaming via `streamGenerateContent?alt=sse`. Each `data:` event is
     * a GenerateContentResponse chunk; text parts are forwarded through
     * [onTextDelta] as they arrive, function-call parts (which always arrive
     * complete) are accumulated for the tool loop.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override suspend fun streamContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
        onTextDelta: (String) -> Unit,
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or is placeholder.")
            return@withContext GeminiResponse(text = null, error = "API_KEY_MISSING")
        }

        try {
            val toolCatalog = ProviderToolCatalog.create(tools, ProviderToolNamePolicy.GEMINI)
            val requestJson = buildRequestBody(history, systemInstruction, toolCatalog)
            val targetModel = if (modelName.contains("/")) modelName else "models/$modelName"
            val baseUrl = config.baseUrl.trimEnd('/')
                .ifBlank { "https://generativelanguage.googleapis.com/v1beta" }
            val url = "$baseUrl/$targetModel:streamGenerateContent?alt=sse"

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .build()

            val textBuilder = StringBuilder()
            val functionCalls = mutableListOf<GeminiPart.FunctionCall>()
            var streamError: String? = null
            var streamRejected = false
            var terminalResponse: GeminiResponse? = null

            client.newCall(request).sseStreamFlow().collect { event ->
                when (event) {
                    is SseStreamEvent.Data -> {
                        try {
                            val json = JSONObject(event.payload)
                            json.optJSONObject("error")?.let { err ->
                                if (streamError == null) streamError = err.optString("message", "stream error")
                                return@collect
                            }
                            val parts = json.optJSONArray("candidates")?.optJSONObject(0)
                                ?.optJSONObject("content")?.optJSONArray("parts") ?: return@collect
                            for (i in 0 until parts.length()) {
                                val partObj = parts.getJSONObject(i)
                                if (partObj.has("text")) {
                                    val delta = partObj.getString("text")
                                    if (delta.isNotEmpty()) {
                                        textBuilder.append(delta)
                                        onTextDelta(delta)
                                    }
                                }
                                if (partObj.has("functionCall")) {
                                    functionCalls.add(parseFunctionCall(partObj, toolCatalog))
                                }
                            }
                        } catch (_: Exception) {
                            // Malformed chunk — skip it; the stream continues.
                        }
                    }

                    is SseStreamEvent.PlainBody ->
                        terminalResponse =
                            parseGeminiResponse(event.body, toolCatalog).also { parsed -> parsed.text?.let(onTextDelta) }

                    is SseStreamEvent.StreamRejected ->
                        // Endpoint rejects alt=sse — retry once without it.
                        streamRejected = true

                    is SseStreamEvent.HttpError ->
                        terminalResponse = errorResponse(modelName, event.code, event.errorBody)
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

                else -> GeminiResponse(
                    text = textBuilder.toString().takeIf { it.isNotEmpty() },
                    functionCalls = functionCalls,
                    rawJson = "",
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini streamContent", e)
            GeminiResponse(
                text = null,
                error = e.message ?: "Unknown error calling Gemini API",
            )
        }
    }

    private fun buildRequestBody(history: List<GeminiMessage>, systemInstruction: String, toolCatalog: ProviderToolCatalog): JSONObject {
        val requestJson = JSONObject()
        requestJson.put("contents", buildContents(history, toolCatalog))

        if (systemInstruction.isNotBlank()) {
            requestJson.put(
                "systemInstruction",
                JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().apply {
                            put(JSONObject().put("text", systemInstruction))
                        },
                    )
                },
            )
        }

        val functionDeclarations = buildFunctionDeclarations(toolCatalog)
        if (functionDeclarations.length() > 0) {
            requestJson.put(
                "tools",
                JSONArray().apply {
                    put(JSONObject().put("functionDeclarations", functionDeclarations))
                },
            )
        }
        return requestJson
    }

    private fun buildContents(history: List<GeminiMessage>, toolCatalog: ProviderToolCatalog): JSONArray {
        val contentsArray = JSONArray()
        for (msg in history) {
            val role = when (msg.role) {
                "assistant" -> "model"
                "tool" -> "user"
                else -> msg.role
            }
            val partsArray = JSONArray()
            for (part in msg.parts) {
                partsArray.put(
                    when (part) {
                        is GeminiPart.Text -> JSONObject().put("text", part.text)
                        is GeminiPart.InlineData -> JSONObject().put(
                            "inlineData",
                            JSONObject()
                                .put("mimeType", part.mimeType)
                                .put("data", part.base64Data),
                        )
                        is GeminiPart.FunctionCall -> functionCallPart(part, toolCatalog)
                        is GeminiPart.FunctionResponse -> functionResponsePart(part, toolCatalog)
                    },
                )
            }

            val previous = if (contentsArray.length() > 0) contentsArray.getJSONObject(contentsArray.length() - 1) else null
            if (role == "user" && previous?.optString("role") == "user") {
                val previousParts = previous.getJSONArray("parts")
                for (index in 0 until partsArray.length()) previousParts.put(partsArray.get(index))
            } else {
                contentsArray.put(JSONObject().put("role", role).put("parts", partsArray))
            }
        }
        return contentsArray
    }

    private fun functionCallPart(part: GeminiPart.FunctionCall, toolCatalog: ProviderToolCatalog): JSONObject {
        return JSONObject().apply {
            put(
                "functionCall",
                JSONObject().apply {
                    put("name", toolCatalog.wireName(part.toolId, part.name))
                    put("args", JSONObject(part.args))
                },
            )
            if (part.thoughtSignature != null) {
                put("thoughtSignature", part.thoughtSignature)
            }
        }
    }

    private fun functionResponsePart(part: GeminiPart.FunctionResponse, toolCatalog: ProviderToolCatalog): JSONObject {
        return JSONObject().apply {
            put(
                "functionResponse",
                JSONObject().apply {
                    put("name", toolCatalog.wireName(part.toolId, part.name))
                    put("response", JSONObject(part.response))
                },
            )
            if (part.thoughtSignature != null) {
                put("thoughtSignature", part.thoughtSignature)
            }
        }
    }

    private fun buildFunctionDeclarations(toolCatalog: ProviderToolCatalog): JSONArray {
        val funcDeclarations = JSONArray()
        for (tool in toolCatalog.tools) {
            try {
                val schemaObj = if (tool.inputSchemaJson.isNotBlank()) {
                    JSONObject(tool.inputSchemaJson)
                } else {
                    JSONObject().put("type", "OBJECT")
                }

                funcDeclarations.put(
                    JSONObject().apply {
                        put("name", toolCatalog.wireName(tool.id, tool.name))
                        put("description", tool.description.take(1024))
                        put("parameters", sanitizeSchemaForGemini(schemaObj))
                    },
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse schema for tool ${tool.name}: ${e.message}")
            }
        }
        return funcDeclarations
    }

    private fun parseGeminiResponse(responseBody: String, toolCatalog: ProviderToolCatalog): GeminiResponse {
        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return GeminiResponse(text = "No response candidates returned.", rawJson = responseBody)
        }

        val firstCandidate = candidates.getJSONObject(0)
        val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")

        val functionCalls = mutableListOf<GeminiPart.FunctionCall>()
        val textBuilder = StringBuilder()

        if (parts != null) {
            collectParts(parts, textBuilder, functionCalls, toolCatalog)
        }

        return GeminiResponse(
            text = if (textBuilder.isNotEmpty()) textBuilder.toString() else null,
            functionCalls = functionCalls,
            rawJson = responseBody,
        )
    }

    private fun collectParts(
        parts: JSONArray,
        textBuilder: StringBuilder,
        functionCalls: MutableList<GeminiPart.FunctionCall>,
        toolCatalog: ProviderToolCatalog,
    ) {
        for (i in 0 until parts.length()) {
            val partObj = parts.getJSONObject(i)
            if (partObj.has("text")) {
                textBuilder.append(partObj.getString("text"))
            }
            if (partObj.has("functionCall")) {
                functionCalls.add(parseFunctionCall(partObj, toolCatalog))
            }
        }
    }

    private fun parseFunctionCall(partObj: JSONObject, toolCatalog: ProviderToolCatalog): GeminiPart.FunctionCall {
        val fc = partObj.getJSONObject("functionCall")
        val argsMap = mutableMapOf<String, Any?>()
        val argsObj = fc.optJSONObject("args") ?: JSONObject()
        val keys = argsObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            argsMap[key] = argsObj.get(key)
        }
        val thoughtSignature = if (partObj.has("thoughtSignature") && !partObj.isNull("thoughtSignature")) {
            partObj.optString("thoughtSignature").takeIf { it.isNotBlank() }
        } else {
            null
        }
        return toolCatalog.functionCall(fc.getString("name"), argsMap, thoughtSignature)
    }

    private fun sanitizeSchemaForGemini(schema: JSONObject): JSONObject {
        return sanitizeSchema(schema)
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            val json = JSONObject(body)
            val err = json.optJSONObject("error")
            err?.optString("message") ?: body
        } catch (_: Exception) {
            body.take(200)
        }
    }

    companion object {
        private const val TAG = "GeminiApiService"

        // Keys allowed by the Gemini API schema specification; anything else
        // (e.g. "$schema", "additionalProperties", "exclusiveMaximum") is
        // rejected with a 400 and must be stripped before sending.
        private val allowedSchemaKeys =
            setOf(
                "type", "format", "description", "nullable", "enum", "properties",
                "required", "items", "minimum", "maximum", "minLength", "maxLength",
                "minItems", "maxItems", "example",
            )
        fun sanitizeSchema(schema: JSONObject): JSONObject {
            val sanitized = JSONObject()
            for (key in schema.keys()) {
                if (key !in allowedSchemaKeys) continue
                when (key) {
                    "type" -> sanitized.put("type", normalizeType(schema.optString(key)))
                    "properties" -> sanitized.put("properties", sanitizeProperties(schema.optJSONObject(key)))
                    "items" -> {
                        val items = schema.optJSONObject(key)
                        if (items != null) {
                            sanitized.put("items", sanitizeSchema(items))
                        }
                    }
                    else -> sanitized.put(key, schema.get(key))
                }
            }
            if (!sanitized.has("type")) {
                // Gemini rejects property schemas without an explicit type.
                if (sanitized.has("properties") || sanitized.has("required")) {
                    sanitized.put("type", "OBJECT")
                } else {
                    sanitized.put("type", "STRING")
                }
            }
            return sanitized
        }

        private fun sanitizeProperties(properties: JSONObject?): JSONObject {
            val cleanProperties = JSONObject()
            if (properties == null) return cleanProperties
            for (propKey in properties.keys()) {
                val propObj = properties.optJSONObject(propKey) ?: continue
                cleanProperties.put(propKey, sanitizeSchema(propObj))
            }
            return cleanProperties
        }

        private fun normalizeType(raw: String): String {
            return when (raw.lowercase()) {
                "string" -> "STRING"
                "number" -> "NUMBER"
                "integer" -> "INTEGER"
                "boolean" -> "BOOLEAN"
                "array" -> "ARRAY"
                "object" -> "OBJECT"
                else -> "STRING"
            }
        }
    }
}
