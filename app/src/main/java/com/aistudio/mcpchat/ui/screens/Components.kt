package com.aistudio.mcpchat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import org.json.JSONObject

@Composable
fun ServerStatusBadge(status: ServerStatus, latencyMs: Long = 0, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        ServerStatus.CONNECTED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.tertiary,
            if (latencyMs > 0) "${latencyMs}ms" else "Online",
        )
        ServerStatus.CONNECTING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.secondary,
            "Connecting...",
        )
        ServerStatus.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.error,
            "Offline",
        )
        ServerStatus.DISCONNECTED -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Disabled",
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(Dimens.cornerRadiusMd),
        border = BorderStroke(Dimens.borderWidth, textColor.copy(alpha = 0.3f)),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        ) {
            Box(
                modifier = Modifier
                    .size(Spacing.xs)
                    .clip(CircleShape)
                    .background(textColor),
            )
            Spacer(modifier = Modifier.width(Spacing.xxs))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
fun ToolCallCard(toolCall: McpToolCall, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val borderColor = when (toolCall.status) {
        ToolExecutionStatus.EXECUTING -> MaterialTheme.colorScheme.secondary
        ToolExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.tertiary
        ToolExecutionStatus.ERROR -> MaterialTheme.colorScheme.error
        ToolExecutionStatus.PENDING -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(Dimens.borderWidth, borderColor.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .animateContentSize()
            .testTag("tool_call_card_${toolCall.toolName}"),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.cornerRadiusLg))
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(borderColor),
            )
            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            role = Role.Button
                            contentDescription = "Expand or collapse tool call details for ${toolCall.toolName}"
                        }
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = ripple(),
                        ) { isExpanded = !isExpanded },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        when (toolCall.status) {
                            ToolExecutionStatus.EXECUTING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                    strokeWidth = Dimens.borderWidthThick,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            ToolExecutionStatus.SUCCESS -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                            ToolExecutionStatus.ERROR -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                            ToolExecutionStatus.PENDING -> {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Pending",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(Spacing.sm))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MCP Tool:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = toolCall.toolName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = toolCall.serverName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (toolCall.executionTimeMs > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                                modifier = Modifier.padding(end = Spacing.xs),
                            ) {
                                Text(
                                    text = "${toolCall.executionTimeMs}ms",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .padding(Spacing.xs),
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = Spacing.cardPadding)) {
                        Text(
                            text = "INPUT PARAMETERS (JSON-RPC):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        CodeBlock(
                            code = formatJsonArguments(toolCall.inputArguments),
                            language = "json",
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        if (toolCall.outputResult != null || toolCall.errorMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "OUTPUT RESULT:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (toolCall.isSuccess) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    fontFamily = FontFamily.Monospace,
                                )

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("MCP Result", toolCall.outputResult ?: toolCall.errorMessage),
                                        )
                                        Toast.makeText(context, "Copied payload to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(Dimens.touchTargetMin)
                                        .minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(Dimens.iconSizeSm),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.xs))
                            CodeBlock(
                                code = toolCall.outputResult ?: toolCall.errorMessage ?: "",
                                language = "json",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String = "", modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val headerBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColorCode = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val codeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Surface(
        color = codeBgColor,
        shape = RoundedCornerShape(Dimens.cornerRadiusMd),
        border = BorderStroke(Dimens.borderWidth, borderColorCode),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            if (language.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBgColor)
                        .padding(horizontal = Spacing.cardPadding, vertical = Spacing.xs),
                ) {
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontFamily = FontFamily.Monospace,
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                            Toast.makeText(context, "Copied code", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(Dimens.touchTargetMin)
                            .minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = iconTintColor,
                            modifier = Modifier.size(Dimens.iconSizeXs),
                        )
                    }
                }
            }

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.sm)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = code,
                        color = codeTextColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownFormattedText(text: String, modifier: Modifier = Modifier) {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val lines = remember(text) { text.split("\n") }
    Column(modifier = modifier) {
        var inCodeBlock = false
        var codeLang = ""
        val codeBuilder = StringBuilder()

        for (line in lines) {
            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    CodeBlock(
                        code = codeBuilder.toString().trimEnd(),
                        language = codeLang,
                        modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                    codeBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLang = line.trim().removePrefix("```").trim()
                }
            } else if (inCodeBlock) {
                codeBuilder.append(line).append("\n")
            } else {
                if (line.startsWith("### ")) {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
                    )
                } else if (line.startsWith("## ")) {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.cardPadding, bottom = Spacing.xs),
                    )
                } else if (line.startsWith("# ")) {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
                    )
                } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                    MarkdownBullet(
                        bulletText = line.trimStart().drop(2),
                        codeBackground = codeBackground,
                        accent = accent,
                    )
                } else if (line.isNotBlank()) {
                    MarkdownParagraph(
                        line = line,
                        codeBackground = codeBackground,
                        accent = accent,
                    )
                } else {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }
            }
        }

        if (inCodeBlock && codeBuilder.isNotEmpty()) {
            CodeBlock(
                code = codeBuilder.toString().trimEnd(),
                language = codeLang,
                modifier = Modifier.padding(vertical = Spacing.xs),
            )
        }
    }
}

