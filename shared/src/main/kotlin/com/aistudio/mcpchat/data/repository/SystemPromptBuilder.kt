package com.aistudio.mcpchat.data.repository

class SystemPromptBuilder {

    fun buildSystemPrompt(userSystemPrompt: String): String {
        val builder = StringBuilder()
        builder.append(
            "You are an advanced AI assistant augmented with Model Context Protocol (MCP) " +
                "capabilities connecting to remote tools and servers.\n",
        )
        builder.append(
            "You have access to live remote MCP tools. When the user asks for real-time information, " +
                "calculations, weather, financial markets, web search, GitHub data, or world time, invoke " +
                "the appropriate tools using function calling.\n",
        )
        builder.append(
            "Provide concise, well-formatted markdown responses. Synthesize tool results seamlessly into " +
                "natural, helpful answers with proper formatting, bullet points, and tables when helpful.\n",
        )
        builder.append(
            "\nEnd every final answer with exactly 3 short follow-up prompts the user may want next, " +
                "formatted as:\n---SUGGESTIONS---\n1. <follow-up>\n2. <follow-up>\n3. <follow-up>\n" +
                "Keep each under 80 characters. Never add the block after a tool call, only after the final text.\n",
        )

        if (userSystemPrompt.isNotBlank()) {
            builder.append("\nUser Instructions:\n")
            builder.append(userSystemPrompt)
        }

        return builder.toString()
    }
}
