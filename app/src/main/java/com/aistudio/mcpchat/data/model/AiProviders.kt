package com.aistudio.mcpchat.data.model

import java.util.UUID

/** Wire protocol / request shape used to talk to a provider. */
enum class AiProviderType {
    /** Google Gemini generative REST API. */
    GOOGLE,

    /** OpenAI Chat Completions shape: OpenAI, OpenCode, Ollama, vLLM, Groq, … */
    OPENAI_COMPATIBLE,

    /** Anthropic Messages API. */
    ANTHROPIC,
}

/** A fully user-configurable AI provider endpoint. */
data class AiProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AiProviderType,
    /** Base URL, e.g. https://api.openai.com/v1 — path suffixes are appended per protocol. */
    val baseUrl: String,
    val apiKey: String = "",
    val model: String = "",
    val allowInsecureHttp: Boolean = false,
) {
    companion object {
        fun google(apiKey: String = "", model: String = "gemini-3.6-flash") = AiProviderConfig(
            id = ID_GOOGLE,
            name = "Google Gemini",
            type = AiProviderType.GOOGLE,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            apiKey = apiKey,
            model = model,
        )

        fun openAi(apiKey: String = "", model: String = "gpt-4o-mini") = AiProviderConfig(
            id = ID_OPENAI,
            name = "OpenAI ChatGPT",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.openai.com/v1",
            apiKey = apiKey,
            model = model,
        )

        fun anthropic(apiKey: String = "", model: String = "claude-sonnet-4-5") = AiProviderConfig(
            id = ID_ANTHROPIC,
            name = "Anthropic Claude",
            type = AiProviderType.ANTHROPIC,
            baseUrl = "https://api.anthropic.com",
            apiKey = apiKey,
            model = model,
        )

        fun openCode(endpoint: String = "http://localhost:4096/v1", model: String = "opencode") = AiProviderConfig(
            id = ID_OPENCODE,
            name = "OpenCode (local)",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = endpoint,
            model = model,
        )

        fun groq(apiKey: String = "", model: String = "llama-3.1-8b-instant") = AiProviderConfig(
            id = ID_GROQ,
            name = "Groq",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.groq.com/openai/v1",
            apiKey = apiKey,
            model = model,
        )

        fun openRouter(apiKey: String = "", model: String = "meta-llama/llama-3.1-8b-instant") = AiProviderConfig(
            id = ID_OPENROUTER,
            name = "OpenRouter",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = apiKey,
            model = model,
        )

        fun ollama(endpoint: String = "http://localhost:11434/v1", model: String = "llama3.2") = AiProviderConfig(
            id = ID_OLLAMA,
            name = "Ollama (local)",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = endpoint,
            model = model,
        )

        fun custom() = AiProviderConfig(
            name = "Custom provider",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "",
        )
    }
}

object AiProviderIds {
    const val ID_GOOGLE = "preset_google"
    const val ID_OPENAI = "preset_openai"
    const val ID_ANTHROPIC = "preset_anthropic"
    const val ID_OPENCODE = "preset_opencode"
    const val ID_GROQ = "preset_groq"
    const val ID_OPENROUTER = "preset_openrouter"
    const val ID_OLLAMA = "preset_ollama"
}

private const val ID_GOOGLE = AiProviderIds.ID_GOOGLE
private const val ID_OPENAI = AiProviderIds.ID_OPENAI
private const val ID_ANTHROPIC = AiProviderIds.ID_ANTHROPIC
private const val ID_OPENCODE = AiProviderIds.ID_OPENCODE
private const val ID_GROQ = AiProviderIds.ID_GROQ
private const val ID_OPENROUTER = AiProviderIds.ID_OPENROUTER
private const val ID_OLLAMA = AiProviderIds.ID_OLLAMA
