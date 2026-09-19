package com.aistudio.mcpchat.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.data.model.McpHeaderCodec
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.ui.theme.AmberWarning
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel

/** One editable key/value row in the headers section. */
private data class HeaderEntry(val key: String = "", val value: String = "")

/**
 * Full-screen form for adding or editing a remote MCP server. [serverId] == null
 * means "add"; a non-null id looks the server up in [McpServersViewModel.servers]
 * and prefills the form. On a successful save the user is returned via
 * [onNavigateBack]; validation and repository errors surface as a snackbar
 * while staying on this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditScreen(viewModel: McpServersViewModel, serverId: String?, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val isEdit = serverId != null
    val initial = serverId?.let { id -> servers.find { it.id == id } }

    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var endpointUrl by remember(initial?.id) { mutableStateOf(initial?.endpointUrl ?: "") }
    var headerEntries by remember(initial?.id) {
        mutableStateOf(McpHeaderCodec.decode(initial?.authHeader ?: "").map { (key, value) -> HeaderEntry(key, value) })
    }
    var showInsecureConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val trimmedUrl = endpointUrl.trim()
    val isInsecureInput = McpRepository.isInsecureEndpoint(trimmedUrl)
    val urlError = trimmedUrl.isNotBlank() &&
        !(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))
    val nameError = name.isBlank()
    val duplicateHeader = headerEntries
        .map { it.key.trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .any { it.value > 1 }
    val emptyHeaderKey = headerEntries.any { it.key.isBlank() && it.value.isNotBlank() }
    val isDirty = name != (initial?.name ?: "") ||
        description != (initial?.description ?: "") ||
        endpointUrl != (initial?.endpointUrl ?: "") ||
        headerEntries != McpHeaderCodec.decode(initial?.authHeader ?: "").map { (key, value) -> HeaderEntry(key, value) }

    BackHandler(enabled = isDirty) { showDiscardConfirm = true }

    // Success pops back to the servers list; errors stay here as a snackbar.
    LaunchedEffect(feedback) {
        val message = feedback ?: return@LaunchedEffect
        if (message.isError) {
            snackbarHostState.showSnackbar(message.text)
            viewModel.clearFeedback()
        } else {
            viewModel.clearFeedback()
            onNavigateBack()
        }
    }

    fun submit() {
        // Key/value rows → the JSON multi-header form stored in authHeader.
        val authHeader = McpHeaderCodec.encode(
            headerEntries
                .filter { it.key.trim().isNotEmpty() }
                .associate { it.key.trim() to it.value.trim() },
        )
        val cleanName = name.trim()
        val cleanDescription = description.trim()
        if (isEdit && initial != null) {
            viewModel.updateServer(initial, cleanName, cleanDescription, trimmedUrl, authHeader)
        } else if (!isEdit) {
            viewModel.addCustomServer(cleanName, cleanDescription, trimmedUrl, authHeader)
        }
    }

    fun requestBack() {
        if (isDirty) {
            showDiscardConfirm = true
        } else {
            onNavigateBack()
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
                    Text(
                        text = if (isEdit) "Edit server" else "Add server",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize().testTag("server_edit_screen"),
    ) { padding ->
        // Edit target vanished (e.g. deleted on another screen) — nothing to edit.
        // While the list is still loading show a placeholder instead of a blank
        // add-form so a stale id can never silently create a duplicate.
        if (isEdit && servers.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Text("Loading server…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        if (isEdit && initial == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Server not found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "It may have been removed. Go back and refresh the list.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Button(onClick = onNavigateBack) { Text("Go Back") }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        ) {
            Text(
                text = if (isEdit) {
                    "Update the connection details. Changing the endpoint or auth re-syncs this server's tools."
                } else {
                    "Connect any remote server implementing the Model Context Protocol (HTTP POST or SSE)."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            if (!isEdit) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    QuickActionChip("Local (10.0.2.2:3000)") {
                        if (name.isBlank()) name = "Localhost Dev MCP"
                        if (description.isBlank()) description = "Local node / python MCP server"
                        if (endpointUrl.isBlank()) endpointUrl = "http://10.0.2.2:3000/mcp"
                    }
                    QuickActionChip("FastMCP SSE") {
                        if (name.isBlank()) name = "FastMCP Remote"
                        if (description.isBlank()) description = "FastMCP Python cloud runner"
                        if (endpointUrl.isBlank()) endpointUrl = "https://fastmcp-runner.fly.dev/sse"
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    placeholder = { Text("e.g. Production Database MCP") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Server name is required") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("server_name_field"),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Query production read replica") },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Endpoint URL") },
                    placeholder = { Text("https://mcp.my-domain.com/v1") },
                    singleLine = true,
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text("Must start with http:// or https://", color = MaterialTheme.colorScheme.error) }
                    } else if (trimmedUrl.isBlank()) {
                        { Text("Endpoint URL is required") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("server_url_field"),
                )

                // Custom HTTP headers (key/value) sent with every request to
                // this server — add as many as the server requires.
                Text(
                    text = "HTTP headers",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (duplicateHeader || emptyHeaderKey) {
                    Text(
                        text = if (duplicateHeader) {
                            "Duplicate header names will overwrite each other"
                        } else {
                            "Header name is required when a value is set"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                headerEntries.forEachIndexed { index, entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth().testTag("header_row_$index"),
                    ) {
                        OutlinedTextField(
                            value = entry.key,
                            onValueChange = { newKey ->
                                headerEntries = headerEntries.mapIndexed { i, e ->
                                    if (i == index) e.copy(key = newKey) else e
                                }
                            },
                            label = { Text("Name") },
                            placeholder = { Text("Authorization") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Right) },
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("header_key_$index"),
                        )
                        OutlinedTextField(
                            value = entry.value,
                            onValueChange = { newValue ->
                                headerEntries = headerEntries.mapIndexed { i, e ->
                                    if (i == index) e.copy(value = newValue) else e
                                }
                            },
                            label = { Text("Value") },
                            placeholder = { Text("Bearer sk-mcp-token-xyz") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("header_value_$index"),
                        )
                        IconButton(
                            onClick = {
                                headerEntries = headerEntries.filterIndexed { i, _ -> i != index }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remove_header_$index"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove header",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        // First row defaults to the common auth header name.
                        headerEntries = headerEntries +
                            HeaderEntry(key = if (headerEntries.isEmpty()) "Authorization" else "")
                    },
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSizeSm),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Add Header", style = MaterialTheme.typography.bodyMedium)
                }

                if (isInsecureInput) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
                                    text = "Not secure",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "This server uses plain HTTP. Tokens and tool data can be intercepted. " +
                                        "Only connect if you trust the network.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            val canSubmit = name.trim().isNotBlank() && trimmedUrl.isNotBlank() && !urlError && !duplicateHeader && !emptyHeaderKey
            Button(
                onClick = {
                    if (canSubmit) {
                        if (isInsecureInput) {
                            showInsecureConfirm = true
                        } else {
                            submit()
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(Dimens.touchTargetMin).testTag("submit_server_button"),
            ) {
                Text(if (isEdit) "Save Changes" else "Connect & Sync")
            }

            if (!canSubmit) {
                Text(
                    text = "Enter a server name and a valid http(s) URL to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            if (showDiscardConfirm) {
                AlertDialog(
                    onDismissRequest = { showDiscardConfirm = false },
                    title = { Text("Discard changes?") },
                    text = { Text("You have unsaved changes. Discard them and go back?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDiscardConfirm = false
                                onNavigateBack()
                            },
                        ) {
                            Text("Discard", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep editing") }
                    },
                )
            }

            if (showInsecureConfirm) {
                AlertDialog(
                    onDismissRequest = { showInsecureConfirm = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                    title = { Text("Not Secure MCP — Continue?") },
                    text = {
                        Text(
                            "You are about to connect to \"$trimmedUrl\" over plain HTTP.\n\n" +
                                "Auth headers and tool payloads will be sent without encryption and can be intercepted on the network. " +
                                "Only proceed if you trust this server and network.",
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showInsecureConfirm = false
                                submit()
                            },
                        ) { Text("Accept & Connect") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInsecureConfirm = false }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}
