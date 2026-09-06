package com.aistudio.mcpchat.ui.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlin.math.abs

/**
 * Auxiliary destination rendered beside chat when the window is expanded
 * (tablets, unfolded foldables, wide desktop windows).
 */
enum class SecondaryPane { NONE, SERVERS, INSPECTOR, SETTINGS }

/**
 * Snapshot of a FoldingFeature's geometry, decoupled from androidx.window
 * so the split/inset math below is unit-testable on the JVM.
 */
data class HingeInfo(
    val isVertical: Boolean,
    val isHalfOpened: Boolean,
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
) {
    val widthPx: Int get() = rightPx - leftPx
    val heightPx: Int get() = bottomPx - topPx
    val centerXpx: Float get() = (leftPx + rightPx) / 2f
    val centerYpx: Float get() = (topPx + bottomPx) / 2f
}

fun FoldingFeature.toHingeInfo(): HingeInfo = HingeInfo(
    isVertical = orientation == FoldingFeature.Orientation.VERTICAL,
    isHalfOpened = state == FoldingFeature.State.HALF_OPENED,
    leftPx = bounds.left,
    topPx = bounds.top,
    rightPx = bounds.right,
    bottomPx = bounds.bottom,
)

/**
 * Weight (fraction) of the primary chat pane in the expanded two-pane row.
 * A vertical hinge pins the split to the fold center so neither pane renders
 * on the hinge band; anything degenerate falls back to an even split.
 */
fun twoPaneSplitFraction(parentWidthPx: Float, hinge: HingeInfo?, defaultFraction: Float = 0.5f): Float {
    if (parentWidthPx <= 0f || hinge == null || !hinge.isVertical) return defaultFraction
    val fraction = hinge.centerXpx / parentWidthPx
    if (fraction < 0.15f || fraction > 0.85f) return defaultFraction
    return fraction
}

/**
 * Symmetric vertical inset that keeps content clear of a horizontal hinge
 * in half-opened (tabletop) posture. Only engages when the hinge band
 * sits near the vertical center — an off-center band can't be avoided by
 * simple insets, so content flows normally instead of being squeezed.
 * Returns (topPx, bottomPx).
 */
fun horizontalHingeInsetPx(parentHeightPx: Float, hinge: HingeInfo?): Pair<Float, Float> {
    if (parentHeightPx <= 0f || hinge == null) return 0f to 0f
    if (hinge.isVertical || !hinge.isHalfOpened) return 0f to 0f
    if (abs(hinge.centerYpx - parentHeightPx / 2f) > parentHeightPx / 4f) return 0f to 0f
    val half = hinge.heightPx / 2f
    return half to half
}

/**
 * Current fold state of the device, or null on flat/unfolded/non-foldable
 * hardware (no FoldingFeature is reported).
 */
@Composable
fun rememberHingeInfo(): HingeInfo? {
    val activity = LocalContext.current as? ComponentActivity ?: return null
    val layoutInfo by remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
    }.collectAsStateWithLifecycle(initialValue = null)
    return remember(layoutInfo) {
        layoutInfo?.displayFeatures
            ?.filterIsInstance<FoldingFeature>()
            ?.firstOrNull()
            ?.toHingeInfo()
    }
}
