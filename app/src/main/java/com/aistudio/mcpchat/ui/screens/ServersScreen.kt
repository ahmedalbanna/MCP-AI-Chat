package com.aistudio.mcpchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ServersScreen(viewModel: McpServersViewModel, modifier: Modifier = Modifier) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    var showAddServerSheet by remember { mutableStateOf(false) }
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Model Context Protocol (2024-11-05) Integrations",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showAddServerSheet = true },
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
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Connected servers expose tools to Gemini via JSON-RPC. " +
                                        "When enabled, tools are dynamically registered for conversational function calling.",
                                    fontSize = 11.sp,
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
                                Text("Search servers...", fontSize = 13.sp)
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
                                fontSize = 12.sp,
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
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "No MCP servers connected",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = "Add your first server to get started",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Button(
                                    onClick = { showAddServerSheet = true },
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

        if (showAddServerSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showAddServerSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                AddServerSheetContent(
                    onDismiss = { showAddServerSheet = false },
                    onAdd = { name, desc, url, auth ->
                        viewModel.addCustomServer(name, desc, url, auth)
                        showAddServerSheet = false
                    },
                )
            }
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
                            fontSize = 15.sp,
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
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
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
                        modifier = Modifier.testTag("toggle_server_${server.id}"),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = server.description,
                    fontSize = 12.sp,
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
                        fontSize = 11.sp,
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
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "Not Secure — plain HTTP, data can be intercepted",
                                fontSize = 11.sp,
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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(Dimens.iconSizeXs), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = "Ping server", modifier = Modifier.size(Dimens.iconSizeSm))
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("Ping", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onSync,
                            enabled = !isTesting,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync tools", modifier = Modifier.size(Dimens.iconSizeSm))
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Sync (${tools.size})", fontSize = 11.sp)
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Tools" else "View Tools (${tools.size})",
                            fontSize = 11.sp,
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )

                        if (tools.isEmpty()) {
                            Text(
                                text = "No tools discovered yet. Tap 'Sync' to handshake with this server.",
                                fontSize = 11.sp,
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
                                                        fontSize = 12.sp,
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
                                                    )
                                                }
                                            }

                                            Text(
                                                text = tool.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerSheetContent(onDismiss: () -> Unit, onAdd: (name: String, description: String, endpointUrl: String, authHeader: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var endpointUrl by remember { mutableStateOf("") }
    var authHeader by remember { mutableStateOf("") }
    var showInsecureConfirm by remember { mutableStateOf(false) }
    val isInsecureInput = McpRepository.isInsecureEndpoint(endpointUrl)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.sheetPadding),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(Spacing.xs)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(Dimens.cornerRadiusFull))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Add Remote MCP Server",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Connect any remote server implementing the Model Context Protocol (HTTP POST or SSE).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xxs),
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            QuickActionChip("Local (10.0.2.2:3000)") {
                name = "Localhost Dev MCP"
                description = "Local node / python MCP server"
                endpointUrl = "http://10.0.2.2:3000/mcp"
            }
            QuickActionChip("FastMCP SSE") {
                name = "FastMCP Remote"
                description = "FastMCP Python cloud runner"
                endpointUrl = "https://fastmcp-runner.fly.dev/sse"
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Server Name") },
                placeholder = { Text("e.g. Production Database MCP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g. Query production read replica") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            val urlError = endpointUrl.isNotBlank() && !endpointUrl.startsWith("http")
            OutlinedTextField(
                value = endpointUrl,
                onValueChange = { endpointUrl = it },
                label = { Text("Endpoint URL") },
                placeholder = { Text("https://mcp.my-domain.com/v1") },
                singleLine = true,
                isError = urlError,
                supportingText = if (urlError) {
                    { Text("Must start with http:// or https://", color = RoseError) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = authHeader,
                onValueChange = { authHeader = it },
                label = { Text("Auth Header (Optional)") },
                placeholder = { Text("Bearer sk-mcp-token-xyz") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isInsecureInput) {
                Surface(
                    color = AmberWarning.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                    border = BorderStroke(Dimens.borderWidth, AmberWarning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(Spacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(Dimens.iconSizeMd),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Not Secure MCP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                            )
                            Text(
                                text = "This server uses plain HTTP. Tokens and tool data can be intercepted. Only connect if you trust the network.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (name.isNotBlank() && endpointUrl.isNotBlank()) {
                        if (isInsecureInput) {
                            showInsecureConfirm = true
                        } else {
                            onAdd(name, description, endpointUrl, authHeader)
                        }
                    }
                },
                enabled = name.isNotBlank() && endpointUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.weight(1f),
            ) {
                Text("Connect & Sync")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        if (showInsecureConfirm) {
            AlertDialog(
                onDismissRequest = { showInsecureConfirm = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning) },
                title = { Text("Not Secure MCP — Continue?") },
                text = {
                    Text(
                        "You are about to connect to \"$endpointUrl\" over plain HTTP.\n\n" +
                            "Auth headers and tool payloads will be sent without encryption and can be intercepted on the network. " +
                            "Only proceed if you trust this server and network.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showInsecureConfirm = false
                            onAdd(name, description, endpointUrl, authHeader)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    ) { Text("Accept & Connect") }
                },
                dismissButton = {
                    TextButton(onClick = { showInsecureConfirm = false }) { Text("Cancel") }
                },
            )
        }
    }
}
