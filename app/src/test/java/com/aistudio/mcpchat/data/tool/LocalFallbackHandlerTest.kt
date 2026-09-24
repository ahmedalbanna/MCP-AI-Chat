package com.aistudio.mcpchat.data.tool

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalFallbackHandlerTest {

    private lateinit var handler: LocalFallbackHandler

    @Before
    fun setUp() {
        handler = LocalFallbackHandler()
    }

    @Test
    fun `getResponse returns greeting for hello`() {
        val response = handler.getResponse("hello")
        assertTrue(response.contains("AI assistant") || response.contains("Gemini API"))
    }

    @Test
    fun `getResponse returns greeting for hi`() {
        val response = handler.getResponse("hi")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
    }

    @Test
    fun `getResponse returns greeting for good morning`() {
        val response = handler.getResponse("good morning")
        assertTrue(response.contains("morning") || response.contains("Good"))
    }

    @Test
    fun `getResponse returns farewell for bye`() {
        val response = handler.getResponse("bye")
        assertTrue(response.contains("Goodbye") || response.contains("come back"))
    }

    @Test
    fun `getResponse returns farewell for goodbye`() {
        val response = handler.getResponse("goodbye")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
    }

    @Test
    fun `getResponse returns time info for time query`() {
        val response = handler.getResponse("what time is it")
        assertTrue(response.contains("AM") || response.contains("PM"))
    }

    @Test
    fun `getResponse returns time info for date query`() {
        val response = handler.getResponse("what is today")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
    }

    @Test
    fun `getResponse returns help for help query`() {
        val response = handler.getResponse("help")
        assertTrue(response.contains("Chat") || response.contains("MCP") || response.contains("help"))
    }

    @Test
    fun `getResponse returns help for capabilities query`() {
        val response = handler.getResponse("what can you do")
        assertTrue(response.contains("help") || response.contains("Chat") || response.contains("MCP"))
    }

    @Test
    fun `getResponse returns MCP info for mcp query`() {
        val response = handler.getResponse("tell me about mcp")
        assertTrue(response.contains("MCP") || response.contains("Model Context Protocol"))
    }

    @Test
    fun `getResponse returns server info for server query`() {
        val response = handler.getResponse("how do I connect to a server")
        assertTrue(response.contains("MCP Servers") || response.contains("server"))
    }

    @Test
    fun `getResponse returns thanks message for thank you`() {
        val response = handler.getResponse("thank you")
        assertTrue(response.contains("welcome") || response.contains("here to help"))
    }

    @Test
    fun `getResponse returns identity info for name query`() {
        val response = handler.getResponse("what is your name")
        assertTrue(response.contains("AI") || response.contains("assistant"))
    }

    @Test
    fun `getResponse returns generic for unknown message`() {
        val response = handler.getResponse("tell me a joke about programming")
        assertTrue(response.contains("fallback mode") || response.contains("Gemini API"))
    }

    @Test
    fun `getResponse returns short message for very short input`() {
        val response = handler.getResponse("ab")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
    }

    @Test
    fun `getResponse handles how are you`() {
        val response = handler.getResponse("how are you")
        assertTrue(response.contains("doing well") || response.contains("ready to help"))
    }

    @Test
    fun `getResponse handles case insensitivity`() {
        val response = handler.getResponse("HELLO")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
    }

    @Test
    fun `getResponse handles leading and trailing whitespace`() {
        val response = handler.getResponse("  help  ")
        assertTrue(response.contains("Chat") || response.contains("MCP") || response.contains("help"))
    }

    @Test
    fun `fallback key setup points to in-app provider settings`() {
        val response = handler.getResponse("tell me a joke about programming")

        assertTrue(response.contains("AI Provider settings"))
        assertFalse(response.contains("Secrets"))
    }
}
