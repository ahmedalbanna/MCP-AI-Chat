package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.McpTool
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

    @Test
    fun `generateContent returns API_KEY_MISSING for blank key`() = runTest {
        val noKey = config.copy(apiKey = "  ")

        val response = client.generateContent(noKey, "gemini-2.0-flash", history, emptyList(), "")

        assertNull(response.text)
        assertEquals("API_KEY_MISSING", response.error)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `generateContent returns API_KEY_MISSING for placeholder key`() = runTest {
        val placeholder = config.copy(apiKey = "MY_GEMINI_API_KEY")

        val response = client.generateContent(placeholder, "gemini-2.0-flash", history, emptyList(), "")

        assertNull(response.text)
        assertEquals("API_KEY_MISSING", response.error)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `generateContent parses text response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"candidates":[{"content":{"parts":[{"text":"Sunny, 22°C."}]}}]}"""),
        )

        val response = client.generateContent(config, "gemini-2.0-flash", history, emptyList(), "")

        assertEquals("Sunny, 22°C.", response.text)
        assertNull(response.error)

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertTrue(recorded!!.path!!.contains("models/gemini-2.0-flash:generateContent"))
        assertEquals("test-key", recorded.getHeader("x-goog-api-key"))
    }

    @Test
    fun `generateContent parses function calls`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(
                    """{"candidates":[{"content":{"parts":[{"functionCall":{"name":"get_weather","args":{"city":"Tokyo"}}}]}}]}""",
                ),
        )

        val response = client.generateContent(config, "gemini-2.0-flash", history, emptyList(), "")

        assertNull(response.text)
        assertEquals(1, response.functionCalls.size)
        assertEquals("get_weather", response.functionCalls[0].name)
        assertEquals("Tokyo", response.functionCalls[0].args["city"])
    }

    @Test
    fun `generateContent resolves wire function names to original tool identity`() = runTest {
        val tool = McpTool(
            id = "weather_tool",
            serverId = "weather",
            serverName = "Weather",
            name = "get-weather",
            description = "Current weather",
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"candidates":[{"content":{"parts":[{"functionCall":{"name":"get_weather","args":{}}}]}}]}""",
            ),
        )

        val response = client.generateContent(config, "gemini-2.0-flash", history, listOf(tool), "")

        assertEquals("get-weather", response.functionCalls.single().name)
        assertEquals(tool.id, response.functionCalls.single().toolId)
    }

    @Test
    fun `generateContent maps rate limit to retry message`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))

        val response = client.generateContent(config, "gemini-2.0-flash", history, emptyList(), "")

        assertNull(response.text)
        assertNotNull(response.error)
        assertTrue(response.error!!.contains("Rate limit"))
    }

    @Test
    fun `generateContent maps access denied and missing model`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
        val denied = client.generateContent(config, "gemini-2.0-flash", history, emptyList(), "")
        assertTrue(denied.error!!.contains("access denied", ignoreCase = true))

        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
        val missing = client.generateContent(config, "gemini-2.0-flash", history, emptyList(), "")
        assertTrue(missing.error!!.contains("gemini-2.0-flash"))
    }

    @Test
    fun `generateContent surfaces server message for unknown codes`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":{"message":"request contains an invalid argument"}}"""),
        )

        val response = client.generateContent(config, "gemini-2.0-flash", history, emptyList(), "")

        assertNull(response.text)
        assertTrue(response.error!!.contains("400"))
        assertTrue(response.error!!.contains("invalid argument"))
    }

    @Test
    fun `generateContent serializes multi-turn history tools and system prompt`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"candidates":[{"content":{"parts":[{"text":"Done."}]}}]}"""),
        )
        val tools = listOf(
            McpTool(
                id = "srv_1_get-weather",
                serverId = "srv_1",
                serverName = "Weather MCP",
                name = "get-weather",
                description = "Current weather",
                inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"}},"additionalProperties":false}""",
            ),
        )
        val multiTurn = listOf(
            GeminiMessage("user", listOf(GeminiPart.Text("Weather?"))),
            GeminiMessage(
                "assistant",
                listOf(
                    GeminiPart.FunctionCall(
                        name = "get_weather",
                        args = mapOf("city" to "Tokyo"),
                        thoughtSignature = "sig-1",
                        toolId = "srv_1_get-weather",
                    ),
                ),
            ),
            GeminiMessage(
                "tool",
                listOf(
                    GeminiPart.FunctionResponse(
                        name = "get_weather",
                        response = mapOf("temp" to 22),
                        thoughtSignature = "sig-1",
                        toolId = "srv_1_get-weather",
                    ),
                ),
            ),
        )

        val response = client.generateContent(config, "gemini-2.0-flash", multiTurn, tools, "Be concise.")

        assertEquals("Done.", response.text)

        val sent = JSONObject(server.takeRequest(10, TimeUnit.SECONDS)!!.body.readUtf8())
        assertEquals(
            "Be concise.",
            sent.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"),
        )
        val contents = sent.getJSONArray("contents")
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals("user", contents.getJSONObject(2).getString("role"))
        val callPart = contents.getJSONObject(1).getJSONArray("parts").getJSONObject(0)
        assertEquals("get_weather", callPart.getJSONObject("functionCall").getString("name"))
        assertEquals("sig-1", callPart.getString("thoughtSignature"))
        val responsePart = contents.getJSONObject(2).getJSONArray("parts").getJSONObject(0)
        assertEquals("get_weather", responsePart.getJSONObject("functionResponse").getString("name"))
        val declarations = sent.getJSONArray("tools").getJSONObject(0).getJSONArray("functionDeclarations")
        assertEquals("get_weather", declarations.getJSONObject(0).getString("name"))
        val params = declarations.getJSONObject(0).getJSONObject("parameters")
        assertEquals("OBJECT", params.getString("type"))
        assertEquals("STRING", params.getJSONObject("properties").getJSONObject("city").getString("type"))
        assertTrue(!params.has("additionalProperties"))
    }

    @Test
    fun `generateContent merges a tool response and following user text into one user turn`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"candidates":[{"content":{"parts":[{"text":"Done."}]}}]}"""),
        )
        val exchange = listOf(
            GeminiMessage("user", listOf(GeminiPart.Text("Use the tool"))),
            GeminiMessage("assistant", listOf(GeminiPart.FunctionCall("lookup", emptyMap()))),
            GeminiMessage("tool", listOf(GeminiPart.FunctionResponse("lookup", mapOf("result" to "ok")))),
            GeminiMessage("user", listOf(GeminiPart.Text("Now summarize it"))),
        )

        val response = client.generateContent(config, "gemini-2.0-flash", exchange, emptyList(), "")

        assertEquals("Done.", response.text)
        val contents = JSONObject(server.takeRequest(10, TimeUnit.SECONDS)!!.body.readUtf8()).getJSONArray("contents")
        assertEquals(3, contents.length())
        val finalUserTurn = contents.getJSONObject(2)
        assertEquals("user", finalUserTurn.getString("role"))
        assertEquals(2, finalUserTurn.getJSONArray("parts").length())
        assertEquals(
            "Now summarize it",
            finalUserTurn.getJSONArray("parts").getJSONObject(1).getString("text"),
        )
    }
}
