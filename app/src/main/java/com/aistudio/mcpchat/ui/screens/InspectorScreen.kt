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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.data.model.McpLogItem
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.repository.ResourceSummary
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
fun InspectorScreen(viewModel: McpInspectorViewModel, modifier: Modifier = Modifier, onNavigateToServers: (() -> Unit)? = null) {
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
    var showClearConfirm by remember { mutableStateOf(false) }
    val logDateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(selectedToolId, tools.size) {
        if (selectedToolId == null && tools.isNotEmpty()) {
            viewModel.selectTool(tools.first())
        }
    }

    val selectedTool = tools.find { it.id == selectedToolId }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Column {
                        Text(
                            text = "MCP Inspector & Test Bench",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Direct JSON-RPC tool invocation & telemetry logs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
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
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "Resources tab",
                                modifier = Modifier.size(Dimens.iconSizeMd),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Resources", fontWeight = FontWeight.SemiBold)
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Connect to an MCP server and sync tools to get started",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.xxs))
                                    Text(
                                        text = "Go to MCP Servers tab",
                                        style = MaterialTheme.typography.bodyMedium,
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
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanSecondary,
                                        fontFamily = FontFamily.Monospace,
                                    )

                                    if (logs.isNotEmpty()) {
                                        TextButton(
                                            onClick = { showClearConfirm = true },
                                            colors = ButtonDefaults.textButtonColors(contentColor = RoseError),
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeMd))
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text("Clear Logs", style = MaterialTheme.typography.bodyMedium)
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "  |  ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "$successCount success",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldTertiary,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "  |  ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "$failCount failed",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = RoseError,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Text(
                                                text = "  ($successRate% success rate)",
                                                style = MaterialTheme.typography.bodyMedium,
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "Execute tools in Chat or Playground to view audit logs.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(logs, key = { it.id }) { logItem ->
                                    LogItemCard(
                                        logItem = logItem,
                                        dateFormatter = logDateFormatter,
                                        onReExecute = { toolName, serverId, args -> viewModel.reExecuteTool(toolName, serverId, args) },
                                    )
                                }
                            }
                            if (showClearConfirm) {
                                item {
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { showClearConfirm = false },
                                        title = { Text("Clear audit logs?") },
                                        text = { Text("This will delete all ${logs.size} log entries.") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.clearLogs()
                                                    showClearConfirm = false
                                                },
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
                    2 -> ResourcesTab(viewModel = viewModel, onNavigateToServers = onNavigateToServers)
                }
            }
        }
    }
}

/**
 * MCP resources browser: pick a server, list its resources, read content
 * and subscribe for server-push updates (live content swaps in via
 * resourceEvents).
 */
