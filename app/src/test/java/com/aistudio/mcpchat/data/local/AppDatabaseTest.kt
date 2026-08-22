package com.aistudio.mcpchat.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var serverDao: McpServerDao
    private lateinit var toolDao: McpToolDao
    private lateinit var chatDao: ChatDao
    private lateinit var logDao: McpLogDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        serverDao = database.mcpServerDao()
        toolDao = database.mcpToolDao()
        chatDao = database.chatDao()
        logDao = database.mcpLogDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun createServer(id: String = "server_1", name: String = "Test Server", endpointUrl: String = "https://test.example.com/mcp") =
        McpServerEntity(
            id = id,
            name = name,
            description = "A test server",
            endpointUrl = endpointUrl,
            isEnabled = true,
        )

    private fun createTool(id: String = "tool_1", serverId: String = "server_1", name: String = "test_tool") = McpToolEntity(
        id = id,
        serverId = serverId,
        serverName = "Test Server",
        name = name,
        description = "A test tool",
        inputSchemaJson = "{}",
        isEnabled = true,
    )

    private fun createSession(id: String = "session_1", title: String = "Test Session") = ChatSessionEntity(
        id = id,
        title = title,
    )

    private fun createMessage(id: String = "msg_1", sessionId: String = "session_1", role: String = "user", content: String = "Hello") =
        ChatMessageEntity(
            id = id,
            sessionId = sessionId,
            role = role,
            content = content,
        )

    private fun createLog(serverId: String = "server_1", toolName: String = "test_tool") = McpLogEntity(
        serverId = serverId,
        serverName = "Test Server",
        toolName = toolName,
        requestPayload = "{}",
        responsePayload = "{}",
        isSuccess = true,
        latencyMs = 100,
    )

    // ---- McpServerDao Tests ----

    @Test
    fun serverDao_insertAndGetAll() = runTest {
        val server = createServer()
        serverDao.insertServer(server)

        val servers = serverDao.getAllServers().first()
        assertEquals(1, servers.size)
        assertEquals("server_1", servers[0].id)
        assertEquals("Test Server", servers[0].name)
    }

    @Test
    fun serverDao_getServerById() = runTest {
        val server = createServer()
        serverDao.insertServer(server)

        val found = serverDao.getServerById("server_1")
        assertNotNull(found)
        assertEquals("Test Server", found!!.name)
    }

    @Test
    fun serverDao_getServerById_notFound() = runTest {
        val found = serverDao.getServerById("nonexistent")
        assertNull(found)
    }

    @Test
    fun serverDao_getEnabledServers() = runTest {
        serverDao.insertServer(createServer(id = "s1", name = "Enabled"))
        serverDao.insertServer(createServer(id = "s2", name = "Disabled").copy(isEnabled = false))

        val enabled = serverDao.getEnabledServersList()
        assertEquals(1, enabled.size)
        assertEquals("Enabled", enabled[0].name)
    }

    @Test
    fun serverDao_updateServer() = runTest {
        serverDao.insertServer(createServer())
        val updated = createServer(name = "Updated Server")
        serverDao.updateServer(updated)

        val found = serverDao.getServerById("server_1")
        assertEquals("Updated Server", found!!.name)
    }

    @Test
    fun serverDao_deleteServer() = runTest {
        serverDao.insertServer(createServer())
        serverDao.deleteServerById("server_1")

        val found = serverDao.getServerById("server_1")
        assertNull(found)
    }

    @Test
    fun serverDao_insertReplace() = runTest {
        serverDao.insertServer(createServer(name = "Original"))
        serverDao.insertServer(createServer(name = "Replaced"))

        val servers = serverDao.getAllServers().first()
        assertEquals(1, servers.size)
        assertEquals("Replaced", servers[0].name)
    }

    // ---- McpToolDao Tests ----

    @Test
    fun toolDao_insertAndGetAll() = runTest {
        toolDao.insertTools(listOf(createTool()))

        val tools = toolDao.getAllTools().first()
        assertEquals(1, tools.size)
        assertEquals("test_tool", tools[0].name)
    }

    @Test
    fun toolDao_getToolsForServer() = runTest {
        toolDao.insertTools(
            listOf(
                createTool(id = "t1", serverId = "s1"),
                createTool(id = "t2", serverId = "s2"),
                createTool(id = "t3", serverId = "s1"),
            ),
        )

        val tools = toolDao.getToolsForServer("s1").first()
        assertEquals(2, tools.size)
    }

    @Test
    fun toolDao_getEnabledTools() = runTest {
        toolDao.insertTools(
            listOf(
                createTool(id = "t1"),
                createTool(id = "t2").copy(isEnabled = false),
            ),
        )

        val enabled = toolDao.getAllEnabledTools()
        assertEquals(1, enabled.size)
        assertEquals("t1", enabled[0].id)
    }

    @Test
    fun toolDao_setToolEnabled() = runTest {
        toolDao.insertTools(listOf(createTool()))
        toolDao.setToolEnabled("tool_1", false)

        val tools = toolDao.getAllEnabledTools()
        assertEquals(0, tools.size)
    }

    @Test
    fun toolDao_deleteToolsForServer() = runTest {
        toolDao.insertTools(
            listOf(
                createTool(id = "t1", serverId = "s1"),
                createTool(id = "t2", serverId = "s2"),
            ),
        )
        toolDao.deleteToolsForServer("s1")

        val remaining = toolDao.getAllTools().first()
        assertEquals(1, remaining.size)
        assertEquals("s2", remaining[0].serverId)
    }

    // ---- ChatDao Tests ----

    @Test
    fun chatDao_insertAndGetSession() = runTest {
        chatDao.insertSession(createSession())

        val session = chatDao.getSessionById("session_1")
        assertNotNull(session)
        assertEquals("Test Session", session!!.title)
    }

    @Test
    fun chatDao_getAllSessions() = runTest {
        chatDao.insertSession(createSession(id = "s1", title = "First"))
        chatDao.insertSession(createSession(id = "s2", title = "Second"))

        val sessions = chatDao.getAllSessions().first()
        assertEquals(2, sessions.size)
    }

    @Test
    fun chatDao_updateSession() = runTest {
        chatDao.insertSession(createSession())
        chatDao.updateSession(createSession(title = "Updated"))

        val session = chatDao.getSessionById("session_1")
        assertEquals("Updated", session!!.title)
    }

    @Test
    fun chatDao_deleteSession() = runTest {
        chatDao.insertSession(createSession())
        chatDao.deleteSessionById("session_1")

        val session = chatDao.getSessionById("session_1")
        assertNull(session)
    }

    @Test
    fun chatDao_insertAndGetMessages() = runTest {
        chatDao.insertMessage(createMessage(id = "m1"))
        chatDao.insertMessage(createMessage(id = "m2", content = "World"))

        val messages = chatDao.getMessagesList("session_1")
        assertEquals(2, messages.size)
        assertEquals("Hello", messages[0].content)
        assertEquals("World", messages[1].content)
    }

    @Test
    fun chatDao_deleteMessagesForSession() = runTest {
        chatDao.insertMessage(createMessage(id = "m1"))
        chatDao.insertMessage(createMessage(id = "m2"))
        chatDao.deleteMessagesForSession("session_1")

        val messages = chatDao.getMessagesList("session_1")
        assertEquals(0, messages.size)
    }

    @Test
    fun chatDao_messagesFlow() = runTest {
        chatDao.insertMessage(createMessage())
        val flowMessages = chatDao.getMessagesForSession("session_1").first()
        assertEquals(1, flowMessages.size)
    }

    @Test
    fun chatDao_sessionNotFound() = runTest {
        val session = chatDao.getSessionById("nonexistent")
        assertNull(session)
    }

    // ---- McpLogDao Tests ----

    @Test
    fun logDao_insertAndGetAll() = runTest {
        logDao.insertLog(createLog())

        val logs = logDao.getAllLogs().first()
        assertEquals(1, logs.size)
        assertEquals("test_tool", logs[0].toolName)
    }

    @Test
    fun logDao_clearLogs() = runTest {
        logDao.insertLog(createLog())
        logDao.insertLog(createLog(toolName = "another_tool"))

        logDao.clearLogs()

        val logs = logDao.getAllLogs().first()
        assertEquals(0, logs.size)
    }

    @Test
    fun logDao_orderedByTimestampDesc() = runTest {
        logDao.insertLog(createLog(toolName = "first"))
        logDao.insertLog(createLog(toolName = "second"))

        val logs = logDao.getAllLogs().first()
        assertEquals("second", logs[0].toolName)
        assertEquals("first", logs[1].toolName)
    }

    // ---- Cross-table Tests ----

    @Test
    fun cascadeDelete_serverAndTools() = runTest {
        serverDao.insertServer(createServer(id = "s1"))
        toolDao.insertTools(
            listOf(
                createTool(id = "t1", serverId = "s1"),
                createTool(id = "t2", serverId = "s1"),
            ),
        )

        serverDao.deleteServerById("s1")

        val servers = serverDao.getAllServers().first()
        assertEquals(0, servers.size)
    }
}
