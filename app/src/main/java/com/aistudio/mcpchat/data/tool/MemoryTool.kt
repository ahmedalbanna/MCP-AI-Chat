package com.aistudio.mcpchat.data.tool

import android.util.Log
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.MemoryNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MemoryTool(private val database: AppDatabase) {
    private val memoryDao = database.memoryNoteDao()

    suspend fun handleRequest(toolName: String, arguments: Map<String, Any?>): String {
        return withContext(Dispatchers.IO) {
            try {
                when (toolName) {
                    "save_memory_note" -> saveNote(arguments)
                    "search_memory_notes" -> searchNotes(arguments)
                    "list_memory_notes" -> listNotes(arguments)
                    "delete_memory_note" -> deleteNote(arguments)
                    else -> "Unknown memory tool: $toolName"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Memory tool error: ${e.message}")
                "Error: ${e.message}"
            }
        }
    }

    private suspend fun saveNote(arguments: Map<String, Any?>): String {
        val content = arguments["content"] as? String
            ?: return "Error: 'content' parameter is required"
        val tags = arguments["tags"] as? String ?: ""

        val note = MemoryNoteEntity(
            content = content,
            tags = tags,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val id = memoryDao.insertNote(note)
        return JSONObject().apply {
            put("status", "saved")
            put("id", id)
            put("message", "Note saved successfully")
        }.toString()
    }

    private suspend fun searchNotes(arguments: Map<String, Any?>): String {
        val query = arguments["query"] as? String
            ?: return "Error: 'query' parameter is required"

        val notes = memoryDao.searchNotes(query)
        if (notes.isEmpty()) {
            return JSONObject().apply {
                put("status", "no_results")
                put("query", query)
                put("message", "No notes found matching '$query'")
            }.toString()
        }

        val results = notes.map { note ->
            JSONObject().apply {
                put("id", note.id)
                put("content", note.content)
                put("tags", note.tags)
                put("createdAt", note.createdAt)
            }
        }

        return JSONObject().apply {
            put("status", "found")
            put("query", query)
            put("count", notes.size)
            put("results", results)
        }.toString()
    }

    private suspend fun listNotes(arguments: Map<String, Any?>): String {
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 10
        // Room Flows never complete on their own — take the first emission
        // instead of collecting forever.
        val notes = memoryDao.getAllNotes().first().take(limit)

        if (notes.isEmpty()) {
            return JSONObject().apply {
                put("status", "empty")
                put("message", "No memory notes stored yet")
            }.toString()
        }

        val results = notes.map { note ->
            JSONObject().apply {
                put("id", note.id)
                put("content", note.content)
                put("tags", note.tags)
                put("createdAt", note.createdAt)
            }
        }

        return JSONObject().apply {
            put("status", "listed")
            put("count", notes.size)
            put("notes", results)
        }.toString()
    }

    private suspend fun deleteNote(arguments: Map<String, Any?>): String {
        val id = (arguments["id"] as? Number)?.toLong()
            ?: return "Error: 'id' parameter is required"

        memoryDao.getNoteById(id)
            ?: return "Error: Note with id $id not found"

        memoryDao.deleteNoteById(id)
        return JSONObject().apply {
            put("status", "deleted")
            put("id", id)
            put("message", "Note deleted successfully")
        }.toString()
    }

    fun getToolDefinitions(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "name" to "save_memory_note",
                "description" to "Save a note to persistent memory for future reference",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "content" to mapOf("type" to "string", "description" to "The content to remember"),
                        "tags" to mapOf("type" to "string", "description" to "Comma-separated tags for categorization"),
                    ),
                    "required" to listOf("content"),
                ),
            ),
            mapOf(
                "name" to "search_memory_notes",
                "description" to "Search saved memory notes by content or tags",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "query" to mapOf("type" to "string", "description" to "Search query to match against notes"),
                    ),
                    "required" to listOf("query"),
                ),
            ),
            mapOf(
                "name" to "list_memory_notes",
                "description" to "List recent memory notes",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "limit" to mapOf("type" to "integer", "description" to "Maximum number of notes to return (default 10)"),
                    ),
                ),
            ),
            mapOf(
                "name" to "delete_memory_note",
                "description" to "Delete a memory note by its ID",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "id" to mapOf("type" to "integer", "description" to "The ID of the note to delete"),
                    ),
                    "required" to listOf("id"),
                ),
            ),
        )
    }

    private companion object {
        private const val TAG = "MemoryTool"
    }
}
