package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GeminiApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: GeminiApiService

    private val config get() = AiProviderConfig(
        name = "Test Gemini",
        type = com.aistudio.mcpchat.data.model.AiProviderType.GOOGLE,
        baseUrl = server.url("/v1beta").toString().trimEnd('/'),
        apiKey = "test-key",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = GeminiApiService(
            OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val history = listOf(
        GeminiMessage("user", listOf(GeminiPart.Text("Weather in Tokyo?"))),
    )

    @Test
    fun `streamContent emits text deltas from SSE chunks`() = runTest {
        val sseBody = buildString {
            append("""data: {"candidates":[{"content":{"parts":[{"text":"Tokyo is "}]}}]}""").append("\n\n")
            append("""data: {"candidates":[{"content":{"parts":[{"text":"22°C."}]}}]}""").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gemini-2.0-flash", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Tokyo is 22°C.", response.text)
        assertEquals(listOf("Tokyo is ", "22°C."), deltas)
        assertTrue(response.functionCalls.isEmpty())

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        assertTrue(recorded!!.path!!.contains(":streamGenerateContent"))
        assertTrue(recorded.path!!.contains("alt=sse"))
        assertEquals("test-key", recorded.getHeader("x-goog-api-key"))
        val sent = JSONObject(recorded.body.readUtf8())
        assertEquals("Weather in Tokyo?", sent.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    @Test
    fun `streamContent accumulates function calls from stream`() = runTest {
        val sseBody = buildString {
            append("""data: {"candidates":[{"content":{"parts":[{"text":"Let me check."}]}}]}""").append("\n\n")
            append(
                """data: {"candidates":[{"content":{"parts":[{"functionCall":""" +
                    """{"name":"get_weather","args":{"city":"Tokyo"}}}]}}]}""",
            ).append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val response = client.streamContent(config, "gemini-2.0-flash", history, emptyList(), "") {}

        assertEquals("Let me check.", response.text)
        assertEquals(1, response.functionCalls.size)
        assertEquals("get_weather", response.functionCalls[0].name)
        assertEquals("Tokyo", response.functionCalls[0].args["city"])
    }

    @Test
    fun `streamContent surfaces mid-stream error events`() = runTest {
        val sseBody = buildString {
            append("""data: {"candidates":[{"content":{"parts":[{"text":"partial"}]}}]}""").append("\n\n")
            append("""data: {"error":{"code":500,"message":"boom"}}""").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val response = client.streamContent(config, "gemini-2.0-flash", history, emptyList(), "") {}

        assertNull(response.text)
        assertEquals("boom", response.error)
    }

    @Test
    fun `streamContent falls back when body arrives as JSON not SSE`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"candidates":[{"content":{"parts":[{"text":"Full reply."}]}}]}"""),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gemini-2.0-flash", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Full reply.", response.text)
        assertEquals(listOf("Full reply."), deltas)
    }

    @Test
    fun `streamContent retries without stream when endpoint rejects it`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":{"message":"streaming is not supported on this model"}}"""),
        )
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"candidates":[{"content":{"parts":[{"text":"Plain reply."}]}}]}"""),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gemini-2.0-flash", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Plain reply.", response.text)
        assertEquals(listOf("Plain reply."), deltas)

        val first = server.takeRequest(10, TimeUnit.SECONDS)
        val second = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull(first)
        assertNotNull(second)
        assertTrue(first!!.path!!.contains(":streamGenerateContent"))
        assertTrue(second!!.path!!.contains(":generateContent"))
    }

    @Test
    fun `streamContent sends image turns as inlineData parts`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""data: {"candidates":[{"content":{"parts":[{"text":"A cat."}]}}]}""" + "\n\n"),
        )
        val visionHistory = listOf(
            GeminiMessage(
                "user",
                listOf(
                    GeminiPart.Text("What is in this picture?"),
                    GeminiPart.InlineData("image/png", "aGVsbG8="),
                ),
            ),
        )

        client.streamContent(config, "gemini-2.0-flash", visionHistory, emptyList(), "") {}

        val sent = JSONObject(server.takeRequest(10, TimeUnit.SECONDS)!!.body.readUtf8())
        val parts = sent.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        assertEquals("What is in this picture?", parts.getJSONObject(0).getString("text"))
        val inline = parts.getJSONObject(1).getJSONObject("inlineData")
        assertEquals("image/png", inline.getString("mimeType"))
        assertEquals("aGVsbG8=", inline.getString("data"))
    }
}
