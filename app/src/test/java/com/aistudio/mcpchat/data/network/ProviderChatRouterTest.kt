package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderType
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `remote cleartext provider is rejected before network access`() = runTest {
        var networkCalled = false
        val guardedRouter = ProviderChatRouter(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    networkCalled = true
                    chain.proceed(chain.request())
                }
                .build(),
        )
        val config = AiProviderConfig(
            name = "Insecure remote",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "http://api.example.com/v1",
            apiKey = "secret",
            model = "model",
        )

        val response = guardedRouter.generateContent(
            config = config,
            modelName = config.model,
            history = emptyList(),
            tools = emptyList(),
            systemInstruction = "",
        )

        assertEquals("Remote HTTP endpoints are not allowed; use HTTPS", response.error)
        assertFalse(networkCalled)
    }

    @Test
    fun `private cleartext provider requires an explicit persisted grant`() = runTest {
        var networkCalls = 0
        val privateRouter = ProviderChatRouter(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    networkCalls++
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(
                            """{"choices":[{"message":{"content":"done"}}]}""".toResponseBody(
                                "application/json".toMediaType(),
                            ),
                        )
                        .build()
                }
                .build(),
        )
        val config = AiProviderConfig(
            name = "Private LAN",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "http://192.168.1.20:8000/v1",
            apiKey = "secret",
            model = "model",
        )

        val rejected = privateRouter.generateContent(config, config.model, emptyList(), emptyList(), "")
        val allowed = privateRouter.generateContent(
            config.copy(allowInsecureHttp = true),
            config.model,
            emptyList(),
            emptyList(),
            "",
        )

        assertEquals("Private-network HTTP requires explicit confirmation", rejected.error)
        assertNull(allowed.error)
        assertEquals(1, networkCalls)
    }
}
