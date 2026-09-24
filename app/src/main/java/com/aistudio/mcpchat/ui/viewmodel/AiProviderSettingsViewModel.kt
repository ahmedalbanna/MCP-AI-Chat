package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.core.endpoint.EndpointDecision
import com.aistudio.mcpchat.core.endpoint.EndpointPolicy
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.security.AiProviderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Draft being edited in the provider sheet; null when the sheet is closed. */
data class ProviderDraft(
    // null => creating a new provider
    val id: String? = null,
    val name: String,
    val typeLabel: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val allowInsecureHttp: Boolean = false,
)

data class AiProviderUiState(
    val configs: List<AiProviderConfig> = emptyList(),
    val configsLoaded: Boolean = false,
    val activeId: String = "",
    val draft: ProviderDraft? = null,
    val feedback: com.aistudio.mcpchat.ui.viewmodel.FeedbackMessage? = null,
)

@HiltViewModel
class AiProviderSettingsViewModel @Inject constructor(
    private val settings: AiProviderSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiProviderUiState())
    val uiState: StateFlow<AiProviderUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                configs = settings.getAll(),
                configsLoaded = true,
                activeId = settings.getActiveId(),
            )
        }
    }

    fun selectActive(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setActive(id)
            _uiState.value = _uiState.value.copy(
                activeId = id,
                feedback = FeedbackMessage("Active AI provider updated"),
            )
        }
    }

    fun startEdit(config: AiProviderConfig) {
        _uiState.value = _uiState.value.copy(
            draft = ProviderDraft(
                id = config.id,
                name = config.name,
                typeLabel = config.type.name,
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                model = config.model,
                allowInsecureHttp = config.allowInsecureHttp,
            ),
        )
    }

    fun updateDraft(transform: (ProviderDraft) -> ProviderDraft) {
        _uiState.value.draft?.let { current ->
            _uiState.value = _uiState.value.copy(draft = transform(current))
        }
    }

    fun dismissDraft() {
        _uiState.value = _uiState.value.copy(draft = null)
    }

    fun saveDraft(confirmPrivateHttp: Boolean = false) {
        val draft = _uiState.value.draft ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val proposedAllowInsecureHttp = draft.allowInsecureHttp || confirmPrivateHttp
            val allowInsecureHttp = EndpointPolicy.evaluate(draft.baseUrl.trim()) is EndpointDecision.RequiresConfirmation &&
                proposedAllowInsecureHttp
            val endpointDecision = EndpointPolicy.authorize(draft.baseUrl.trim(), allowInsecureHttp)
            val config = buildConfig(draft, endpointDecision, allowInsecureHttp)
            val validationFailure = validationFailure(config, endpointDecision)

            if (validationFailure != null) {
                _uiState.value = _uiState.value.copy(
                    feedback = FeedbackMessage(validationFailure, isError = true),
                )
            } else {
                settings.upsert(config)
                _uiState.value = _uiState.value.copy(
                    draft = null,
                    configs = settings.getAll(),
                    configsLoaded = true,
                    feedback = FeedbackMessage("Saved ${config.name}"),
                )
            }
        }
    }

    private fun buildConfig(draft: ProviderDraft, endpointDecision: EndpointDecision, allowInsecureHttp: Boolean): AiProviderConfig {
        val normalizedEndpoint = (endpointDecision as? EndpointDecision.Allowed)?.normalizedUrl ?: draft.baseUrl.trim()
        return AiProviderConfig(
            id = draft.id ?: java.util.UUID.randomUUID().toString(),
            name = draft.name.trim(),
            type = runCatching {
                com.aistudio.mcpchat.data.model.AiProviderType.valueOf(draft.typeLabel)
            }.getOrDefault(com.aistudio.mcpchat.data.model.AiProviderType.OPENAI_COMPATIBLE),
            baseUrl = normalizedEndpoint,
            apiKey = draft.apiKey.trim(),
            model = draft.model.trim(),
            allowInsecureHttp = allowInsecureHttp,
        )
    }

    private fun validationFailure(config: AiProviderConfig, endpointDecision: EndpointDecision): String? {
        return when {
            config.name.isBlank() -> "Name cannot be empty"
            endpointDecision is EndpointDecision.Rejected -> endpointDecision.message
            config.model.isBlank() -> "Model cannot be empty"
            else -> null
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id == _uiState.value.activeId) {
                _uiState.value = _uiState.value.copy(
                    feedback = FeedbackMessage("Switch away before deleting the active provider", isError = true),
                )
                return@launch
            }
            settings.delete(id)
            _uiState.value = _uiState.value.copy(
                configs = settings.getAll(),
                configsLoaded = true,
                feedback = FeedbackMessage("Provider removed"),
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedback = null)
    }
}