@Composable
private fun MarkdownBullet(bulletText: String, codeBackground: Color, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = Spacing.xxs)) {
        Text(
            text = "•",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = Spacing.sm),
        )
        val annotated = remember(bulletText, codeBackground, accent) {
            parseInlineMarkdown(bulletText, codeBackground, accent)
        }
        InlineMarkdownText(
            annotatedString = annotated,
            modifier = Modifier,
        )
    }
}

@Composable
private fun MarkdownParagraph(line: String, codeBackground: Color, accent: Color, modifier: Modifier = Modifier) {
    val annotated = remember(line, codeBackground, accent) {
        parseInlineMarkdown(line, codeBackground, accent)
    }
    InlineMarkdownText(
        annotatedString = annotated,
        modifier = modifier.padding(vertical = Spacing.xxs),
    )
}

@Suppress("CyclomaticComplexMethod")
private fun parseInlineMarkdown(text: String, codeBackground: Color, accent: Color): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < text.length) {
            val boldStart = text.indexOf("**", idx)
            val codeStart = text.indexOf("`", idx)
            val linkStart = text.indexOf("[", idx)

            val nextSpecial = listOf(boldStart, codeStart, linkStart)
                .filter { it != -1 }
                .minOrNull() ?: -1

            if (nextSpecial == -1) {
                append(text.substring(idx))
                break
            }

            if (nextSpecial > idx) {
                append(text.substring(idx, nextSpecial))
            }

            idx = when (nextSpecial) {
                boldStart -> appendBold(text, boldStart)
                codeStart -> appendCode(text, codeStart, codeBackground, accent)
                linkStart -> appendLink(text, linkStart, accent)
                else -> {
                    append(text.substring(idx, nextSpecial + 1))
                    nextSpecial + 1
                }
            }
        }
    }
}

/** Appends a `**bold**` span starting at [start]; returns the next scan index. */
private fun AnnotatedString.Builder.appendBold(text: String, start: Int): Int {
    val end = text.indexOf("**", start + 2)
    return if (end != -1) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(start + 2, end))
        }
        end + 2
    } else {
        append("**")
        start + 2
    }
}

/** Appends a `` `code` `` span starting at [start]; returns the next scan index. */
private fun AnnotatedString.Builder.appendCode(text: String, start: Int, backgroundColor: Color, accent: Color): Int {
    val end = text.indexOf("`", start + 1)
    return if (end != -1) {
        withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                color = accent,
                background = backgroundColor.copy(alpha = 0.6f),
            ),
        ) {
            append(text.substring(start + 1, end))
        }
        end + 1
    } else {
        append("`")
        start + 1
    }
}

/** Appends a `[label](url)` link starting at [start]; returns the next scan index. */
private fun AnnotatedString.Builder.appendLink(text: String, start: Int, accent: Color): Int {
    val linkEnd = text.indexOf("]", start + 1)
    val parenStart = if (linkEnd != -1) text.indexOf("(", linkEnd) else -1
    val parenEnd = if (parenStart == linkEnd + 1) text.indexOf(")", parenStart + 1) else -1
    if (linkEnd != -1 && parenStart == linkEnd + 1 && parenEnd != -1) {
        val label = text.substring(start + 1, linkEnd)
        val url = text.substring(parenStart + 1, parenEnd)
        if (url.startsWith("http://") || url.startsWith("https://")) {
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(color = accent, fontWeight = FontWeight.Medium),
                    ),
                ),
            ) {
                append(label)
            }
        } else {
            append(text.substring(start, parenEnd + 1))
        }
        return parenEnd + 1
    }
    append("[")
    return start + 1
}

@Composable
private fun InlineMarkdownText(annotatedString: androidx.compose.ui.text.AnnotatedString, modifier: Modifier = Modifier) {
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            textDirection = androidx.compose.ui.text.style.TextDirection.Content,
        ),
        modifier = modifier,
    )
}

private fun formatJsonArguments(map: Map<String, Any?>): String {
    return try {
        JSONObject(map).toString(2)
    } catch (_: Exception) {
        map.toString()
    }
}
