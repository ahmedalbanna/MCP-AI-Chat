package com.aistudio.mcpchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.model.ServerStatus
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.ui.theme.AmberWarning
import com.aistudio.mcpchat.ui.theme.CyanSecondary
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.EmeraldTertiary
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
import com.aistudio.mcpchat.ui.theme.IndigoPrimaryLight
import com.aistudio.mcpchat.ui.theme.RoseError
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    viewModel: McpServersViewModel,
    onAddServer: () -> Unit = {},
    onEditServer: (McpServer) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var serverToDelete by remember { mutableStateOf<McpServer?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredServers = remember(servers, searchQuery) {
        if (searchQuery.isBlank()) {
            servers
        } else {
            servers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.endpointUrl.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.clearFeedback()
        }
    }

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
                            text = "MCP Remote Servers Hub",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Model Context Protocol (2024-11-05) Integrations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddServer,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_custom_server_fab"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Remote MCP Server")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    for (server in servers) {
                        viewModel.syncToolsSuspend(server)
                    }
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("servers_list"),
            ) {
                item {
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                        border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(Spacing.cardPadding),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Hub",
                                tint = IndigoPrimaryLight,
                                modifier = Modifier.size(Dimens.iconSizeLg),
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Dynamic Tool Discovery Active",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Connected servers expose tools to Gemini via JSON-RPC. " +
                                        "When enabled, tools are dynamically registered for conversational function calling.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (servers.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Search servers...", style = MaterialTheme.typography.bodyMedium)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search servers", modifier = Modifier.size(Dimens.iconSizeMd))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_search_field"),
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "${filteredServers.size} of ${servers.size} servers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xxs),
                            )
                        }
                    }
                }

                if (filteredServers.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xxl),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "No servers",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(Dimens.touchTargetMin),
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "No MCP servers connected",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = "Add your first server to get started",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Button(
                                    onClick = onAddServer,
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                ) { Text("Add Server") }
                            }
                        }
                    }
                }

                items(filteredServers, key = { it.id }) { server ->
                    val serverTools = tools.filter { it.serverId == server.id }
                    ServerCardItem(
                        server = server,
                        tools = serverTools,
                        isTesting = isTesting == server.id,
                        onToggleEnabled = { isEnabled -> viewModel.toggleServer(server.id, isEnabled) },
                        onToggleTool = { toolId, isEnabled -> viewModel.toggleTool(toolId, isEnabled) },
                        onPing = { viewModel.pingServer(server) },
                        onSync = { viewModel.syncTools(server) },
                        onEdit = { onEditServer(server) },
                        onDelete = { serverToDelete = server },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }

        serverToDelete?.let { server ->
            AlertDialog(
                onDismissRequest = { serverToDelete = null },
                title = { Text("Delete Server") },
                text = { Text("Are you sure you want to delete \"${server.name}\"? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteServer(server.id)
                            serverToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { serverToDelete = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
fun ServerCardItem(
    server: McpServer,
    tools: List<McpTool>,
    isTesting: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    onPing: () -> Unit,
    onSync: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when {
        !server.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        server.status == ServerStatus.CONNECTED -> EmeraldTertiary
        server.status == ServerStatus.CONNECTING -> AmberWarning
        else -> RoseError
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(
            Dimens.borderWidth,
            if (server.isEnabled) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("server_card_${server.id}"),
    ) {
        Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Dimens.cornerRadiusFull))
                    .background(statusColor),
            )

            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.width(Spacing.sm))

                        if (server.isPreset) {
                            Surface(
                                color = CyanSecondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                            ) {
                                Text(
                                    text = "PRESET",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyanSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Switch(
                        checked = server.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IndigoPrimary,
                            checkedTrackColor = IndigoPrimary.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier
                            .semantics {
                                contentDescription = "${server.name} ${if (server.isEnabled) "enabled" else "disabled"}"
                                stateDescription = if (server.isEnabled) "On" else "Off"
                            }
                            .testTag("toggle_server_${server.id}"),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = server.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = server.endpointUrl,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    ServerStatusBadge(
                        status = server.status,
                        latencyMs = server.latencyMs,
                    )
                }

                if (McpRepository.isInsecureEndpoint(server.endpointUrl)) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Surface(
                        color = AmberWarning.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                        border = BorderStroke(Dimens.borderWidth, AmberWarning.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(Dimens.iconSizeSm),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "Not Secure â€” plain HTTP, data can be intercepted",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberWarning,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sectionGap))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(
                            onClick = onPing,
                            enabled = !isTesting,
                            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
                            modifier = Modifier.height(Spacing.xxl),
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(Dimens.iconSizeXs), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = "Ping server", modifier = Modifier.size(Dimens.iconSizeSm))
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("Ping", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedButton(
                            onClick = onSync,
                            enabled = !isTesting,
                            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
                            modifier = Modifier.height(Spacing.xxl),
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync tools", modifier = Modifier.size(Dimens.iconSizeSm))
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Sync (${tools.size})", style = MaterialTheme.typography.labelSmall)
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(32.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("edit_server_${server.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit server",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(Dimens.iconSizeMd),
                            )
                        }

                        if (!server.isPreset) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(32.dp)
                                    .minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete server",
                                    tint = RoseError,
                                    modifier = Modifier.size(Dimens.iconSizeMd),
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
                        modifier = Modifier.height(Spacing.xxl),
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Tools" else "View Tools (${tools.size})",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse tools" else "Expand tools",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sectionGap),
                    ) {
                        Text(
                            text = "DISCOVERED MCP TOOLS:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )

                        if (tools.isEmpty()) {
                            Text(
                                text = "No tools discovered yet. Tap 'Sync' to handshake with this server.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                for (tool in tools) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                                        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(Spacing.sm)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Build,
                                                        contentDescription = "Tool",
                                                        tint = IndigoPrimaryLight,
                                                        modifier = Modifier.size(Dimens.iconSizeSm),
                                                    )
                                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                                    Text(
                                                        text = tool.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontFamily = FontFamily.Monospace,
                                                    )
                                                }

                                                Box(modifier = Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                                                    Switch(
                                                        checked = tool.isEnabled,
                                                        onCheckedChange = { onToggleTool(tool.id, it) },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = CyanSecondary,
                                                            checkedTrackColor = CyanSecondary.copy(alpha = 0.3f),
                                                        ),
                                                        modifier = Modifier
                                                            .semantics {
                                                                contentDescription = "${tool.name} ${if (tool.isEnabled) "enabled" else "disabled"}"
                                                                stateDescription = if (tool.isEnabled) "On" else "Off"
                                                            }
                                                            .testTag("toggle_tool_${tool.id}"),
                                                    )
                                                }
                                            }

                                            Text(
                                                text = tool.description,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = Spacing.xxs),
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
    }
}
