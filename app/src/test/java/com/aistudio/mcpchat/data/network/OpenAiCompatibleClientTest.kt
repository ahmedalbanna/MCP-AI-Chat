package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
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
            id = "t1", serverId = "s1", serverName = "Srv",
            name = "get_weather", description = "Get weather",
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

        val sent = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("gpt-4o-mini", sent.getString("model"))
        assertEquals("Be helpful.", sent.getJSONArray("messages").getJSONObject(0).getString("content"))
        assertEquals("system", sent.getJSONArray("messages").getJSONObject(0).getString("role"))

        // tools declared with function wrapper
        val tool = sent.getJSONArray("tools").getJSONObject(0).getJSONObject("function")
        assertEquals("get_weather", tool.getString("name"))

        // assistant turn carries positional synthetic tool_calls id
        val assistant = sent.getJSONArray("messages").getJSONObject(2)
        assertEquals("assistant", assistant.getString("role"))
        assertEquals("call_0", assistant.getJSONArray("tool_calls").getJSONObject(0).getString("id"))

        // response feeds back as role:"tool" with matching id
        val toolMsg = sent.getJSONArray("messages").getJSONObject(3)
        assertEquals("tool", toolMsg.getString("role"))
        assertEquals("call_0", toolMsg.getString("tool_call_id"))
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
}
