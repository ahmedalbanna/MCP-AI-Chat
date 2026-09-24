package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.GeminiPart
import com.aistudio.mcpchat.data.model.McpTool
import java.security.MessageDigest

enum class ProviderToolNamePolicy(
    private val maxLength: Int,
    private val invalidCharacters: Regex,
) {
    GEMINI(63, Regex("[^a-zA-Z0-9_]")),
    OPENAI_COMPATIBLE(64, Regex("[^a-zA-Z0-9_-]")),
    ANTHROPIC(64, Regex("[^a-zA-Z0-9_-]")),
    ;

    fun sanitize(name: String): String {
        val normalized = name.replace(invalidCharacters, "_").ifBlank { return "tool" }
        val validStart = if (this == GEMINI && normalized.first().isDigit()) "_$normalized" else normalized
        return validStart.take(maxLength)
    }

    fun disambiguate(baseName: String, toolId: String, attempt: Int): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(toolId.toByteArray())
            .take(6)
            .joinToString("") { byte ->
                val value = byte.toInt().mod(256)
                "${HEX[value / 16]}${HEX[value % 16]}"
            }
        val ordinal = if (attempt == 1) "" else "_$attempt"
        val suffix = "_$hash$ordinal"
        return baseName.take(maxLength - suffix.length) + suffix
    }

    private companion object {
        val HEX = "0123456789abcdef".toCharArray()
    }
}

class ProviderToolCatalog private constructor(
    val tools: List<McpTool>,
    private val wireNamesByToolId: Map<String, String>,
    private val toolsByWireName: Map<String, McpTool>,
    private val policy: ProviderToolNamePolicy,
) {
    fun wireName(toolId: String?, originalName: String): String {
        wireNamesByToolId[toolId]?.let { return it }
        if (toolsByWireName.isEmpty() && toolId == null) return policy.sanitize(originalName)

        val baseName = policy.sanitize(originalName)
        val identity = toolId?.let { "missing:$it" } ?: "missing-name:$originalName"
        var attempt = 1
        var candidate: String
        do {
            candidate = policy.disambiguate(baseName, identity, attempt)
            attempt++
        } while (candidate in toolsByWireName)
        return candidate
    }

    fun resolve(wireName: String): McpTool? = toolsByWireName[wireName]

    fun functionCall(wireName: String, args: Map<String, Any?>, thoughtSignature: String? = null): GeminiPart.FunctionCall {
        val tool = resolve(wireName)
        return GeminiPart.FunctionCall(
            name = tool?.name ?: wireName,
            args = args,
            thoughtSignature = thoughtSignature,
            toolId = tool?.id,
        )
    }

    companion object {
        fun create(tools: List<McpTool>, policy: ProviderToolNamePolicy): ProviderToolCatalog {
            val sortedTools = tools.sortedBy { it.id }
            require(sortedTools.map { it.id }.distinct().size == sortedTools.size) {
                "Provider tool ids must be unique"
            }

            val wireNamesByToolId = mutableMapOf<String, String>()
            val toolsByWireName = mutableMapOf<String, McpTool>()
            val usedWireNames = mutableSetOf<String>()
            sortedTools.forEach { tool ->
                val baseName = policy.sanitize(tool.name)
                var attempt = 0
                var wireName: String
                do {
                    wireName = if (attempt == 0) baseName else policy.disambiguate(baseName, tool.id, attempt)
                    attempt++
                } while (!usedWireNames.add(wireName))
                wireNamesByToolId[tool.id] = wireName
                toolsByWireName[wireName] = tool
            }

            return ProviderToolCatalog(
                tools = sortedTools,
                wireNamesByToolId = wireNamesByToolId,
                toolsByWireName = toolsByWireName,
                policy = policy,
            )
        }
    }
}
