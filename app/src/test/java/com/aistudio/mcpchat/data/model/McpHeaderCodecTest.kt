package com.aistudio.mcpchat.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class McpHeaderCodecTest {

    @Test
    fun `encode round trips a multi header map`() {
        val headers = mapOf(
            "Authorization" to "Bearer token",
            "X-API-Key" to "key123",
        )
        val encoded = McpHeaderCodec.encode(headers)
        assertEquals(headers, McpHeaderCodec.decode(encoded))
    }

    @Test
    fun `encode returns empty string for blank keys only`() {
        assertEquals("", McpHeaderCodec.encode(mapOf("" to "value")))
        assertEquals("", McpHeaderCodec.encode(emptyMap()))
    }

    @Test
    fun `decode maps legacy bare value to Authorization`() {
        assertEquals(
            mapOf("Authorization" to "Bearer token123"),
            McpHeaderCodec.decode("Bearer token123"),
        )
    }

    @Test
    fun `decode maps legacy colon form to single header`() {
        assertEquals(
            mapOf("X-API-Key" to "key123"),
            McpHeaderCodec.decode("X-API-Key: key123"),
        )
    }

    @Test
    fun `decode keeps ERPNext token value intact as Authorization`() {
        // "token <api_key>:<api_secret>" — the colon belongs to the value.
        val erpnextToken = "token 4ed7815c3d6bddb:78c57e56fb3fdd7"
        assertEquals(
            mapOf("Authorization" to erpnextToken),
            McpHeaderCodec.decode(erpnextToken),
        )
    }

    @Test
    fun `decode does not split colon values with invalid header names`() {
        // Spaces make "Bearer x" an invalid header NAME → whole value is Authorization.
        assertEquals(
            mapOf("Authorization" to "Bearer abc:def"),
            McpHeaderCodec.decode("Bearer abc:def"),
        )
    }

    @Test
    fun `decode returns empty map for blank input`() {
        assertTrue(McpHeaderCodec.decode("").isEmpty())
        assertTrue(McpHeaderCodec.decode("   ").isEmpty())
    }

    @Test
    fun `decode falls back to single header for corrupt JSON`() {
        assertEquals(
            mapOf("Authorization" to "{broken json"),
            McpHeaderCodec.decode("{broken json"),
        )
    }

    @Test
    fun `decode trims names and values`() {
        val encoded = McpHeaderCodec.encode(mapOf("Authorization" to "Bearer x"))
        val decoded = McpHeaderCodec.decode(" $encoded ")
        assertEquals(mapOf("Authorization" to "Bearer x"), decoded)
    }

    @Test
    fun `decode ignores blank header names in JSON form`() {
        val raw = """{"":"v","X-API-Key":"k"}"""
        assertEquals(mapOf("X-API-Key" to "k"), McpHeaderCodec.decode(raw))
    }
}
