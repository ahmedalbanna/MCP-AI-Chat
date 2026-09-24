package com.aistudio.mcpchat.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseLifecycleTest {

    private lateinit var database: AppDatabase
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `database is created with correct version`() {
        assertEquals(6, database.openHelper.writableDatabase.version)
    }

    @Test
    fun `database starts empty with no pre-seeded data`() = runTest {
        val servers = database.mcpServerDao().getAllServers().first()
        val tools = database.mcpToolDao().getAllTools().first()
        val sessions = database.chatDao().getAllSessions().first()
        val logs = database.mcpLogDao().getAllLogs().first()
        val notes = database.memoryNoteDao().getAllNotes().first()

        assertTrue(servers.isEmpty())
        assertTrue(tools.isEmpty())
        assertTrue(sessions.isEmpty())
        assertTrue(logs.isEmpty())
        assertTrue(notes.isEmpty())
    }

    @Test
    fun `entity field defaults are applied correctly for McpServerEntity`() = runTest {
        val server = McpServerEntity(
            id = "test_server",
            name = "Test Server",
            description = "A test server",
            endpointUrl = "http://localhost:3000",
        )
        database.mcpServerDao().insertServer(server)

        val retrieved = database.mcpServerDao().getServerById("test_server")
        assertNotNull(retrieved)
        assertEquals(true, retrieved?.isEnabled)
        assertEquals(false, retrieved?.isPreset)
        assertEquals("DISCONNECTED", retrieved?.status)
        assertEquals(0L, retrieved?.latencyMs)
        assertEquals("2024-11-05", retrieved?.protocolVersion)
        assertEquals(0, retrieved?.toolsCount)
        assertEquals(0L, retrieved?.lastSyncTime)
        assertEquals("", retrieved?.authHeader)
    }

    @Test
    fun `entity field defaults are applied correctly for ChatSessionEntity`() = runTest {
        val session = ChatSessionEntity(
            id = "test_session",
            title = "Test Session",
        )
        database.chatDao().insertSession(session)

        val retrieved = database.chatDao().getSessionById("test_session")
        assertNotNull(retrieved)
        assertEquals("gemini-3.6-flash", retrieved?.model)
        assertEquals("", retrieved?.systemPrompt)
        assertEquals("[]", retrieved?.enabledServerIdsJson)
        assertEquals(false, retrieved?.isPinned)
        assertTrue(retrieved?.createdAt!! > 0)
        assertTrue(retrieved?.updatedAt!! > 0)
    }

    @Test
    fun `entity field defaults are applied correctly for ChatMessageEntity`() = runTest {
        val message = ChatMessageEntity(
            id = "test_msg",
            sessionId = "test_session",
            role = "user",
            content = "Hello",
        )
        database.chatDao().insertMessage(message)

        val messages = database.chatDao().getMessagesList("test_session")
        assertEquals(1, messages.size)
        assertEquals(null, messages[0].toolCallsJson)
        assertEquals(0L, messages[0].latencyMs)
        assertEquals(false, messages[0].isError)
        assertTrue(messages[0].timestamp > 0)
    }

    @Test
    fun `entity field defaults are applied correctly for McpLogEntity`() = runTest {
        val log = McpLogEntity(
            serverId = "srv1",
            serverName = "Server 1",
            toolName = "tool1",
            requestPayload = "{}",
            responsePayload = "{}",
            isSuccess = true,
            latencyMs = 100,
        )
        database.mcpLogDao().insertLog(log)

        val logs = database.mcpLogDao().getAllLogs().first()
        assertEquals(1, logs.size)
        // autoGenerate replaces the sentinel id=0 with a real row id.
        assertTrue(logs[0].id > 0)
        assertEquals(null, logs[0].errorMessage)
        assertTrue(logs[0].timestamp > 0)
    }

    @Test
    fun `entity field defaults are applied correctly for MemoryNoteEntity`() = runTest {
        val note = MemoryNoteEntity(
            content = "Test note",
        )
        val id = database.memoryNoteDao().insertNote(note)

        val retrieved = database.memoryNoteDao().getNoteById(id)
        assertNotNull(retrieved)
        assertEquals("", retrieved?.tags)
        assertTrue(retrieved?.createdAt!! > 0)
        assertTrue(retrieved?.updatedAt!! > 0)
    }

    @Test
    fun `entity field defaults are applied correctly for McpToolEntity`() = runTest {
        val tool = McpToolEntity(
            id = "tool1",
            serverId = "srv1",
            serverName = "Server 1",
            name = "get_weather",
            description = "Get weather",
            inputSchemaJson = "{}",
        )
        database.mcpToolDao().insertTools(listOf(tool))

        val tools = database.mcpToolDao().getAllTools().first()
        assertEquals(1, tools.size)
        assertEquals(true, tools[0].isEnabled)
    }

    @Test
    fun `database close and reopen preserves data`() {
        val server = McpServerEntity(
            id = "persistent_server",
            name = "Persistent Server",
            description = "Should persist",
            endpointUrl = "http://test.com",
        )

        // Persistence only applies to file-backed databases; the shared setUp
        // database is in-memory.
        runTest {
            val db1 = Room.databaseBuilder(context, AppDatabase::class.java, "test_db_lifecycle")
                .allowMainThreadQueries()
                .build()
            db1.mcpServerDao().insertServer(server)
            db1.close()
        }

        val newDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "test_db_lifecycle")
            .allowMainThreadQueries()
            .build()

        runTest {
            val retrieved = newDatabase.mcpServerDao().getServerById("persistent_server")
            assertNotNull(retrieved)
            assertEquals("Persistent Server", retrieved?.name)
        }

        newDatabase.close()
        context.deleteDatabase("test_db_lifecycle")
    }

    @Test
    fun `singleton pattern returns same instance`() {
        val db1 = AppDatabase.getDatabase(context)
        val db2 = AppDatabase.getDatabase(context)
        assertEquals(db1, db2)
    }

    @Test
    fun `concurrent reads do not block each other`() = runTest {
        val results = coroutineScope {
            (1..2).map {
                async(Dispatchers.IO) { database.mcpServerDao().getAllServers().first() }
            }.awaitAll()
        }
        assertEquals(2, results.size)
    }

    @Test
    fun `Flow emits on data insert`() = runTest {
        val initialSessions = database.chatDao().getAllSessions().first()
        assertTrue(initialSessions.isEmpty())

        val session = ChatSessionEntity(id = "new_session", title = "New Session")
        database.chatDao().insertSession(session)

        val updatedSessions = database.chatDao().getAllSessions().first()
        assertEquals(1, updatedSessions.size)
        assertEquals("New Session", updatedSessions[0].title)
    }

    @Test
    fun `Flow emits on data update`() = runTest {
        val server = McpServerEntity(
            id = "srv1",
            name = "Server 1",
            description = "desc",
            endpointUrl = "http://test.com",
        )
        database.mcpServerDao().insertServer(server)

        val initial = database.mcpServerDao().getAllServers().first()
        assertEquals("DISCONNECTED", initial[0].status)

        val updated = server.copy(status = "CONNECTED", latencyMs = 42)
        database.mcpServerDao().updateServer(updated)

        val afterUpdate = database.mcpServerDao().getAllServers().first()
        assertEquals("CONNECTED", afterUpdate[0].status)
        assertEquals(42L, afterUpdate[0].latencyMs)
    }

    @Test
    fun `Flow emits on data delete`() = runTest {
        val session = ChatSessionEntity(id = "to_delete", title = "Delete Me")
        database.chatDao().insertSession(session)

        val before = database.chatDao().getAllSessions().first()
        assertEquals(1, before.size)

        database.chatDao().deleteSessionById("to_delete")

        val after = database.chatDao().getAllSessions().first()
        assertTrue(after.isEmpty())
    }

    @Test
    fun `multiple Flow collectors receive same updates`() = runTest {
        val session = ChatSessionEntity(id = "s1", title = "Session 1")
        database.chatDao().insertSession(session)

        val flow1 = database.chatDao().getAllSessions()
        val flow2 = database.chatDao().getAllSessions()

        val result1 = flow1.first()
        val result2 = flow2.first()

        assertEquals(result1.size, result2.size)
        assertEquals(result1[0].id, result2[0].id)
    }

    @Test
    fun `large dataset does not cause issues`() = runTest {
        val sessions = (1..100).map {
            ChatSessionEntity(id = "session_$it", title = "Session $it")
        }

        sessions.forEach { database.chatDao().insertSession(it) }

        val allSessions = database.chatDao().getAllSessions().first()
        assertEquals(100, allSessions.size)
    }

    @Test
    fun `memory notes full CRUD lifecycle`() = runTest {
        val note1 = MemoryNoteEntity(content = "First note", tags = "important")
        val note2 = MemoryNoteEntity(content = "Second note", tags = "reference")

        val id1 = database.memoryNoteDao().insertNote(note1)
        val id2 = database.memoryNoteDao().insertNote(note2)

        val allNotes = database.memoryNoteDao().getAllNotes().first()
        assertEquals(2, allNotes.size)

        val retrieved = database.memoryNoteDao().getNoteById(id1)
        assertNotNull(retrieved)
        assertEquals("First note", retrieved?.content)

        val searchResults = database.memoryNoteDao().searchNotes("First")
        assertEquals(1, searchResults.size)

        database.memoryNoteDao().deleteNoteById(id1)
        val afterDelete = database.memoryNoteDao().getAllNotes().first()
        assertEquals(1, afterDelete.size)
        assertEquals(id2, afterDelete[0].id)
    }

    @Test
    fun `mcp_logs ordered by timestamp descending`() = runTest {
        val log1 = McpLogEntity(
            serverId = "srv1",
            serverName = "S1",
            toolName = "t1",
            requestPayload = "{}",
            responsePayload = "{}",
            isSuccess = true,
            latencyMs = 10,
            timestamp = 1000,
        )
        val log2 = McpLogEntity(
            serverId = "srv1",
            serverName = "S1",
            toolName = "t2",
            requestPayload = "{}",
            responsePayload = "{}",
            isSuccess = true,
            latencyMs = 20,
            timestamp = 2000,
        )

        database.mcpLogDao().insertLog(log1)
        database.mcpLogDao().insertLog(log2)

        val logs = database.mcpLogDao().getAllLogs().first()
        assertEquals(2, logs.size)
        assertTrue(logs[0].timestamp >= logs[1].timestamp)
    }

    @Test
    fun `chat messages ordered by timestamp ascending`() = runTest {
        val session = ChatSessionEntity(id = "s1", title = "Test")
        database.chatDao().insertSession(session)

        val msg1 = ChatMessageEntity(
            id = "m1",
            sessionId = "s1",
            role = "user",
            content = "Hello",
            timestamp = 1000,
        )
        val msg2 = ChatMessageEntity(
            id = "m2",
            sessionId = "s1",
            role = "assistant",
            content = "Hi",
            timestamp = 2000,
        )

        database.chatDao().insertMessage(msg1)
        database.chatDao().insertMessage(msg2)

        val messages = database.chatDao().getMessagesList("s1")
        assertEquals(2, messages.size)
        assertEquals("Hello", messages[0].content)
        assertEquals("Hi", messages[1].content)
    }

    @Test
    fun `session ordering by pinned and updatedAt`() = runTest {
        val session1 = ChatSessionEntity(
            id = "s1",
            title = "Unpinned",
            isPinned = false,
            updatedAt = 1000,
        )
        val session2 = ChatSessionEntity(
            id = "s2",
            title = "Pinned",
            isPinned = true,
            updatedAt = 2000,
        )
        val session3 = ChatSessionEntity(
            id = "s3",
            title = "Another Pinned",
            isPinned = true,
            updatedAt = 3000,
        )

        database.chatDao().insertSession(session1)
        database.chatDao().insertSession(session2)
        database.chatDao().insertSession(session3)

        val sessions = database.chatDao().getAllSessions().first()
        assertEquals(3, sessions.size)
        assertTrue(sessions[0].isPinned)
        assertTrue(sessions[1].isPinned)
        assertFalse(sessions[2].isPinned)
    }
}
