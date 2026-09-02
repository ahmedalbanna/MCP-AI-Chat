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
