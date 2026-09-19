package com.aistudio.mcpchat.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.model.ImageAttachment
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.StreamProgress
import com.aistudio.mcpchat.ui.viewmodel.formatRemainingMs
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm),
    ) {
        PulsingDots()
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "Generating…",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Three quiet dots signalling liveness; usable bare or inside a bubble. */
@Composable
fun PulsingDots(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics {
            contentDescription = "AI is generating a response"
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(Spacing.xs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            )
        }
    }
}

@Composable
internal fun sessionSurfaceColor(isSelected: Boolean): Color = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDirection = TextDirection.Content,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "●",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
        StreamProgressIndicator(
            progress = progress,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
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
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusFull),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("regenerate_action")
            .semantics { role = Role.Button },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconSizeSm),
            )
            Text(
                text = "Regenerate",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .size(Dimens.attachmentThumb)
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
                            .minimumInteractiveComponentSize()
                            .testTag("remove_attachment_button")
                            .semantics {
                                contentDescription = "Remove attachment"
                                role = Role.Button
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
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
                    .size(Dimens.attachmentThumb)
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
