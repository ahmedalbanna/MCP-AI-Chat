package com.aistudio.mcpchat.ui.viewmodel

/**
 * Live throughput/ETA snapshot for the in-flight SSE generation, rendered by
 * the stream progress bar. [fraction] is a smoothed elapsed/estimate ratio
 * deliberately capped below 1f — a generation has no known total length, so
 * the estimate stretches as the stream runs instead of pretending to know
 * the end.
 */
data class StreamProgress(
    val elapsedMs: Long,
    val charsStreamed: Int,
    val tokensPerSecond: Int,
    val estimatedRemainingMs: Long,
    val fraction: Float,
)

/**
 * Accumulates [StreamProgress] snapshots from token deltas.
 *
 * Tokens are approximated as CHARS_PER_TOKEN characters (the usual LLM
 * average) so the rate is comparable across providers regardless of how they
 * chunk their deltas. The throughput is an EMA over per-interval rates so a
 * single burst or pause does not swing the number.
 */
class StreamProgressTracker {

    private var started = false
    private var startNanos = 0L
    private var lastSampleNanos = 0L
    private var charsStreamed = 0
    private var intervalChars = 0
    private var estimatedTotalMs = INITIAL_ESTIMATE_MS
    private var lastEmitMs = -1L
    private var emaTokensPerSec = 0f

    fun onStreamStarted(nowNanos: Long) {
        started = true
        startNanos = nowNanos
        lastSampleNanos = nowNanos
        charsStreamed = 0
        intervalChars = 0
        estimatedTotalMs = INITIAL_ESTIMATE_MS
        lastEmitMs = -1L
        emaTokensPerSec = 0f
    }

    /**
     * Feeds one token delta; returns an updated snapshot, or null when the
     * call lands within [EMIT_THROTTLE_MS] of the previous emission (the
     * first delta always emits so the bar turns determinate immediately).
     */
    fun onTokenDelta(deltaChars: Int, nowNanos: Long): StreamProgress? {
        if (!started) onStreamStarted(nowNanos)
        charsStreamed += deltaChars
        intervalChars += deltaChars

        val dtSec = (nowNanos - lastSampleNanos) / 1_000_000_000.0
        if (dtSec >= MIN_SAMPLE_SEC) {
            val intervalRate = intervalChars / CHARS_PER_TOKEN / dtSec.toFloat()
            emaTokensPerSec = if (emaTokensPerSec == 0f) {
                intervalRate
            } else {
                emaTokensPerSec + EMA_ALPHA * (intervalRate - emaTokensPerSec)
            }
            lastSampleNanos = nowNanos
            intervalChars = 0
        }

        val elapsedMs = (nowNanos - startNanos) / 1_000_000
        // Stretch the estimate whenever the stream outlives it, so the bar
        // slows down but never completes before the generation does.
        if (elapsedMs > estimatedTotalMs * FILL_TARGET) {
            estimatedTotalMs = elapsedMs + (elapsedMs / 2)
        }

        if (lastEmitMs >= 0 && elapsedMs - lastEmitMs < EMIT_THROTTLE_MS) return null
        lastEmitMs = elapsedMs
        return snapshot(elapsedMs)
    }

    private fun snapshot(elapsedMs: Long): StreamProgress {
        val remaining = (estimatedTotalMs - elapsedMs).coerceAtLeast(0)
        val fraction = (elapsedMs.toFloat() / estimatedTotalMs)
            .coerceIn(MIN_FRACTION, MAX_FRACTION)
        return StreamProgress(
            elapsedMs = elapsedMs,
            charsStreamed = charsStreamed,
            tokensPerSecond = emaTokensPerSec.toInt().coerceAtLeast(1),
            estimatedRemainingMs = remaining,
            fraction = fraction,
        )
    }

    companion object {
        private const val CHARS_PER_TOKEN = 4f
        private const val INITIAL_ESTIMATE_MS = 12_000L
        private const val EMA_ALPHA = 0.35f
        private const val MIN_SAMPLE_SEC = 0.05
        private const val EMIT_THROTTLE_MS = 100L
        private const val FILL_TARGET = 0.8f
        private const val MIN_FRACTION = 0.02f
        private const val MAX_FRACTION = 0.95f
    }
}

/** "8s" / "1m 04s" style for the remaining-time label. */
fun formatRemainingMs(ms: Long): String {
    if (ms <= 0) return "0s"
    val totalSeconds = (ms + 999) / 1000
    return if (totalSeconds < 60) {
        "${totalSeconds}s"
    } else {
        "${totalSeconds / 60}m %02ds".format(totalSeconds % 60)
    }
}
