package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.model.McpTool
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SystemPromptBuilderTest {

    private lateinit var builder: SystemPromptBuilder

    @Before
    fun setUp() {
        builder = SystemPromptBuilder()
    }

    @Test
    fun `buildSystemPrompt includes base MCP instructions`() {
        val result = builder.buildSystemPrompt("", emptyList())

        assertTrue(result.contains("Model Context Protocol"))
        assertTrue(result.contains("function calling"))
    }

    @Test
    fun `buildSystemPrompt includes user instructions when provided`() {
        val userPrompt = "Always respond in Spanish"
        val result = builder.buildSystemPrompt(userPrompt, emptyList())

        assertTrue(result.contains(userPrompt))
        assertTrue(result.contains("User Instructions"))
    }

    @Test
    fun `buildSystemPrompt excludes user instructions section when empty`() {
        val result = builder.buildSystemPrompt("", emptyList())

        assertFalse(result.contains("User Instructions"))
    }

    @Test
    fun `buildSystemPrompt includes tool names without descriptions`() {
        val tools = listOf(
            McpTool(
                id = "erpnext_1",
                serverId = "erpnext",
                serverName = "ERPNext",
                name = "get_balance",
                description = "Get account balance",
                inputSchemaJson = "{}",
                isEnabled = true,
            ),
            McpTool(
                id = "erpnext_2",
                serverId = "erpnext",
                serverName = "ERPNext",
                name = "create_invoice",
                description = "Create a sales invoice",
                inputSchemaJson = "{}",
                isEnabled = true,
            ),
        )

        val result = builder.buildSystemPrompt("", tools)

        assertTrue(result.contains("Available MCP Tools"))
        assertTrue(result.contains("- get_balance (ERPNext)"))
        assertTrue(result.contains("- create_invoice (ERPNext)"))
        // Full descriptions live in the tools array of the API request, not
        // in the system prompt — duplicating them inflates every call's tokens.
        assertFalse(result.contains("Get account balance"))
        assertFalse(result.contains("Create a sales invoice"))
    }

    @Test
    fun `buildSystemPrompt with empty tools list omits tools section`() {
        val result = builder.buildSystemPrompt("", emptyList())

        assertFalse(result.contains("Available MCP Tools"))
    }

    @Test
    fun `buildSystemPrompt combines tools and user instructions`() {
        val tools = listOf(
            McpTool(
                id = "test_tool",
                serverId = "test",
                serverName = "TestServer",
                name = "test_fn",
                description = "A test function",
                inputSchemaJson = "{}",
                isEnabled = true,
            ),
        )

        val result = builder.buildSystemPrompt("Be concise", tools)

        assertTrue(result.contains("Available MCP Tools"))
        assertTrue(result.contains("test_fn"))
        assertTrue(result.contains("Be concise"))
        assertTrue(result.contains("User Instructions"))
    }
}
