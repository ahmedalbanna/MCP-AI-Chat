package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.McpTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GeminiApiServiceTest {

    private lateinit var service: GeminiApiService

    @Before
    fun setUp() {
        service = GeminiApiService()
    }

    @Test
    fun `getApiKey returns string`() {
        val key = service.getApiKey()
        assertNotNull(key)
    }

    @Test
    fun `generateContent returns error when API key is missing`() = runTest {
        val response = service.generateContent(
            modelName = "gemini-3.5-flash",
            history = listOf(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text("Hello")))),
            tools = emptyList(),
        )

        if (response.error == "API_KEY_MISSING") {
            assertNull(response.text)
            assertEquals("API_KEY_MISSING", response.error)
        }
    }

    @Test
    fun `generateContent with empty history returns response or error`() = runTest {
        val response = service.generateContent(
            modelName = "gemini-3.5-flash",
            history = emptyList(),
            tools = emptyList(),
        )
        assertNotNull(response)
    }

    @Test
    fun `generateContent with tools does not crash`() = runTest {
        val tools = listOf(
            McpTool(
                id = "t1",
                serverId = "s1",
                serverName = "Server",
                name = "get_weather",
                description = "Get weather info",
                inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"}}}""",
            ),
        )
        val response = service.generateContent(
            modelName = "gemini-3.5-flash",
            history = listOf(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text("Weather in Tokyo")))),
            tools = tools,
            systemInstruction = "You are a helpful assistant.",
        )
        assertNotNull(response)
    }

    @Test
    fun `generateContent with system instruction does not crash`() = runTest {
        val response = service.generateContent(
            modelName = "gemini-3.5-flash",
            history = listOf(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text("Hi")))),
            tools = emptyList(),
            systemInstruction = "Always be polite.",
        )
        assertNotNull(response)
    }

    @Test
    fun `generateContent with function call history does not crash`() = runTest {
        val history = listOf(
            GeminiMessage(role = "user", parts = listOf(GeminiPart.Text("Weather?"))),
            GeminiMessage(
                role = "model",
                parts = listOf(
                    GeminiPart.FunctionCall("get_weather", mapOf("city" to "Tokyo")),
                ),
            ),
            GeminiMessage(
                role = "tool",
                parts = listOf(
                    GeminiPart.FunctionResponse("get_weather", mapOf("temp" to 22)),
                ),
            ),
        )
        val response = service.generateContent(
            modelName = "gemini-3.5-flash",
            history = history,
            tools = emptyList(),
        )
        assertNotNull(response)
    }

    @Test
    fun `generateContent response has rawJson field`() = runTest {
        val response = service.generateContent(
            modelName = "gemini-3.5-flash",
            history = listOf(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text("Test")))),
            tools = emptyList(),
        )
        assertNotNull(response.rawJson)
    }

    @Test
    fun `generateContent with custom model name does not crash`() = runTest {
        val response = service.generateContent(
            modelName = "models/gemini-2.0-flash",
            history = listOf(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text("Hi")))),
            tools = emptyList(),
        )
        assertNotNull(response)
    }
}
