package com.aistudio.mcpchat.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `Success returns data`() {
        val result = NetworkResult.Success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals(42, result.data)
    }

    @Test
    fun `Error returns message`() {
        val result = NetworkResult.Error("something went wrong")
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
        assertEquals("something went wrong", result.message)
    }

    @Test
    fun `Loading state`() {
        val result = NetworkResult.Loading
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }

    @Test
    fun `getOrNull returns data for Success`() {
        val result = NetworkResult.Success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Error`() {
        val result = NetworkResult.Error("fail")
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Loading`() {
        val result = NetworkResult.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrDefault returns data for Success`() {
        val result = NetworkResult.Success(10)
        assertEquals(10, result.getOrDefault(0))
    }

    @Test
    fun `getOrDefault returns default for Error`() {
        val result = NetworkResult.Error("fail")
        assertEquals(99, result.getOrDefault(99))
    }

    @Test
    fun `map transforms Success data`() {
        val result = NetworkResult.Success(5)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is NetworkResult.Success)
        assertEquals(10, (mapped as NetworkResult.Success).data)
    }

    @Test
    fun `map preserves Error`() {
        val result = NetworkResult.Error("fail")
        val mapped = result.map { "transformed" }
        assertTrue(mapped is NetworkResult.Error)
    }

    @Test
    fun `getOrElse returns data for Success`() {
        val result = NetworkResult.Success("data")
        val value = result.getOrElse { "default" }
        assertEquals("data", value)
    }

    @Test
    fun `getOrElse invokes onError for Error`() {
        val result = NetworkResult.Error("fail")
        val value = result.getOrElse { "default from error: $it" }
        assertEquals("default from error: fail", value)
    }

    @Test
    fun `getOrElse invokes onError for Loading`() {
        val result = NetworkResult.Loading
        val value = result.getOrElse { "loading fallback" }
        assertEquals("loading fallback", value)
    }

    @Test
    fun `asSuccess extension creates Success`() {
        val result = "hello".asSuccess()
        assertTrue(result is NetworkResult.Success)
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `asError extension creates Error`() {
        val result = "error message".asError<String>()
        assertTrue(result is NetworkResult.Error)
        assertEquals("error message", (result as NetworkResult.Error).message)
    }
}
