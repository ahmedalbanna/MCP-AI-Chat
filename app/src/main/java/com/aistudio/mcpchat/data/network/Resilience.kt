package com.aistudio.mcpchat.data.network

import android.util.Log
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.random.Random

class RetryManager(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 500,
    private val maxDelayMs: Long = 5000
) {
    private val TAG = "RetryManager"

    suspend fun <T> executeWithRetry(
        tag: String,
        action: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                lastException = e
                val delayMs = calculateBackoff(attempt)
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed for $tag: ${e.message}. Retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }
        throw lastException ?: Exception("All $maxRetries attempts failed for $tag")
    }

    private fun calculateBackoff(attempt: Int): Long {
        val exponentialDelay = initialDelayMs * (1 shl attempt)
        val jitter = Random.nextLong(0, exponentialDelay / 2)
        return min(exponentialDelay + jitter, maxDelayMs)
    }
}

class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeoutMs: Long = 60000
) {
    private val TAG = "CircuitBreaker"

    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val failures = ConcurrentHashMap<String, Int>()
    private val lastFailureTime = ConcurrentHashMap<String, Long>()
    private val circuitState = ConcurrentHashMap<String, State>()

    fun getState(serverId: String): State {
        val state = circuitState[serverId] ?: State.CLOSED
        if (state == State.OPEN) {
            val lastFailure = lastFailureTime[serverId] ?: 0
            if (System.currentTimeMillis() - lastFailure > resetTimeoutMs) {
                circuitState[serverId] = State.HALF_OPEN
                Log.d(TAG, "Circuit half-open for server $serverId")
                return State.HALF_OPEN
            }
        }
        return state
    }

    fun recordSuccess(serverId: String) {
        failures.remove(serverId)
        circuitState[serverId] = State.CLOSED
    }

    fun recordFailure(serverId: String) {
        val currentFailures = failures.getOrDefault(serverId, 0) + 1
        failures[serverId] = currentFailures
        lastFailureTime[serverId] = System.currentTimeMillis()

        if (currentFailures >= failureThreshold) {
            circuitState[serverId] = State.OPEN
            Log.w(TAG, "Circuit opened for server $serverId after $currentFailures failures")
        }
    }

    fun canExecute(serverId: String): Boolean {
        return getState(serverId) != State.OPEN
    }
}
