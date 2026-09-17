package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderChatRouterTest {

    private val router = ProviderChatRouter(OkHttpClient())

    @Test
    fun `gemini session model kept on google provider`() {
        assertEquals("gemini-3.6-flash", router.resolveModel(AiProviderConfig.google(), "gemini-3.6-flash"))
    }

    @Test
    fun `non-gemini session model falls back to provider default on google`() {
        assertEquals("gemini-3.6-flash", router.resolveModel(AiProviderConfig.google(), "gpt-4o-mini"))
        assertEquals(
            "gemini-3.6-flash",
            router.resolveModel(AiProviderConfig.google(), "claude-sonnet-4-5"),
        )
    }

    @Test
    fun `openai-compatible keeps its family models and replaces gemini ones`() {
        val cfg = AiProviderConfig.openAi()
        assertEquals("o3-mini", router.resolveModel(cfg, "o3-mini"))
        assertEquals("llama-3.1-8b", router.resolveModel(cfg, "llama-3.1-8b"))
        // Session was created under Google; switch must swap to provider default.
        assertEquals("gpt-4o-mini", router.resolveModel(cfg, "gemini-3.6-flash"))
    }

    @Test
    fun `anthropic replaces gemini models with its default`() {
        assertEquals("claude-sonnet-4-5", router.resolveModel(AiProviderConfig.anthropic(), "gemini-3.6-flash"))
    }

    @Test
    fun `blank session model uses provider default`() {
        assertEquals("gpt-4o-mini", router.resolveModel(AiProviderConfig.openAi(), ""))
        assertEquals("gemini-3.6-flash", router.resolveModel(AiProviderConfig.google(), ""))
    }
}
