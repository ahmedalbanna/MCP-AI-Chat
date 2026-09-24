package com.aistudio.mcpchat.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val endpointUrl: String,
    val authHeader: String = "",
    val allowInsecureHttp: Boolean = false,
    val isEnabled: Boolean = true,
    val isPreset: Boolean = false,
    val status: String = "DISCONNECTED",
    val latencyMs: Long = 0,
    val protocolVersion: String = "2024-11-05",
    val toolsCount: Int = 0,
    val lastSyncTime: Long = 0,
    val sessionTtlMs: Long = 0,
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
    val model: String = "gemini-3.6-flash",
    val systemPrompt: String = "",
    val enabledServerIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId", "timestamp"])],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,

    // One of "user", "assistant", "system"
    val role: String,
    val content: String,

    // JSON-serialized List<McpToolCall>
    val toolCallsJson: String? = null,

    // JSON-serialized List<ImageAttachment>; bytes stay on disk, only URIs persist
    val attachmentsJson: String? = null,
    val latencyMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

/**
 * Full-text index over [ChatMessageEntity.content]. Room keeps it in sync
 * with the content table via triggers; queries JOIN through `rowid`.
 */
@Fts4(contentEntity = ChatMessageEntity::class)
@Entity(tableName = "chat_messages_fts")
data class ChatMessageFtsEntity(
    val content: String,
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
