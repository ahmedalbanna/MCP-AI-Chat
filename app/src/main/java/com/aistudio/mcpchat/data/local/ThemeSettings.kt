package com.aistudio.mcpchat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useAmoledDark: Boolean = false,
    val useDynamicColor: Boolean = false,
)

@Singleton
class ThemeSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>? = null,
) {
    private val store: DataStore<Preferences>
        get() = dataStore ?: context.themeDataStore

    val preferencesFlow: Flow<ThemePreferences> = store.data.map { prefs ->
        ThemePreferences(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[KEY_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            useAmoledDark = prefs[KEY_AMOLED] ?: false,
            useDynamicColor = prefs[KEY_DYNAMIC] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setUseAmoledDark(enabled: Boolean) {
        store.edit { it[KEY_AMOLED] = enabled }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC] = enabled }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("theme_mode")
        val KEY_AMOLED = booleanPreferencesKey("use_amoled_dark")
        val KEY_DYNAMIC = booleanPreferencesKey("use_dynamic_color")
    }
}
