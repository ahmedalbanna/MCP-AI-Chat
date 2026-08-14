package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GeminiMessage
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiResponse
import com.example.data.model.McpTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiService {
    private val TAG = "GeminiApiService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateContent(
        modelName: String = "gemini-3.5-flash",
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String = ""
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or is placeholder.")
            return@withContext GeminiResponse(
                text = null,
                error = "API_KEY_MISSING"
            )
        }

        try {
            val requestJson = JSONObject()

            // Build contents
            val contentsArray = JSONArray()
            for (msg in history) {
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.role == "assistant") "model" else if (msg.role == "tool") "function" else msg.role)
                
                val partsArray = JSONArray()
                for (part in msg.parts) {
                    when (part) {
                        is GeminiPart.Text -> {
                            partsArray.put(JSONObject().put("text", part.text))
                        }
                        is GeminiPart.FunctionCall -> {
                            partsArray.put(JSONObject().apply {
                                put("functionCall", JSONObject().apply {
                                    put("name", part.name)
                                    put("args", JSONObject(part.args))
                                })
                            })
                        }
                        is GeminiPart.FunctionResponse -> {
                            partsArray.put(JSONObject().apply {
                                put("functionResponse", JSONObject().apply {
                                    put("name", part.name)
                                    put("response", JSONObject(part.response))
                                })
                            })
                        }
                    }
                }
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
            requestJson.put("contents", contentsArray)

            // System Instruction
            if (systemInstruction.isNotBlank()) {
                requestJson.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
            }

            // MCP Tools converted to Gemini function declarations
            if (tools.isNotEmpty()) {
                val funcDeclarations = JSONArray()
                for (tool in tools) {
                    try {
                        val schemaObj = if (tool.inputSchemaJson.isNotBlank()) {
                            JSONObject(tool.inputSchemaJson)
                        } else {
                            JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject())
                            }
                        }

                        // Sanitize schema for Gemini API specification
                        val sanitizedSchema = sanitizeSchemaForGemini(schemaObj)

                        funcDeclarations.put(JSONObject().apply {
                            put("name", sanitizeFunctionName(tool.name))
                            put("description", tool.description.take(1024))
                            put("parameters", sanitizedSchema)
                        })
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse schema for tool ${tool.name}: ${e.message}")
                    }
                }

                if (funcDeclarations.length() > 0) {
                    requestJson.put("tools", JSONArray().apply {
                        put(JSONObject().put("functionDeclarations", funcDeclarations))
                    })
                }
            }

            val targetModel = if (modelName.contains("/")) modelName else "models/$modelName"
            val url = "https://generativelanguage.googleapis.com/v1beta/$targetModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API failed with code ${response.code}: $responseBody")
                return@withContext GeminiResponse(
                    text = null,
                    rawJson = responseBody,
                    error = "Gemini API Error (${response.code}): ${parseErrorMessage(responseBody)}"
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResponse(
                    text = "No response candidates returned.",
                    rawJson = responseBody
                )
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val functionCalls = mutableListOf<GeminiPart.FunctionCall>()
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val partObj = parts.getJSONObject(i)
                    if (partObj.has("text")) {
                        textBuilder.append(partObj.getString("text"))
                    }
                    if (partObj.has("functionCall")) {
                        val fc = partObj.getJSONObject("functionCall")
                        val funcName = fc.getString("name")
                        val argsObj = fc.optJSONObject("args") ?: JSONObject()
                        val argsMap = mutableMapOf<String, Any?>()
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            argsMap[key] = argsObj.get(key)
                        }
                        functionCalls.add(GeminiPart.FunctionCall(funcName, argsMap))
                    }
                }
            }

            GeminiResponse(
                text = if (textBuilder.isNotEmpty()) textBuilder.toString() else null,
                functionCalls = functionCalls,
                rawJson = responseBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini generateContent", e)
            GeminiResponse(
                text = null,
                error = e.message ?: "Unknown error calling Gemini API"
            )
        }
    }

    private fun sanitizeFunctionName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(63)
    }

    private fun sanitizeSchemaForGemini(schema: JSONObject): JSONObject {
        val copy = JSONObject(schema.toString())
        if (!copy.has("type")) {
            copy.put("type", "OBJECT")
        } else {
            val typeStr = copy.optString("type").uppercase()
            copy.put("type", typeStr)
        }

        val properties = copy.optJSONObject("properties")
        if (properties != null) {
            val keys = properties.keys()
            while (keys.hasNext()) {
                val propKey = keys.next()
                val propObj = properties.optJSONObject(propKey)
                if (propObj != null) {
                    if (propObj.has("type")) {
                        propObj.put("type", propObj.getString("type").uppercase())
                    } else {
                        propObj.put("type", "STRING")
                    }
                }
            }
        }
        return copy
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            val json = JSONObject(body)
            val err = json.optJSONObject("error")
            err?.optString("message") ?: body
        } catch (e: Exception) {
            body.take(200)
        }
    }
}
