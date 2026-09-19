package com.aistudio.mcpchat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
            ) {
                Text(
                    text = ArabicNumerals.convertIfArabic("MCP Invocations (${toolCalls.size}):"),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
                for (call in toolCalls) {
                    ToolCallCard(toolCall = call)
                }
            }
        }

        if (isUser) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(Dimens.chatUserBubbleRadius),
                    modifier = Modifier
                        .fillMaxWidth(Dimens.CHAT_BUBBLE_MAX_WIDTH)
                        .widthIn(max = Dimens.chatBubbleMaxWidthDp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = {},
                            onLongClick = {
                                copyMessage(context, message.content)
                            },
                        ),
                ) {
                    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
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
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Content,
                                ),
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            copyMessage(context, message.content)
                        },
                    )
                    .padding(vertical = Spacing.xs),
            ) {
                val displayText = if (shouldTruncate && !isExpanded) {
                    message.content.take(500)
                } else {
                    message.content
                }
                SelectionContainer {
                    MarkdownFormattedText(text = displayText)
                }

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
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }

                if (message.latencyMs > 0) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs),
                    ) {
                        Text(
                            text = ArabicNumerals.convertIfArabic("${message.latencyMs}ms"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

private fun copyMessage(context: Context, content: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Message", content)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
}
