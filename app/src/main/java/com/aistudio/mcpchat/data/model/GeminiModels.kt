package com.aistudio.mcpchat.data.model

import org.json.JSONObject

data class GeminiMessage(
    // One of "user", "model", "tool"
    val role: String,
    val parts: List<GeminiPart>,
)

sealed class GeminiPart {
    data class Text(val text: String) : GeminiPart()
    data class InlineData(val mimeType: String, val base64Data: String) : GeminiPart()
    data class FunctionCall(
        val name: String,
        val args: Map<String, Any?>,
        val thoughtSignature: String? = null,
        val toolId: String? = null,
    ) : GeminiPart()
    data class FunctionResponse(
        val name: String,
        val response: Map<String, Any?>,
        val thoughtSignature: String? = null,
        val toolId: String? = null,
    ) : GeminiPart()
}

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JSONObject,
)

data class GeminiResponse(
    val text: String?,
    val functionCalls: List<GeminiPart.FunctionCall> = emptyList(),
    val rawJson: String = "",
    val error: String? = null,
)
