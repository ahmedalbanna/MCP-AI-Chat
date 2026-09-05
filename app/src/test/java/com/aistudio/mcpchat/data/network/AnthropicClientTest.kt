package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class AnthropicClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AnthropicClient

    private val config get() = AiProviderConfig(
        name = "Test Claude",
        type = com.aistudio.mcpchat.data.model.AiProviderType.ANTHROPIC,
        baseUrl = server.url("").toString().trimEnd('/'),
        apiKey = "sk-ant-test",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = AnthropicClient(
            OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build(),
        )
    }

    @After
    fun tearDown() {
        // Tolerant shutdown: after the cancellation test the server still has
        // an in-flight delayed response whose socket close surfaces here.
        runCatching { server.shutdown() }
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

    @Test
    fun `sends messages wire format with tool_use and tool_result pairing`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[{"type":"text","text":"Tokyo is 22°C."}],"stop_reason":"end_turn"}""",
            ),
        )

        val response = client.generateContent(config, "claude-sonnet-4-5", history, emptyList(), "Be brief.")

        assertEquals("Tokyo is 22°C.", response.text)

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        val sent = JSONObject(recorded!!.body.readUtf8())
        assertEquals("claude-sonnet-4-5", sent.getString("model"))
        assertEquals("Be brief.", sent.getString("system"))
        assertTrue(sent.getLong("max_tokens") > 0)
        assertEquals("sk-ant-test", recorded.getHeader("x-api-key"))

        val messages = sent.getJSONArray("messages")
        // [user text, assistant tool_use, user tool_result]
        assertEquals(3, messages.length())

        val assistantBlock = messages.getJSONObject(1)
            .getJSONArray("content").getJSONObject(0)
        assertEquals("tool_use", assistantBlock.getString("type"))
        assertEquals("toolu_t0_c0", assistantBlock.getString("id"))
        assertEquals("get_weather", assistantBlock.getString("name"))

        val resultBlock = messages.getJSONObject(2)
            .getJSONArray("content").getJSONObject(0)
        assertEquals("tool_result", resultBlock.getString("type"))
        assertEquals("toolu_t0_c0", resultBlock.getString("tool_use_id"))
        assertEquals("""{"temp":22}""", resultBlock.getString("content"))
    }

    @Test
    fun `base URL already ending in slash-v1 is not doubled`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[{"type":"text","text":"ok"}],"stop_reason":"end_turn"}""",
            ),
        )
        val configWithV1 = config.copy(baseUrl = server.url("/v1").toString().trimEnd('/'))

        client.generateContent(configWithV1, "claude-opus-4-8", emptyList(), emptyList(), "")

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        assertEquals("/v1/messages", recorded!!.path)
    }

    @Test
    fun `tool use ids stay unique across multiple tool rounds in one request`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[{"type":"text","text":"done"}],"stop_reason":"end_turn"}""",
            ),
        )
        val multiHop = listOf(
            GeminiMessage("user", listOf(GeminiPart.Text("Q"))),
            GeminiMessage("model", listOf(GeminiPart.FunctionCall("tool_a", mapOf("x" to 1)))),
            GeminiMessage("tool", listOf(GeminiPart.FunctionResponse("tool_a", mapOf("x" to 1)))),
            GeminiMessage("model", listOf(GeminiPart.FunctionCall("tool_b", mapOf("y" to 2)))),
            GeminiMessage("tool", listOf(GeminiPart.FunctionResponse("tool_b", mapOf("y" to 2)))),
        )

        client.generateContent(config, "claude-x", multiHop, emptyList(), "")

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        val messages = JSONObject(recorded!!.body.readUtf8()).getJSONArray("messages")

        val useIds = mutableSetOf<String>()
        val resultIds = mutableSetOf<String>()
        for (i in 0 until messages.length()) {
            val content = messages.getJSONObject(i).optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val block = content.getJSONObject(j)
                when (block.optString("type")) {
                    "tool_use" -> useIds.add(block.getString("id"))
                    "tool_result" -> resultIds.add(block.getString("tool_use_id"))
                }
            }
        }

        // Rounds get distinct ids and each tool_use keeps its tool_result —
        // reusing "toolu_call_0" per round makes strict servers reject/mispair.
        assertEquals(setOf("toolu_t0_c0", "toolu_t1_c0"), useIds)
        assertEquals(useIds, resultIds)
    }

    @Test
    fun `cancelling the coroutine cuts the in-flight request`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"content":[{"type":"text","text":"late"}],"stop_reason":"end_turn"}""")
                .setHeadersDelay(10, TimeUnit.SECONDS),
        )

        val start = System.currentTimeMillis()
        runBlocking {
            val job = launch {
                client.generateContent(config, "claude-x", emptyList(), emptyList(), "")
            }
            delay(300)
            job.cancelAndJoin()
        }
        val elapsed = System.currentTimeMillis() - start

        // Without cancellable HTTP + CancellationException rethrow the call
        // would keep blocking until the server responds (~10s here).
        assertTrue("cancellation should cut the request quickly (took ${elapsed}ms)", elapsed < 5000)
    }

    @Test
    fun `parses text and tool_use blocks from response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"content":[
                    {"type":"tool_use","id":"t1","name":"get_weather",
                     "input":{"city":"Paris"}}
                ],"stop_reason":"tool_use"}
                """.trimIndent(),
            ),
        )

        val response = client.generateContent(config, "claude-sonnet-4-5", history, emptyList(), "")

        org.junit.Assert.assertNull(response.text)
        assertEquals(1, response.functionCalls.size)
        assertEquals("get_weather", response.functionCalls[0].name)
        assertEquals("Paris", response.functionCalls[0].args["city"])
    }

    @Test
    fun `maps anthropic errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"nope"}}"""))

        val response = client.generateContent(config, "claude", history, emptyList(), "")
        assertTrue(response.error!!.contains("API key"))
    }

    @Test
    fun `maps rate limit errors`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(429).setBody("""{"error":{"message":"slow down"}}"""),
        )

        val response = client.generateContent(config, "claude", history, emptyList(), "")
        assertTrue(response.error!!.contains("Rate limit"))
    }

    @Test
    fun `handles malformed JSON response gracefully`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not valid json"))

        val response = client.generateContent(config, "claude", history, emptyList(), "")
        assertTrue(response.text == null)
        assertNotNull(response.error)
    }

    @Test
    fun `streamContent emits text deltas and requests stream mode`() = runTest {
        val sseBody = buildString {
            append("""data: {"type":"message_start","message":{"content":[]}}""").append("\n\n")
            append("""data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""").append("\n\n")
            append("""data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Tokyo is "}}""").append("\n\n")
            append("""data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"22°C."}}""").append("\n\n")
            append("""data: {"type":"content_block_stop","index":0}""").append("\n\n")
            append("""data: {"type":"message_delta","delta":{"stop_reason":"end_turn"}}""").append("\n\n")
            append("""data: {"type":"message_stop"}""").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "claude-sonnet-4-5", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Tokyo is 22°C.", response.text)
        assertEquals(listOf("Tokyo is ", "22°C."), deltas)
        assertTrue(response.functionCalls.isEmpty())

        val recorded = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull("expected the client to send a request", recorded)
        val sent = JSONObject(recorded!!.body.readUtf8())
        assertEquals(true, sent.getBoolean("stream"))
    }

    @Test
    fun `streamContent assembles tool_use blocks from json fragments`() = runTest {
        val sseBody = buildString {
            append("""data: {"type":"message_start","message":{"content":[]}}""").append("\n\n")
            append("""data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""").append("\n\n")
            append("""data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Checking…"}}""").append("\n\n")
            append("""data: {"type":"content_block_stop","index":0}""").append("\n\n")
            append(
                """data: {"type":"content_block_start","index":1,"content_block":""" +
                    """{"type":"tool_use","id":"toolu_1","name":"get_weather","input":{}}}""",
            ).append("\n\n")
            append("""data: {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\"ci"}}""").append("\n\n")
            append(
                """data: {"type":"content_block_delta","index":1,"delta":""" +
                    """{"type":"input_json_delta","partial_json":"ty\":\"Paris\"}"}}""",
            ).append("\n\n")
            append("""data: {"type":"content_block_stop","index":1}""").append("\n\n")
            append("""data: {"type":"message_delta","delta":{"stop_reason":"tool_use"}}""").append("\n\n")
            append("""data: {"type":"message_stop"}""").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "claude-sonnet-4-5", history, emptyList(), "") { deltas.add(it) }

        assertEquals(listOf("Checking…"), deltas)
        assertEquals("Checking…", response.text)
        assertEquals(1, response.functionCalls.size)
        assertEquals("get_weather", response.functionCalls[0].name)
        assertEquals("Paris", response.functionCalls[0].args["city"])
    }

    @Test
    fun `streamContent appends truncation notice on max_tokens stop`() = runTest {
        val sseBody = buildString {
            append("""data: {"type":"message_start","message":{"content":[]}}""").append("\n\n")
            append("""data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""").append("\n\n")
            append("""data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"partial"}}""").append("\n\n")
            append("""data: {"type":"message_delta","delta":{"stop_reason":"max_tokens"}}""").append("\n\n")
            append("""data: {"type":"message_stop"}""").append("\n\n")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val response = client.streamContent(config, "claude-sonnet-4-5", history, emptyList(), "") {}

        assertTrue(response.text!!.startsWith("partial"))
        assertTrue(response.text!!.contains("max_tokens"))
    }

    @Test
    fun `cancelling mid-stream interrupts the throttled SSE body read`() {
        val sseBody = buildString {
            repeat(10) { i ->
                append("""data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"c$i "}}""")
                    .append("\n\n")
            }
            append("""data: {"type":"message_delta","delta":{"stop_reason":"end_turn"}}""").append("\n\n")
            append("""data: {"type":"message_stop"}""").append("\n\n")
        }
        // ~4 bytes per 150ms: a full drain takes ~30s, so only the socket
        // close from call.cancel() can end the read promptly.
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
                .throttleBody(4, 150, TimeUnit.MILLISECONDS),
        )

        val start = System.currentTimeMillis()
        runBlocking {
            var cancellation: CancellationException? = null
            val job = launch {
                try {
                    client.streamContent(config, "claude-x", emptyList(), emptyList(), "") {}
                } catch (e: CancellationException) {
                    cancellation = e
                    throw e
                }
            }
            delay(1500)
            job.cancelAndJoin()
            assertNotNull("expected CancellationException from the interrupted read", cancellation)
        }
        val elapsed = System.currentTimeMillis() - start

        // Without the job-tied call.cancel() the read would drain ~30s.
        assertTrue("cancel should interrupt the throttled read promptly (took ${elapsed}ms)", elapsed < 5000)
    }

    @Test
    fun `streamContent falls back when body arrives as JSON not SSE`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"content":[{"type":"text","text":"Full reply."}],"stop_reason":"end_turn"}"""),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "claude-sonnet-4-5", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Full reply.", response.text)
        assertEquals(listOf("Full reply."), deltas)
    }

    @Test
    fun `streamContent retries without stream when endpoint rejects it`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":{"message":"streaming is not supported on this endpoint"}}"""),
        )
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"content":[{"type":"text","text":"Plain reply."}],"stop_reason":"end_turn"}"""),
        )

        val deltas = mutableListOf<String>()
        val response = client.streamContent(config, "claude-sonnet-4-5", history, emptyList(), "") { deltas.add(it) }

        assertEquals("Plain reply.", response.text)
        assertEquals(listOf("Plain reply."), deltas)

        val first = server.takeRequest(10, TimeUnit.SECONDS)
        val second = server.takeRequest(10, TimeUnit.SECONDS)
        assertNotNull(first)
        assertNotNull(second)
        val sentFirst = JSONObject(first!!.body.readUtf8())
        val sentSecond = JSONObject(second!!.body.readUtf8())
        assertEquals(true, sentFirst.getBoolean("stream"))
        org.junit.Assert.assertFalse(sentSecond.has("stream"))
    }
}
