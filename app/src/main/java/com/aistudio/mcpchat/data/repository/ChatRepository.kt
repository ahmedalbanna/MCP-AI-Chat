package com.aistudio.mcpchat.data.repository

import android.util.Log
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.ChatException
import com.aistudio.mcpchat.data.model.DEFAULT_MODEL
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.data.network.ActiveAiProviderSource
import com.aistudio.mcpchat.data.network.AiChatService
import com.aistudio.mcpchat.data.network.GeminiApiService
import com.aistudio.mcpchat.data.model.AiProviderConfig
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
    // Provider-agnostic chat backend; the MCP tool loop is unchanged because
    // every provider normalizes to GeminiResponse.
    private val aiService: AiChatService = GeminiApiService(),
    private val activeProvider: ActiveAiProviderSource =
        ActiveAiProviderSource { AiProviderConfig.google() },
) {
    private val chatDao = database.chatDao()
    private val systemPromptBuilder = SystemPromptBuilder()
    private val fallbackHandler = LocalFallbackHandler()

    companion object {
        private const val TAG = "ChatRepository"
        private const val MAX_TOOL_HOPS = 5

        private val hopLimitMessage =
            "I reached the maximum number of tool-call rounds ($MAX_TOOL_HOPS) without completing this task. " +
                "Please try rephrasing your request or breaking it into smaller steps."
    }

    val allSessionsFlow: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(title: String = "New Chat", model: String = DEFAULT_MODEL, systemPrompt: String = ""): ChatSessionEntity =
        withContext(Dispatchers.IO) {
            val session = ChatSessionEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                model = model,
                systemPrompt = systemPrompt,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
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
        onToolCallUpdate: ((List<McpToolCall>) -> Unit)? = null,
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId) ?: createNewSession(
            title = userPrompt.take(28) + if (userPrompt.length > 28) "..." else "",
        )
        persistUserTurn(sessionId, session, userPrompt)

        // 2. Fetch all enabled tools from active MCP servers
        val enabledServers = mcpRepository.getEnabledServers()
        val allEnabledTools = mcpRepository.getEnabledTools()
        val serverMap = enabledServers.associateBy { it.id }

        // Filter tools whose server is also enabled
        val activeTools = allEnabledTools.filter { tool ->
            serverMap[tool.serverId]?.isEnabled == true
        }

        val toolNameToToolMap = activeTools.associateBy { it.name }
        // Gemini receives sanitized function names; map them back so tool
        // execution uses the original MCP tool name.
        val sanitizedToOriginalName = activeTools.associate { tool ->
            GeminiApiService.sanitizeFunctionNameStatic(tool.name) to tool.name
        }

        // 3. Construct conversation history for Gemini
        val allMsgs = chatDao.getMessagesList(sessionId)
        val systemInstruction = systemPromptBuilder.buildSystemPrompt(session.systemPrompt, activeTools)
        val history = buildHistory(allMsgs)

        val accumulatedToolCalls = mutableListOf<McpToolCall>()
        var finalResponseText = ""
        var isError = false
        var failureException: ChatException? = null
        val startTime = System.currentTimeMillis()

        // 4. Multi-turn execution loop (up to 5 function call hops)
        var currentHop = 0
        var continueLoop = true

        while (continueLoop && currentHop < MAX_TOOL_HOPS) {
            currentHop++

            val providerConfig = activeProvider.getActive()
            val geminiResponse = aiService.generateContent(
                config = providerConfig,
                modelName = session.model,
                history = history,
                tools = activeTools,
                systemInstruction = systemInstruction,
            )

            if (geminiResponse.error != null) {
                val outcome = handleGeminiError(geminiResponse.error, userPrompt)
                isError = outcome.isError
                failureException = outcome.exception
                finalResponseText = outcome.message
                continueLoop = false
                break
            }

            // Check if Gemini requested function calls
            if (geminiResponse.functionCalls.isNotEmpty()) {
                val functionResponsesParts = executeFunctionCalls(
                    geminiResponse.functionCalls,
                    toolNameToToolMap,
                    sanitizedToOriginalName,
                    serverMap,
                    accumulatedToolCalls,
                    onToolCallUpdate,
                )

                // Append assistant's function call message to history
                history.add(GeminiMessage(role = "model", parts = geminiResponse.functionCalls))

                // Append function response message to history
                history.add(GeminiMessage(role = "tool", parts = functionResponsesParts))

                // Continue to next hop to get Gemini's natural language response!
                continueLoop = true
            } else {
                // Final text received from model
                finalResponseText = geminiResponse.text ?: "No response text generated."
                continueLoop = false
            }
        }

        val totalLatency = System.currentTimeMillis() - startTime
        return@withContext persistAssistantTurn(
            sessionId,
            session,
            finalResponseText,
            accumulatedToolCalls,
            isError,
            failureException,
            totalLatency,
        )
    }

    private suspend fun persistUserTurn(sessionId: String, session: ChatSessionEntity, userPrompt: String) {
        // If first user message, use it as the session title
        if (chatDao.getMessagesList(sessionId).isEmpty()) {
            chatDao.updateSession(
                session.copy(
                    title = userPrompt.take(30) + if (userPrompt.length > 30) "..." else "",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            timestamp = System.currentTimeMillis(),
        )
        chatDao.insertMessage(userMessage)
    }

    private suspend fun persistAssistantTurn(
        sessionId: String,
        session: ChatSessionEntity,
        finalResponseText: String,
        accumulatedToolCalls: List<McpToolCall>,
        isError: Boolean,
        failureException: ChatException?,
        totalLatency: Long,
    ): Result<ChatMessageEntity> {
        var responseText = finalResponseText
        var error = isError

        // Loop exhausted while the model still wanted tools — surface it
        // instead of saving an empty reply.
        if (responseText.isBlank() && accumulatedToolCalls.isNotEmpty()) {
            error = true
            responseText = hopLimitMessage
        }

        val assistantMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "assistant",
            content = responseText,
            toolCallsJson = if (accumulatedToolCalls.isNotEmpty()) serializeToolCalls(accumulatedToolCalls) else null,
            latencyMs = totalLatency,
            timestamp = System.currentTimeMillis(),
            isError = error,
        )

        chatDao.insertMessage(assistantMessage)
        chatDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))

        // Surface hard failures to the caller as typed exceptions while
        // keeping the persisted assistant message for history/UX.
        failureException?.let { return Result.failure(it) }

        return Result.success(assistantMessage)
    }

    private data class ErrorOutcome(
        val message: String,
        val isError: Boolean,
        val exception: ChatException?,
    )

    private fun handleGeminiError(error: String, userPrompt: String): ErrorOutcome {
        if (error == "API_KEY_MISSING") {
            // Graceful degradation: local fallback keeps the chat usable.
            return ErrorOutcome(fallbackHandler.getResponse(userPrompt), false, null)
        }
        val exception = when {
            error.contains("Rate limit", ignoreCase = true) ->
                ChatException.RateLimit(error)
            error.contains("timeout", ignoreCase = true) ||
                error.contains("timed out", ignoreCase = true) ||
                error.contains("unreachable", ignoreCase = true) ||
                error.contains("connect", ignoreCase = true) ->
                ChatException.Network(error)
            else -> ChatException.GeminiApi(error)
        }
        return ErrorOutcome(error, true, exception)
    }

    private fun buildHistory(messages: List<ChatMessageEntity>): MutableList<GeminiMessage> {
        val history = mutableListOf<GeminiMessage>()
        for (m in messages) {
            if (m.role == "user") {
                history.add(GeminiMessage(role = "user", parts = listOf(GeminiPart.Text(m.content))))
            } else if (m.role == "assistant") {
                appendAssistantTurn(history, m)
            }
        }
        return history
    }

    private fun appendAssistantTurn(history: MutableList<GeminiMessage>, m: ChatMessageEntity) {
        var responseParts: List<GeminiPart>? = null
        val parts = mutableListOf<GeminiPart>()
        if (m.toolCallsJson != null) {
            val calls = parseToolCallsJson(m.toolCallsJson)
            for (c in calls) {
                parts.add(GeminiPart.FunctionCall(c.toolName, c.inputArguments, c.thoughtSignature))
            }
            // Rebuild the matching function responses: Gemini rejects a
            // request where a functionCall is not followed by its
            // functionResponse in the next message.
            val rebuiltResponses = calls.mapNotNull { c ->
                c.outputResult?.takeIf { it.isNotBlank() }?.let { output ->
                    GeminiPart.FunctionResponse(
                        name = c.toolName,
                        response = parseResultToMap(output),
                        thoughtSignature = c.thoughtSignature,
                    )
                }
            }
            if (rebuiltResponses.isNotEmpty()) {
                responseParts = rebuiltResponses
            }
        }
        if (m.content.isNotBlank()) {
            parts.add(GeminiPart.Text(m.content))
        }
        if (parts.isNotEmpty()) {
            history.add(GeminiMessage(role = "model", parts = parts))
        }
        if (responseParts != null) {
            history.add(GeminiMessage(role = "tool", parts = responseParts))
        }
    }

    private suspend fun executeFunctionCalls(
        functionCalls: List<GeminiPart.FunctionCall>,
        toolNameToToolMap: Map<String, McpTool>,
        sanitizedToOriginalName: Map<String, String>,
        serverMap: Map<String, McpServer>,
        accumulatedToolCalls: MutableList<McpToolCall>,
        onToolCallUpdate: ((List<McpToolCall>) -> Unit)?,
    ): List<GeminiPart> {
        val functionResponsesParts = mutableListOf<GeminiPart>()

        for (fc in functionCalls) {
            // Gemini receives sanitized names; resolve the original MCP tool.
            val originalToolName = toolNameToToolMap[fc.name]?.name
                ?: sanitizedToOriginalName[fc.name]
            val toolDef = originalToolName?.let { toolNameToToolMap[it] }
            val server = toolDef?.let { def -> serverMap[def.serverId] }

            val toolCallId = UUID.randomUUID().toString()
            val toolCall = McpToolCall(
                callId = toolCallId,
                serverId = server?.id ?: "unknown",
                serverName = server?.name ?: "Remote MCP Server",
                toolName = originalToolName ?: fc.name,
                inputArguments = fc.args,
                status = ToolExecutionStatus.EXECUTING,
                thoughtSignature = fc.thoughtSignature,
            )

            accumulatedToolCalls.add(toolCall)
            onToolCallUpdate?.invoke(accumulatedToolCalls)

            // Guard unknown tool (provider hallucinated a name or server was disabled
            // after tool discovery) — fail fast without calling MCP over an empty endpoint.
            val resolvedToolName = originalToolName ?: fc.name
            val (execResult, logItem) = if (server == null) {
                val errorJson = JSONObject().put("error", "Unknown tool '$resolvedToolName' — no enabled MCP server provides this tool").toString()
                val syntheticLog = McpLogItem(
                    serverId = "unknown",
                    serverName = "Unknown",
                    toolName = resolvedToolName,
                    requestPayload = JSONObject().apply {
                        put("name", resolvedToolName)
                        put("arguments", JSONObject(fc.args))
                    }.toString(2),
                    responsePayload = errorJson,
                    isSuccess = false,
                    latencyMs = 0,
                    errorMessage = "Unknown tool",
                )
                errorJson to syntheticLog
            } else {
                mcpRepository.executeTool(
                    server,
                    resolvedToolName,
                    fc.args,
                )
            }

            replaceToolCall(
                accumulatedToolCalls,
                toolCallId,
                toolCall.copy(
                    outputResult = execResult,
                    isSuccess = logItem.isSuccess,
                    status = if (logItem.isSuccess) ToolExecutionStatus.SUCCESS else ToolExecutionStatus.ERROR,
                    executionTimeMs = logItem.latencyMs,
                    errorMessage = logItem.errorMessage,
                ),
            )
            onToolCallUpdate?.invoke(accumulatedToolCalls)

            // Build function response payload for next turn
            functionResponsesParts.add(
                GeminiPart.FunctionResponse(
                    name = fc.name,
                    response = parseResultToMap(execResult),
                    thoughtSignature = fc.thoughtSignature,
                ),
            )
        }
        return functionResponsesParts
    }

    private fun replaceToolCall(accumulatedToolCalls: MutableList<McpToolCall>, callId: String, updated: McpToolCall) {
        val index = accumulatedToolCalls.indexOfFirst { it.callId == callId }
        if (index >= 0) {
            accumulatedToolCalls[index] = updated
        }
    }

    private fun parseResultToMap(execResult: String): Map<String, Any?> {
        return try {
            val jsonObj = JSONObject(execResult)
            buildMap {
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, jsonObj.get(k))
                }
            }
        } catch (_: Exception) {
            mapOf("result" to (if (execResult.isNotBlank()) execResult.take(1000) else "Empty response"))
        }
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
                        } catch (_: Exception) {
                            ToolExecutionStatus.SUCCESS
                        },
                        thoughtSignature = obj.optString("thoughtSignature", null).takeIf { it != "null" },
                        errorMessage = obj.optString("errorMessage").takeIf { it.isNotBlank() },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tool calls json", e)
        }
        return list
    }
}
