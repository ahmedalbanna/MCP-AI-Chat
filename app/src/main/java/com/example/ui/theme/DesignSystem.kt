package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignSystemPreview() {
    Column(
        modifier = Modifier
            .padding(Spacing.lg)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        Text("Design System", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Color Swatches
        Text("Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(
                "Primary" to IndigoPrimary,
                "Secondary" to CyanSecondary,
                "Tertiary" to EmeraldTertiary,
                "Warning" to AmberWarning,
                "Error" to RoseError,
                "Tool Pending" to ColorTokens.toolPending,
                "Tool Success" to ColorTokens.toolSuccess,
                "Tool Error" to ColorTokens.toolError,
            ).forEach { (name, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(Dimens.cornerRadiusMd))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
            }
        }

        // Spacing Scale
        Text("Spacing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf(
                "xxs (2dp)" to Spacing.xxs,
                "xs (4dp)" to Spacing.xs,
                "sm (8dp)" to Spacing.sm,
                "md (12dp)" to Spacing.md,
                "lg (16dp)" to Spacing.lg,
                "xl (24dp)" to Spacing.xl,
                "xxl (32dp)" to Spacing.xxl,
                "xxxl (48dp)" to Spacing.xxxl,
            ).forEach { (name, size) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(Spacing.sm)
                            .width(size)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Spacing.xxs))
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(name, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Corner Radii
        Text("Corner Radii", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf(
                "Sm" to Dimens.cornerRadiusSm,
                "Md" to Dimens.cornerRadiusMd,
                "Lg" to Dimens.cornerRadiusLg,
                "Xl" to Dimens.cornerRadiusXl,
                "Full" to Dimens.cornerRadiusFull,
            ).forEach { (name, radius) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(radius))
                            .border(Dimens.borderWidth, MaterialTheme.colorScheme.outline, RoundedCornerShape(radius))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Touch Targets
        Text("Touch Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.touchTargetMin)
                    .border(Dimens.borderWidthThick, MaterialTheme.colorScheme.primary, RoundedCornerShape(Dimens.cornerRadiusMd)),
                contentAlignment = Alignment.Center
            ) {
                Text("48dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text("Minimum touch target size", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Icon Sizes
        Text("Icon Sizes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            listOf(
                "Sm" to Dimens.iconSizeSm,
                "Md" to Dimens.iconSizeMd,
                "Lg" to Dimens.iconSizeLg,
            ).forEach { (name, size) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Design System")
@Composable
fun DesignSystemPreviewLight() {
    MyApplicationTheme(darkTheme = false) {
        DesignSystemPreview()
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Design System (Dark)")
@Composable
fun DesignSystemPreviewDark() {
    MyApplicationTheme(darkTheme = true) {
        DesignSystemPreview()
    }
}
