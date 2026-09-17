package com.aistudio.mcpchat.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.model.ImageAttachment
import com.aistudio.mcpchat.ui.theme.CyanSecondary
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
import com.aistudio.mcpchat.ui.theme.IndigoPrimaryLight
import com.aistudio.mcpchat.ui.theme.RoseError
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.StreamProgress
import com.aistudio.mcpchat.ui.viewmodel.formatRemainingMs
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(Dimens.cornerRadiusXl, Dimens.cornerRadiusXl, Dimens.cornerRadiusXl, Dimens.cornerRadiusSm),
            )
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm),
    ) {
        Surface(
            color = IndigoPrimary.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.size(Spacing.xxl),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI is typing",
                    tint = IndigoPrimaryLight,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        PulsingDots()
    }
}

/** Three pulsing dots signalling liveness; usable bare or inside a bubble. */
@Composable
fun PulsingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "typingAlpha",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
internal fun sessionSurfaceColor(isSelected: Boolean): Color = if (isSelected) {
    IndigoPrimary.copy(alpha = 0.15f)
} else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
}

/**
 * One full-text search hit: content snippet, originating conversation, and
 * timestamp. Tapping jumps to that conversation.
 */
@Composable
fun MessageSearchResultItem(
    message: ChatMessageEntity,
    sessionTitle: String?,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = sessionSurfaceColor(false),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        modifier = modifier
            .fillMaxWidth()
            .testTag("message_search_result")
            .semantics {
                role = Role.Button
                contentDescription = "Open conversation: ${sessionTitle ?: "Conversation"}"
            }
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = message.content.take(120).ifBlank { "(no text)" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "${sessionTitle ?: "Conversation"} • ${dateFormatter.format(Date(message.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Live preview bubble for the assistant reply while tokens stream in over
 * SSE. Mirrors the assistant bubble styling; replaced by the persisted
 * message (via the Room messages flow) once the stream finishes.
 */
@Composable
fun StreamingMessageBubble(text: String, progress: StreamProgress? = null, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Surface(
            color = IndigoPrimary.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
                .size(Spacing.xxl)
                .padding(top = Spacing.xxs),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = IndigoPrimaryLight,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = Dimens.elevationSm,
            shape = RoundedCornerShape(
                topStart = Dimens.cornerRadiusXl,
                topEnd = Dimens.cornerRadiusXl,
                bottomStart = Dimens.cornerRadiusSm,
                bottomEnd = Dimens.cornerRadiusXl,
            ),
            border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(Dimens.CHAT_BUBBLE_MAX_WIDTH),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm)) {
                MarkdownFormattedText(text = text)
                StreamProgressIndicator(
                    progress = progress,
                    modifier = Modifier.padding(top = Spacing.xxs),
                )
            }
        }
    }
}

/**
 * Determinate stream progress bar with throughput and remaining-time
 * estimates; indeterminate (with a waiting label) until the first token
 * arrives. [progress] snapshots come from the ViewModel's tracker.
 */
@Composable
fun StreamProgressIndicator(progress: StreamProgress?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(Dimens.borderWidthThick),
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(Dimens.borderWidthThick),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = if (progress == null) {
                "Waiting for first token…"
            } else {
                "${progress.tokensPerSecond} tok/s · ${progress.charsStreamed} chars · ~${formatRemainingMs(progress.estimatedRemainingMs)} left"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            },
        )
    }
}

/**
 * Re-runs generation for the last user turn: replaces the previous assistant
 * reply, a stopped partial, or an error message.
 */
@Composable
fun RegenerateAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusMd),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        onClick = onClick,
        modifier = modifier
            .testTag("regenerate_action")
            .semantics { role = Role.Button },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CyanSecondary,
                modifier = Modifier.size(Dimens.iconSizeSm),
            )
            Text(
                text = "Regenerate",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = CyanSecondary,
            )
        }
    }
}

/**
 * Thumbnails of images staged in the input bar, plus a trailing tile that
 * reopens the photo picker.
 */
@Composable
fun PendingAttachmentsBar(
    attachments: List<ImageAttachment>,
    onRemove: (ImageAttachment) -> Unit,
    onAddMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.testTag("pending_attachments_bar"),
    ) {
        items(attachments, key = { it.uri }) { attachment ->
            Box {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = "Attached image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(Dimens.cornerRadiusSm)),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xxs),
                ) {
                    IconButton(
                        onClick = { onRemove(attachment) },
                        modifier = Modifier
                            .testTag("remove_attachment_button")
                            .semantics {
                                contentDescription = "Remove attachment"
                                role = Role.Button
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(Dimens.iconSizeSm),
                        )
                    }
                }
            }
        }
        item(key = "add_attachment_tile") {
            IconButton(
                onClick = onAddMore,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(Dimens.cornerRadiusSm))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("add_attachment_tile"),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add another image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
    }
}
