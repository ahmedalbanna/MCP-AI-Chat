package com.aistudio.mcpchat.data.repository

import android.util.Log
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.data.network.GeminiApiService
import com.aistudio.mcpchat.data.tool.LocalFallbackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
    private val database: AppDatabase,
    private val mcpRepository: McpRepository,
    private val geminiService: GeminiApiService = GeminiApiService()
) {
    private val TAG = "ChatRepository"
    private val chatDao = database.chatDao()
    private val systemPromptBuilder = SystemPromptBuilder()
    private val fallbackHandler = LocalFallbackHandler()

    val allSessionsFlow: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(
        title: String = "New Chat",
        model: String = "gemini-3.5-flash",
        systemPrompt: String = ""
    ): ChatSessionEntity = withContext(Dispatchers.IO) {
        val session = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            model = model,
            systemPrompt = systemPrompt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        session
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateSessionModel(sessionId: String, model: String) = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(model = model, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        onToolCallUpdate: ((List<McpToolCall>) -> Unit)? = null
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId) ?: createNewSession(
            title = userPrompt.take(28) + if (userPrompt.length > 28) "..." else ""
        )

        // If first user message, update session title
        val existingMessages = chatDao.getMessagesList(sessionId)
        if (existingMessages.isEmpty()) {
            chatDao.updateSession(session.copy(
                title = userPrompt.take(30) + if (userPrompt.length > 30) "..." else "",
                updatedAt = System.currentTimeMillis()
            ))
        }

        // 1. Save User Message
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessageEntity(
            id = userMsgId,
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMessage)

        // 2. Fetch all enabled tools from active MCP servers
        val enabledServers = mcpRepository.getEnabledServers()
        val allEnabledTools = mcpRepository.getEnabledTools()
        val serverMap = enabledServers.associateBy { it.id }

        // Filter tools whose server is also enabled
        val activeTools = allEnabledTools.filter { tool ->
            serverMap[tool.serverId]?.isEnabled == true
        }

        val toolNameToToolMap = activeTools.associateBy { it.name }

        // 3. Construct conversation history for Gemini
        val allMsgs = chatDao.getMessagesList(sessionId)
        val history = mutableListOf<GeminiMessage>()

        // System prompt instructing model about MCP capabilities
        val systemInstruction = systemPromptBuilder.buildSystemPrompt(session.systemPrompt, activeTools)

        for (m in allMsgs) {
            if (m.role == "user") {
                history.add(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text(m.content))))
            } else if (m.role == "assistant") {
                val parts = mutableListOf<GeminiPart>()
                if (m.toolCallsJson != null) {
                    val calls = parseToolCallsJson(m.toolCallsJson)
                    for (c in calls) {
                        parts.add(GeminiPart.FunctionCall(c.toolName, c.inputArguments, c.thoughtSignature))
                    }
                }
                if (m.content.isNotBlank()) {
                    parts.add(GeminiPart.Text(m.content))
                }
                if (parts.isNotEmpty()) {
                    history.add(GeminiMessage(role = "model", parts = parts))
                }
            }
        }

        val accumulatedToolCalls = mutableListOf<McpToolCall>()
        var finalResponseText = ""
        var isError = false
        val startTime = System.currentTimeMillis()

        // 4. Multi-turn execution loop (up to 5 function call hops)
        var maxHops = 5
        var currentHop = 0
        var continueLoop = true

        while (continueLoop && currentHop < maxHops) {
            currentHop++

            val geminiResponse = geminiService.generateContent(
                modelName = session.model,
                history = history,
                tools = activeTools,
                systemInstruction = systemInstruction
            )

            if (geminiResponse.error != null) {
                if (geminiResponse.error == "API_KEY_MISSING") {
                    finalResponseText = fallbackHandler.getResponse(userPrompt)
                    continueLoop = false
                    break
                } else if (geminiResponse.error.contains("Rate limit", ignoreCase = true)) {
                    isError = true
                    finalResponseText = "Rate limit exceeded. Please wait a moment and try sending your message again."
                    continueLoop = false
                    break
                } else {
                    isError = true
                    finalResponseText = "${geminiResponse.error}"
                    continueLoop = false
                    break
                }
            }

            // Check if Gemini requested function calls
            if (geminiResponse.functionCalls.isNotEmpty()) {
                val functionResponsesParts = mutableListOf<GeminiPart>()

                for (fc in geminiResponse.functionCalls) {
                    val toolDef = toolNameToToolMap[fc.name]
                    val server = if (toolDef != null) serverMap[toolDef.serverId] else null

                    val toolCallId = UUID.randomUUID().toString()
                    var toolCall = McpToolCall(
                        callId = toolCallId,
                        serverId = server?.id ?: "unknown",
                        serverName = server?.name ?: "Remote MCP Server",
                        toolName = fc.name,
                        inputArguments = fc.args,
                        status = ToolExecutionStatus.EXECUTING,
                        thoughtSignature = fc.thoughtSignature
                    )

                    accumulatedToolCalls.add(toolCall)
                    onToolCallUpdate?.invoke(accumulatedToolCalls)

                    // Execute over remote MCP HTTP / transport
                    val targetServer = server ?: McpServer(
                        id = "dynamic",
                        name = "Remote Server",
                        description = "Discovered tool",
                        endpointUrl = "",
                        isPreset = false
                    )

                    val (execResult, logItem) = mcpRepository.executeTool(targetServer, fc.name, fc.args)

                    val updatedToolCall = toolCall.copy(
                        outputResult = execResult,
                        isSuccess = logItem.isSuccess,
                        status = if (logItem.isSuccess) ToolExecutionStatus.SUCCESS else ToolExecutionStatus.ERROR,
                        executionTimeMs = logItem.latencyMs,
                        errorMessage = logItem.errorMessage
                    )

                    // Replace in accumulated list
                    val index = accumulatedToolCalls.indexOfFirst { it.callId == toolCallId }
                    if (index >= 0) {
                        accumulatedToolCalls[index] = updatedToolCall
                    }
                    onToolCallUpdate?.invoke(accumulatedToolCalls)

                    // Build function response payload for next turn
                    val responseMap = try {
                        val jsonObj = JSONObject(execResult)
                        val map = mutableMapOf<String, Any?>()
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = jsonObj.get(k)
                        }
                        map
                    } catch (_: Exception) {
                        mapOf("result" to (execResult.take(1000) if (execResult.isNotBlank() else "Empty response"))
                    }

                    functionResponsesParts.add(
                        GeminiPart.FunctionResponse(
                            name = fc.name,
                            response = responseMap,
                            thoughtSignature = fc.thoughtSignature
                        )
                    )
                }

                // Append assistant's function call message to history
                history.add(
                    GeminiMessage(
                        role = "model",
                        parts = geminiResponse.functionCalls
                    )
                )

                // Append function response message to history
                history.add(
                    GeminiMessage(
                        role = "tool",
                        parts = functionResponsesParts
                    )
                )

                // Continue to next hop to get Gemini's natural language response!
                continueLoop = true
            } else {
                // Final text received from model
                finalResponseText = geminiResponse.text ?: "No response text generated."
                continueLoop = false
            }
        }

        val totalLatency = System.currentTimeMillis() - startTime

        // 5. Save Assistant message to Database
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantMessage = ChatMessageEntity(
            id = assistantMsgId,
            sessionId = sessionId,
            role = "assistant",
            content = finalResponseText,
            toolCallsJson = if (accumulatedToolCalls.isNotEmpty()) serializeToolCalls(accumulatedToolCalls) else null,
            latencyMs = totalLatency,
            timestamp = System.currentTimeMillis(),
            isError = isError
        )

        chatDao.insertMessage(assistantMessage)
        chatDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))

        Result.success(assistantMessage)
    }

    private fun serializeToolCalls(calls: List<McpToolCall>): String {
        val array = JSONArray()
        for (c in calls) {
            val obj = JSONObject().apply {
                put("callId", c.callId)
                put("serverId", c.serverId)
                put("serverName", c.serverName)
                put("toolName", c.toolName)
                put("inputArguments", JSONObject(c.inputArguments))
                put("outputResult", c.outputResult ?: "")
                put("isSuccess", c.isSuccess)
                put("executionTimeMs", c.executionTimeMs)
                put("status", c.status.name)
                put("thoughtSignature", c.thoughtSignature ?: JSONObject.NULL)
                put("errorMessage", c.errorMessage ?: "")
                put("timestamp", c.timestamp)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun parseToolCallsJson(jsonStr: String): List<McpToolCall> {
        val list = mutableListOf<McpToolCall>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val argsObj = obj.optJSONObject("inputArguments") ?: JSONObject()
                val argsMap = mutableMapOf<String, Any?>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.get(k)
                }

                list.add(
                    McpToolCall(
                        callId = obj.optString("callId", UUID.randomUUID().toString()),
                        serverId = obj.optString("serverId", "unknown"),
                        serverName = obj.optString("serverName", "Remote MCP Server"),
                        toolName = obj.optString("toolName", "tool"),
                        inputArguments = argsMap,
                        outputResult = obj.optString("outputResult").takeIf { it.isNotBlank() },
                        isSuccess = obj.optBoolean("isSuccess", true),
                        executionTimeMs = obj.optLong("executionTimeMs", 0),
                        status = try {
                            ToolExecutionStatus.valueOf(obj.optString("status", "SUCCESS"))
                        } catch (e: Exception) {
                            ToolExecutionStatus.SUCCESS
                        },
                        thoughtSignature = obj.optString("thoughtSignature", null).takeIf { it != "null" },
                        errorMessage = obj.optString("errorMessage").takeIf { it.isNotBlank() },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tool calls json", e)
        }
        return list
    }
}
