package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.ServerStatus
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class McpHttpClientTest {

    private lateinit var client: McpHttpClient

    @Before
    fun setUp() {
        client = McpHttpClient(
            client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build(),
        )
    }

    private fun createServer(endpointUrl: String = "http://localhost:8080/mcp", authHeader: String = "", sessionTtlMs: Long = 0) = McpServer(
        id = "test_server",
        name = "Test Server",
        description = "A test server",
        endpointUrl = endpointUrl,
        authHeader = authHeader,
        sessionTtlMs = sessionTtlMs,
    )

    @Test
    fun `default client uses documented MCP timeout contract`() {
        val defaultClient = McpHttpClient()
        val okHttp = defaultClient.defaultClientForTesting
        assertEquals(15000, okHttp.connectTimeoutMillis)
        assertEquals(30000, okHttp.readTimeoutMillis)
        assertEquals(30000, okHttp.writeTimeoutMillis)
    }

    @Test
    fun `testConnection returns ERROR for unreachable server`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (status, latency) = client.testConnection(server)
        assertEquals(ServerStatus.ERROR, status)
        assertTrue(latency >= 0)
    }

    @Test
    fun `testConnection records handshake failure reason for HTTP error`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(404),
            )
            val server = createServer(endpointUrl = mockServer.url("/mcp").toString())
            val (status, _) = client.testConnection(server)

            assertEquals(ServerStatus.ERROR, status)
            val reason = client.lastHandshakeError(server.id)
            assertNotNull(reason)
            assertTrue(reason!!.contains("HTTP 404"))
        } finally {
            mockServer.shutdown()
        }
    }

    @Test
    fun `testConnection records network failure reason for unreachable server`() = runTest {
        val server = createServer(endpointUrl = "http://localhost:1/mcp")
        val (status, _) = client.testConnection(server)

        assertEquals(ServerStatus.ERROR, status)
        assertNotNull(client.lastHandshakeError(server.id))
    }

    @Test
    fun `testConnection clears failure reason after later success`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(500))
            val server = createServer(endpointUrl = mockServer.url("/mcp").toString())
            val (first, _) = client.testConnection(server)
            assertEquals(ServerStatus.ERROR, first)
            assertTrue(client.lastHandshakeError(server.id)!!.contains("HTTP 500"))

            // initialize + notifications/initialized.
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody(
                    """{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05",
                       "capabilities":{},"serverInfo":{"name":"t","version":"1"}}}""",
                ),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )
            val (second, _) = client.testConnection(server)
            assertEquals(ServerStatus.CONNECTED, second)
            assertNull(client.lastHandshakeError(server.id))
        } finally {
            mockServer.shutdown()
        }
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
            authHeader = "Bearer token123",
        )
        val tools = client.discoverTools(server)
        assertTrue(tools.isEmpty())
    }

    @Test
    fun `initialize sends every configured header from JSON multi-header form`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            // initialize + notifications/initialized both POST to the endpoint.
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody(
                    """{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05",
                       "capabilities":{},"serverInfo":{"name":"t","version":"1"}}}""",
                ),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )

            val server = createServer(
                endpointUrl = mockServer.url("/mcp").toString(),
                authHeader = """{"Authorization":"Bearer t1","X-API-Key":"k1"}""",
            )
            val (status, _) = client.testConnection(server)

            assertEquals(ServerStatus.CONNECTED, status)
            val recorded = mockServer.takeRequest(10, TimeUnit.SECONDS)
            assertNotNull(recorded)
            assertEquals("Bearer t1", recorded!!.getHeader("Authorization"))
            assertEquals("k1", recorded.getHeader("X-API-Key"))
        } finally {
            mockServer.shutdown()
        }
    }

    @Test
    fun `initialize maps legacy bare auth value to Authorization header`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody(
                    """{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05",
                       "capabilities":{},"serverInfo":{"name":"t","version":"1"}}}""",
                ),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )

            val server = createServer(
                endpointUrl = mockServer.url("/mcp").toString(),
                authHeader = "Bearer legacy-token",
            )
            client.testConnection(server)

            val recorded = mockServer.takeRequest(10, TimeUnit.SECONDS)
            assertNotNull(recorded)
            assertEquals("Bearer legacy-token", recorded!!.getHeader("Authorization"))
        } finally {
            mockServer.shutdown()
        }
    }

    @Test
    fun `extractJsonRpcBody parses raw json`() {
        val raw = """{"jsonrpc":"2.0","id":1,"result":{"tools":[]}}"""
        val json = McpHttpClient.extractJsonRpcBody(raw)
        assertNotNull(json)
        assertEquals("2.0", json!!.getString("jsonrpc"))
        assertTrue(json.has("result"))
    }

    @Test
    fun `extractJsonRpcBody parses sse framed response`() {
        val raw = "event: message\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"tools\":[{\"name\":\"t\"}]}}\n\n"
        val json = McpHttpClient.extractJsonRpcBody(raw)
        assertNotNull(json)
        val tools = json!!.getJSONObject("result").getJSONArray("tools")
        assertEquals(1, tools.length())
        assertEquals("t", tools.getJSONObject(0).getString("name"))
    }

    @Test
    fun `extractJsonRpcBody parses error framed response`() {
        val raw = "event: message\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":\"server-error\",\"error\":{\"code\":-32600,\"message\":\"Bad Request: Missing session ID\"}}\n\n"
        val json = McpHttpClient.extractJsonRpcBody(raw)
        assertNotNull(json)
        assertEquals(-32600, json!!.getJSONObject("error").getInt("code"))
    }

    @Test
    fun `extractJsonRpcBody uses last data frame when multiple present`() {
        val raw = "event: message\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":1}\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":2}\n\n"
        val json = McpHttpClient.extractJsonRpcBody(raw)
        assertNotNull(json)
        assertEquals(2, json!!.getInt("id"))
    }

    @Test
    fun `extractJsonRpcBody returns null for blank body`() {
        assertNull(McpHttpClient.extractJsonRpcBody(""))
        assertNull(McpHttpClient.extractJsonRpcBody("   \n  "))
    }

    @Test
    fun `extractJsonRpcBody returns null for malformed content`() {
        assertNull(McpHttpClient.extractJsonRpcBody("{not json"))
        assertNull(McpHttpClient.extractJsonRpcBody("event: message\ndata: [1,2,3]\n"))
        assertNull(McpHttpClient.extractJsonRpcBody("event: ping\n\n"))
    }

    @Test
    fun `session cache expires after TTL and triggers fresh handshake`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).apply {
                    setHeader("Mcp-Session-Id", "session-1")
                    setBody(
                        """{"jsonrpc":"2.0","id":1,"result":{
                            "protocolVersion":"2024-11-05",
                            "capabilities":{},
                            "serverInfo":{"name":"t","version":"1"}
                        }}""",
                    )
                },
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200)
                    .setBody("""{"jsonrpc":"2.0","id":3,"result":{"tools":[]}}"""),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).apply {
                    setHeader("Mcp-Session-Id", "session-2")
                    setBody(
                        """{"jsonrpc":"2.0","id":1,"result":{
                            "protocolVersion":"2024-11-05",
                            "capabilities":{},
                            "serverInfo":{"name":"t","version":"1"}
                        }}""",
                    )
                },
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200)
                    .setBody("""{"jsonrpc":"2.0","id":3,"result":{"tools":[]}}"""),
            )

            val server = createServer(
                endpointUrl = mockServer.url("/mcp").toString(),
                sessionTtlMs = 50,
            )

            client.discoverTools(server)
            Thread.sleep(100)
            client.discoverTools(server)

            assertEquals(6, mockServer.requestCount)
        } finally {
            mockServer.shutdown()
        }
    }

    @Test
    fun `session cache is reused within TTL without re-handshake`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).apply {
                    setHeader("Mcp-Session-Id", "session-1")
                    setBody(
                        """{"jsonrpc":"2.0","id":1,"result":{
                            "protocolVersion":"2024-11-05",
                            "capabilities":{},
                            "serverInfo":{"name":"t","version":"1"}
                        }}""",
                    )
                },
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200)
                    .setBody("""{"jsonrpc":"2.0","id":3,"result":{"tools":[]}}"""),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200)
                    .setBody("""{"jsonrpc":"2.0","id":3,"result":{"tools":[]}}"""),
            )

            val server = createServer(
                endpointUrl = mockServer.url("/mcp").toString(),
                sessionTtlMs = 5000,
            )

            client.discoverTools(server)
            Thread.sleep(100)
            client.discoverTools(server)

            assertEquals(4, mockServer.requestCount)
        } finally {
            mockServer.shutdown()
        }
    }

    @Test
    fun `server with sessionTtlMs zero uses five minute default TTL`() = runTest {
        val mockServer = okhttp3.mockwebserver.MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).apply {
                    setHeader("Mcp-Session-Id", "session-1")
                    setBody(
                        """{"jsonrpc":"2.0","id":1,"result":{
                            "protocolVersion":"2024-11-05",
                            "capabilities":{},
                            "serverInfo":{"name":"t","version":"1"}
                        }}""",
                    )
                },
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200)
                    .setBody("""{"jsonrpc":"2.0","id":3,"result":{"tools":[]}}"""),
            )

            val server = createServer(
                endpointUrl = mockServer.url("/mcp").toString(),
                sessionTtlMs = 0,
            )

            client.discoverTools(server)

            val sessionsField = McpHttpClient::class.java.getDeclaredField("sessions")
            sessionsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val sessionsMap = sessionsField.get(client) as java.util.concurrent.ConcurrentHashMap<String, *>
            val key = sessionsMap.keys.first()
            val entry = sessionsMap[key]!!
            val createdAtField = entry.javaClass.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            createdAtField.setLong(entry, System.currentTimeMillis() - 6 * 60_000L)

            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).apply {
                    setHeader("Mcp-Session-Id", "session-2")
                    setBody(
                        """{"jsonrpc":"2.0","id":1,"result":{
                            "protocolVersion":"2024-11-05",
                            "capabilities":{},
                            "serverInfo":{"name":"t","version":"1"}
                        }}""",
                    )
                },
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200).setBody("{}"),
            )
            mockServer.enqueue(
                okhttp3.mockwebserver.MockResponse().setResponseCode(200)
                    .setBody("""{"jsonrpc":"2.0","id":3,"result":{"tools":[]}}"""),
            )

            client.discoverTools(server)

            assertEquals(6, mockServer.requestCount)
        } finally {
            mockServer.shutdown()
        }
    }
}
