package com.aistudio.mcpchat.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class SseStreamFlowTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun newCall(): okhttp3.Call = client.newCall(
        Request.Builder().url(server.url("/stream")).build(),
    )

    @Test
    fun `emits each complete data event and joins multi-line data fields`() = runTest {
        val body = buildString {
            append("data: first\n")
            append("data: continued\n\n")
            append(": comment line\n\n")
            append("event: named\n")
            append("data: {\"a\":1}\n\n")
            append("data: unterminated-tail")
        }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body),
        )

        val events = newCall().sseStreamFlow().toList()

        assertEquals(
            listOf(
                SseStreamEvent.Data("first\ncontinued"),
                SseStreamEvent.Data("{\"a\":1}"),
                SseStreamEvent.Data("unterminated-tail"),
            ),
            events,
        )
    }

    @Test
    fun `emits PlainBody for a 200 without SSE content type`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"single":true}"""),
        )

        val events = newCall().sseStreamFlow().toList()

        assertEquals(listOf(SseStreamEvent.PlainBody("""{"single":true}""")), events)
    }

    @Test
    fun `emits StreamRejected for 4xx naming streaming`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":"streaming is disabled"}"""),
        )

        val events = newCall().sseStreamFlow().toList()

        val rejected = events.single() as SseStreamEvent.StreamRejected
        assertEquals(400, rejected.code)
        assertTrue(rejected.errorBody.contains("streaming"))
    }

    @Test
    fun `emits HttpError for other non-2xx codes`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(429)
                .setBody("""{"error":"rate limited"}"""),
        )

        val events = newCall().sseStreamFlow().toList()

        val error = events.single() as SseStreamEvent.HttpError
        assertEquals(429, error.code)
        assertTrue(error.errorBody.contains("rate limited"))
    }

    @Test
    fun `collector cancellation interrupts the throttled SSE read promptly`() {
        val sseBody = buildString {
            repeat(10) { i -> append("data: chunk-$i\n\n") }
        }
        // ~4 bytes per 150ms: a full drain takes ~30s, so only the socket
        // close from awaitClose's call.cancel() can end the read promptly.
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
                .throttleBody(4, 150, TimeUnit.MILLISECONDS),
        )

        val start = System.currentTimeMillis()
        runBlocking {
            var cancellation: CancellationException? = null
            val job = launch {
                try {
                    newCall().sseStreamFlow().collect { }
                } catch (e: CancellationException) {
                    cancellation = e
                    throw e
                }
            }
            delay(1000)
            job.cancelAndJoin()
            assertNotNull("expected CancellationException from the interrupted read", cancellation)
        }
        val elapsed = System.currentTimeMillis() - start

        assertTrue("cancel should interrupt the throttled read promptly (took ${elapsed}ms)", elapsed < 5000)
    }
}
