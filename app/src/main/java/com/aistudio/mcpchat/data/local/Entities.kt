package com.aistudio.mcpchat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val endpointUrl: String,
    val authHeader: String = "",
    val isEnabled: Boolean = true,
    val isPreset: Boolean = false,
    val status: String = "DISCONNECTED",
    val latencyMs: Long = 0,
    val protocolVersion: String = "2024-11-05",
    val toolsCount: Int = 0,
    val lastSyncTime: Long = 0,
)

@Entity(tableName = "mcp_tools")
data class McpToolEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val serverName: String,
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val isEnabled: Boolean = true,
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val model: String = "gemini-3.5-flash",
    val systemPrompt: String = "",
    val enabledServerIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,

    // One of "user", "assistant", "system"
    val role: String,
    val content: String,

    // JSON-serialized List<McpToolCall>
    val toolCallsJson: String? = null,
    val latencyMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

@Entity(tableName = "mcp_logs")
data class McpLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String,
    val serverName: String,
    val toolName: String,
    val requestPayload: String,
    val responsePayload: String,
    val isSuccess: Boolean,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
)

@Entity(tableName = "memory_notes")
data class MemoryNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
