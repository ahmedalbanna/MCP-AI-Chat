# MCP AI Chat - Clean Code Roadmap

A phased plan to transform the current prototype into a production-ready, maintainable Android application.

> **Status audit (Aug 2026 code review):** Several items previously marked `[x]`
> were not actually implemented or were only partially implemented. Statuses below
> have been corrected to reflect the actual state of the codebase. New
> **Phase 9** tracks remediation work identified by the review.

---

## Phase 1: UI/UX Polish & Consistency

### 1.1 Design System Foundation [x]
- Audit existing `Color.kt`, `Type.kt`, `Theme.kt` — define a formal color token system (primary, secondary, surface, error, outline, etc.)
- Extract repeated spacing/padding values into a `Spacing` object (`Spacing.xs = 4.dp`, `Spacing.sm = 8.dp`, etc.)
- Create a `Dimens` object for consistent corner radii, icon sizes, touch targets (min 48dp)
- Document the design system in a `DesignSystem.kt` preview file showing all tokens

### 1.2 Typography & Readability [x]
- Review `Type.kt` — ensure font scale hierarchy is clear (headline, title, body, label, caption)
- Test all screens at Android font scale 0.85 / 1.0 / 1.15 / 1.3 — fix overflow and truncation
- Add `textAppearance` composables for consistent text styling across screens
- Ensure code blocks in chat use a monospace font at a readable size (min 13sp)

### 1.3 Accessibility [x]
- Add `contentDescription` to all `Icon` composables (currently many are decorative-only without `Modifier.semantics`)
- Ensure all interactive elements have minimum 48dp touch target
- Test with TalkBack: verify logical focus traversal across chat input, messages, tool call cards, server list
- Ensure color contrast meets WCAG AA (4.5:1 for text, 3:1 for large text) — audit the dark theme slate/indigo palette
- Add `semantics { liveRegion = LiveRegionMode.Polite }` for new assistant messages so TalkBack announces them
- Ensure all buttons and toggles have accessible labels (not just icons)

### 1.4 Chat Screen UX [x]
- Add loading skeleton/shimmer placeholder while waiting for Gemini response (currently just a spinner or nothing)
- Show typing indicator animation during AI generation
- Auto-scroll to bottom on new message, but detect if user has scrolled up — pause auto-scroll, show "Jump to bottom" FAB
- Add swipe-to-reply or long-press-to-copy on message bubbles
- Show token/cost estimate or tool call count in message metadata
- Truncate very long assistant messages with "Show more" / "Show less" toggle
- Fix message input: dismiss keyboard on send, support multi-line with Shift+Enter, auto-resize input field

### 1.5 Tool Call UX [x]
- Show tool execution progress inline in chat (currently just a final result card)
- Add expand/collapse on tool call details (request params, response payload)
- Color-code tool status: pending (amber), success (green), error (red)
- Show tool execution duration next to tool name
- Add "Copy tool result" action on tool call cards

### 1.6 Server Management UX [x]
- Add pull-to-refresh on server list
- Show server health status with last-ping timestamp (not just a dot)
- Add confirmation dialog before deleting a server (currently deletes immediately)
- Show tool count badge on each server card
- Add search/filter for servers when list grows long
- Empty state illustration when no servers are configured

### 1.7 Inspector Screen UX [x]
- Add parameter input validation with inline error messages (currently accepts any input)
- Show execution history with timestamps in a scrollable list
- Add "Re-execute" button on previous tool calls to quickly re-run with same params
- Add JSON syntax highlighting in the raw request/response viewer
- Show side-by-side request/response view on wider screens (tablets)

### 1.8 Navigation & Transitions [x]
- Add bottom navigation badge on Chat tab when there are unread messages or active tool calls
- Smooth animated transitions between tabs (shared element or crossfade)
- Preserve scroll position when switching between tabs
- Add back navigation support from deep links or notifications

### 1.9 Error & Empty States [x]
- Design consistent empty state illustrations/composables for: no messages, no servers, no tool results, no audit logs
- Add error state composables with retry action for: network failure, API key missing, server unreachable
- Add in-app banner/toast system for non-critical feedback (server synced, tool added, settings saved)
- Handle orientation changes gracefully — no data loss, preserve scroll position

### 1.10 Dark Mode & Theming [x]
- Ensure all composables respect `isSystemInDarkTheme()` — audit for hardcoded colors
- Add AMOLED dark mode option (pure black backgrounds for OLED screens)
- Test theme switching mid-session — no flicker, no state loss
- Ensure status bar and navigation bar colors adapt to theme (fix deprecated `window.statusBarColor`)

---

## Phase 2: Foundation & Security

### 2.1 Dependency Injection [x]
- Add Hilt for DI across the app
- Create a single shared `McpRepository` instance (currently 3 redundant instances across ViewModels)
- Inject `McpHttpClient`, `GeminiApiService`, DAOs, and repositories
- Eliminate manual `new` instantiation in ViewModels

