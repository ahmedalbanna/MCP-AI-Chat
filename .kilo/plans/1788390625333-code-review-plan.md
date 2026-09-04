# MCP AI Chat — Code Review Plan

## Executive Summary

The codebase is a mature, well-structured Android application with solid architecture (MVVM, Hilt DI, Room, Jetpack Compose) and comprehensive Phase 1–13 remediation already completed. This review identifies **2 P0 findings**, **4 P1 follow-ups**, and **4 P2/P3 improvements** that remain open or were deferred.

---

## P0 — Critical (fix before next release)

### 1. McpHttpClient default timeout mismatch with README

**File:** `data/network/McpHttpClient.kt:24-28`

The no-arg constructor builds an `OkHttpClient` with `readTimeout(12s)` / `writeTimeout(12s)`, but `README.md:82` and `di/AppModule.kt:51-53` document MCP timeouts as **15s connect / 30s read/write**. Production uses the DI-provided `@Named("mcp")` client (correct values), so this is a latent bug in tests and any direct instantiation.

**Action:**
- Align the default builder in `McpHttpClient` to `readTimeout(30s)` / `writeTimeout(30s)` to match the documented contract.
- Add a unit test asserting the default client's timeout values.
- Audit all direct `McpHttpClient()` instantiations in tests to ensure they either rely on the corrected default or inject an explicit client.

### 2. Chat typing indicator may show on non-user last message

**File:** `ui/screens/ChatScreen.kt:372`

The condition:
```kotlin
if (isGenerating && inProgressToolCalls.isEmpty() && messages.lastOrNull()?.role == "user") {
```
shows a typing indicator only when the last persisted message is from the user. This is fragile because:
- If a `system` message is injected between user and assistant turns, the indicator vanishes.
- If messages include interim `tool`-role results (not typical today but possible), the indicator may not appear.

**Action:**
- Replace the role check with a stable flag: track whether a user message was the *most recent input* using a separate `MutableStateFlow<Boolean>` in `ChatViewModel` (e.g., `hasPendingUserTurn`), set to `true` in `sendMessage` and cleared when the assistant turn is persisted.
- Update `ChatScreen` to observe this flag instead of inspecting `messages.lastOrNull()?.role`.

---

## P1 — High (next sprint)

### 3. Missing unit tests for AnthropicClient and OpenAiCompatibleClient

**Files:** `data/network/AnthropicClient.kt`, `data/network/OpenAiCompatibleClient.kt`

These clients contain non-trivial request building and response parsing:
- Anthropic: alternating user/assistant turns, `tool_use`/`tool_result` block pairing, `syntheticToolUseId` turn-scoping.
- OpenAI-compatible: `tool_call` id generation, turn-indexed `role:"tool"` messages, thought-signature pass-through.

**Current state:** Zero direct tests. `ProviderChatRouterTest` only covers `resolveModel`.

**Action:**
- Add `AnthropicClientTest` and `OpenAiCompatibleClientTest` using `MockWebServer`.
- Cover: successful text response, tool-call round-trip with `tool_result` pairing, rate-limit (429) error mapping, malformed JSON handling, and the `syntheticToolUseId`/`toolCallIdFor` uniqueness contract.

### 4. AiProviderSettings encryption round-trip untested in JVM

**File:** `data/security/AiProviderSettings.kt`

`CryptoManagerTest` verifies AES/GCM encrypt/decrypt in isolation, but the full path of writing API keys to `EncryptedSharedPreferences` (or its Robolectric fallback) and reading them back via `AiProviderSettings.getAll()` is only implicitly covered by Android tests.

**Action:**
- Add a JVM test that:
  1. Constructs `AiProviderSettings` with an in-memory `SharedPreferences` factory.
  2. Calls `upsert(AiProviderConfig(...))` with a non-empty `apiKey`.
  3. Reads back via `getAll()` and asserts the key survives serialization.
  4. Verifies the stored JSON contains the raw key (prefs encryption handles it) and that `getActive()` resolves correctly.

### 5. ChatViewModel session-switch race during generation

**File:** `ui/viewmodel/ChatViewModel.kt:249-254`

`followPersistedSession` unconditionally calls `updateCurrentSessionId(persistedSessionId)` if the repository returned a different ID. If the user navigated to another session while the request was in flight, the UI will snap back to the completed turn.

**Action:**
- Before switching, check whether the user is still viewing the originally requested session:
  ```kotlin
  private fun followPersistedSession(requestedSessionId: String, result: Result<ChatMessageEntity>) {
      val persistedSessionId = result.getOrNull()?.sessionId ?: return
      if (persistedSessionId != requestedSessionId && _currentSessionId.value == requestedSessionId) {
          updateCurrentSessionId(persistedSessionId)
      }
  }
  ```
