package com.aistudio.mcpchat.data.repository

import android.util.Log
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.local.ChatSessionEntity
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.ChatException
import com.aistudio.mcpchat.data.model.DEFAULT_MODEL
import com.aistudio.mcpchat.data.model.GeminiMessage
import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.ImageAttachment
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.data.model.parseImageAttachments
import com.aistudio.mcpchat.data.model.toJsonString
import com.aistudio.mcpchat.data.network.ActiveAiProviderSource
import com.aistudio.mcpchat.data.network.AiChatService
import com.aistudio.mcpchat.data.network.GeminiApiService
import com.aistudio.mcpchat.data.tool.BuiltinResourceTools
import com.aistudio.mcpchat.data.tool.LocalFallbackHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.supervisorScope
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
    // Reads image bytes for attached images; faked in JVM tests.
    private val attachmentSource: AttachmentContentSource = AttachmentContentSource { null },
) {
    private val chatDao = database.chatDao()
    private val systemPromptBuilder = SystemPromptBuilder()
    private val fallbackHandler = LocalFallbackHandler()

    companion object {
        private const val TAG = "ChatRepository"
        private const val MAX_TOOL_HOPS = 5

        /**
         * Upper bound on messages fed to the provider and to history
         * rebuilds — protects pathological sessions from unbounded memory
         * and request sizes. Chosen at ~100 user/assistant turns.
         */
        const val MAX_HISTORY_MESSAGES = 200

        private val hopLimitMessage =
            "I reached the maximum number of tool-call rounds ($MAX_TOOL_HOPS) without completing this task. " +
                "Please try rephrasing your request or breaking it into smaller steps."

        /**
         * Turns a raw user query into an FTS4 MATCH expression: each
         * whitespace-separated token becomes a prefix term ("tok*"), tokens
         * are AND-combined, and FTS operator characters are stripped so the
         * input can never produce a MATCH syntax error. Returns null when
         * nothing searchable remains.
         */
        fun buildFtsQuery(rawQuery: String): String? {
            val tokens = rawQuery.trim()
                .split(Regex("\\s+"))
                .map { it.replace(Regex("[\"'()\\-:*^]"), "") }
                .filter { it.isNotBlank() }
                .distinct()
                .take(8)
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }
    }

    val allSessionsFlow: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    /**
     * Hot stream of SSE token deltas for the in-flight generation. Emissions
     * are best-effort: an overflow drops the oldest buffered tokens rather
     * than slowing the network read, and the persisted assistant message is
     * always the authoritative full text.
     */
    private val _streamEvents = MutableSharedFlow<ChatStreamEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val streamEvents: SharedFlow<ChatStreamEvent> = _streamEvents.asSharedFlow()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    /** See [ChatDao.getMessagesWindow] — the chat UI's paged message feed. */
    fun getMessagesWindow(sessionId: String, limit: Int): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesWindow(sessionId, limit)
    }

    /**
     * Full-text message search across all sessions. Blank/operator-only
     * queries resolve to an empty result without touching the FTS index.
     */
    fun searchMessages(query: String): Flow<List<ChatMessageEntity>> {
        val ftsQuery = buildFtsQuery(query) ?: return flowOf(emptyList())
        return chatDao.searchMessages(ftsQuery)
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

    @Suppress("LongMethod")
    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        onToolCallUpdate: ((List<McpToolCall>) -> Unit)? = null,
        attachments: List<ImageAttachment> = emptyList(),
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId) ?: createNewSession(
            title = userPrompt.take(28) + if (userPrompt.length > 28) "..." else "",
        )
        // The fallback above creates a NEW session when the requested id is
        // gone (e.g. deleted while a send was in flight). Every write below
        // must target the session that actually exists — persisting under the
        // stale id would create orphaned rows no Flow ever reads.
        val effectiveSessionId = session.id
        persistUserTurn(effectiveSessionId, session, userPrompt, attachments)

        generateAndPersist(session, userPrompt, onToolCallUpdate)
    }

    /**
     * Re-runs generation for the last user turn. The trailing assistant
     * message (including a stopped-partial or error one) is dropped so the
     * rebuilt history ends at the last user message; when the conversation
     * already ends with a user turn (Stop before any output), it is simply
     * retried as-is.
     */
    suspend fun regenerateLastMessage(sessionId: String, onToolCallUpdate: ((List<McpToolCall>) -> Unit)? = null): Result<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            val session = chatDao.getSessionById(sessionId)
                ?: return@withContext Result.failure(ChatException.GeminiApi("Session no longer exists"))

            val messages = chatDao.getRecentMessages(session.id, MAX_HISTORY_MESSAGES)
            messages.lastOrNull()?.takeIf { it.role == "assistant" }?.let { last ->
                chatDao.deleteMessageById(last.id)
            }
            val fallbackPrompt = messages.lastOrNull { it.role == "user" }?.content ?: ""

            generateAndPersist(session, fallbackPrompt, onToolCallUpdate)
        }

    /**
     * Shared generation core for send and regenerate: builds history from the
     * persisted messages, runs the tool-execution loop, and persists the
     * assistant turn. On coroutine cancellation (Stop button) any partial
     * stream text is persisted first — inside [NonCancellable] because a
     * cancelled coroutine cannot suspend normally — then the cancellation
     * propagates for structured concurrency.
     */
    @Suppress("LongMethod")
    private suspend fun generateAndPersist(
        session: ChatSessionEntity,
        userPromptForFallback: String,
        onToolCallUpdate: ((List<McpToolCall>) -> Unit)?,
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val effectiveSessionId = session.id

        // 2. Fetch all enabled tools from active MCP servers
        val enabledServers = mcpRepository.getEnabledServers()
        val allEnabledTools = mcpRepository.getEnabledTools()
        val serverMap = enabledServers.associateBy { it.id }

        // Filter tools whose server is also enabled
        val activeTools = allEnabledTools.filter { tool ->
            serverMap[tool.serverId]?.isEnabled == true
        }

        // Locally-executed resource tools ride the same function-calling
        // loop as remote tools, so resource content shows up in chat like
        // any tool result.
        val builtinResourceTools = if (enabledServers.isNotEmpty()) {
            BuiltinResourceTools.definitions()
        } else {
            emptyList()
        }
        val toolsForProvider = activeTools + builtinResourceTools

        val toolNameToToolMap = toolsForProvider.associateBy { it.name }
        // Gemini receives sanitized function names; map them back so tool
        // execution uses the original MCP tool name.
        val sanitizedToOriginalName = toolsForProvider.associate { tool ->
            GeminiApiService.sanitizeFunctionNameStatic(tool.name) to tool.name
        }

        // 3. Construct conversation history for the provider — synchronous realtime,
        // no cache: every message rebuilds fresh history + prompt. The read is
        // capped at MAX_HISTORY_MESSAGES; a truncation that would start the
        // window mid-exchange is trimmed forward to the next user turn so
        // strict providers never see an orphaned tool result.
        val allMsgs = chatDao.getRecentMessages(effectiveSessionId, MAX_HISTORY_MESSAGES)
            .dropWhile { it.role != "user" }
        val systemInstruction = withContext(Dispatchers.Default) {
            systemPromptBuilder.buildSystemPrompt(session.systemPrompt, toolsForProvider)
        }
        val history = withContext(Dispatchers.IO) { buildHistory(allMsgs) }

        val accumulatedToolCalls = mutableListOf<McpToolCall>()
        val streamedText = StringBuilder()
        var finalResponseText = ""
        var isError = false
        var failureException: ChatException? = null
        val startTime = System.currentTimeMillis()

        // Capture provider once per message: the current generation finishes
        // with the config it started with; the next message picks up any
        // settings change the user made in between.
        val providerConfig = activeProvider.getActive()

        // 4. Multi-turn execution loop (up to 5 function call hops)
        var currentHop = 0
        var continueLoop = true

        _streamEvents.tryEmit(ChatStreamEvent.StreamStarted(effectiveSessionId))

        try {
            while (continueLoop && currentHop < MAX_TOOL_HOPS) {
                currentHop++
                streamedText.setLength(0)
                if (currentHop > 1) {
                    // The previous hop streamed narration that is now superseded
                    // by tool results — restart the preview from a clean bubble.
                    _streamEvents.tryEmit(ChatStreamEvent.StreamStarted(effectiveSessionId))
                }
                val geminiResponse = aiService.streamContent(
                    config = providerConfig,
                    modelName = session.model,
                    history = history,
                    tools = toolsForProvider,
                    systemInstruction = systemInstruction,
                ) { delta ->
                    streamedText.append(delta)
                    _streamEvents.tryEmit(ChatStreamEvent.TokenDelta(effectiveSessionId, delta))
                }

                if (geminiResponse.error != null) {
                    val outcome = handleGeminiError(geminiResponse.error, userPromptForFallback)
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
        } catch (e: CancellationException) {
            persistStoppedPartial(
                effectiveSessionId,
                session,
                streamedText.toString().trim(),
                accumulatedToolCalls,
                System.currentTimeMillis() - startTime,
            )
            throw e
        }

        val totalLatency = System.currentTimeMillis() - startTime
        val (cleanText, parsedSuggestions) = SuggestionParser.splitResponse(finalResponseText)
        finalResponseText = cleanText
        val suggestions = if (isError || failureException != null) {
            emptyList()
        } else {
            parsedSuggestions.ifEmpty {
                SuggestionFallbacks.forConversation(accumulatedToolCalls.map { it.toolName })
            }
        }
        val persisted = persistAssistantTurn(
            effectiveSessionId,
            session,
            finalResponseText,
            accumulatedToolCalls,
            isError,
            failureException,
            totalLatency,
        )
        // The Room flow emits the persisted message asynchronously; this
        // tells collectors the preview bubble can be swapped for the real one.
        _streamEvents.tryEmit(ChatStreamEvent.StreamFinished(effectiveSessionId, suggestions))
        persisted
    }

    /**
     * Persists whatever the user saw when they hit Stop. Runs under
     * [NonCancellable]: the coroutine is already cancelled, so ordinary
     * suspension would throw before the partial reply could be saved.
     */
    private suspend fun persistStoppedPartial(
        sessionId: String,
        session: ChatSessionEntity,
        partialText: String,
        accumulatedToolCalls: List<McpToolCall>,
        totalLatency: Long,
    ) {
        if (partialText.isBlank() && accumulatedToolCalls.isEmpty()) return

        withContext(NonCancellable) {
            val stoppedText = if (partialText.isBlank()) {
                "[Generation stopped by user]"
            } else {
                partialText + "\n\n[Generation stopped by user]"
            }
            persistAssistantTurn(
                sessionId,
                session,
                stoppedText,
                accumulatedToolCalls,
                isError = false,
                failureException = null,
                totalLatency = totalLatency,
            )
            _streamEvents.tryEmit(ChatStreamEvent.StreamFinished(sessionId))
        }
    }

    private suspend fun persistUserTurn(
        sessionId: String,
        session: ChatSessionEntity,
        userPrompt: String,
        attachments: List<ImageAttachment> = emptyList(),
    ) {
        // If first user message, use it as the session title
        if (chatDao.getMessagesList(sessionId).isEmpty()) {
            val titleBase = userPrompt.ifBlank { "Image" }
            chatDao.updateSession(
                session.copy(
                    title = titleBase.take(30) + if (titleBase.length > 30) "..." else "",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            attachmentsJson = attachments.toJsonString(),
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
                val parts = buildList {
                    if (m.content.isNotBlank()) add(GeminiPart.Text(m.content))
                    for (attachment in parseImageAttachments(m.attachmentsJson)) {
                        // Unreadable URIs (revoked grants, deleted files) are
                        // dropped rather than failing the whole send.
                        attachmentSource.read(attachment)?.let { loaded ->
                            add(GeminiPart.InlineData(loaded.mimeType, loaded.base64Data))
                        }
                    }
                }
                if (parts.isNotEmpty()) history.add(GeminiMessage(role = "user", parts = parts))
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
            // Rebuild the matching function responses: strict providers
            // (Anthropic, AgentRouter) reject a request where any tool_use
            // lacks its tool_result, so always pair — even for calls whose
            // output was blank/lost.
            val rebuiltResponses = calls.map { c ->
                val output = c.outputResult?.takeIf { it.isNotBlank() } ?: "Empty response"
                GeminiPart.FunctionResponse(
                    name = c.toolName,
                    response = parseResultToMap(output),
                    thoughtSignature = c.thoughtSignature,
                )
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

    /**
     * Runs the model's tool calls. Calls issued together in one hop are
     * independent by definition, so they execute CONCURRENTLY under
     * [supervisorScope]: a failure in one call is isolated into that call's
     * error result instead of cancelling its in-flight siblings, and
     * [CancellationException] (Stop button) still tears down everything.
     *
     * Responses are returned in the request order regardless of completion
     * order — strict providers pair nth tool_use with nth tool_result.
     */
    private suspend fun executeFunctionCalls(
        functionCalls: List<GeminiPart.FunctionCall>,
        toolNameToToolMap: Map<String, McpTool>,
        sanitizedToOriginalName: Map<String, String>,
        serverMap: Map<String, McpServer>,
        accumulatedToolCalls: MutableList<McpToolCall>,
        onToolCallUpdate: ((List<McpToolCall>) -> Unit)?,
    ): List<GeminiPart> = supervisorScope {
        val progressLock = Any()

        val preparedCalls = functionCalls.map { fc ->
            prepareToolCall(fc, toolNameToToolMap, sanitizedToOriginalName, serverMap)
        }
        synchronized(progressLock) {
            accumulatedToolCalls.addAll(preparedCalls.map { it.toolCall })
            onToolCallUpdate?.invoke(accumulatedToolCalls.toList())
        }

        val jobs = preparedCalls.map { prepared ->
            async {
                val outcome = runPreparedToolCall(prepared)
                synchronized(progressLock) {
                    replaceToolCall(accumulatedToolCalls, prepared.toolCall.callId, outcome.completed)
                    onToolCallUpdate?.invoke(accumulatedToolCalls.toList())
                }
                outcome.response
            }
        }
        // Join before awaiting so sibling jobs always run to completion even
        // if one fails unexpectedly; only cancellation interrupts the wait.
        jobs.forEach { it.join() }
        jobs.map { it.await() }
    }

    private data class PreparedToolCall(
        val fc: GeminiPart.FunctionCall,
        val resolvedToolName: String,
        val server: McpServer?,
        val isBuiltin: Boolean,
        val toolCall: McpToolCall,
    )

    private fun prepareToolCall(
        fc: GeminiPart.FunctionCall,
        toolNameToToolMap: Map<String, McpTool>,
        sanitizedToOriginalName: Map<String, String>,
        serverMap: Map<String, McpServer>,
    ): PreparedToolCall {
        // Gemini receives sanitized names; map them back so tool execution
        // uses the original MCP tool name.
        val originalToolName = toolNameToToolMap[fc.name]?.name
            ?: sanitizedToOriginalName[fc.name]
        val toolDef = originalToolName?.let { toolNameToToolMap[it] }
        val server = toolDef?.let { def -> serverMap[def.serverId] }
        val isBuiltin = toolDef?.serverId == BuiltinResourceTools.SERVER_ID

        return PreparedToolCall(
            fc = fc,
            resolvedToolName = originalToolName ?: fc.name,
            server = server,
            isBuiltin = isBuiltin,
            toolCall = McpToolCall(
                callId = UUID.randomUUID().toString(),
                serverId = server?.id ?: BuiltinResourceTools.SERVER_ID,
                serverName = server?.name ?: BuiltinResourceTools.SERVER_NAME,
                toolName = originalToolName ?: fc.name,
                inputArguments = fc.args,
                status = ToolExecutionStatus.EXECUTING,
                thoughtSignature = fc.thoughtSignature,
            ),
        )
    }

    private data class ToolExecution(
        val execResult: String,
        val isSuccess: Boolean,
        val latencyMs: Long,
        val errorMessage: String?,
    )

    private data class ToolCallOutcome(
        val completed: McpToolCall,
        val response: GeminiPart.FunctionResponse,
    )

    private suspend fun runPreparedToolCall(prepared: PreparedToolCall): ToolCallOutcome {
        val execution = try {
            performToolExecution(prepared)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Isolation: an unexpected failure becomes this call's error
            // result; sibling calls keep running.
            ToolExecution(
                execResult = JSONObject().put("error", "Tool execution failed: ${e.message}").toString(),
                isSuccess = false,
                latencyMs = 0,
                errorMessage = e.message ?: e.javaClass.simpleName,
            )
        }

        val completed = prepared.toolCall.copy(
            outputResult = execution.execResult,
            isSuccess = execution.isSuccess,
            status = if (execution.isSuccess) ToolExecutionStatus.SUCCESS else ToolExecutionStatus.ERROR,
            executionTimeMs = execution.latencyMs,
            errorMessage = execution.errorMessage,
        )
        return ToolCallOutcome(
            completed = completed,
            response = GeminiPart.FunctionResponse(
                name = prepared.fc.name,
                response = parseResultToMap(execution.execResult),
                thoughtSignature = prepared.fc.thoughtSignature,
            ),
        )
    }

    private suspend fun performToolExecution(prepared: PreparedToolCall): ToolExecution {
        if (prepared.isBuiltin) {
            val start = System.currentTimeMillis()
            val result = BuiltinResourceTools.execute(
                prepared.resolvedToolName,
                prepared.fc.args,
                mcpRepository,
            )
            val failed = try {
                JSONObject(result).has("error")
            } catch (_: Exception) {
                false
            }
            return ToolExecution(
                execResult = result,
                isSuccess = !failed,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = if (failed) result else null,
            )
        }
        if (prepared.server == null) {
            // Guard unknown tool (provider hallucinated a name or server was
            // disabled after tool discovery) — fail fast without calling MCP
            // over an empty endpoint.
            return ToolExecution(
                execResult = JSONObject()
                    .put("error", "Unknown tool '${prepared.resolvedToolName}' — no enabled MCP server provides this tool")
                    .toString(),
                isSuccess = false,
                latencyMs = 0,
                errorMessage = "Unknown tool",
            )
        }
        val (result, logItem) = mcpRepository.executeTool(
            prepared.server,
            prepared.resolvedToolName,
            prepared.fc.args,
        )
        return ToolExecution(
            execResult = result,
            isSuccess = logItem.isSuccess,
            latencyMs = logItem.latencyMs,
            errorMessage = logItem.errorMessage,
        )
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
