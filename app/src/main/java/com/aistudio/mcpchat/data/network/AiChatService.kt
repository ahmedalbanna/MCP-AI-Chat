package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiResponse
import com.aistudio.mcpchat.data.model.McpTool

/**
 * A chat-completion backend capable of multi-turn function calling.
 *
 * [GeminiResponse] is the canonical internal result shape regardless of the
 * wire protocol, so the MCP tool-execution loop in [com.aistudio.mcpchat.data
 * .repository.ChatRepository] stays provider-agnostic.
 */
interface AiChatService {
    suspend fun generateContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): GeminiResponse

    /**
     * Streaming variant: emits partial text through [onTextDelta] as tokens
     * arrive over SSE and returns the complete response — including any
     * function calls — once the stream ends. Function-call arguments always
     * arrive complete (assembled by the implementation), so the repository's
     * tool loop is unchanged.
     *
     * The default implementation degrades to non-streaming and emits the
     * whole text as a single delta, keeping mock/test backends working.
     */
    suspend fun streamContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
        onTextDelta: (String) -> Unit,
    ): GeminiResponse {
        val response = generateContent(config, modelName, history, tools, systemInstruction)
        response.text?.let { onTextDelta(it) }
        return response
    }
}
