package com.aistudio.mcpchat.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionParserTest {

    @Test
    fun `splitResponse without marker returns text and no suggestions`() {
        val (clean, suggestions) = SuggestionParser.splitResponse("Hello world")

        assertEquals("Hello world", clean)
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `splitResponse strips marker block and parses numbered items`() {
        val raw = "Here is Tokyo weather: sunny.\n\n---SUGGESTIONS---\n" +
            "1. Show the extended forecast\n2. Compare with London\n3. Convert to Fahrenheit"
        val (clean, suggestions) = SuggestionParser.splitResponse(raw)

        assertEquals("Here is Tokyo weather: sunny.", clean)
        assertEquals(listOf("Show the extended forecast", "Compare with London", "Convert to Fahrenheit"), suggestions)
    }

    @Test
    fun `splitResponse caps at three suggestions and truncates long lines`() {
        val long = "x".repeat(200)
        val raw = "Answer\n---SUGGESTIONS---\n- one\n- two\n- three\n- four\n- $long"
        val (clean, suggestions) = SuggestionParser.splitResponse(raw)

        assertEquals("Answer", clean)
        assertEquals(3, suggestions.size)
        assertEquals(listOf("one", "two", "three"), suggestions)
    }

    @Test
    fun `splitResponse drops blank lines and bullet prefixes`() {
        val raw = "Answer\n---SUGGESTIONS---\n\n*  First  \n\n2) Second\n"
        val (_, suggestions) = SuggestionParser.splitResponse(raw)

        assertEquals(listOf("First", "Second"), suggestions)
    }

    @Test
    fun `splitResponse with marker but no items returns clean text only`() {
        val (clean, suggestions) = SuggestionParser.splitResponse("Answer\n---SUGGESTIONS---\n")

        assertEquals("Answer", clean)
        assertTrue(suggestions.isEmpty())
    }
}
