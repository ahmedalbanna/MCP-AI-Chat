# MCP AI Chat - Clean Code Roadmap

A phased plan to transform the current prototype into a production-ready, maintainable Android application.

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

### 2.2 Security Hardening [x]
- Remove hardcoded `"gateway-dev-token"` from `McpRepository.kt` — move to `BuildConfig` or encrypted storage
- Add URL validation for user-provided MCP server endpoints (scheme, host, path checks)
- Store Gemini API key in `EncryptedSharedPreferences` instead of `BuildConfig` plain text
- Remove `fallbackToDestructiveMigration()` — implement proper Room migrations

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

### 4.4 Snapshot Tests
- Use Roborazzi (already in deps) for screenshot regression tests
- Capture key screens: chat with messages, server list, inspector with tool results

---

## Phase 5: Error Handling & Robustness

### 5.1 Network Error Handling [x]
- Add retry logic with exponential backoff for MCP server calls
- Implement circuit breaker pattern for failing MCP servers
- Add timeout configuration (connect, read, write) per server
- Surface network errors to UI with actionable messages ("Server unreachable", "Invalid response")

### 5.2 Database Migrations [x]
- Export Room schema (`exportSchema = true`, add schema directory)
- Write migration from version 1 → 2 for any schema changes
- Add CI check that validates migrations against exported schemas

### 5.3 Graceful Degradation [x]
- When an MCP server is unreachable, disable its tools gracefully (not crash)
- Handle Gemini API rate limits with user-facing message and retry
- Handle malformed JSON-RPC responses without crashing the chat loop

---

## Phase 6: Code Quality & Consistency

### 6.1 Linting & Formatting [x]
- Add `ktlint` or `detekt` with shared config
- Add `.editorconfig` for consistent formatting
- Run lint as part of CI

### 6.2 Remove Dead Code [x]
- Remove unused dependencies from `libs.versions.toml` (Camera, Accompanist, Firestore, Play Services Location, Datastore)
- Remove commented-out dependency blocks from `build.gradle.kts`
- Clean up unused imports across all files
- Remove `@Suppress("DEPRECATION")` by migrating to non-deprecated APIs

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

### 7.1 GitHub Actions [x]
- PR checks: build, lint, unit tests
- Nightly: full test suite + screenshot comparison
- Release: signed build + artifact upload

### 7.2 Quality Gates [x]
- Minimum code coverage threshold (e.g., 60% for new code)
- Lint must pass with zero errors
- No hardcoded secrets (use `gitleaks` or similar)

---

## Phase 8: Feature Improvements

### 8.1 Real MCP SSE Transport [x]
- Implement Server-Sent Events transport alongside HTTP POST
- Auto-detect transport type from server response headers
- Add connection health monitoring for SSE connections

### 8.2 Memory Tool [x]
- Replace hardcoded memory responses with actual Room-backed persistent storage
- Add semantic search or simple keyword matching for `search_memory_notes`

### 8.3 Fallback Mode Improvement [x]
- Replace simple keyword matching with a lightweight local LLM or better heuristic
- Add a configuration toggle for fallback mode behavior

---

## Priority Order

| Priority | Phase | Effort | Impact |
|----------|-------|--------|--------|
| P0 | 1.3 Accessibility | Medium | Critical |
| P0 | 1.4 Chat Screen UX | Medium | High |
| P0 | 2.2 Security Hardening | Small | Critical |
| P0 | 2.1 Dependency Injection | Medium | High |
| P1 | 1.1 Design System Foundation | Small | High |
| P1 | 1.2 Typography & Readability | Small | High |
| P1 | 1.5 Tool Call UX | Medium | High |
| P1 | 1.9 Error & Empty States | Medium | High |
| P1 | 3.1 McpHttpClient Refinement | Small | High |
| P1 | 4.1 Unit Tests (core logic) | Medium | High |
| P1 | 5.1 Network Error Handling | Medium | High |
| P2 | 1.6 Server Management UX | Medium | Medium |
| P2 | 1.10 Dark Mode & Theming | Medium | Medium |
| P2 | 2.3 Package Cleanup | Small | Medium |
| P2 | 3.2-3.4 Architecture | Medium | Medium |
| P2 | 6.1-6.2 Linting & Dead Code | Small | Medium |
| P3 | 1.7 Inspector Screen UX | Medium | Medium |
| P3 | 1.8 Navigation & Transitions | Medium | Medium |
| P3 | 4.2-4.4 Integration/UI/Snapshot Tests | Large | Medium |
| P3 | 5.2-5.3 Migrations & Degradation | Medium | Medium |
| P3 | 6.3-6.4 Documentation & ProGuard | Small | Medium |
| P4 | 7.1-7.2 CI/CD | Medium | Medium |
| P5 | 8.1-8.3 Feature Improvements | Large | Low-Medium |