@Composable
fun ResourcesTab(viewModel: McpInspectorViewModel, modifier: Modifier = Modifier, onNavigateToServers: (() -> Unit)? = null) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val summaries by viewModel.resourceSummaries.collectAsStateWithLifecycle()
    val selectedServerId by viewModel.selectedResourceServerId.collectAsStateWithLifecycle()
    val viewedResource by viewModel.viewedResource.collectAsStateWithLifecycle()
    val viewedContent by viewModel.viewedResourceContent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingResources.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscribedResources.collectAsStateWithLifecycle()

    val enabledServers = servers.filter { it.isEnabled }

    LaunchedEffect(enabledServers.map { it.id }.joinToString()) {
        // Auto-select the first server the first time the tab is opened.
        if (selectedServerId == null && enabledServers.isNotEmpty()) {
            viewModel.selectResourceServer(enabledServers.first().id)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier
            .fillMaxSize()
            .testTag("resources_view"),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "MCP RESOURCES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanSecondary,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "Server-exposed documents and data. Subscribe to track live changes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (enabledServers.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(Spacing.xl),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(Dimens.iconSizeXl),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "No enabled MCP servers",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Go to MCP Servers tab",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary,
                            modifier = Modifier
                                .clickable { onNavigateToServers?.invoke() }
                                .semantics { role = Role.Button }
                                .padding(Spacing.xs),
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    enabledServers.forEach { server ->
                        val selected = server.id == selectedServerId
                        Surface(
                            onClick = { viewModel.selectResourceServer(server.id) },
                            color = if (selected) IndigoPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(Dimens.cornerRadiusXl),
                            modifier = Modifier
                                .testTag("resource_server_chip_${server.id}")
                                .semantics { role = Role.Button },
                        ) {
                            Text(
                                text = server.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (!isLoading && summaries.isEmpty()) {
                item {
                    Text(
                        text = "No resources exposed by this server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.md),
                    )
                }
            }

            items(summaries, key = { it.serverId + it.resource.uri }) { summary ->
                ResourceItemCard(
                    summary = summary,
                    isViewed = viewedResource?.uri == summary.resource.uri,
                    isSubscribed = summary.resource.uri in (subscriptions[summary.serverId] ?: emptySet()),
                    onClick = { viewModel.openResource(summary.resource) },
                    onToggleSubscription = { viewModel.toggleSubscription(summary.resource) },
                )
            }

            if (viewedResource != null) {
                item {
                    ResourceContentCard(
                        title = viewedResource?.name ?: "",
                        uri = viewedResource?.uri ?: "",
                        mimeType = viewedContent?.mimeType,
                        content = viewedContent?.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceItemCard(
    summary: ResourceSummary,
    isViewed: Boolean,
    isSubscribed: Boolean,
    onClick: () -> Unit,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isViewed) IndigoPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = if (isViewed) BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.5f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("resource_item")
            .clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.resource.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary.resource.uri,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onToggleSubscription,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("resource_subscribe_button"),
            ) {
                Icon(
                    imageVector = if (isSubscribed) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                    contentDescription = if (isSubscribed) "Unsubscribe from ${summary.resource.name}" else "Subscribe to ${summary.resource.name}",
                    tint = if (isSubscribed) EmeraldTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
    }
}

@Composable
private fun ResourceContentCard(title: String, uri: String, mimeType: String?, content: String?, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("resource_content_card"),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (mimeType != null) {
                    Text(
                        text = mimeType,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanSecondary,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = uri,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            if (content == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.iconSizeSm),
                        strokeWidth = Dimens.borderWidthThick,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Reading resource…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
fun ToolSelectorCard(tools: List<McpTool>, selectedTool: McpTool?, onToolSelected: (McpTool) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    val filteredTools = remember(tools, pickerQuery) {
        if (pickerQuery.isBlank()) {
            tools
        } else {
            tools.filter {
                it.name.contains(pickerQuery, ignoreCase = true) ||
                    it.serverName.contains(pickerQuery, ignoreCase = true)
            }
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
                style = MaterialTheme.typography.labelSmall,
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (selectedTool != null) {
                            Text(
                                text = "Server: ${selectedTool.serverName}",
                                style = MaterialTheme.typography.labelSmall,
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
                    onDismissRequest = {
                        expanded = false
                        pickerQuery = ""
                    },
                    modifier = Modifier.heightIn(max = 320.dp),
                ) {
                    if (tools.size > 5) {
                        OutlinedTextField(
                            value = pickerQuery,
                            onValueChange = { pickerQuery = it },
                            placeholder = { Text("Filter tools...", style = MaterialTheme.typography.bodyMedium) },
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
                                    Text("${t.serverName} • ${t.description.take(50)}...", style = MaterialTheme.typography.bodySmall)
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
                            text = { Text("No matches", style = MaterialTheme.typography.bodyMedium) },
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
                    style = MaterialTheme.typography.bodyMedium,
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CyanSecondary,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            if (parameterInputs.isEmpty()) {
                Text(
                    text = "No parameters required for this tool.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val requiredKeys = remember(tool.inputSchemaJson) {
                    try {
                        val arr = JSONObject(tool.inputSchemaJson).optJSONArray("required")
                        (0 until (arr?.length() ?: 0)).map { i -> arr!!.getString(i) }.toSet()
                    } catch (_: Exception) {
                        emptySet()
                    }
                }
                val propTypes = remember(tool.inputSchemaJson) {
                    try {
                        val props = JSONObject(tool.inputSchemaJson).optJSONObject("properties")
                        buildMap {
                            props?.keys()?.forEach { k ->
                                put(k, props.optJSONObject(k)?.optString("type")?.lowercase())
                            }
                        }
                    } catch (_: Exception) {
                        emptyMap()
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    for ((key, value) in parameterInputs) {
                        val isError = value.isBlank() && requiredKeys.contains(key)
                        val helperText = when (propTypes[key]) {
                            "integer" -> if (value.isNotBlank() && value.toIntOrNull() == null) "Must be an integer" else null
                            "number" -> if (value.isNotBlank() && value.toDoubleOrNull() == null) "Must be a number" else null
                            "boolean" -> if (value.isNotBlank() && value.lowercase() !in setOf("true", "false")) "Must be true or false" else null
                            else -> null
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { onParameterChange(key, it) },
                            label = { Text(key, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium) },
                            isError = isError || helperText != null,
                            supportingText = helperText?.let { msg ->
                                { Text(msg, style = MaterialTheme.typography.labelSmall, color = RoseError) }
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
                    Text("Invoking over JSON-RPC...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeMd))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Invoke MCP Tool", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                        style = MaterialTheme.typography.labelSmall,
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
                        style = MaterialTheme.typography.labelSmall,
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
fun LogItemCard(
    logItem: McpLogItem,
    dateFormatter: SimpleDateFormat? = null,
    onReExecute: ((String, String, Map<String, Any?>) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${logItem.latencyMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldTertiary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(
                        text = (dateFormatter ?: SimpleDateFormat("HH:mm:ss", Locale.getDefault())).format(Date(logItem.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Server: ${logItem.serverName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = Spacing.md)) {
                    Text(
                        text = "REQUEST JSON-RPC:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanSecondary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    CodeBlock(code = logItem.requestPayload, language = "json")

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "RESPONSE PAYLOAD:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                            Text("Re-execute", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