### 2.2 Security Hardening [ ] — REOPENED Aug 2026
- ~~Remove hardcoded `"gateway-dev-token"`~~ — **regression:** a live bearer token + IP for an ERPNext gateway is hardcoded in `McpRepository.kt` presets; must be removed and rotated
- Add URL validation for user-provided MCP server endpoints (scheme, host, path checks) [x] — but must additionally require https except loopback hosts
- Store Gemini API key in `EncryptedSharedPreferences` instead of `BuildConfig` plain text [ ]
- Remove `fallbackToDestructiveMigration()` — still present in `AppDatabase.kt` despite real migrations existing [ ]
- Encrypt server auth headers stored in Room, or exclude database from cloud backups (`data_extraction_rules.xml` is template-only today) [ ]

### 2.3 Package & Identity Cleanup [x]
- Replace `com.example` package with a real namespace (e.g., `com.aistudio.mcpchat`)
- Set a meaningful `applicationId` in `build.gradle.kts`
- Remove placeholder AI Studio `metadata.json` and `.aistudio` assets

---

## Phase 3: Architecture & Code Splitting

### 3.1 McpHttpClient Refinement (~250 lines) [x]
- Extract `buildAuthHeaders(server)` helper to eliminate repeated auth header parsing (3 occurrences)
- Consider extracting JSON-RPC request building into a `JsonRpcRequest` data class
- Add response parsing into a dedicated `McpResponseParser` if more transport types are added

### 3.2 Repository Layer [x]
- Extract `ChatRepository` tool orchestration into a dedicated `ToolOrchestrator` class
- Move system prompt construction to a `SystemPromptBuilder`

### 3.3 Data Layer [x]
- Create domain model mappers (Entity ↔ Domain) instead of 1:1 mapping classes
- Add `NetworkResult<T>` sealed class for consistent error handling across repositories
- Replace raw `String` error messages with typed error states in ViewModels

### 3.4 ViewModel Layer [x]
- Extract UI state into dedicated `UiState` data classes (e.g., `ChatUiState`, `ServersUiState`)
- Add `SavedStateHandle` support where appropriate
- Standardize error handling — all ViewModels emit `UiState` with loading/error/content

---

## Phase 4: Testing

### 4.1 Unit Tests [x]
- `McpProtocolClient` — mock HTTP, test JSON-RPC request/response serialization
- `SystemPromptBuilder` — prompt construction with various tool sets
- `ToolOrchestrator` — multi-turn tool calling loop (mock Gemini + MCP)

### 4.2 Integration Tests [x]
- `McpRepository` — Room CRUD operations with in-memory database
- `ChatRepository` — end-to-end message flow with mocked network

### 4.3 UI Tests [x]
- Replace boilerplate `ExampleUnitTest` / `ExampleRobolectricTest` / `ExampleInstrumentedTest`
- Fix `GreetingScreenshotTest.kt` — reference correct composable or remove
- Add Compose UI tests for ChatScreen, ServersScreen, InspectorScreen
- Test navigation between tabs
- Test server enable/disable toggle, tool call card rendering

### 4.4 Snapshot Tests [ ]
- Use Roborazzi (already in deps) for screenshot regression tests — plugin configured, but no golden images recorded
- Capture key screens: chat with messages, server list, inspector with tool results

---

## Phase 5: Error Handling & Robustness

### 5.1 Network Error Handling [ ] — partially done
- Retry logic with exponential backoff: `RetryManager` exists but is **not wired** into any network call [ ]
- Circuit breaker pattern: `CircuitBreaker` exists but is **not consulted** anywhere [ ]
- Timeout configuration (connect, read, write) per server [x] — global defaults only
- Surface network errors to UI with actionable messages [~] — currently via fragile substring matching of error strings; needs typed exceptions

### 5.2 Database Migrations [ ] — partially done
- Export Room schema: `exportSchema = true` set, but **no `room.schemaLocation` KSP arg**, so schemas are never exported [ ]
- Write migration from version 1 → 2 [x]
- Add CI check that validates migrations against exported schemas [ ]

### 5.3 Graceful Degradation [~]
- When an MCP server is unreachable, its cached tools are **deleted permanently** on a single failed ping instead of being marked unavailable [ ]
- Handle Gemini API rate limits with user-facing message [x]
- Handle malformed JSON-RPC responses without crashing the chat loop [x]

---

## Phase 6: Code Quality & Consistency

### 6.1 Linting & Formatting [x]
- Add `ktlint` or `detekt` with shared config
- Add `.editorconfig` for consistent formatting
- Run lint as part of CI *(blocked on Phase 7)*

### 6.2 Remove Dead Code [ ] — REOPENED Aug 2026
- Unused dependencies remain in `libs.versions.toml`: Retrofit, Moshi (+converter/codegen), Coil, Firebase AI/App Check/BOM, logging-interceptor, mockk-android [ ]
- Dead classes never wired into production code: `RetryManager`, `CircuitBreaker` (`Resilience.kt`), `McpSseClient`, `MemoryTool` [ ]
- Clean up unused imports across all files [~]
- Remove `@Suppress("DEPRECATION")` by migrating to non-deprecated APIs [x]

