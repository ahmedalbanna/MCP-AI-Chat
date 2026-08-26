package com.aistudio.mcpchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderType
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.ProviderDraft

/**
 * Bottom sheet for managing AI providers: pick the active one, add/edit
 * custom endpoints (OpenAI-compatible, Anthropic, Google, local runtimes).
 */
@Composable
fun ProviderSettingsSheetContent(
    uiState: com.aistudio.mcpchat.ui.viewmodel.AiProviderUiState,
    onSelectActive: (String) -> Unit,
    onStartEdit: (AiProviderConfig) -> Unit,
    onDelete: (String) -> Unit,
    onDraftChange: ((ProviderDraft) -> ProviderDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onDismissDraft: () -> Unit,
    onNewDraft: (AiProviderType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.cardPadding),
    ) {
        Text(
            text = "AI Providers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Choose any chat backend. MCP tools work with every provider that supports function calling.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))

        uiState.configs.forEach { config ->
            ProviderRow(
                config = config,
                isActive = config.id == uiState.activeId,
                onSelect = { onSelectActive(config.id) },
                onEdit = { onStartEdit(config) },
                onDelete = { onDelete(config.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = false,
                onClick = { onNewDraft(AiProviderType.OPENAI_COMPATIBLE) },
                label = { Text("Add custom") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
            FilterChip(
                selected = false,
                onClick = { onNewDraft(AiProviderType.GOOGLE) },
                label = { Text("Add Google") },
            )
            FilterChip(
                selected = false,
                onClick = { onNewDraft(AiProviderType.ANTHROPIC) },
                label = { Text("Add Anthropic") },
            )
        }

        uiState.draft?.let { draft ->
            Spacer(Modifier.height(Spacing.md))
            DraftEditor(
                draft = draft,
                onChange = onDraftChange,
                onSave = onSaveDraft,
                onCancel = onDismissDraft,
            )
        }

        uiState.feedback?.let { feedback ->
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = feedback.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (feedback.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun ProviderRow(config: AiProviderConfig, isActive: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        ) {
            RadioButton(selected = isActive, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name.ifBlank { "Unnamed" } + if (isActive) "  •  active" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${config.model} — ${config.baseUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${config.name}")
            }
        }
    }
}

@Composable
private fun DraftEditor(draft: ProviderDraft, onChange: ((ProviderDraft) -> ProviderDraft) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    val isGoogle = draft.typeLabel == AiProviderType.GOOGLE.name

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (draft.id == null) "New provider" else "Edit provider",
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = draft.name,
                onValueChange = { name: String ->
                    onChange { current: ProviderDraft -> current.copy(name = name) }
                },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = draft.typeLabel,
                onValueChange = { typeLabel: String ->
                    onChange { current: ProviderDraft -> current.copy(typeLabel = typeLabel.uppercase()) }
                },
                label = { Text("Protocol (GOOGLE / OPENAI_COMPATIBLE / ANTHROPIC)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = draft.baseUrl,
                onValueChange = { url: String ->
                    onChange { current: ProviderDraft -> current.copy(baseUrl = url.trim()) }
                },
                label = { Text("Endpoint base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = draft.apiKey,
                onValueChange = { key: String ->
                    onChange { current: ProviderDraft -> current.copy(apiKey = key.trim()) }
                },
                label = { Text(if (isGoogle) "API key" else "API key (blank ok for local runtimes)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = draft.model,
                onValueChange = { model: String ->
                    onChange { current: ProviderDraft -> current.copy(model = model.trim()) }
                },
                label = { Text("Model") },
                placeholder = { Text("gpt-4o-mini / claude-sonnet-4-5 / gemini-3.5-flash") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onSave) { Text("Save") }
            }
        }
    }
}
