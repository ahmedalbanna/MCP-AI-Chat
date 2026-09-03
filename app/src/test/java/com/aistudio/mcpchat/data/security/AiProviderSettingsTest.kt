package com.aistudio.mcpchat.data.security

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.mcpchat.BuildConfig
import com.aistudio.mcpchat.data.model.AiProviderConfig
import com.aistudio.mcpchat.data.model.AiProviderIds
import com.aistudio.mcpchat.data.model.AiProviderType
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiProviderSettingsTest {

    private lateinit var context: Context
    private lateinit var prefs: InMemoryPrefs
    private lateinit var settings: AiProviderSettings

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = InMemoryPrefs()
        settings = AiProviderSettings(context) { prefs }
    }

    @Test
    fun `first access seeds provider presets with google active`() {
        val all = settings.getAll()
        assertEquals(7, all.size)
        assertTrue(all.any { it.type == AiProviderType.GOOGLE })
        assertTrue(all.any { it.type == AiProviderType.ANTHROPIC })
        assertTrue(all.any { it.type == AiProviderType.OPENAI_COMPATIBLE })
        assertEquals(AiProviderIds.ID_GOOGLE, settings.getActiveId())
    }

    @Test
    fun `google preset seeds api key from BuildConfig env`() {
        val google = settings.getAll().first { it.id == AiProviderIds.ID_GOOGLE }
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
    fun `stored JSON contains the raw API key untransformed`() {
        val config = AiProviderConfig(
            id = "key_check",
            name = "Key Provider",
            type = AiProviderType.GOOGLE,
            baseUrl = "https://google.com",
            apiKey = "AKIAIOSFODNN7EXAMPLE",
            model = "gemini-3.5-flash",
        )

        settings.upsert(config)

        val raw = prefs.raw(KEY_PROVIDERS)
        assertNotNull(raw)
        val array = JSONArray(raw!!)
        var found = false
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString("id") == "key_check") {
                assertEquals("AKIAIOSFODNN7EXAMPLE", obj.getString("apiKey"))
                found = true
            }
        }
        assertTrue("key_check should be present in stored JSON", found)
    }

    @Test
    fun `upsert overwrites existing config by id`() {
        val original = AiProviderConfig(
            id = "dup",
            name = "Original",
            type = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://orig.com",
            apiKey = "orig-key",
            model = "model-a",
        )
        settings.upsert(original)

        val updated = original.copy(name = "Updated", apiKey = "updated-key", model = "model-b")
        settings.upsert(updated)

        val all = settings.getAll()
        assertEquals(1, all.count { it.id == "dup" })
        val stored = all.find { it.id == "dup" }!!
        assertEquals("Updated", stored.name)
        assertEquals("updated-key", stored.apiKey)
        assertEquals("model-b", stored.model)
    }

    @Test
    fun `getActive falls back to google when active id is stale`() {
        settings.setActive("nonexistent_id")
        val active = settings.getActive()
        assertEquals(AiProviderIds.ID_GOOGLE, active.id)
    }

    @Test
    fun `default prefs factory tolerates missing keystore in test runtimes`() {
        val prefs = AiProviderSettings.createEncryptedPrefs(context)
        prefs.edit().putString("probe", "ok").apply()
        assertEquals("ok", prefs.getString("probe", null))
    }

    @Test
    fun `encrypted prefs failure refuses plaintext fallback in production`() {
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            AiProviderSettings.createEncryptedPrefs(context) { false }
        }
    }

    @Test
    fun `encrypted prefs failure may fall back when policy allows`() {
        val prefs = AiProviderSettings.createEncryptedPrefs(context) { true }
        prefs.edit().putString("probe", "ok").apply()
        assertEquals("ok", prefs.getString("probe", null))
    }

    companion object {
        private const val KEY_PROVIDERS = "ai_providers"
    }

    private class InMemoryPrefs : SharedPreferences {
        private val map = mutableMapOf<String, String>()

        /** Returns the raw stored value for inspection in tests. */
        fun raw(key: String): String? = map[key]

        private val editor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                map[key ?: return this] = value ?: ""
                return this
            }

            override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
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
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = map[key]?.toInt() ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key]?.toLong() ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key]?.toFloat() ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key]?.toBoolean() ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = editor
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }
}
