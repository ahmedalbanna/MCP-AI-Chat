# Plan: Per-server MCP session TTL to fix stale-session timeouts

## Root cause

`McpHttpClient.sessions` is an in-memory `ConcurrentHashMap<String, String>` that caches session IDs forever. `ensureSession` (line 180) returns `true` whenever a cached entry exists, so after a server-side session expiry (typical after 5–30 min of inactivity), the client sends requests with a dead `Mcp-Session-Id` and waits for the 30s read timeout. A process restart or new chat clears the map, which is why fresh sessions work.

## Design

Add `sessionTtlMs: Long = 0` to `McpServer`. The `McpHttpClient` tracks a creation timestamp alongside each session ID and invalidates entries whose age exceeds the server's TTL (or a 5-minute default when the field is 0).

### Key decisions

- **TTL=0 means use default** (5 min). No "never expire" sentinel; users who want that set a very large value.
- **SessionEntry replaces String** in the `sessions` map: `data class SessionEntry(val sessionId: String, val createdAt: Long = System.currentTimeMillis())`.
- **Expiration check is cheap**: single timestamp comparison in `ensureSession` before the cache hit short-circuit.
- **Room migration** bumps version 2 → 3 with `ALTER TABLE … ADD COLUMN sessionTtlMs INTEGER NOT NULL DEFAULT 0`. Preset servers get 0 (use default). Custom servers created before the update also get 0.
- **UI exposure**: optional follow-up. The model change is the minimum viable fix; the server-edit screen can expose the spinner in a separate task.

## Implementation steps

1. **`McpModels.kt`** — add `sessionTtlMs: Long = 0` to `McpServer` data class.
2. **`Entities.kt`** — add `sessionTtlMs: Long = 0` to `McpServerEntity`.
3. **`AppDatabase.kt`** — bump `version` to 3; add `migration2To3` that runs `ALTER TABLE mcp_servers ADD COLUMN sessionTtlMs INTEGER NOT NULL DEFAULT 0`; register it in `getDatabase`.
4. **`McpHttpClient.kt`**:
   - Add `private data class SessionEntry(val sessionId: String, val createdAt: Long = System.currentTimeMillis())`
   - Change `sessions: ConcurrentHashMap<String, String>` → `ConcurrentHashMap<String, SessionEntry>`
   - Add `companion object { private const val DEFAULT_SESSION_TTL_MS = 5 * 60_000L }`
   - Update `applySessionHeader` to read `sessions[key]?.sessionId`
   - Update `performHandshake` to store `SessionEntry(sessionIdHeader)`
   - Update `ensureSession`:
     ```kotlin
     private suspend fun ensureSession(server: McpServer): Boolean {
         val entry = sessions[sessionKey(server)]
         if (entry != null && !entry.isExpired(server.sessionTtlMs)) return true
         if (entry != null) sessions.remove(sessionKey(server))
         return performHandshake(server)
     }
     ```
   - Add `SessionEntry.isExpired(ttlMs: Long): Boolean` helper.
   - Update `callWithSessionRetry` session-error path and `testConnection` to work with `SessionEntry` (remove call stays the same).
5. **`McpRepository.kt`** — no changes needed; `McpHttpClient` reads TTL from the `McpServer` parameter it already receives.
6. **`McpRepositoryTest.kt`** — update any `McpServer` constructor calls to pass the new field if needed (default value makes this mostly transparent).

## Validation

- Run `./gradlew detekt spotlessCheck testDebugUnitTest` — must be green.
- Add unit test in `McpHttpClientTest`: create server with `sessionTtlMs = 1_000`, cache a session, advance time past TTL, verify `ensureSession` performs a fresh handshake (MockWebServer receives a second `initialize`).
- Add unit test: server with `sessionTtlMs = 0` uses the 5-minute default (verify via reflection or time advancement).
- Manual smoke test: connect to MCP server, wait past TTL, send message → should handshake fresh instead of timing out.

## Out of scope (deferred)

- UI for per-server TTL configuration in `ServersScreen` / server edit.
- Making TTL user-configurable in milliseconds vs. seconds/minutes picker.
- Telemetry/logging for session evictions.
