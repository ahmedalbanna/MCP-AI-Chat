package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.data.local.ThemePreferences
import com.aistudio.mcpchat.data.local.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeSettings: ThemeSettings,
) : ViewModel() {

    val uiState: StateFlow<ThemePreferences> = themeSettings.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemePreferences(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeSettings.setThemeMode(mode)
        }
    }

    fun setUseAmoledDark(enabled: Boolean) {
        viewModelScope.launch {
            themeSettings.setUseAmoledDark(enabled)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themeSettings.setUseDynamicColor(enabled)
        }
    }
}
