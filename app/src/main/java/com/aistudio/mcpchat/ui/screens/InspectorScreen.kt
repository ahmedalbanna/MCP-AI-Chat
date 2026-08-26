package com.aistudio.mcpchat.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.ui.theme.CyanSecondary
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.EmeraldTertiary
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
import com.aistudio.mcpchat.ui.theme.RoseError
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.McpInspectorViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    viewModel: McpInspectorViewModel,
    modifier: Modifier = Modifier,
    onNavigateToServers: (() -> Unit)? = null,
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val selectedToolId by viewModel.selectedToolId.collectAsStateWithLifecycle()
    val parameterInputs by viewModel.parameterInputs.collectAsStateWithLifecycle()
    val isInvoking by viewModel.isInvoking.collectAsStateWithLifecycle()
    val latestResult by viewModel.latestResult.collectAsStateWithLifecycle()
    val latestLatencyMs by viewModel.latestLatencyMs.collectAsStateWithLifecycle()
    val latestIsSuccess by viewModel.latestIsSuccess.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(tools) {
        if (selectedToolId == null && tools.isNotEmpty()) {
            viewModel.selectTool(tools.first())
        }
    }

    val selectedTool = tools.find { it.id == selectedToolId }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Column {
                        Text(
                            text = "MCP Inspector & Test Bench",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Direct JSON-RPC tool invocation & telemetry logs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IndigoPrimary,
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = "Tool Playground tab",
                                modifier = Modifier.size(Dimens.iconSizeMd),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Tool Playground", fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Audit Logs tab",
                                modifier = Modifier.size(Dimens.iconSizeMd),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Audit Logs (${logs.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
            }

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn(tween(200)))
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut(tween(200)))
                },
                modifier = Modifier.fillMaxSize(),
                label = "tab_content",
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        if (tools.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(Spacing.lg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(Dimens.iconSizeXl),
                                    )
                                    Text(
                                        text = "No tools available",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Connect to an MCP server and sync tools to get started",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.xxs))
                                    Text(
                                        text = "Go to MCP Servers tab",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary,
                                        modifier = Modifier
                                            .clickable { onNavigateToServers?.invoke() }
                                            .semantics { role = Role.Button }
                                            .padding(Spacing.xs),
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tool_playground_view"),
                            ) {
                                item {
                                    ToolSelectorCard(
                                        tools = tools,
                                        selectedTool = selectedTool,
                                        onToolSelected = { viewModel.selectTool(it) },
                                    )
                                }

                                if (selectedTool != null) {
                                    item {
                                        ParametersCard(
                                            tool = selectedTool,
                                            parameterInputs = parameterInputs,
                                            onParameterChange = { k, v -> viewModel.updateParameter(k, v) },
                                            isInvoking = isInvoking,
                                            onInvoke = { viewModel.invokeSelectedTool() },
                                        )
                                    }
                                }

                                if (latestResult != null) {
                                    item {
                                        ExecutionResultCard(
                                            result = latestResult ?: "",
                                            latencyMs = latestLatencyMs ?: 0,
                                            isSuccess = latestIsSuccess ?: true,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("audit_logs_list"),
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "RAW JSON-RPC TELEMETRY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanSecondary,
                                        fontFamily = FontFamily.Monospace,
                                    )

                                    if (logs.isNotEmpty()) {
                                        var showClearConfirm by remember { mutableStateOf(false) }
                                        TextButton(
                                            onClick = { showClearConfirm = true },
                                            colors = ButtonDefaults.textButtonColors(contentColor = RoseError),
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeMd))
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text("Clear Logs", fontSize = 12.sp)
                                        }
                                        if (showClearConfirm) {
                                            androidx.compose.material3.AlertDialog(
                                                onDismissRequest = { showClearConfirm = false },
                                                title = { Text("Clear audit logs?") },
                                                text = { Text("This will delete all ${logs.size} log entries.") },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = { viewModel.clearLogs(); showClearConfirm = false },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = RoseError),
                                                    ) { Text("Clear") }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            if (logs.isNotEmpty()) {
                                item {
                                    val totalCount = logs.size
                                    val successCount = logs.count { it.isSuccess }
                                    val failCount = totalCount - successCount
                                    val successRate = if (totalCount > 0) (successCount * 100) / totalCount else 0

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                        ) {
                                            Text(
                                                text = "$totalCount calls",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "  |  ",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "$successCount success",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldTertiary,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "  |  ",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "$failCount failed",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = RoseError,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "  ($successRate% success rate)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                    }
                                }
                            }

                            if (logs.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.xxl),
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(Spacing.xl),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Terminal,
                                                contentDescription = "Audit log",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(Dimens.iconSizeXl),
                                            )
                                            Spacer(modifier = Modifier.height(Spacing.sectionGap))
                                            Text(
                                                text = "No MCP invocations recorded yet.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "Execute tools in Chat or Playground to view audit logs.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(logs, key = { it.id }) { logItem ->
                                    LogItemCard(
                                        logItem = logItem,
                                        onReExecute = { toolName, serverId, args -> viewModel.reExecuteTool(toolName, serverId, args) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolSelectorCard(tools: List<McpTool>, selectedTool: McpTool?, onToolSelected: (McpTool) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    val filteredTools = remember(tools, pickerQuery) {
        if (pickerQuery.isBlank()) tools else tools.filter {
            it.name.contains(pickerQuery, ignoreCase = true) ||
                it.serverName.contains(pickerQuery, ignoreCase = true)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Text(
                text = "TARGET MCP TOOL:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanSecondary,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        contentDescription = "Select a tool"
                    }
                    .clickable { expanded = true }
                    .testTag("tool_picker_dropdown"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sectionGap),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTool?.name ?: "Select a Tool...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (selectedTool != null) {
                            Text(
                                text = "Server: ${selectedTool.serverName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false; pickerQuery = "" },
                ) {
                    if (tools.size > 5) {
                        OutlinedTextField(
                            value = pickerQuery,
                            onValueChange = { pickerQuery = it },
                            placeholder = { Text("Filter tools...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        )
                    }
                    for (t in filteredTools) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(t.name, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text("${t.serverName} • ${t.description.take(50)}...", fontSize = 11.sp)
                                }
                            },
                            onClick = {
                                onToolSelected(t)
                                expanded = false
                                pickerQuery = ""
                            },
                        )
                    }
                    if (filteredTools.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No matches", fontSize = 12.sp) },
                            onClick = {},
                            enabled = false,
                        )
                    }
                }
            }

            if (selectedTool != null) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = selectedTool.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ParametersCard(
    tool: McpTool,
    parameterInputs: Map<String, String>,
    onParameterChange: (String, String) -> Unit,
    isInvoking: Boolean,
    onInvoke: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Text(
                text = "INVOCATION PARAMETERS:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanSecondary,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            if (parameterInputs.isEmpty()) {
                Text(
                    text = "No parameters required for this tool.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val requiredKeys = remember(tool.inputSchemaJson) {
                    try {
                        val arr = JSONObject(tool.inputSchemaJson).optJSONArray("required")
                        (0 until (arr?.length() ?: 0)).map { i -> arr!!.getString(i) }.toSet()
                    } catch (_: Exception) { emptySet() }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    for ((key, value) in parameterInputs) {
                        val isError = value.isBlank() && requiredKeys.contains(key)
                        val helperText = when {
                            key == "days" && value.toIntOrNull() == null && value.isNotBlank() -> "Must be a number"
                            key == "amount" && value.toDoubleOrNull() == null && value.isNotBlank() -> "Must be a number"
                            key == "value" && value.toDoubleOrNull() == null && value.isNotBlank() -> "Must be a number"
                            else -> null
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { onParameterChange(key, it) },
                            label = { Text(key, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            isError = isError || helperText != null,
                            supportingText = helperText?.let { msg ->
                                { Text(msg, fontSize = 11.sp, color = RoseError) }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isError || helperText != null) RoseError else IndigoPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("param_input_$key"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.cardPadding))

            Button(
                onClick = onInvoke,
                enabled = !isInvoking,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.minHeightInput)
                    .testTag("invoke_tool_button"),
            ) {
                if (isInvoking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.iconSizeMd),
                        strokeWidth = Dimens.borderWidthThick,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text("Invoking over JSON-RPC...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeMd))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Invoke MCP Tool", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExecutionResultCard(result: String, latencyMs: Long, isSuccess: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(Dimens.borderWidth, if (isSuccess) EmeraldTertiary.copy(alpha = 0.5f) else RoseError.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = if (isSuccess) "Success" else "Error",
                        tint = if (isSuccess) EmeraldTertiary else RoseError,
                        modifier = Modifier.size(Dimens.iconSizeMd),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = if (isSuccess) "EXECUTION SUCCESS" else "EXECUTION ERROR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) EmeraldTertiary else RoseError,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                ) {
                    Text(
                        text = "${latencyMs}ms",
                        fontSize = 11.sp,
                        color = EmeraldTertiary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sectionGap))

            CodeBlock(
                code = result,
                language = "json",
            )
        }
    }
}

@Composable
fun LogItemCard(logItem: McpLogItem, onReExecute: ((String, String, Map<String, Any?>) -> Unit)? = null, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(Dimens.borderWidth, if (logItem.isSuccess) EmeraldTertiary.copy(alpha = 0.3f) else RoseError.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Expand or collapse log details for ${logItem.toolName}"
            }
            .clickable { isExpanded = !isExpanded },
    ) {
        Column(modifier = Modifier.padding(Spacing.sectionGap)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (logItem.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = if (logItem.isSuccess) "Success" else "Error",
                        tint = if (logItem.isSuccess) EmeraldTertiary else RoseError,
                        modifier = Modifier.size(Dimens.iconSizeSm),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = logItem.toolName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${logItem.latencyMs}ms",
                        fontSize = 10.sp,
                        color = EmeraldTertiary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(logItem.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Server: ${logItem.serverName}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = Spacing.md)) {
                    Text(
                        text = "REQUEST JSON-RPC:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanSecondary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    CodeBlock(code = logItem.requestPayload, language = "json")

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "RESPONSE PAYLOAD:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (logItem.isSuccess) EmeraldTertiary else RoseError,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    CodeBlock(code = logItem.responsePayload, language = "json")

                    if (onReExecute != null) {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        OutlinedButton(
                            onClick = {
                                val args = try {
                                    val rpc = JSONObject(logItem.requestPayload)
                                    val params = rpc.optJSONObject("params")
                                    params?.keys()?.asSequence()?.associateWith { k -> params.opt(k) } ?: emptyMap()
                                } catch (_: Exception) {
                                    emptyMap<String, Any?>()
                                }
                                onReExecute(logItem.toolName, logItem.serverId, args)
                            },
                            contentPadding = PaddingValues(horizontal = Spacing.sectionGap, vertical = Spacing.xs),
                            modifier = Modifier
                                .height(30.dp)
                                .minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeSm))
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Re-execute", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
