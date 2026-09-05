package com.aistudio.mcpchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface McpServerDao {
    @Query("SELECT * FROM mcp_servers ORDER BY isPreset DESC, name ASC")
    fun getAllServers(): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE id = :id")
    suspend fun getServerById(id: String): McpServerEntity?

    @Query("SELECT * FROM mcp_servers WHERE isEnabled = 1")
    suspend fun getEnabledServersList(): List<McpServerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<McpServerEntity>)

    @Update
    suspend fun updateServer(server: McpServerEntity)

    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteServerById(id: String)
}

@Dao
interface McpToolDao {
    @Query("SELECT * FROM mcp_tools WHERE serverId = :serverId ORDER BY name ASC")
    fun getToolsForServer(serverId: String): Flow<List<McpToolEntity>>

    @Query("SELECT * FROM mcp_tools ORDER BY serverName ASC, name ASC")
    fun getAllTools(): Flow<List<McpToolEntity>>

    @Query("SELECT * FROM mcp_tools WHERE isEnabled = 1")
    suspend fun getAllEnabledTools(): List<McpToolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<McpToolEntity>)

    @Query("DELETE FROM mcp_tools WHERE serverId = :serverId")
    suspend fun deleteToolsForServer(serverId: String)

    @Query("UPDATE mcp_tools SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setToolEnabled(id: String, isEnabled: Boolean)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesList(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface McpLogDao {
    @Query("SELECT * FROM mcp_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<McpLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: McpLogEntity)

    @Query("DELETE FROM mcp_logs")
    suspend fun clearLogs()
}

@Dao
interface MemoryNoteDao {
    @Query("SELECT * FROM memory_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<MemoryNoteEntity>>

    @Query("SELECT * FROM memory_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): MemoryNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: MemoryNoteEntity): Long

    @Update
    suspend fun updateNote(note: MemoryNoteEntity)

    @Query("DELETE FROM memory_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM memory_notes WHERE content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchNotes(query: String): List<MemoryNoteEntity>

    @Query("DELETE FROM memory_notes")
    suspend fun clearAllNotes()
}
