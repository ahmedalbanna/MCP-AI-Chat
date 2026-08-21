package com.aistudio.mcpchat.data.tool

import java.util.Calendar
import java.util.Locale

class LocalFallbackHandler {
    private val greetings = listOf("hello", "hi", "hey", "good morning", "good afternoon", "good evening")
    private val farewells = listOf("bye", "goodbye", "see you", "later", "take care")
    private val timeQueries = listOf("time", "date", "today", "what day", "what time")
    private val helpQueries = listOf("help", "what can you do", "capabilities", "features")
    private val mcpQueries = listOf("mcp", "server", "tool", "protocol", "connect")

    fun getResponse(userMessage: String): String {
        val lowerMessage = userMessage.lowercase().trim()

        return when {
            greetings.any { lowerMessage.contains(it) } -> getGreetingResponse()
            farewells.any { lowerMessage.contains(it) } -> getFarewellResponse()
            timeQueries.any { lowerMessage.contains(it) } -> getTimeResponse()
            helpQueries.any { lowerMessage.contains(it) } -> getHelpResponse()
            mcpQueries.any { lowerMessage.contains(it) } -> getMcpResponse(lowerMessage)
            lowerMessage.contains("thank") -> "You're welcome! I'm here to help."
            lowerMessage.contains("how are you") || lowerMessage.contains("how're you") ->
                "I'm doing well, thank you! I'm ready to help you with anything you need."
            lowerMessage.contains("your name") || lowerMessage.contains("who are you") ->
                "I'm your AI Chat assistant with MCP integration. I can help you connect to remote " +
                    "servers and use their tools."
            lowerMessage.length < 5 ->
                "I see you're trying to reach me. To enable full AI responses, please configure your " +
                    "Gemini API key in the Secrets panel."
            else -> getGenericResponse(userMessage)
        }
    }

    private fun getGreetingResponse(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        return "$timeGreeting! I'm your AI assistant with MCP integration. " +
            "To unlock my full capabilities, please set up your Gemini API key."
    }

    private fun getFarewellResponse(): String {
        return "Goodbye! Feel free to come back anytime. " +
            "Don't forget to configure your Gemini API key for full AI capabilities."
    }

    private fun getTimeResponse(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")

        val dayName = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val monthName = monthNames[calendar.get(Calendar.MONTH)]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)

        val minuteText = String.format(Locale.US, "%02d", minute)
        return "It's $dayName, $monthName $day, $year at $displayHour:$minuteText $amPm."
    }

    private val helpResponse =
        """I can help you with:

• **Chat** - Send messages and I'll respond (requires Gemini API key)
• **MCP Servers** - Connect to remote servers with JSON-RPC or SSE
• **Tools** - Discover and invoke capabilities from MCP servers
• **Memory** - Save and search notes for future reference

**Current Status:** Running in fallback mode. For full AI responses, configure your Gemini API key in the Secrets panel."""

    private fun getHelpResponse(): String = helpResponse

    private fun getMcpResponse(message: String): String {
        return when {
            message.contains("connect") || message.contains("add") -> 
                "To connect to an MCP server, go to the **MCP Servers** tab and tap **+**. " +
                "Enter the server's endpoint URL and optional authentication header."
            message.contains("tool") && message.contains("list") -> 
                "I can discover tools from connected servers. Go to the **Inspector** tab to see all available tools."
            message.contains("server") && (message.contains("status") || message.contains("list")) -> 
                "Check the **MCP Servers** tab to see all your servers and their connection status."
            else -> 
                "MCP (Model Context Protocol) allows me to connect to remote servers and use their tools. " +
                "Check the **MCP Servers** tab to manage your connections."
        }
    }

    private fun getGenericResponse(message: String): String {
        return "I received your message: \"$message\"\n\n" +
            "I'm currently running in fallback mode without AI capabilities. " +
            "To enable intelligent responses, please configure your Gemini API key in the **Secrets** panel.\n\n" +
            "In the meantime, I can still help you:\n" +
            "• Connect to MCP servers\n" +
            "• Discover and invoke tools\n" +
            "• Save and search memory notes"
    }
}
