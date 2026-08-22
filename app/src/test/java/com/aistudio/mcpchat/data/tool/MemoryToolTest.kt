package com.aistudio.mcpchat.data.tool

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryToolTest {

    private lateinit var database: com.aistudio.mcpchat.data.local.AppDatabase
    private lateinit var memoryTool: MemoryTool

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            com.aistudio.mcpchat.data.local.AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        memoryTool = MemoryTool(database)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun `handleRequest save_memory_note saves note and returns success`() = runTest {
        val result = memoryTool.handleRequest(
            "save_memory_note",
            mapOf(
                "content" to "Remember this",
                "tags" to "important",
            ),
        )
        val json = JSONObject(result)
        assertEquals("saved", json.getString("status"))
        assertTrue(json.getLong("id") > 0)
    }

    @Test
    fun `handleRequest save_memory_note without content returns error`() = runTest {
        val result = memoryTool.handleRequest("save_memory_note", emptyMap())
        assertTrue(result.contains("Error"))
    }

    @Test
    fun `handleRequest search_memory_notes finds matching notes`() = runTest {
        memoryTool.handleRequest("save_memory_note", mapOf("content" to "Kotlin is great", "tags" to "lang"))
        memoryTool.handleRequest("save_memory_note", mapOf("content" to "Python is also nice", "tags" to "lang"))

        val result = memoryTool.handleRequest("search_memory_notes", mapOf("query" to "Kotlin"))
        val json = JSONObject(result)
        assertEquals("found", json.getString("status"))
        assertEquals(1, json.getInt("count"))
    }

    @Test
    fun `handleRequest search_memory_notes with no results`() = runTest {
        val result = memoryTool.handleRequest("search_memory_notes", mapOf("query" to "nonexistent"))
        val json = JSONObject(result)
        assertEquals("no_results", json.getString("status"))
    }

    @Test
    fun `handleRequest search_memory_notes without query returns error`() = runTest {
        val result = memoryTool.handleRequest("search_memory_notes", emptyMap())
        assertTrue(result.contains("Error"))
    }

    @Test
    fun `handleRequest list_memory_notes lists saved notes`() = runTest {
        memoryTool.handleRequest("save_memory_note", mapOf("content" to "Note 1"))
        memoryTool.handleRequest("save_memory_note", mapOf("content" to "Note 2"))

        val result = memoryTool.handleRequest("list_memory_notes", emptyMap())
        val json = JSONObject(result)
        assertEquals("listed", json.getString("status"))
        assertEquals(2, json.getInt("count"))
    }

    @Test
    fun `handleRequest list_memory_notes respects limit`() = runTest {
        repeat(5) {
            memoryTool.handleRequest("save_memory_note", mapOf("content" to "Note $it"))
        }

        val result = memoryTool.handleRequest("list_memory_notes", mapOf("limit" to 2))
        val json = JSONObject(result)
        assertEquals(2, json.getInt("count"))
    }

    @Test
    fun `handleRequest list_memory_notes returns empty when no notes`() = runTest {
        val result = memoryTool.handleRequest("list_memory_notes", emptyMap())
        val json = JSONObject(result)
        assertEquals("empty", json.getString("status"))
    }

    @Test
    fun `handleRequest delete_memory_note deletes note`() = runTest {
        val saveResult = memoryTool.handleRequest("save_memory_note", mapOf("content" to "To delete"))
        val saveJson = JSONObject(saveResult)
        val noteId = saveJson.getLong("id")

        val result = memoryTool.handleRequest("delete_memory_note", mapOf("id" to noteId))
        val json = JSONObject(result)
        assertEquals("deleted", json.getString("status"))
    }

    @Test
    fun `handleRequest delete_memory_note with nonexistent id returns error`() = runTest {
        val result = memoryTool.handleRequest("delete_memory_note", mapOf("id" to 99999))
        assertTrue(result.contains("Error"))
    }

    @Test
    fun `handleRequest delete_memory_note without id returns error`() = runTest {
        val result = memoryTool.handleRequest("delete_memory_note", emptyMap())
        assertTrue(result.contains("Error"))
    }

    @Test
    fun `handleRequest unknown tool returns error`() = runTest {
        val result = memoryTool.handleRequest("unknown_tool", emptyMap())
        assertTrue(result.contains("Unknown"))
    }

    @Test
    fun `getToolDefinitions returns 4 tools`() {
        val definitions = memoryTool.getToolDefinitions()
        assertEquals(4, definitions.size)
    }

    @Test
    fun `getToolDefinitions tool names are correct`() {
        val definitions = memoryTool.getToolDefinitions()
        val names = definitions.map { it["name"] as String }
        assertTrue(names.contains("save_memory_note"))
        assertTrue(names.contains("search_memory_notes"))
        assertTrue(names.contains("list_memory_notes"))
        assertTrue(names.contains("delete_memory_note"))
    }
}
