package com.aistudio.mcpchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderType
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
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

    var draftInitialized by remember { mutableStateOf(false) }
    var providerMissing by remember { mutableStateOf(false) }
    var saveAttempted by remember { mutableStateOf(false) }

    // Seed the shared draft once route args + configs are available.
    LaunchedEffect(uiState.configs) {
        if (draftInitialized) return@LaunchedEffect
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

            uiState.configs.isNotEmpty() -> providerMissing = true
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
                        text = if (configId == null) "Add AI Provider" else "Edit AI Provider",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
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
        if (draft == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { Text("Loading provider…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            onSave = {
                saveAttempted = true
                viewModel.saveDraft()
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
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGoogle = draft.typeLabel == AiProviderType.GOOGLE.name

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
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                protocolOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onChange { current -> current.copy(typeLabel = option) }
                            typeMenuExpanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = draft.baseUrl,
            onValueChange = { url: String ->
                onChange { current -> current.copy(baseUrl = url) }
            },
            label = { Text("Endpoint base URL") },
            placeholder = { Text("https://api.openai.com/v1") },
            singleLine = true,
            isError = draft.baseUrl.isNotBlank() && !draft.baseUrl.startsWith("http"),
            supportingText = if (draft.baseUrl.isNotBlank() && !draft.baseUrl.startsWith("http")) {
                { Text("Must start with http:// or https://", color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        var apiKeyVisible by rememberSaveable { mutableStateOf(false) }
        OutlinedTextField(
            value = draft.apiKey,
            onValueChange = { key: String ->
                onChange { current -> current.copy(apiKey = key) }
            },
            label = { Text(if (isGoogle) "API key" else "API key (blank ok for local runtimes)") },
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
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
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = draft.model,
            onValueChange = { model: String ->
                onChange { current -> current.copy(model = model) }
            },
            label = { Text("Model") },
            placeholder = { Text("gpt-4o-mini / claude-sonnet-4-5 / gemini-3.5-flash") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val isDraftValid = draft.name.isNotBlank() &&
            draft.baseUrl.isNotBlank() &&
            draft.baseUrl.startsWith("http") &&
            draft.model.isNotBlank()

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
                onClick = onSave,
                enabled = isDraftValid,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.weight(1f).testTag("save_provider_button"),
            ) {
                Text(if (isEdit) "Save Changes" else "Save Provider")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}
