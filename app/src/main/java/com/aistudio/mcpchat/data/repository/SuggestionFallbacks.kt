package com.aistudio.mcpchat.data.repository

object SuggestionFallbacks {

    const val MAX_SUGGESTIONS = 3

    private val generic = listOf(
        "Summarize the key points",
        "Explain this in more detail",
        "What should I ask next?",
    )

    private val toolKeywords = listOf(
        listOf("weather", "forecast", "temperature") to "Show the extended forecast",
        listOf("github", "repo", "code") to "Show details of the top result",
        listOf("bitcoin", "crypto", "price", "forex", "convert") to "Convert another amount",
        listOf("time", "timezone") to "Convert this to my timezone",
        listOf("calc", "math", "sqrt") to "Run another calculation",
        listOf("search", "web") to "Search for something related",
    )

    fun forConversation(toolNames: List<String>): List<String> {
        val tailored = toolNames.distinct().mapNotNull { suggestionForTool(it) }.distinct()
        return (tailored + generic).distinct().take(MAX_SUGGESTIONS)
    }

    private fun suggestionForTool(toolName: String): String? {
        val name = toolName.lowercase()
        return toolKeywords.firstOrNull { (keywords, _) -> keywords.any { it in name } }?.second
    }
}
