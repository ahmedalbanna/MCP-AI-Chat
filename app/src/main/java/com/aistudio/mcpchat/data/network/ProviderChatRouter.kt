package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.core.endpoint.EndpointPolicy
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderType
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiResponse
import com.aistudio.mcpchat.data.model.McpTool
import okhttp3.OkHttpClient

/**
 * Picks the protocol client matching the active [AiProviderConfig] and keeps
 * the requested model compatible with that provider.
 *
 * The MCP tool-execution loop is identical for every provider because all
 * clients normalize to [GeminiResponse].
 */
class ProviderChatRouter(
    client: OkHttpClient,
) : AiChatService {

    private val openAiCompatible = OpenAiCompatibleClient(client)
    private val anthropic = AnthropicClient(client)
    private val google = GeminiApiService(client)

    override suspend fun generateContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
    ): GeminiResponse {
        endpointFailure(config)?.let { failure ->
            return GeminiResponse(text = null, error = failure)
        }
        val model = resolveModel(config, modelName)

        return when (config.type) {
            AiProviderType.GOOGLE -> google.generateContent(config, model, history, tools, systemInstruction)
            AiProviderType.OPENAI_COMPATIBLE ->
                openAiCompatible.generateContent(config, model, history, tools, systemInstruction)
            AiProviderType.ANTHROPIC ->
                anthropic.generateContent(config, model, history, tools, systemInstruction)
        }
    }

    override suspend fun streamContent(
        config: AiProviderConfig,
        modelName: String,
        history: List<GeminiMessage>,
        tools: List<McpTool>,
        systemInstruction: String,
        onTextDelta: (String) -> Unit,
    ): GeminiResponse {
        endpointFailure(config)?.let { failure ->
            return GeminiResponse(text = null, error = failure)
        }
        val model = resolveModel(config, modelName)

        return when (config.type) {
            AiProviderType.GOOGLE -> google.streamContent(config, model, history, tools, systemInstruction, onTextDelta)
            AiProviderType.OPENAI_COMPATIBLE ->
                openAiCompatible.streamContent(config, model, history, tools, systemInstruction, onTextDelta)
            AiProviderType.ANTHROPIC ->
                anthropic.streamContent(config, model, history, tools, systemInstruction, onTextDelta)
        }
    }

    private fun endpointFailure(config: AiProviderConfig): String? {
        return EndpointPolicy.authorizationFailure(
            rawUrl = config.baseUrl,
            allowPrivateHttp = config.allowInsecureHttp,
        )
    }

    /**
     * Sessions persist their model string. When the active provider changes,
     * a model from a different family would be rejected — fall back to the
     * provider's own default in that case.
     */
    fun resolveModel(config: AiProviderConfig, sessionModel: String): String {
        val isGeminiModel = sessionModel.startsWith("gemini")
        return when (config.type) {
            AiProviderType.GOOGLE -> if (isGeminiModel || sessionModel.isBlank()) {
                sessionModel.ifBlank { config.model }
            } else {
                config.model
            }

            else -> if (!isGeminiModel && sessionModel.isNotBlank()) sessionModel else config.model
        }
    }
}

/** Read-only access to the user's currently selected provider. */
fun interface ActiveAiProviderSource {
    fun getActive(): AiProviderConfig
}
