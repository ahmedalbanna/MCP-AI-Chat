package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.model.McpTool

class SystemPromptBuilder {

    fun buildSystemPrompt(userSystemPrompt: String, tools: List<McpTool>): String {
        val builder = StringBuilder()
        builder.append("You are an advanced AI assistant augmented with Model Context Protocol (MCP) capabilities connecting to remote tools and servers.\n")
        builder.append("You have access to live remote MCP tools. When the user asks for real-time information, calculations, weather, financial markets, web search, GitHub data, or world time, invoke the appropriate tools using function calling.\n")
        builder.append("Provide concise, well-formatted markdown responses. Synthesize tool results seamlessly into natural, helpful answers with proper formatting, bullet points, and tables when helpful.\n")

        if (tools.isNotEmpty()) {
            builder.append("\nAvailable MCP Tools:\n")
            for (tool in tools) {
                builder.append("- ${tool.name} (${tool.serverName}): ${tool.description}\n")
            }
        }

        if (userSystemPrompt.isNotBlank()) {
            builder.append("\nUser Instructions:\n")
            builder.append(userSystemPrompt)
        }

        return builder.toString()
    }
}
