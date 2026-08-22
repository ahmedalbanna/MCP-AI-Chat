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
}
