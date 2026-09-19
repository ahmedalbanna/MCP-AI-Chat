package com.aistudio.mcpchat.ui.adaptive

import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun hinge(isVertical: Boolean, isHalfOpened: Boolean = false, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) = HingeInfo(
    isVertical = isVertical,
    isHalfOpened = isHalfOpened,
    leftPx = left,
    topPx = top,
    rightPx = right,
    bottomPx = bottom,
)

/** FoldingFeature is an interface, so tests stub it directly. */
private class FakeFoldingFeature(
    override val bounds: Rect,
    override val state: FoldingFeature.State,
    override val orientation: FoldingFeature.Orientation,
) : FoldingFeature {
    override val isSeparating: Boolean = state == FoldingFeature.State.HALF_OPENED
    override val occlusionType: FoldingFeature.OcclusionType = FoldingFeature.OcclusionType.NONE
}

@RunWith(RobolectricTestRunner::class)
class AdaptiveLayoutTest {

    // ---- twoPaneSplitFraction ----

    @Test
    fun `split fraction is even without a hinge`() {
        assertEquals(0.5f, twoPaneSplitFraction(1000f, null))
    }

    @Test
    fun `split fraction is even for a horizontal hinge`() {
        val tabletop = hinge(isVertical = false, top = 490, bottom = 510)
        assertEquals(0.5f, twoPaneSplitFraction(1000f, tabletop))
    }

    @Test
    fun `vertical hinge pins the split at the fold center`() {
        val fold = hinge(isVertical = true, left = 298, right = 302)
        assertEquals(0.3f, twoPaneSplitFraction(1000f, fold), 0.001f)

        val offCenter = hinge(isVertical = true, left = 698, right = 702)
        assertEquals(0.7f, twoPaneSplitFraction(1000f, offCenter), 0.001f)
    }

    @Test
    fun `vertical hinge near an edge falls back to an even split`() {
        val tooFarLeft = hinge(isVertical = true, left = 20, right = 40)
        assertEquals(0.5f, twoPaneSplitFraction(1000f, tooFarLeft))

        val tooFarRight = hinge(isVertical = true, left = 970, right = 990)
        assertEquals(0.5f, twoPaneSplitFraction(1000f, tooFarRight))
    }

    @Test
    fun `degenerate parent width falls back to an even split`() {
        val fold = hinge(isVertical = true, left = 0, right = 100)
        assertEquals(0.5f, twoPaneSplitFraction(0f, fold))
    }

    // ---- paneSplitFraction (window-coords hinge vs narrower pane area) ----

    @Test
    fun `pane split subtracts the rail inset`() {
        val fold = hinge(isVertical = true, left = 380, right = 384)
        assertEquals(0.3f, paneSplitFraction(1000f, fold, leadingInsetPx = 82f), 0.001f)
    }

    @Test
    fun `pane split falls back when adjusted center leaves the pane`() {
        val fold = hinge(isVertical = true, left = 20, right = 40)
        assertEquals(0.5f, paneSplitFraction(1000f, fold, leadingInsetPx = 82f))
    }

    // ---- horizontalHingeInsetPx ----

    @Test
    fun `inset is zero without a hinge, for a vertical hinge, or when flat`() {
        assertEquals(0f to 0f, horizontalHingeInsetPx(1000f, null))
        assertEquals(0f to 0f, horizontalHingeInsetPx(1000f, hinge(isVertical = true, top = 490, bottom = 510)))
        val flatTabletop = hinge(isVertical = false, isHalfOpened = false, top = 490, bottom = 510)
        assertEquals(0f to 0f, horizontalHingeInsetPx(1000f, flatTabletop))
    }

    @Test
    fun `centered half-opened horizontal hinge clears its band`() {
        val tabletop = hinge(isVertical = false, isHalfOpened = true, top = 480, bottom = 520)
        assertEquals(20f to 20f, horizontalHingeInsetPx(1000f, tabletop))
    }

    @Test
    fun `off-center horizontal hinge gets no inset`() {
        val lowHinge = hinge(isVertical = false, isHalfOpened = true, top = 780, bottom = 820)
        assertEquals(0f to 0f, horizontalHingeInsetPx(1000f, lowHinge))
    }

    // ---- FoldingFeature.toHingeInfo ----

    @Test
    fun `toHingeInfo maps orientation, state and bounds`() {
        val vertical = FakeFoldingFeature(
            bounds = Rect(298, 0, 302, 1000),
            state = FoldingFeature.State.FLAT,
            orientation = FoldingFeature.Orientation.VERTICAL,
        )
        val info = vertical.toHingeInfo()
        assertTrue(info.isVertical)
        assertFalse(info.isHalfOpened)
        assertEquals(4, info.widthPx)
        assertEquals(300f, info.centerXpx, 0.001f)

        val horizontal = FakeFoldingFeature(
            bounds = Rect(0, 480, 1000, 520),
            state = FoldingFeature.State.HALF_OPENED,
            orientation = FoldingFeature.Orientation.HORIZONTAL,
        )
        val hInfo = horizontal.toHingeInfo()
        assertFalse(hInfo.isVertical)
        assertTrue(hInfo.isHalfOpened)
        assertEquals(40, hInfo.heightPx)
        assertEquals(500f, hInfo.centerYpx, 0.001f)
    }
}
