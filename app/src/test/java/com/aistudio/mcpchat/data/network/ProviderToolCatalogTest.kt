package com.aistudio.mcpchat.data.network

import com.aistudio.mcpchat.data.model.McpTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderToolCatalogTest {

    @Test
    fun `duplicate tool names receive distinct wire names and resolve to stable ids`() {
        val first = tool("server_a", "search")
        val second = tool("server_b", "search")
        val catalog = ProviderToolCatalog.create(
            tools = listOf(first, second),
            policy = ProviderToolNamePolicy.GEMINI,
        )

        val firstWireName = catalog.wireName(first.id, first.name)
        val secondWireName = catalog.wireName(second.id, second.name)

        assertNotEquals(firstWireName, secondWireName)
        assertEquals(first.id, catalog.resolve(firstWireName)?.id)
        assertEquals(second.id, catalog.resolve(secondWireName)?.id)
    }

    @Test
    fun `normalization and truncation collisions remain distinct`() {
        val punctuation = tool("punctuation", "foo-bar.baz")
        val normalized = tool("normalized", "foo_bar_baz")
        val longA = tool("long_a", "a".repeat(80))
        val longB = tool("long_b", "a".repeat(79) + "b")
        val catalog = ProviderToolCatalog.create(
            tools = listOf(punctuation, normalized, longA, longB),
            policy = ProviderToolNamePolicy.GEMINI,
        )
        val wireNames = listOf(punctuation, normalized, longA, longB).map {
            catalog.wireName(it.id, it.name)
        }

        assertEquals(wireNames.size, wireNames.distinct().size)
        wireNames.forEachIndexed { index, wireName ->
            assertEquals(listOf(punctuation, normalized, longA, longB)[index].id, catalog.resolve(wireName)?.id)
        }
    }

    @Test
    fun `provider policies own their wire character rules`() {
        val tool = tool("tool", "reports-export")

        val gemini = ProviderToolCatalog.create(listOf(tool), ProviderToolNamePolicy.GEMINI)
        val openAi = ProviderToolCatalog.create(listOf(tool), ProviderToolNamePolicy.OPENAI_COMPATIBLE)
        val anthropic = ProviderToolCatalog.create(listOf(tool), ProviderToolNamePolicy.ANTHROPIC)

        assertEquals("reports_export", gemini.wireName(tool.id, tool.name))
        assertEquals("reports-export", openAi.wireName(tool.id, tool.name))
        assertEquals("reports-export", anthropic.wireName(tool.id, tool.name))
    }

    @Test
    fun `gemini names cannot start with a digit`() {
        val tool = tool("numeric", "123-search")

        val gemini = ProviderToolCatalog.create(listOf(tool), ProviderToolNamePolicy.GEMINI)
        val openAi = ProviderToolCatalog.create(listOf(tool), ProviderToolNamePolicy.OPENAI_COMPATIBLE)

        assertEquals("_123_search", gemini.wireName(tool.id, tool.name))
        assertEquals("123-search", openAi.wireName(tool.id, tool.name))
    }

    @Test
    fun `wire names are independent of tool discovery order`() {
        val first = tool("server_a", "search")
        val second = tool("server_b", "search")
        val forward = ProviderToolCatalog.create(listOf(first, second), ProviderToolNamePolicy.GEMINI)
        val reversed = ProviderToolCatalog.create(listOf(second, first), ProviderToolNamePolicy.GEMINI)

        listOf(first, second).forEach { tool ->
            assertEquals(forward.wireName(tool.id, tool.name), reversed.wireName(tool.id, tool.name))
        }
    }

    @Test
    fun `missing persisted id cannot borrow an active tool wire name`() {
        val active = tool("active", "search")
        val catalog = ProviderToolCatalog.create(listOf(active), ProviderToolNamePolicy.GEMINI)
        val activeWireName = catalog.wireName(active.id, active.name)
        val missingWireName = catalog.wireName("removed-tool", active.name)

        assertNotEquals(activeWireName, missingWireName)
        assertNull(catalog.resolve(missingWireName))
    }

    @Test
    fun `missing persisted ids stay distinct when no tools are active`() {
        val catalog = ProviderToolCatalog.create(emptyList(), ProviderToolNamePolicy.GEMINI)

        val first = catalog.wireName("removed-a", "search")
        val second = catalog.wireName("removed-b", "search")

        assertNotEquals(first, second)
        assertNull(catalog.resolve(first))
        assertNull(catalog.resolve(second))
    }

    @Test
    fun `wire function calls normalize back to original names and tool ids`() {
        val tool = tool("weather", "get-weather")
        val catalog = ProviderToolCatalog.create(listOf(tool), ProviderToolNamePolicy.GEMINI)
        val wireName = catalog.wireName(tool.id, tool.name)

        val resolved = catalog.functionCall(wireName, mapOf("city" to "Tokyo"), "signature")
        val unknown = catalog.functionCall("hallucinated_tool", emptyMap())

        assertEquals("get-weather", resolved.name)
        assertEquals(tool.id, resolved.toolId)
        assertEquals("signature", resolved.thoughtSignature)
        assertEquals("hallucinated_tool", unknown.name)
        assertNull(unknown.toolId)
    }

    @Test
    fun `duplicate stable tool ids fail closed`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ProviderToolCatalog.create(
                tools = listOf(tool("duplicate", "one"), tool("duplicate", "two")),
                policy = ProviderToolNamePolicy.GEMINI,
            )
        }

        assertTrue(error.message!!.contains("unique"))
    }

    private fun tool(id: String, name: String) = McpTool(
        id = id,
        serverId = "server",
        serverName = "Server",
        name = name,
        description = name,
    )
}
