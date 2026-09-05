package com.aistudio.mcpchat.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.coroutineContext
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

/**
 * Executes the call and consumes the response body inside [action] with the
 * blocking read tied to the calling job: [await] covers the headers phase,
 * and a completion handler cancels the call if the job dies mid-body — the
 * socket close interrupts the blocking read instead of letting it drain to
 * EOF on a dispatcher thread. A read interrupted that way resurfaces as
 * [CancellationException].
 */
suspend fun <T> Call.executeStreaming(action: (Response) -> T): T {
    val job = requireNotNull(coroutineContext[Job]) { "executeStreaming requires a coroutine Job" }
    return await().use { response ->
        val handle = job.invokeOnCompletion { cause -> if (cause != null) cancel() }
        try {
            action(response)
        } catch (e: IOException) {
            if (isCanceled()) {
                throw CancellationException("request cancelled during body read", e)
            }
            throw e
        } finally {
            handle.dispose()
        }
    }
}

/**
 * Parses a Server-Sent Events (`text/event-stream`) body and invokes [onData]
 * with each complete `data:` field value.
 *
 * Blocks the calling thread while reading — call from `Dispatchers.IO` via
 * [executeStreaming], which interrupts the blocking read on coroutine
 * cancellation.
 *
 * Handles the SSE spec essentials: multi-line `data:` fields are joined with
 * `\n`, events are dispatched on blank lines, and `event:`/`id:`/`retry:`
 * fields and `:` comments are ignored — the AI providers encode everything
 * in `data:` payloads.
 */
fun Response.forEachSseData(onData: (String) -> Unit) {
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
