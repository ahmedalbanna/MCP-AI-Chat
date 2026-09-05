package com.aistudio.mcpchat.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspends until the call completes. Cancelling the coroutine cancels the
 * in-flight HTTP request immediately (Stop button) instead of letting a
 * blocking [Call.execute] run to its full timeout on a dispatcher thread.
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) {
                    // Surface cancellation as CancellationException so
                    // structured concurrency keeps working.
                    continuation.cancel(e)
                } else {
                    continuation.resumeWithException(e)
                }
            }
        },
    )
    continuation.invokeOnCancellation { cancel() }
}

/** True when the response body is actually delivered as an SSE stream. */
fun Response.deliversSse(): Boolean = header("Content-Type")?.trim()?.startsWith("text/event-stream", ignoreCase = true) == true

/**
 * Heuristic for endpoints that reject streaming outright: 501 Not
 * Implemented, or a 4xx whose body names streaming ("streaming is not
 * supported", "stream mode is disabled", …). Callers retry the request
 * without `stream:true` / `alt=sse` in that case.
 */
fun looksLikeStreamUnsupported(code: Int, errorBody: String): Boolean {
    if (code == 501) return true
    if (code != 400 && code != 404 && code != 405) return false
    return errorBody.contains("stream", ignoreCase = true)
}

/**
 * Terminal or in-flight outcomes of a streaming call, normalizing the
 * transport concerns (SSE framing, fallback detection) away from the
 * provider-specific payload parsing.
 */
sealed interface SseStreamEvent {
    /** One complete `data:` field payload from the SSE body. */
    data class Data(val payload: String) : SseStreamEvent

    /** 200 whose body is plain JSON — the gateway ignored the stream flag. */
    data class PlainBody(val body: String) : SseStreamEvent

    /** Endpoint rejected streaming outright — retry once without it. */
    data class StreamRejected(val code: Int, val errorBody: String) : SseStreamEvent

    /** Any other non-2xx response. */
    data class HttpError(val code: Int, val errorBody: String) : SseStreamEvent
}

/**
 * Executes the call and exposes its outcome as a cold [Flow]: SSE `data:`
 * events stream as [SseStreamEvent.Data], while non-SSE responses surface as
 * one terminal event each.
 *
 * Structured concurrency replaces the manual job-completion wiring of the
 * old `executeStreaming`: the reader coroutine runs on [Dispatchers.IO],
 * `send` backpressures when the collector falls behind, and [awaitClose]
 * cancels the underlying call the moment the collector goes away — the
 * socket close interrupts the blocking read instead of draining it to EOF
 * on a dispatcher thread.
 */
fun Call.sseStreamFlow(): Flow<SseStreamEvent> = channelFlow {
    launch(Dispatchers.IO) {
        try {
            val response = await()
            response.use {
                if (!it.isSuccessful) {
                    val errorBody = it.body?.string() ?: ""
                    val event = if (looksLikeStreamUnsupported(it.code, errorBody)) {
                        SseStreamEvent.StreamRejected(it.code, errorBody)
                    } else {
                        SseStreamEvent.HttpError(it.code, errorBody)
                    }
                    send(event)
                } else if (!it.deliversSse()) {
                    send(SseStreamEvent.PlainBody(it.body?.string() ?: ""))
                } else {
                    it.readSseData { data -> send(SseStreamEvent.Data(data)) }
                }
            }
            close()
        } catch (e: IOException) {
            close(e)
        }
    }
    // Runs on collector cancellation AND on normal completion (no-op for a
    // finished call) — either way the call never outlives the flow.
    awaitClose { this@sseStreamFlow.cancel() }
}

/**
 * Parses an SSE body and hands each complete `data:` field value to [onData]
 * as it arrives. Handles the spec essentials: multi-line `data:` fields are
 * joined with `\n`, events dispatch on blank lines, and `event:`/`id:`/
 * `retry:` fields and `:` comments are ignored — the AI providers encode
 * everything in `data:` payloads.
 */
private suspend fun Response.readSseData(onData: suspend (String) -> Unit) {
    val source = body?.source() ?: return
    val buffer = StringBuilder()
    while (true) {
        val line = source.readUtf8Line() ?: break // EOF ends the stream
        when {
            line.isEmpty() -> {
                if (buffer.isNotEmpty()) {
                    onData(buffer.toString())
                    buffer.setLength(0)
                }
            }

            line.startsWith("data:") -> {
                var data = line.removePrefix("data:")
                if (data.startsWith(" ")) data = data.removePrefix(" ")
                if (buffer.isNotEmpty()) buffer.append('\n')
                buffer.append(data)
            }

            // event:, id:, retry: and :comment lines carry no payload here.
        }
    }
    // Flush a trailing event that was not terminated by a blank line.
    if (buffer.isNotEmpty()) onData(buffer.toString())
}
