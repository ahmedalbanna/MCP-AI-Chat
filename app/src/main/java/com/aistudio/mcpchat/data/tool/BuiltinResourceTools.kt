package com.aistudio.mcpchat.data.tool

import com.aistudio.mcpchat.data.model.McpResourceContent
import com.aistudio.mcpchat.data.model.McpTool
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.data.repository.ResourceSummary
import org.json.JSONArray
import org.json.JSONObject

/**
 * Locally-executed tools that surface MCP resources in chat: the AI can
 * enumerate a server's resources and read their contents like any other
 * tool result, so file contents / database rows appear in the conversation.
 */
object BuiltinResourceTools {
    const val SERVER_ID = "builtin"
    const val SERVER_NAME = "Builtin"

    const val LIST_TOOL_NAME = "list_mcp_resources"
    const val READ_TOOL_NAME = "read_mcp_resource"

    private val LIST_SCHEMA = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject())
        .put("required", JSONArray())
        .toString()

    private val READ_SCHEMA = JSONObject()
        .put("type", "object")
        .put(
            "properties",
            JSONObject()
                .put(
                    "server",
                    JSONObject().put("type", "string").put("description", "Name of the MCP server owning the resource, if known"),
                )
                .put(
                    "uri",
                    JSONObject().put("type", "string").put("description", "Resource uri as returned by list_mcp_resources"),
                ),
        )
        .put("required", JSONArray().put("uri"))
        .toString()

    fun definitions(): List<McpTool> = listOf(
        McpTool(
            id = "builtin_list_mcp_resources",
            serverId = SERVER_ID,
            serverName = SERVER_NAME,
            name = LIST_TOOL_NAME,
            description = "List all resources (files, data records, documents) exposed by the connected MCP " +
                "servers, with their owning server, uri, mime type and description.",
            inputSchemaJson = LIST_SCHEMA,
            isEnabled = true,
        ),
        McpTool(
            id = "builtin_read_mcp_resource",
            serverId = SERVER_ID,
            serverName = SERVER_NAME,
            name = READ_TOOL_NAME,
            description = "Read the current content of one MCP resource by uri (from list_mcp_resources). " +
                "Pass the owning server name when known. Live data stays fresh via subscriptions.",
            inputSchemaJson = READ_SCHEMA,
            isEnabled = true,
        ),
    )

    /**
     * Executes a builtin tool. Returns JSON: a `resources` array for the
     * list tool, a `content` object for reads, or an `error` message —
     * callers derive success from the absence of `error`.
     */
    suspend fun execute(toolName: String, args: Map<String, Any?>, mcpRepository: McpRepository): String {
        return when (toolName) {
            LIST_TOOL_NAME -> listResources(mcpRepository)
            READ_TOOL_NAME -> readResource(args, mcpRepository)
            else -> JSONObject().put("error", "Unknown builtin tool '$toolName'").toString()
        }
    }

    private suspend fun listResources(mcpRepository: McpRepository): String {
        val summaries: List<ResourceSummary> = try {
            mcpRepository.listAllResourceSummaries()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return JSONObject().put("error", "Failed to list resources: ${e.message}").toString()
        }
        if (summaries.isEmpty()) {
            return JSONObject().put("resources", JSONArray()).put("message", "No resources available").toString()
        }
        val array = JSONArray()
        summaries.forEach { summary ->
            array.put(
                JSONObject()
                    .put("server", summary.serverName)
                    .put("uri", summary.resource.uri)
                    .put("name", summary.resource.name)
                    .put("mimeType", summary.resource.mimeType ?: JSONObject.NULL)
                    .put("description", summary.resource.description ?: JSONObject.NULL),
            )
        }
        return JSONObject().put("resources", array).toString()
    }

    private suspend fun readResource(args: Map<String, Any?>, mcpRepository: McpRepository): String {
        val uri = args["uri"]?.toString()?.trim()
        if (uri.isNullOrBlank()) {
            return JSONObject().put("error", "Missing required parameter 'uri'").toString()
        }
        val serverName = args["server"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val content: McpResourceContent? = try {
            mcpRepository.readResourceForChat(serverName, uri)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return JSONObject().put("error", "Failed to read resource: ${e.message}").toString()
        }
        return if (content != null) {
            JSONObject()
                .put("uri", content.uri)
                .put("mimeType", content.mimeType ?: JSONObject.NULL)
                .put("content", content.text)
                .toString()
        } else {
            JSONObject()
                .put("error", "Resource '$uri' not found${serverName?.let { " on server '$it'" } ?: ""}")
                .toString()
        }
    }
}
