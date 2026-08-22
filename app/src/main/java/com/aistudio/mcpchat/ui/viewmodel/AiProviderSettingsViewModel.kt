package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.security.AiProviderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Draft being edited in the provider sheet; null when the sheet is closed. */
data class ProviderDraft(
    val id: String? = null, // null => creating a new provider
    val name: String,
    val typeLabel: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)

data class AiProviderUiState(
    val configs: List<AiProviderConfig> = emptyList(),
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

    fun saveDraft() {
        val draft = _uiState.value.draft ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val config = AiProviderConfig(
                id = draft.id ?: java.util.UUID.randomUUID().toString(),
                name = draft.name.trim(),
                type = runCatching {
                    com.aistudio.mcpchat.data.model.AiProviderType.valueOf(draft.typeLabel)
                }.getOrDefault(com.aistudio.mcpchat.data.model.AiProviderType.OPENAI_COMPATIBLE),
                baseUrl = draft.baseUrl.trim(),
                apiKey = draft.apiKey.trim(),
                model = draft.model.trim(),
            )

            when {
                config.name.isBlank() ->
                    _uiState.value = _uiState.value.copy(
                        feedback = FeedbackMessage("Name cannot be empty", isError = true),
                    )
                config.baseUrl.isBlank() || !config.baseUrl.startsWith("http") ->
                    _uiState.value = _uiState.value.copy(
                        feedback = FeedbackMessage("Endpoint must be a valid http(s) URL", isError = true),
                    )
                config.model.isBlank() ->
                    _uiState.value = _uiState.value.copy(
                        feedback = FeedbackMessage("Model cannot be empty", isError = true),
                    )
                else -> {
                    settings.upsert(config)
                    _uiState.value = _uiState.value.copy(
                        draft = null,
                        configs = settings.getAll(),
                        feedback = FeedbackMessage("Saved ${config.name}"),
                    )
                }
            }
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
                feedback = FeedbackMessage("Provider removed"),
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedback = null)
    }
}
