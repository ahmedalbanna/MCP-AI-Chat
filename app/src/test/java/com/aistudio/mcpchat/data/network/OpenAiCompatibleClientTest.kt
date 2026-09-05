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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenAiCompatibleClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OpenAiCompatibleClient

    private val config get() = AiProviderConfig(
        name = "Test OpenAI",
        type = com.aistudio.mcpchat.data.model.AiProviderType.OPENAI_COMPATIBLE,
        baseUrl = server.url("/v1").toString().trimEnd('/'),
        apiKey = "sk-test",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OpenAiCompatibleClient(
            OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val history = listOf(
        GeminiMessage("user", listOf(GeminiPart.Text("Weather in Tokyo?"))),
        GeminiMessage(
            "model",
            listOf(GeminiPart.FunctionCall("get_weather", mapOf("city" to "Tokyo"))),
        ),
        GeminiMessage(
            "tool",
            listOf(GeminiPart.FunctionResponse("get_weather", mapOf("temp" to 22))),
        ),
    )

    private val tools = listOf(
        com.aistudio.mcpchat.data.model.McpTool(
            id = "t1",
            serverId = "s1",
            serverName = "Srv",
            name = "get_weather",
            description = "Get weather",
            inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"}}}""",
        ),
    )

    @Test
    fun `sends chat completions wire format with tool loop`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"choices":[{"message":{"content":"It is 22°C in Tokyo."}}]}"""),
        )

        val response = client.generateContent(config, "gpt-4o-mini", history, tools, "Be helpful.")

        assertEquals("It is 22°C in Tokyo.", response.text)
        assertTrue(response.functionCalls.isEmpty())

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        val sent = JSONObject(recorded!!.body.readUtf8())
        assertEquals("gpt-4o-mini", sent.getString("model"))
        assertEquals("Be helpful.", sent.getJSONArray("messages").getJSONObject(0).getString("content"))
        assertEquals("system", sent.getJSONArray("messages").getJSONObject(0).getString("role"))

        // tools declared with function wrapper
        val tool = sent.getJSONArray("tools").getJSONObject(0).getJSONObject("function")
        assertEquals("get_weather", tool.getString("name"))

        // assistant turn carries positional synthetic tool_calls id
        val assistant = sent.getJSONArray("messages").getJSONObject(2)
        assertEquals("assistant", assistant.getString("role"))
        assertEquals("call_t0_c0", assistant.getJSONArray("tool_calls").getJSONObject(0).getString("id"))

        // response feeds back as role:"tool" with matching id
        val toolMsg = sent.getJSONArray("messages").getJSONObject(3)
        assertEquals("tool", toolMsg.getString("role"))
        assertEquals("call_t0_c0", toolMsg.getString("tool_call_id"))
        assertEquals("""{"temp":22}""", toolMsg.getString("content"))
    }

    @Test
    fun `parses tool_calls from model response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"choices\":[{\"message\":{\"content\": null," +
                    "\"tool_calls\":[{\"id\":\"x\",\"type\":\"function\",\"function\":{" +
                    "\"name\":\"get_weather\",\"arguments\":\"{\\\"city\\\":\\\"Paris\\\"}\" " +
                    "}}]}}]}",
            ),
        )

        val response = client.generateContent(config, "gpt-4o-mini", history, tools, "")

        assertNull(response.text)
        assertEquals(1, response.functionCalls.size)
        assertEquals("get_weather", response.functionCalls[0].name)
        assertEquals("Paris", response.functionCalls[0].args["city"])
    }

    @Test
    fun `maps provider errors to typed messages`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"bad key"}}"""))
        val r1 = client.generateContent(config, "m", history, emptyList(), "")
        assertTrue(r1.error!!.contains("API key"))

        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        val r2 = client.generateContent(config, "m", history, emptyList(), "")
        assertTrue(r2.error!!.contains("Rate limit"))
    }

    @Test
    fun `tool call ids stay unique across multiple tool rounds in one request`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"choices":[{"message":{"content":"done"}}]}"""),
        )
        val multiHop = listOf(
            GeminiMessage("user", listOf(GeminiPart.Text("Q"))),
            GeminiMessage("model", listOf(GeminiPart.FunctionCall("tool_a", mapOf("x" to 1)))),
            GeminiMessage("tool", listOf(GeminiPart.FunctionResponse("tool_a", mapOf("x" to 1)))),
            GeminiMessage("model", listOf(GeminiPart.FunctionCall("tool_b", mapOf("y" to 2)))),
            GeminiMessage("tool", listOf(GeminiPart.FunctionResponse("tool_b", mapOf("y" to 2)))),
        )

        client.generateContent(config, "gpt-4o-mini", multiHop, emptyList(), "")

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        val messages = JSONObject(recorded!!.body.readUtf8()).getJSONArray("messages")

        val callIds = mutableSetOf<String>()
        val resultIds = mutableSetOf<String>()
        for (i in 0 until messages.length()) {
            val msg = messages.getJSONObject(i)
            msg.optJSONArray("tool_calls")?.let { calls ->
                for (j in 0 until calls.length()) callIds.add(calls.getJSONObject(j).getString("id"))
            }
            if (msg.optString("role") == "tool") resultIds.add(msg.getString("tool_call_id"))
        }

        // Rounds get distinct ids and each call keeps its matching result —
        // reusing "call_0" per round makes strict servers reject/mispair.
        assertEquals(setOf("call_t0_c0", "call_t1_c0"), callIds)
        assertEquals(callIds, resultIds)
    }

    @Test
    fun `handles malformed JSON response gracefully`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not valid json"))

        val response = client.generateContent(config, "gpt-4o-mini", history, emptyList(), "")
        assertTrue(response.text == null)
        assertNotNull(response.error)
    }

    @Test
    fun `streamContent emits text deltas and requests stream mode`() = runTest {
        val sseBody = buildString {
            append("""data: {"choices":[{"delta":{"role":"assistant"}}]}""").append("\n\n")
            append("""data: {"choices":[{"delta":{"content":"Hello"}}]}""").append("\n\n")
            append("""data: {"choices":[{"delta":{"content":" world"}}]}""").append("\n\n")
            append("""data: {"choices":[{"delta":{},"finish_reason":"stop"}]}""").append("\n\n")
            append("data: [DONE]").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gpt-4o-mini", history, tools, "") { deltas.add(it) }

        assertEquals("Hello world", response.text)
        assertEquals(listOf("Hello", " world"), deltas)
        assertTrue(response.functionCalls.isEmpty())

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        val sent = JSONObject(recorded!!.body.readUtf8())
        assertEquals(true, sent.getBoolean("stream"))
    }

    @Test
    fun `streamContent assembles fragmented tool_calls deltas`() = runTest {
        val sseBody = buildString {
            append(
                """data: {"choices":[{"delta":{"tool_calls":[""" +
                    """{"index":0,"id":"call_1","type":"function","function":{"name":"get_weather","arguments":""}}""" +
                    """]}}]}""",
            ).append("\n\n")
            append(
                """data: {"choices":[{"delta":{"tool_calls":[""" +
                    """{"index":0,"function":{"arguments":"{\"ci"}}""" +
                    """]}}]}""",
            ).append("\n\n")
            append(
                """data: {"choices":[{"delta":{"tool_calls":[""" +
                    """{"index":0,"function":{"arguments":"ty\":\"Paris\"}"}}""" +
                    """]}}]}""",
            ).append("\n\n")
            append("""data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}""").append("\n\n")
            append("data: [DONE]").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gpt-4o-mini", history, tools, "") { deltas.add(it) }

        assertTrue(deltas.isEmpty())
        assertNull(response.text)
        assertEquals(1, response.functionCalls.size)
        assertEquals("get_weather", response.functionCalls[0].name)
        assertEquals("Paris", response.functionCalls[0].args["city"])
    }

    @Test
    fun `streamContent falls back when body arrives as JSON not SSE`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"choices":[{"message":{"content":"Full reply."}}]}"""),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gpt-4o-mini", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Full reply.", response.text)
        assertEquals(listOf("Full reply."), deltas)
    }

    @Test
    fun `streamContent retries without stream when provider rejects it`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":{"message":"streaming is disabled for this key"}}"""),
        )
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"choices":[{"message":{"content":"Plain reply."}}]}"""),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "gpt-4o-mini", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Plain reply.", response.text)
        assertEquals(listOf("Plain reply."), deltas)

        val first = server.takeRequest(10, TimeUnit.SECONDS)
        val second = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull(first)
        assertNotNull(second)
        val sentFirst = JSONObject(first!!.body.readUtf8())
        val sentSecond = JSONObject(second!!.body.readUtf8())
        assertEquals(true, sentFirst.getBoolean("stream"))
        assertFalse(sentSecond.has("stream"))
    }

    @Test
    fun `streamContent sends image turns as typed content array`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"A cat.\"}}]}\n\n"),
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

        client.streamContent(config, "gpt-4o", visionHistory, emptyList(), "") {}

        val sent = JSONObject(server.takeRequest(10, TimeUnit.SECONDS)!!.body.readUtf8())
        val content = sent.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("text", content.getJSONObject(0).getString("type"))
        assertEquals("What is in this picture?", content.getJSONObject(0).getString("text"))
        val imageUrl = content.getJSONObject(1).getJSONObject("image_url").getString("url")
        assertEquals("data:image/png;base64,aGVsbG8=", imageUrl)
    }

    @Test
    fun `streamContent keeps plain text turns as simple string content`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"Ok.\"}}]}\n\n"),
        )

        client.streamContent(config, "gpt-4o-mini", history, emptyList(), "") {}

        val sent = JSONObject(server.takeRequest(10, TimeUnit.SECONDS)!!.body.readUtf8())
        val content = sent.getJSONArray("messages").getJSONObject(0).get("content")
        assertTrue(content is String)
    }
}
