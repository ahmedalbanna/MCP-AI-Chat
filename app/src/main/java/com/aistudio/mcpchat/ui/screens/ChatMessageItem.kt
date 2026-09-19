package com.aistudio.mcpchat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.model.parseImageAttachments
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.ChatViewModel
import com.aistudio.mcpchat.util.ArabicNumerals

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(message: ChatMessageEntity, viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val shouldTruncate = !isUser && message.content.length > 500

    val toolCalls = remember(message.toolCallsJson) {
        if (message.toolCallsJson != null) {
            viewModel.chatRepository.parseToolCallsJson(message.toolCallsJson)
        } else {
            emptyList()
        }
    }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        if (!isUser && toolCalls.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(bottom = Spacing.sm),
            ) {
                Text(
                    text = ArabicNumerals.convertIfArabic("MCP Invocations (${toolCalls.size}):"),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = Spacing.xxs),
                )
                for (call in toolCalls) {
                    ToolCallCard(toolCall = call)
                }
            }
        }

        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isUser) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(Spacing.xxl)
                        .padding(top = Spacing.xxs),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI assistant avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(Dimens.iconSizeSm),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
            }

            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (!isUser) Dimens.elevationSm else 0.dp,
                shape = RoundedCornerShape(
                    topStart = Dimens.cornerRadiusXl,
                    topEnd = Dimens.cornerRadiusXl,
                    bottomStart = if (isUser) Dimens.cornerRadiusXl else Dimens.cornerRadiusSm,
                    bottomEnd = if (isUser) Dimens.cornerRadiusSm else Dimens.cornerRadiusXl,
                ),
                border = if (isUser) null else BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth(Dimens.CHAT_BUBBLE_MAX_WIDTH)
                    .widthIn(max = Dimens.chatBubbleMaxWidthDp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = {},
                        onLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Message", message.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        },
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm)) {
                    if (isUser) {
                        val attachments = remember(message.attachmentsJson) {
                            parseImageAttachments(message.attachmentsJson)
                        }
                        for (attachment in attachments) {
                            AsyncImage(
                                model = attachment.uri,
                                contentDescription = "Attached image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(Dimens.cornerRadiusMd))
                                    .padding(bottom = Spacing.xs)
                                    .testTag("message_attachment_image"),
                            )
                        }
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = TextDirection.Content,
                                ),
                            )
                        }
                    } else {
                        val displayText = if (shouldTruncate && !isExpanded) {
                            message.content.take(500)
                        } else {
                            message.content
                        }
                        MarkdownFormattedText(text = displayText)

                        if (shouldTruncate) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            TextButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .semantics {
                                        stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                                    },
                            ) {
                                Text(
                                    text = if (isExpanded) "Show less" else "Show more",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    if (message.latencyMs > 0 && !isUser) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.xs),
                        ) {
                            Text(
                                text = ArabicNumerals.convertIfArabic("${message.latencyMs}ms"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}
