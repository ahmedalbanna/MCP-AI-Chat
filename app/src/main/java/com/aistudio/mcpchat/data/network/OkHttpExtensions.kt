package com.aistudio.mcpchat.data.network

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

/**
 * Parses a Server-Sent Events (`text/event-stream`) body and invokes [onData]
 * with each complete `data:` field value.
 *
 * Blocks the calling thread while reading — call from `Dispatchers.IO`.
 * Coroutine cancellation closes the response via `Call.cancel()` (registered
 * by [await]), which makes the blocking read throw promptly.
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
