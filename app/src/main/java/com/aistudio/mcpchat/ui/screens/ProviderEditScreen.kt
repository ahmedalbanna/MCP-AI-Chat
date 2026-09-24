package com.aistudio.mcpchat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.core.endpoint.EndpointDecision
import com.aistudio.mcpchat.core.endpoint.EndpointPolicy
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderType
import com.aistudio.mcpchat.ui.theme.AmberWarning
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.AiProviderSettingsViewModel

/**
 * Full-screen form for adding or editing an AI provider. A non-null [configId]
 * edits that provider; otherwise [presetType] seeds a new draft (defaults to a
 * custom OpenAI-compatible endpoint). On a successful save the user is
 * returned via [onNavigateBack]; validation errors surface as a snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    viewModel: AiProviderSettingsViewModel,
    configId: String?,
    presetType: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var draftInitialized by rememberSaveable { mutableStateOf(false) }
    var providerMissing by rememberSaveable { mutableStateOf(false) }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }

    // Seed the shared draft once route args + configs are available.
    LaunchedEffect(uiState.configs, uiState.configsLoaded) {
        if (draftInitialized || providerMissing) return@LaunchedEffect
        val existing = configId?.let { id -> uiState.configs.find { it.id == id } }
        when {
            existing != null -> {
                viewModel.startEdit(existing)
                draftInitialized = true
            }

            configId == null -> {
                val preset = when (presetType) {
                    AiProviderType.GOOGLE.name -> AiProviderConfig.google()
                    AiProviderType.ANTHROPIC.name -> AiProviderConfig.anthropic()
                    else -> AiProviderConfig.custom()
                }
                viewModel.startEdit(preset.copy(id = ""))
                draftInitialized = true
            }

            uiState.configsLoaded -> providerMissing = true
        }
    }

    // Save success (draft cleared) pops back; validation errors stay here.
    LaunchedEffect(uiState.draft, uiState.feedback) {
        if (!saveAttempted) return@LaunchedEffect
        val feedback = uiState.feedback
        when {
            feedback != null && feedback.isError -> {
                snackbarHostState.showSnackbar(feedback.text)
                viewModel.clearFeedback()
                saveAttempted = false
            }

            uiState.draft == null -> {
                viewModel.clearFeedback()
                onNavigateBack()
            }
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
                        text = if (configId == null) "Add provider" else "Edit provider",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.dismissDraft()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize().testTag("provider_edit_screen"),
    ) { padding ->
        if (providerMissing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Provider not found",
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

        val draft = uiState.draft
        if (draft == null && !providerMissing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { Text("Loading provider…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@Scaffold
        }

        if (draft == null) {
            return@Scaffold
        }

        ProviderDraftForm(
            draft = draft,
            isEdit = configId != null,
            onChange = viewModel::updateDraft,
            onCancel = {
                viewModel.dismissDraft()
                onNavigateBack()
            },
            onSave = { confirmedPrivateHttp ->
                saveAttempted = true
                viewModel.saveDraft(confirmedPrivateHttp)
            },
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDraftForm(
    draft: com.aistudio.mcpchat.ui.viewmodel.ProviderDraft,
    isEdit: Boolean,
    onChange: ((com.aistudio.mcpchat.ui.viewmodel.ProviderDraft) -> com.aistudio.mcpchat.ui.viewmodel.ProviderDraft) -> Unit,
    onCancel: () -> Unit,
    onSave: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGoogle = draft.typeLabel == AiProviderType.GOOGLE.name
    val focusManager = LocalFocusManager.current
    val trimmedName = draft.name.trim()
    val trimmedUrl = draft.baseUrl.trim()
    val trimmedModel = draft.model.trim()
    val trimmedKey = draft.apiKey.trim()
    val endpointDecision = EndpointPolicy.evaluate(trimmedUrl)
    val endpointError = if (trimmedUrl.isBlank()) null else (endpointDecision as? EndpointDecision.Rejected)?.message
    val requiresPrivateHttpConfirmation = endpointDecision is EndpointDecision.RequiresConfirmation &&
        !draft.allowInsecureHttp
    var showPrivateHttpConfirmation by rememberSaveable { mutableStateOf(false) }
    val googleKeyError = isGoogle && trimmedKey.isBlank()

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        Text(
            text = "Choose any chat backend. MCP tools work with every provider that supports function calling.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = draft.name,
            onValueChange = { name: String ->
                onChange { current -> current.copy(name = name) }
            },
            label = { Text("Display name") },
            singleLine = true,
            isError = trimmedName.isBlank(),
            supportingText = if (trimmedName.isBlank()) {
                { Text("Display name is required") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier.fillMaxWidth().testTag("provider_name_field"),
        )

        var typeMenuExpanded by rememberSaveable { mutableStateOf(false) }
        val protocolOptions = listOf(
            AiProviderType.GOOGLE.name,
            AiProviderType.ANTHROPIC.name,
            AiProviderType.OPENAI_COMPATIBLE.name,
        )
        ExposedDropdownMenuBox(
            expanded = typeMenuExpanded,
            onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
        ) {
            OutlinedTextField(
                value = draft.typeLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Protocol") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .testTag("provider_protocol_field"),
            )
            ExposedDropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                protocolOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            val preset = when (option) {
                                AiProviderType.GOOGLE.name -> AiProviderConfig.google()
                                AiProviderType.ANTHROPIC.name -> AiProviderConfig.anthropic()
                                else -> AiProviderConfig.custom()
                            }
                            onChange { current ->
                                current.copy(
                                    typeLabel = option,
                                    baseUrl = preset.baseUrl,
                                    model = preset.model,
                                    allowInsecureHttp = false,
                                )
                            }
                            typeMenuExpanded = false
                        },
                        modifier = Modifier.testTag("provider_protocol_option_$option"),
                    )
                }
            }
        }

        OutlinedTextField(
            value = draft.baseUrl,
            onValueChange = { url: String ->
                onChange { current -> current.copy(baseUrl = url, allowInsecureHttp = false) }
            },
            label = { Text("Endpoint base URL") },
            placeholder = { Text("https://api.openai.com/v1") },
            singleLine = true,
            isError = endpointError != null,
            supportingText = if (endpointError != null) {
                { Text(endpointError, color = MaterialTheme.colorScheme.error) }
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
            modifier = Modifier.fillMaxWidth().testTag("provider_url_field"),
        )

        if (endpointDecision is EndpointDecision.RequiresConfirmation) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                ),
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
                    Text(
                        text = if (draft.allowInsecureHttp) {
                            "Private-network HTTP is enabled for this provider. Use HTTPS outside a trusted local network."
                        } else {
                            "Private-network HTTP sends API keys without encryption. Confirm this endpoint before saving."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        var apiKeyVisible by rememberSaveable { mutableStateOf(false) }
        OutlinedTextField(
            value = draft.apiKey,
            onValueChange = { key: String ->
                onChange { current -> current.copy(apiKey = key) }
            },
            label = { Text(if (isGoogle) "API key" else "API key (blank ok for local runtimes)") },
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            singleLine = true,
            isError = googleKeyError,
            supportingText = if (googleKeyError) {
                { Text("Google providers require an API key", color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
            trailingIcon = {
                IconButton(
                    onClick = { apiKeyVisible = !apiKeyVisible },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_api_key_visibility"),
                ) {
                    Icon(
                        imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("provider_key_field"),
        )

        OutlinedTextField(
            value = draft.model,
            onValueChange = { model: String ->
                onChange { current -> current.copy(model = model) }
            },
            label = { Text("Model") },
            placeholder = { Text("gpt-4o-mini / claude-sonnet-4-5 / gemini-3.6-flash") },
            singleLine = true,
            isError = trimmedModel.isBlank(),
            supportingText = if (trimmedModel.isBlank()) {
                { Text("Model is required") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.fillMaxWidth().testTag("provider_model_field"),
        )

        val isDraftValid = trimmedName.isNotBlank() &&
            trimmedUrl.isNotBlank() &&
            endpointError == null &&
            trimmedModel.isNotBlank() &&
            !googleKeyError

        if (!isDraftValid) {
            Text(
                text = "Fill the required fields above to enable saving",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (requiresPrivateHttpConfirmation) {
                        showPrivateHttpConfirmation = true
                    } else {
                        onSave(false)
                    }
                },
                enabled = isDraftValid,
                modifier = Modifier.weight(1f).testTag("save_provider_button"),
            ) {
                Text(if (isEdit) "Save Changes" else "Save Provider")
            }
        }

        if (showPrivateHttpConfirmation) {
            AlertDialog(
                onDismissRequest = { showPrivateHttpConfirmation = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning) },
                title = { Text("Enable private-network HTTP?") },
                text = {
                    Text(
                        "The API key and chat content for $trimmedUrl will be sent without transport encryption. " +
                            "Only continue when you trust this device and local network.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPrivateHttpConfirmation = false
                            onSave(true)
                        },
                    ) { Text("Trust & Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showPrivateHttpConfirmation = false }) { Text("Cancel") }
                },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}
