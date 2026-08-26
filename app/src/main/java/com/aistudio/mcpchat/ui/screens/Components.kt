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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
                    )
                } else if (line.startsWith("## ")) {
                    Text(
                        text = line.removePrefix("## "),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.cardPadding, bottom = Spacing.xs),
                    )
                } else if (line.startsWith("# ")) {
                    Text(
                        text = line.removePrefix("# "),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
                    )
                } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                    val bulletText = line.trimStart().drop(2)
                    Row(modifier = Modifier.padding(vertical = Spacing.xxs)) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = Spacing.sm),
                        )
                        InlineMarkdownText(
                            annotatedString = parseInlineMarkdown(bulletText),
                            modifier = Modifier,
                        )
                    }
                } else if (line.isNotBlank()) {
                    InlineMarkdownText(
                        annotatedString = parseInlineMarkdown(line),
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
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
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

            when (nextSpecial) {
                boldStart -> {
                    val boldEnd = text.indexOf("**", boldStart + 2)
                    if (boldEnd != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(boldStart + 2, boldEnd))
                        }
                        idx = boldEnd + 2
                    } else {
                        append("**")
                        idx = boldStart + 2
                    }
                }
                codeStart -> {
                    val codeEnd = text.indexOf("`", codeStart + 1)
                    if (codeEnd != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = CyanSecondary,
                                background = Color(0xFF1E293B).copy(alpha = 0.5f),
                            ),
                        ) {
                            append(text.substring(codeStart + 1, codeEnd))
                        }
                        idx = codeEnd + 1
                    } else {
                        append("`")
                        idx = codeStart + 1
                    }
                }
                linkStart -> {
                    val linkEnd = text.indexOf("]", linkStart + 1)
                    val parenStart = if (linkEnd != -1) text.indexOf("(", linkEnd) else -1
                    val parenEnd = if (parenStart == linkEnd + 1) text.indexOf(")", parenStart + 1) else -1
                    if (linkEnd != -1 && parenStart == linkEnd + 1 && parenEnd != -1) {
                        val label = text.substring(linkStart + 1, linkEnd)
                        val url = text.substring(parenStart + 1, parenEnd)
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(SpanStyle(color = CyanSecondary, fontWeight = FontWeight.Medium)) {
                                append(label)
                            }
                            pop()
                        } else {
                            append(text.substring(linkStart, parenEnd + 1))
                        }
                        idx = parenEnd + 1
                    } else {
                        append("[")
                        idx = linkStart + 1
                    }
                }
                else -> {
                    append(text.substring(idx, nextSpecial + 1))
                    idx = nextSpecial + 1
                }
            }
        }
    }
}

@Composable
private fun InlineMarkdownText(annotatedString: androidx.compose.ui.text.AnnotatedString, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = annotatedString,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp,
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (_: Exception) { }
                }
        },
    )
}

private fun formatJsonArguments(map: Map<String, Any?>): String {
    return try {
        JSONObject(map).toString(2)
    } catch (_: Exception) {
        map.toString()
    }
}
