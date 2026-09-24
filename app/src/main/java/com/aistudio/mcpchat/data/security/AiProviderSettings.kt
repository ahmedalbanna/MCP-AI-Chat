package com.aistudio.mcpchat.data.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aistudio.mcpchat.BuildConfig
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderIds
import com.aistudio.mcpchat.data.model.AiProviderType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the user-configured AI provider endpoints (including API keys) in
 * hardware-backed encrypted preferences. A debug-only build-time key from
 * `.env` (`BuildConfig.GEMINI_API_KEY`) only seeds the Google preset on first run;
 * it is written straight into the encrypted store and everything the user
 * edits afterwards lives in encrypted prefs only.
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

    private fun activeIdSafe(): String = prefs.getString(KEY_ACTIVE_ID, null) ?: AiProviderIds.ID_GOOGLE

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
                    put("allowInsecureHttp", config.allowInsecureHttp)
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
                    allowInsecureHttp = obj.optBoolean("allowInsecureHttp", false),
                )
            }
        } catch (_: Exception) {
            defaultPresets()
        }
    }

    /** First-run seed so chat works out of the box with Google + placeholders. */
    private fun defaultPresets(): List<AiProviderConfig> {
        val presets = listOf(
            AiProviderConfig.google(apiKey = buildTimeGeminiKey()),
            AiProviderConfig.anthropic(),
            AiProviderConfig.openAi(),
            AiProviderConfig.openCode(),
            AiProviderConfig.groq(),
            AiProviderConfig.openRouter(),
            AiProviderConfig.ollama(),
        )
        if (prefs.getString(KEY_PROVIDERS, null) == null) {
            prefs.edit()
                .putString(KEY_PROVIDERS, encode(presets))
                .putString(KEY_ACTIVE_ID, AiProviderIds.ID_GOOGLE)
                .apply()
        }
        return presets
    }

    /**
     * The debug-only `.env` `GEMINI_API_KEY` seeds the Google preset on first run
     * so a development build works
     * without manual key entry. The key is immediately persisted into the
     * encrypted prefs; later runtime edits happen in-app only. The
     * placeholder template value is treated as "not configured".
     */
    private fun buildTimeGeminiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        return if (key.isEmpty() || key == MY_GEMINI_API_KEY_PLACEHOLDER) "" else key
    }

    companion object {
        private const val KEY_PROVIDERS = "ai_providers"
        private const val KEY_ACTIVE_ID = "ai_provider_active"
        private const val MY_GEMINI_API_KEY_PLACEHOLDER = "MY_GEMINI_API_KEY"

        fun createEncryptedPrefs(context: Context): SharedPreferences =
            createEncryptedPrefs(context, allowPlaintextFallback = ::isRuntimeWithoutKeystore)

        /**
         * Opens the encrypted prefs, deciding via [allowPlaintextFallback]
         * whether a failure may degrade to plaintext storage. Production
         * must never silently store API keys unencrypted, so the default
         * ([isRuntimeWithoutKeystore]) only permits the fallback under
         * Robolectric / stubbed android.jar where no AndroidKeyStore exists.
         */
        internal fun createEncryptedPrefs(context: Context, allowPlaintextFallback: () -> Boolean): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    "ai_provider_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                if (!allowPlaintextFallback()) {
                    throw IllegalStateException(
                        "EncryptedSharedPreferences is unavailable on this device; " +
                            "refusing to store provider API keys in plaintext",
                        e,
                    )
                }
                context.getSharedPreferences("ai_provider_prefs", Context.MODE_PRIVATE)
            }
        }

        /**
         * Robolectric reports a "robolectric" fingerprint; the mockable
         * android.jar used by plain JVM tests leaves statics null. A real
         * device always has a genuine non-empty fingerprint.
         */
        private fun isRuntimeWithoutKeystore(): Boolean {
            val fingerprint = Build.FINGERPRINT
            return fingerprint.isNullOrEmpty() || fingerprint.startsWith("robolectric")
        }
    }
}
