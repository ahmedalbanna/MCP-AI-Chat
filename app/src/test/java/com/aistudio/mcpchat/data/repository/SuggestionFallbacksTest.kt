package com.aistudio.mcpchat.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionFallbacksTest {

    @Test
    fun `forConversation without tools returns generic suggestions`() {
        val suggestions = SuggestionFallbacks.forConversation(emptyList())

        assertEquals(3, suggestions.size)
        assertTrue(suggestions.contains("Summarize the key points"))
    }

    @Test
    fun `forConversation tailors first suggestion to weather tools`() {
        val suggestions = SuggestionFallbacks.forConversation(listOf("get_weather"))

        assertEquals("Show the extended forecast", suggestions.first())
        assertEquals(3, suggestions.size)
    }

    @Test
    fun `forConversation ignores unknown tools and stays capped`() {
        val suggestions = SuggestionFallbacks.forConversation(
            listOf("mystery_tool", "get_weather", "github_search", "btc_price", "world_time"),
        )

        assertEquals(3, suggestions.size)
        assertEquals("Show the extended forecast", suggestions[0])
    }
}
