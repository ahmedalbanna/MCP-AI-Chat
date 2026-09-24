package com.aistudio.mcpchat.data.repository

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
        val result = builder.buildSystemPrompt("")

        assertTrue(result.contains("Model Context Protocol"))
        assertTrue(result.contains("function calling"))
    }

    @Test
    fun `buildSystemPrompt includes user instructions when provided`() {
        val result = builder.buildSystemPrompt("Always respond in Spanish")

        assertTrue(result.contains("Always respond in Spanish"))
        assertTrue(result.contains("User Instructions"))
    }

    @Test
    fun `buildSystemPrompt excludes user instructions section when empty`() {
        val result = builder.buildSystemPrompt("")

        assertFalse(result.contains("User Instructions"))
    }

    @Test
    fun `buildSystemPrompt does not duplicate provider tool declarations`() {
        val result = builder.buildSystemPrompt("")

        assertFalse(result.contains("Available MCP Tools"))
    }

    @Test
    fun `buildSystemPrompt requests follow-up suggestions block`() {
        val result = builder.buildSystemPrompt("")

        assertTrue(result.contains("---SUGGESTIONS---"))
    }
}
