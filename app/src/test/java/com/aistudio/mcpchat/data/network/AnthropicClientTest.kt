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

    @Test
    fun `sends messages wire format with tool_use and tool_result pairing`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[{"type":"text","text":"Tokyo is 22°C."}],"stop_reason":"end_turn"}""",
            ),
        )

        val response = client.generateContent(config, "claude-sonnet-4-5", history, emptyList(), "Be brief.")

        assertEquals("Tokyo is 22°C.", response.text)

        val sent = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("claude-sonnet-4-5", sent.getString("model"))
        assertEquals("Be brief.", sent.getString("system"))
        assertTrue(sent.getLong("max_tokens") > 0)
        assertEquals("sk-ant-test", server.takeRequest().getHeader("x-api-key"))

        val messages = sent.getJSONArray("messages")
        // [user text, assistant tool_use, user tool_result]
        assertEquals(3, messages.length())

        val assistantBlock = messages.getJSONObject(1)
            .getJSONArray("content").getJSONObject(0)
        assertEquals("tool_use", assistantBlock.getString("type"))
        assertEquals("toolu_call_0", assistantBlock.getString("id"))
        assertEquals("get_weather", assistantBlock.getString("name"))

        val resultBlock = messages.getJSONObject(2)
            .getJSONArray("content").getJSONObject(0)
        assertEquals("tool_result", resultBlock.getString("type"))
        assertEquals("toolu_call_0", resultBlock.getString("tool_use_id"))
        assertEquals("""{"temp":22}""", resultBlock.getString("content"))
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
}
