package com.aistudio.mcpchat.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderIds
import com.aistudio.mcpchat.data.model.AiProviderType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the user-configured AI provider endpoints (including API keys) in
 * hardware-backed encrypted preferences. API keys therefore never live in
 * plaintext on disk or in BuildConfig.
 *
 * The prefs implementation is injectable so JVM tests can supply an
 * in-memory map (EncryptedSharedPreferences itself needs AndroidKeyStore).
 */
class AiProviderSettings(
    context: Context,
    prefsFactory: (Context) -> SharedPreferences = ::createEncryptedPrefs,
) : com.aistudio.mcpchat.data.network.ActiveAiProviderSource {

    private val prefs: SharedPreferences = prefsFactory(context)

    // -- Reads ------------------------------------------------------------

    @Synchronized
    fun getAll(): List<AiProviderConfig> = decode(prefs.getString(KEY_PROVIDERS, null))

    @Synchronized
    fun getActiveId(): String = prefs.getString(KEY_ACTIVE_ID, null)
        ?: AiProviderIds.ID_GOOGLE

    @Synchronized
    override fun getActive(): AiProviderConfig {
        val all = getAll()
        return all.find { it.id == activeIdSafe() }
            ?: all.find { it.id == AiProviderIds.ID_GOOGLE }
            ?: all.firstOrNull()
            ?: AiProviderConfig.google()
    }

    // -- Writes -----------------------------------------------------------

    @Synchronized
    fun upsert(config: AiProviderConfig) {
        val others = getAll().filterNot { it.id == config.id }
        val updated = others + config
        prefs.edit()
            .putString(KEY_PROVIDERS, encode(updated))
            .apply()
    }

    @Synchronized
    fun setActive(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    @Synchronized
    fun delete(id: String) {
        val remaining = getAll().filterNot { it.id == id }
        prefs.edit().putString(KEY_PROVIDERS, encode(remaining)).apply()
        if (getActiveId() == id) {
            prefs.edit()
                .putString(KEY_ACTIVE_ID, remaining.firstOrNull()?.id ?: AiProviderIds.ID_GOOGLE)
                .apply()
        }
    }

    // -- Internals --------------------------------------------------------

    private fun activeIdSafe(): String =
        prefs.getString(KEY_ACTIVE_ID, null) ?: AiProviderIds.ID_GOOGLE

    private fun encode(configs: List<AiProviderConfig>): String {
        val array = JSONArray()
        configs.forEach { config ->
            array.put(
                JSONObject().apply {
                    put("id", config.id)
                    put("name", config.name)
                    put("type", config.type.name)
                    put("baseUrl", config.baseUrl)
                    put("apiKey", config.apiKey)
                    put("model", config.model)
                },
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<AiProviderConfig> {
        if (raw.isNullOrBlank()) return defaultPresets()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                AiProviderConfig(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    type = runCatching {
                        AiProviderType.valueOf(obj.optString("type"))
                    }.getOrDefault(AiProviderType.OPENAI_COMPATIBLE),
                    baseUrl = obj.optString("baseUrl"),
                    apiKey = obj.optString("apiKey"),
                    model = obj.optString("model"),
                )
            }
        } catch (_: Exception) {
            defaultPresets()
        }
    }

    /** First-run seed so chat works out of the box with Google + placeholders. */
    private fun defaultPresets(): List<AiProviderConfig> {
        val presets = listOf(
            AiProviderConfig.google(),
            AiProviderConfig.anthropic(),
            AiProviderConfig.openAi(),
            AiProviderConfig.openCode(),
        )
        if (prefs.getString(KEY_PROVIDERS, null) == null) {
            prefs.edit()
                .putString(KEY_PROVIDERS, encode(presets))
                .putString(KEY_ACTIVE_ID, AiProviderIds.ID_GOOGLE)
                .apply()
        }
        return presets
    }

    private companion object {
        const val KEY_PROVIDERS = "ai_providers"
        const val KEY_ACTIVE_ID = "ai_provider_active"

        fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                "ai_provider_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
