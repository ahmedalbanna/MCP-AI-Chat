package com.aistudio.mcpchat.data.model

import org.json.JSONObject

data class McpServer(
    val id: String,
    val name: String,
    val description: String,
    val endpointUrl: String,
    val authHeader: String = "",
    val isEnabled: Boolean = true,
    val isPreset: Boolean = false,
    val status: ServerStatus = ServerStatus.DISCONNECTED,
    val latencyMs: Long = 0,
    val protocolVersion: String = "2024-11-05",
    val toolsCount: Int = 0,
    val lastSyncTime: Long = 0
)

enum class ServerStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}

data class McpTool(
    val id: String,
    val serverId: String,
    val serverName: String,
    val name: String,
    val description: String,
    val inputSchemaJson: String = "{}",
    val isEnabled: Boolean = true
)

data class McpToolCall(
    val callId: String,
    val serverId: String,
    val serverName: String,
    val toolName: String,
    val inputArguments: Map<String, Any?>,
    val outputResult: String? = null,
    val isSuccess: Boolean = true,
    val executionTimeMs: Long = 0,
    val status: ToolExecutionStatus = ToolExecutionStatus.PENDING,
    val thoughtSignature: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ToolExecutionStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    ERROR
}

data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

data class McpPrompt(
    val name: String,
    val description: String? = null,
    val arguments: List<String> = emptyList()
)

data class McpLogItem(
    val id: Long = 0,
    val serverId: String,
    val serverName: String,
    val toolName: String,
    val requestPayload: String,
    val responsePayload: String,
    val isSuccess: Boolean,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
