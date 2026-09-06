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
import androidx.compose.ui.unit.sp
import com.aistudio.mcpchat.data.model.McpToolCall
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.model.ToolExecutionStatus
import com.aistudio.mcpchat.ui.theme.AmberWarning
import com.aistudio.mcpchat.ui.theme.CyanSecondary
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.EmeraldTertiary
import com.aistudio.mcpchat.ui.theme.RoseError
import com.aistudio.mcpchat.ui.theme.Spacing
import org.json.JSONObject

@Composable
fun ServerStatusBadge(status: ServerStatus, latencyMs: Long = 0, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        ServerStatus.CONNECTED -> Triple(
            EmeraldTertiary.copy(alpha = 0.15f),
            EmeraldTertiary,
            if (latencyMs > 0) "${latencyMs}ms" else "Online",
        )
        ServerStatus.CONNECTING -> Triple(
            AmberWarning.copy(alpha = 0.15f),
            AmberWarning,
            "Connecting...",
        )
        ServerStatus.ERROR -> Triple(
            RoseError.copy(alpha = 0.15f),
            RoseError,
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
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
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
        ToolExecutionStatus.EXECUTING -> AmberWarning
        ToolExecutionStatus.SUCCESS -> EmeraldTertiary
        ToolExecutionStatus.ERROR -> RoseError
        ToolExecutionStatus.PENDING -> CyanSecondary
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
        Row(modifier = Modifier.clip(RoundedCornerShape(Dimens.cornerRadiusLg))) {
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
                            indication = null,
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
                                    color = AmberWarning,
                                )
                            }
                            ToolExecutionStatus.SUCCESS -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = EmeraldTertiary,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                            ToolExecutionStatus.ERROR -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = RoseError,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                            ToolExecutionStatus.PENDING -> {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Pending",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(Spacing.sm))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MCP Tool:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = toolCall.toolName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = toolCall.serverName,
                                fontSize = 10.sp,
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
                                        fontSize = 10.sp,
                                    ),
                                    color = EmeraldTertiary,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                                )
                            }
                        }

                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(Dimens.iconSizeLg),
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = Spacing.cardPadding)) {
                        Text(
                            text = "INPUT PARAMETERS (JSON-RPC):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (toolCall.isSuccess) EmeraldTertiary else RoseError,
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
                        fontSize = 10.sp,
                        color = CyanSecondary,
                        fontWeight = FontWeight.Bold,
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
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownFormattedText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
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
                    val bulletText = line.trimStart().drop(2)
                    Row(modifier = Modifier.padding(vertical = Spacing.xxs)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = Spacing.sm),
                        )
                        val annotated = remember(bulletText) { parseInlineMarkdown(bulletText) }
                        InlineMarkdownText(
                            annotatedString = annotated,
                            modifier = Modifier,
                        )
                    }
                } else if (line.isNotBlank()) {
                    val annotated = remember(line) { parseInlineMarkdown(line) }
                    InlineMarkdownText(
                        annotatedString = annotated,
                        modifier = Modifier.padding(vertical = Spacing.xxs),
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

@Suppress("CyclomaticComplexMethod")
private fun parseInlineMarkdown(text: String): AnnotatedString {
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
                codeStart -> appendCode(text, codeStart)
                linkStart -> appendLink(text, linkStart)
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
private fun AnnotatedString.Builder.appendCode(text: String, start: Int): Int {
    val end = text.indexOf("`", start + 1)
    return if (end != -1) {
        withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                color = CyanSecondary,
                background = Color(0xFFE2E8F0).copy(alpha = 0.6f),
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
private fun AnnotatedString.Builder.appendLink(text: String, start: Int): Int {
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
                        style = SpanStyle(color = CyanSecondary, fontWeight = FontWeight.Medium),
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
        style = MaterialTheme.typography.bodyMedium,
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
