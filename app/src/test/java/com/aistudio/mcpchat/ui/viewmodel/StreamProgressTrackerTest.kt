package com.aistudio.mcpchat.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamProgressTrackerTest {

    private val tracker = StreamProgressTracker()

    private fun nanos(ms: Long) = ms * 1_000_000L

    @Test
    fun `first token delta emits immediately with char count`() {
        tracker.onStreamStarted(nanos(0))

        val progress = tracker.onTokenDelta(24, nanos(50))

        assertNotNull(progress)
        assertEquals(24, progress!!.charsStreamed)
    }

    @Test
    fun `delta within throttle window returns null without clearing chars`() {
        tracker.onStreamStarted(nanos(0))
        tracker.onTokenDelta(10, nanos(10))

        val throttled = tracker.onTokenDelta(10, nanos(60))

        assertNull(throttled)
        // The next emission after the window still counts both deltas.
        val next = tracker.onTokenDelta(10, nanos(200))
        assertNotNull(next)
        assertEquals(30, next!!.charsStreamed)
    }

    @Test
    fun `throughput approximates chars per four per second`() {
        tracker.onStreamStarted(nanos(0))
        // 8 deltas of 40 chars every 250ms = 160 chars/s ≈ 40 tok/s.
        var progress: StreamProgress? = null
        for (i in 1..8) {
            progress = tracker.onTokenDelta(40, nanos(i * 250L))
        }

        assertNotNull(progress)
        assertEquals(320, progress!!.charsStreamed)
        assertTrue(
            "expected ~40 tok/s, was ${progress.tokensPerSecond}",
            progress.tokensPerSecond in 35..45,
        )
    }

    @Test
    fun `fraction is bounded and never completes early`() {
        tracker.onStreamStarted(nanos(0))
        tracker.onTokenDelta(10, nanos(100))

        val early = tracker.onTokenDelta(10, nanos(15_000))!!

        // Elapsed (15s) already exceeds the initial 12s estimate; the bar
        // must stay below 1 and the remaining estimate must stay positive.
        assertTrue(early.fraction < 1f)
        assertTrue(early.fraction <= 0.95f)
        assertTrue(early.estimatedRemainingMs > 0)
    }

    @Test
    fun `onTokenDelta before StreamStarted self-starts`() {
        val progress = tracker.onTokenDelta(8, nanos(0))

        assertNotNull(progress)
        assertEquals(8, progress!!.charsStreamed)
        assertEquals(0, progress.elapsedMs)
    }

    @Test
    fun `formatRemainingMs renders seconds and minutes`() {
        assertEquals("0s", formatRemainingMs(0))
        assertEquals("1s", formatRemainingMs(1))
        assertEquals("8s", formatRemainingMs(8_000))
        assertEquals("1m 04s", formatRemainingMs(64_000))
        assertEquals("2m 00s", formatRemainingMs(120_000))
    }
}
