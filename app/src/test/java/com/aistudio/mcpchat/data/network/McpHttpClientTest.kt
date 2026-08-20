package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.ServerStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class McpHttpClientTest {

    private lateinit var client: McpHttpClient

    @Before
    fun setUp() {
        client = McpHttpClient()
    }

    private fun createServer(
        endpointUrl: String = "http://localhost:8080/mcp",
        authHeader: String = ""
    ) = McpServer(
        id = "test_server",
        name = "Test Server",
        description = "A test server",
        endpointUrl = endpointUrl,
        authHeader = authHeader
    )

    @Test
    fun `testConnection returns ERROR for unreachable server`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (status, latency) = client.testConnection(server)
        assertEquals(ServerStatus.ERROR, status)
        assertTrue(latency >= 0)
    }

    @Test
    fun `discoverTools returns empty list for unreachable server`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val tools = client.discoverTools(server)
        assertTrue(tools.isEmpty())
    }

    @Test
    fun `executeToolCall returns error result for unreachable server`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (result, logItem) = client.executeToolCall(server, "test_tool", emptyMap())
        assertNotNull(result)
        assertTrue(result.contains("error"))
        assertEquals(false, logItem.isSuccess)
    }

    @Test
    fun `executeToolCall returns logItem with correct server info`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (_, logItem) = client.executeToolCall(server, "my_tool", mapOf("key" to "value"))
        assertEquals("test_server", logItem.serverId)
        assertEquals("Test Server", logItem.serverName)
        assertEquals("my_tool", logItem.toolName)
    }

    @Test
    fun `executeToolCall sets latencyMs`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (_, logItem) = client.executeToolCall(server, "tool", emptyMap())
        assertTrue(logItem.latencyMs >= 0)
    }

    @Test
    fun `executeToolCall includes request payload in log`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (_, logItem) = client.executeToolCall(server, "tool", mapOf("arg1" to "val1"))
        assertNotNull(logItem.requestPayload)
        assertTrue(logItem.requestPayload.contains("tools/call"))
    }

    @Test
    fun `testConnection returns latency for unreachable server`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (_, latency) = client.testConnection(server)
        assertTrue(latency >= 0)
    }

    @Test
    fun `discoverTools with auth header does not crash`() = runTest {
        val server = createServer(
            endpointUrl = "http://localhost:1/mcp",
            authHeader = "Bearer token123"
        )
        val tools = client.discoverTools(server)
        assertTrue(tools.isEmpty())
    }
}
