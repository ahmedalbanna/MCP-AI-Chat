package com.aistudio.mcpchat.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class ResilienceTest {

    /** Specific exception satisfying detekt's TooGenericExceptionThrown. */
    private class SimulatedNetworkException(message: String) : Exception(message)


    private lateinit var retryManager: RetryManager

    @Before
    fun setUp() {
        retryManager = RetryManager(maxRetries = 3, initialDelayMs = 10, maxDelayMs = 100)
    }

    // ---- RetryManager Tests ----

    @Test
    fun `executeWithRetry returns result on first success`() = runTest {
        var attempts = 0
        val result = retryManager.executeWithRetry("test") {
            attempts++
            "success"
        }
        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `executeWithRetry retries on failure then succeeds`() = runTest {
        var attempts = 0
        val result = retryManager.executeWithRetry("test") {
            attempts++
            if (attempts < 2) throw SimulatedNetworkException("failure")
            "success"
        }
        assertEquals("success", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `executeWithRetry throws after max retries exhausted`() = runTest {
        var attempts = 0
        try {
            retryManager.executeWithRetry("test") {
                attempts++
                throw SimulatedNetworkException("persistent failure")
            }
            org.junit.Assert.fail("Expected SimulatedNetworkException")
        } catch (_: SimulatedNetworkException) {
            // expected
        }
    }

    @Test
    fun `executeWithRetry respects maxRetries count`() = runTest {
        val maxRetries = 5
        val retryMgr = RetryManager(maxRetries = maxRetries, initialDelayMs = 10, maxDelayMs = 50)
        var attempts = 0

        try {
            retryMgr.executeWithRetry("test") {
                attempts++
                throw SimulatedNetworkException("always fail")
            }
            org.junit.Assert.fail("Expected SimulatedNetworkException")
        } catch (_: SimulatedNetworkException) {
            // expected
        }
        assertEquals(maxRetries, attempts)
    }

    @Test
    fun `executeWithRetry returns value from successful attempt`() = runTest {
        val result = retryManager.executeWithRetry("test") {
            42
        }
        assertEquals(42, result)
    }

    @Test
    fun `executeWithRetry propagates custom exception type`() = runTest {
        try {
            retryManager.executeWithRetry("test") {
                throw SimulatedNetworkException("custom error")
            }
            org.junit.Assert.fail("Expected SimulatedNetworkException")
        } catch (_: SimulatedNetworkException) {
            // expected
        }
    }

    // ---- CircuitBreaker Tests ----

    private lateinit var circuitBreaker: CircuitBreaker

    @Before
    fun setUpCircuitBreaker() {
        circuitBreaker = CircuitBreaker(failureThreshold = 3, resetTimeoutMs = 1000)
    }

    @Test
    fun `circuit starts in CLOSED state`() {
        val state = circuitBreaker.getState("server_1")
        assertEquals(CircuitBreaker.State.CLOSED, state)
    }

    @Test
    fun `circuit opens after failure threshold reached`() {
        repeat(3) {
            circuitBreaker.recordFailure("server_1")
        }
        val state = circuitBreaker.getState("server_1")
        assertEquals(CircuitBreaker.State.OPEN, state)
    }

    @Test
    fun `circuit blocks execution when OPEN`() {
        repeat(3) {
            circuitBreaker.recordFailure("server_1")
        }
        assertFalse(circuitBreaker.canExecute("server_1"))
    }

    @Test
    fun `circuit allows execution when CLOSED`() {
        assertTrue(circuitBreaker.canExecute("server_1"))
    }

    @Test
    fun `circuit transitions to HALF_OPEN after timeout`() {
        val shortTimeoutCircuit = CircuitBreaker(failureThreshold = 1, resetTimeoutMs = 1)
        shortTimeoutCircuit.recordFailure("server_1")
        assertEquals(CircuitBreaker.State.OPEN, shortTimeoutCircuit.getState("server_1"))

        Thread.sleep(10)
        assertEquals(CircuitBreaker.State.HALF_OPEN, shortTimeoutCircuit.getState("server_1"))
    }

    @Test
    fun `circuit resets to CLOSED on success`() {
        repeat(3) {
            circuitBreaker.recordFailure("server_1")
        }
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState("server_1"))

        circuitBreaker.recordSuccess("server_1")
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState("server_1"))
    }

    @Test
    fun `circuit allows execution in HALF_OPEN state`() {
        val shortTimeoutCircuit = CircuitBreaker(failureThreshold = 1, resetTimeoutMs = 1)
        shortTimeoutCircuit.recordFailure("server_1")
        Thread.sleep(10)
        assertTrue(shortTimeoutCircuit.canExecute("server_1"))
    }

    @Test
    fun `circuit failure count resets after success`() {
        circuitBreaker.recordFailure("server_1")
        circuitBreaker.recordFailure("server_1")
        circuitBreaker.recordSuccess("server_1")

        circuitBreaker.recordFailure("server_1")
        val state = circuitBreaker.getState("server_1")
        assertEquals(CircuitBreaker.State.CLOSED, state)
    }

    @Test
    fun `circuit tracks servers independently`() {
        circuitBreaker.recordFailure("server_1")
        circuitBreaker.recordFailure("server_1")
        circuitBreaker.recordFailure("server_1")

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState("server_1"))
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState("server_2"))
        assertTrue(circuitBreaker.canExecute("server_2"))
    }

    @Test
    fun `circuit defaults to CLOSED for unknown server`() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState("unknown_server"))
    }
}