- Add a ViewModel test simulating a stale-session fallback while the user has already selected a different session.

### 6. McpHttpClient.isCircuitOpen naming

**File:** `data/network/McphHttpClient.kt:228-233`

`isCircuitOpen` returns `true` when the circuit is open *and* the call should be skipped. The double-negative is confusing at call sites (`if (isCircuitOpen(server)) { return@withContext emptyList() }`).

**Action:**
- Rename to `shouldSkipDueToCircuit` (or `isSkippedByCircuit`) and update the two call sites in `discoverTools` and `executeToolCall`. Also update `CircuitBreaker.canExecute` callers for consistency.

---

## P2 — Medium (backlog)

### 7. Enforce Kover coverage gate

**File:** `app/build.gradle.kts:176`

`minBound(0)` is a placeholder. The nightly workflow even prints a message saying coverage is not enforced.

**Action:**
- Run `./gradlew koverReport` locally to establish a baseline.
- Set `minBound(40)` for the first gate; raise to 60 once baseline stabilizes.
- Update `nightly.yml` to run `koverVerify` instead of the informational echo.

### 8. Expand Roborazzi snapshot coverage

**Current state:** 10 goldens (`ServerStatusBadge` ×3, `ToolCallCard` ×3, `SettingsScreen` ×4). `ChatScreen`, `ServersScreen`, and `InspectorScreen` have no snapshot coverage.

**Action:**
- Add `SnapshotTests` for:
  - `ChatScreen` empty state, user bubble, assistant bubble with tool calls, error banner.
  - `ServersScreen` server card (connected, error, insecure).
  - `InspectorScreen` tool playground and audit log entry.
- Capture goldens with `ROBORAZZI_RECORD=1` and commit them.

### 9. Split oversized composables

**Files:** `ui/screens/ChatScreen.kt` (1299 lines), `ui/screens/InspectorScreen.kt` (859 lines), `ui/screens/ServersScreen.kt` (665 lines)

**Action:**
- Extract `ChatInputBar`, `ChatTopBar`, `ServerCardItem` (already extracted), `ToolPlaygroundTab`, `AuditLogsTab` into separate files or at least separate top-level composables with focused previews.
- This improves testability, preview rendering, and reviewability.

### 10. Add connectivity instrumentation test

**Current state:** Unit tests use `MockWebServer`. There is no instrumented test that verifies end-to-end MCP handshake → tool discovery → tool invocation on a real device/emulator.

**Action:**
- Add an `androidTest` that starts a local `MockWebServer` (or a small embedded MCP server), runs through `MainActivity`, and asserts the Inspector can invoke a tool and see the result.

---

## P3 — Low (future)

### 11. RetryManager lacks retriable/non-retriable classification

**File:** `data/network/Resilience.kt:18-30`

`executeWithRetry` blindly retries any `Exception`. Callers currently only wrap idempotent calls (`initialize`, `tools/list`), but a future contributor could accidentally wrap `tools/call`.

**Action:**
- Add an optional `isRetriable: (Exception) -> Boolean` predicate parameter; default to `{ true }` for backward compatibility. Update `McpHttpClient` to pass `{ it is IOException }` for non-idempotent calls if retry is ever needed there.

### 12. Provider preset gaps

**File:** `data/model/AiProviders.kt`

Presets cover Google, OpenAI, Anthropic, and OpenCode. Missing common local/cloud runtimes (Groq, OpenRouter, LM Studio, Ollama).

**Action:**
- Add `groq()`, `openRouter()`, `ollama()` presets to `AiProviderConfig`. Low effort, high UX value for local-first users.

---

## Validation Checklist

- [ ] `./gradlew detekt` — 0 issues
- [ ] `./gradlew spotlessCheck` — pass
- [ ] `./gradlew testDebugUnitTest -PscreenshotTests=true` — all pass
- [ ] `./gradlew lintDebug` — pass
- [ ] `./gradlew koverVerify` — pass at new threshold
- [ ] Manual smoke test: connect to a local MCP server, invoke a tool, switch providers, and verify session model compatibility

---

## Rollout Order

1. **Week 1:** P0-1 (timeout fix) + P0-2 (typing indicator flag) + P1-5 (session race guard).
2. **Week 2:** P1-3 (Anthropic/OpenAI client tests) + P1-4 (AiProviderSettings round-trip test).
3. **Week 3:** P1-6 (circuit-breaker naming) + P2-7 (Kover gate) + P2-8 (snapshot expansion).
4. **Week 4:** P2-9 (composable split) + P2-10 (instrumented test) + P3-12 (provider presets).
