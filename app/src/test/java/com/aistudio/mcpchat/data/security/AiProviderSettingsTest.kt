package com.aistudio.mcpchat.data.security

import android.content.SharedPreferences
import com.aistudio.mcpchat.BuildConfig
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderIds
import com.aistudio.mcpchat.data.model.AiProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Uses an in-memory prefs factory: EncryptedSharedPreferences itself requires
 * AndroidKeyStore, which Robolectric does not provide. Encryption is a
 * production-only wrapper; this test covers store semantics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiProviderSettingsTest {

    private lateinit var settings: AiProviderSettings

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        settings = AiProviderSettings(context) { _ -> InMemoryPrefs() }
    }

    @Test
    fun `first access seeds provider presets with google active`() {
        val all = settings.getAll()
        assertEquals(4, all.size)
        assertTrue(all.any { it.type == AiProviderType.GOOGLE })
        assertTrue(all.any { it.type == AiProviderType.ANTHROPIC })
        assertTrue(all.any { it.type == AiProviderType.OPENAI_COMPATIBLE })
        assertEquals(AiProviderIds.ID_GOOGLE, settings.getActiveId())
    }

    @Test
    fun `google preset seeds api key from BuildConfig env`() {
        val google = settings.getAll().first { it.id == AiProviderIds.ID_GOOGLE }
        // Whatever the build injected (real .env key or the template value),
        // the placeholder must never surface as a usable key.
        val expected = BuildConfig.GEMINI_API_KEY.trim()
            .takeUnless { it.isEmpty() || it == "MY_GEMINI_API_KEY" }
            ?: ""
        assertEquals(expected, google.apiKey)
    }

    @Test
    fun `upsert adds custom and setActive selects it`() {
        val custom = AiProviderConfig(
            name = "My vLLM",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "http://10.0.0.5:8000/v1",
            apiKey = "local",
            model = "qwen2.5-72b",
        )
        settings.upsert(custom)
        settings.setActive(custom.id)

        assertEquals(custom.id, settings.getActiveId())
        assertEquals("My vLLM", settings.getActive().name)
        assertEquals("qwen2.5-72b", settings.getActive().model)
    }

    @Test
    fun `delete removes provider and falls back when active deleted`() {
        val custom = AiProviderConfig(name = "Temp", type = AiProviderType.GOOGLE, baseUrl = "https://x", model = "m")
        settings.upsert(custom)
        settings.setActive(custom.id)

        settings.delete(custom.id)

        assertTrue(settings.getAll().none { it.id == custom.id })
        // Falls back to google preset.
        assertEquals(AiProviderIds.ID_GOOGLE, settings.getActiveId())
    }

    @Test
    fun `round trips every field including api key`() {
        val config = AiProviderConfig(
            name = "Groq",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.groq.com/openai/v1",
            apiKey = "gsk_secret",
            model = "llama-3.3-70b",
        )
        settings.upsert(config)
        val loaded = settings.getAll().first { it.id == config.id }
        assertEquals(config, loaded)
    }

    @Test
    fun `default prefs factory tolerates missing keystore in test runtimes`() {
        val context = RuntimeEnvironment.getApplication()
        // Robolectric fingerprint ("robolectric") → fallback allowed, prefs usable.
        val prefs = AiProviderSettings.createEncryptedPrefs(context)
        prefs.edit().putString("probe", "ok").apply()
        assertEquals("ok", prefs.getString("probe", null))
    }

    @Test
    fun `encrypted prefs failure refuses plaintext fallback in production`() {
        val context = RuntimeEnvironment.getApplication()
        // No AndroidKeyStore under Robolectric, and the policy forbids plaintext.
        assertThrows(IllegalStateException::class.java) {
            AiProviderSettings.createEncryptedPrefs(context) { false }
        }
    }

    @Test
    fun `encrypted prefs failure may fall back when policy allows`() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = AiProviderSettings.createEncryptedPrefs(context) { true }
        prefs.edit().putString("probe", "ok").apply()
        assertEquals("ok", prefs.getString("probe", null))
    }
}

private class InMemoryPrefs : SharedPreferences {
    private val map = mutableMapOf<String, String>()

    private val editor = object : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            map[key ?: return this] = value ?: ""
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            map.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            map.clear()
            return this
        }
        override fun commit() = true

        @Suppress("EmptyFunctionBlock")
        override fun apply() {}
    }

    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = map[key] ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key]?.toInt() ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key]?.toLong() ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key]?.toFloat() ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key]?.toBoolean() ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = editor
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
}