### 6.3 Documentation [x]
- Write `README.md` with: project overview, setup instructions, architecture diagram, environment variables
- Add KDoc to public interfaces and key classes
- Document the MCP protocol flow in code comments where non-obvious

### 6.4 ProGuard / R8 [x]
- Enable minification for release builds (`isMinifyEnabled = true`)
- Add proper ProGuard rules for Moshi, OkHttp, Room, Retrofit
- Test release build to verify no reflection-based crashes

---

## Phase 7: CI/CD & Build Pipeline

### 7.1 GitHub Actions [ ] — `.github/workflows/` is empty; no workflows exist
- PR checks: build, lint, unit tests [ ]
- Nightly: full test suite + screenshot comparison [ ]
- Release: signed build + artifact upload [ ]

### 7.2 Quality Gates [ ]
- Minimum code coverage threshold (e.g., 60% for new code) [ ]
- Lint must pass with zero errors [ ]
- No hardcoded secrets (use `gitleaks` or similar) [ ] — a live credential shipped to git in Aug 2026; see 2.2

---

## Phase 8: Feature Improvements

### 8.1 Real MCP SSE Transport [ ] — partially done
- `McpSseClient` implemented but **not integrated**: no DI binding, no repository/screen uses it [ ]
- Auto-detect transport type from server response headers — HTTP client tolerates SSE-framed JSON-RPC responses, which covers streamable-http servers [x]
- Connection health monitoring for SSE connections [ ]

### 8.2 Memory Tool [ ] — dead code
- Room-backed storage implemented (`MemoryTool`) but never registered as a tool source; unreachable from chat [ ]
- Fix `listNotes`: collects an infinite Room Flow (`collect {}`) — hangs forever if ever called [ ]
- Add semantic search or simple keyword matching for `search_memory_notes` [x]

### 8.3 Fallback Mode Improvement [x]
- Replace simple keyword matching with a lightweight local LLM or better heuristic
- Add a configuration toggle for fallback mode behavior

---

## Phase 9: Review Remediation (Aug 2026)

Findings from the Aug 2026 code review, ordered by severity:

### 9.1 Security [ ]
- Remove hardcoded ERPNext gateway IP + bearer token from `McpRepository`; rotate the leaked credential [ ]
- Require https for user-added servers (loopback exception) [ ]
- Exclude database (auth headers, request/response logs, chat history) from cloud backup & device transfer [ ]

### 9.2 Correctness [ ]
- Persist function responses so multi-turn history satisfies Gemini's functionCall→functionResponse pairing [ ]
- Map sanitized Gemini function names back to original tool names before execution (tools with `-`/`.` in names silently fail today) [ ]
- Emit a visible message when the tool-call hop limit is reached instead of saving an empty reply [ ]
- Stop deleting cached tools on transient ping failures [ ]
- Replace fire-and-forget unscoped `CoroutineScope` seeding in `McpRepository.init` (also removes network I/O at DB construction) [ ]
- Fix `MemoryTool.listNotes` infinite collect [ ]

### 9.3 Resilience wiring [ ]
- Wire `RetryManager` (idempotent calls only: initialize, tools/list — never tools/call) and `CircuitBreaker` into `McpHttpClient` [ ]
- Monotonic JSON-RPC ids (replace random id collisions) [ ]
- Key MCP sessions by endpoint+auth so distinct credentials don't share session state [ ]

### 9.4 Error typing [ ]
- Introduce typed chat exceptions; map them in `ChatViewModel` instead of substring matching [ ]
- Single source of truth for default model name (`gemini-3.5-flash` duplicated in 4 files) [ ]

### 9.5 Build hygiene [ ]
- Remove unused dependencies (Retrofit, Moshi, Coil, Firebase stack, logging-interceptor) and google-services plugin [ ]
- Configure `room.schemaLocation` KSP arg so schemas actually export [ ]
- Remove `fallbackToDestructiveMigration()` now that migration 1→2 exists [ ]

### 9.6 Still open (future)
- EncryptedSharedPreferences for API key / auth headers (needs a crypto module decision)
- GitHub Actions workflows (Phase 7)
- Roborazzi goldens + CI validation of migrations
- SSE transport integration or deletion decision

---

## Priority Order

| Priority | Phase | Effort | Impact |
|----------|-------|--------|--------|
| P0 | 9.1 Security remediation | Small | Critical |
| P0 | 9.2 Correctness fixes | Medium | Critical |
| P0 | 9.5 Build hygiene | Small | High |
| P1 | 9.3 Resilience wiring | Medium | High |
| P1 | 9.4 Error typing | Small | Medium |
| P1 | 1.3 Accessibility | Medium | Critical |
| P1 | 1.4 Chat Screen UX | Medium | High |
| P1 | 2.2 Security Hardening (remaining) | Medium | Critical |
| P2 | 7.1-7.2 CI/CD | Medium | Medium |
| P2 | 4.4 Snapshot Tests | Large | Medium |
| P3 | 8.1-8.2 SSE/Memory integration decision | Medium | Low-Medium |
