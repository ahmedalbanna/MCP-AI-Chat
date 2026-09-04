# MCP AI Chat — Future Technology & Feature Plan

> **Baseline:** Phases 1–13 completed. Codebase is clean, tested, and production-ready in its current form. This plan targets the next major evolution of the app using modern Android technologies and best practices.

---

## Phase 14: Modern Android Foundation Refresh

### 14.1 Kotlin 2.1+ & Compose 2025.01+ [P1]
- Upgrade Kotlin to 2.1.x and Compose BOM to 2025.01.x
- Migrate from `androidx.activity:activity-compose` 1.9.x to 1.9.3+
- `@file:Previews` → Compose Preview annotation migration if needed
- Verify `kotlinx.coroutines` 1.9.x compatibility
- **Impact:** Bug fixes, performance improvements, Compose compiler optimizations
- **Risk:** Low — well-defined upgrade path

### 14.2 Compose Foundation 3.0+ Migration [P1]
- Migrate `Material3` components to stable 3.0+ APIs
- Replace deprecated `TopAppBar` with `TopAppBarScrollBehavior`
- Update `NavigationBarItemDefaults` to latest color API
- Migrate `FilterChip` to stable `assistChip`/`filterChip` patterns
- **Impact:** Stability, fewer deprecation warnings, better RTL support

### 14.3 Kotlin Multiplatform (KMP) Foundation [P2]
- Introduce shared KMP module for business logic (`data/` layer)
- Use `kotlinx.coroutines`, `kotlinx.serialization` in common source sets
- Keep Android UI in `app/` module, move pure Kotlin code to `shared/`
- Enables future iOS companion app or multiplatform MCP tools
- **Impact:** Code reuse, faster feature development across platforms
- **Risk:** Medium — requires architectural refactoring

### 14.4 Gradle Version Catalog Modernization [P1]
- Migrate `libs.versions.toml` to version catalog 2.x format
- Upgrade AGP to 8.6+ (stable)
- Enable `buildFeatures.compose = true` with compiler reports for debugging
- Add `kotlinCompilerExtensionVersion` alignment with Compose BOM
- **Impact:** Faster builds, better dependency resolution

---

## Phase 15: Real-Time Streaming AI Responses

### 15.1 Server-Sent Events (SSE) for AI Streaming [P0]
- Add SSE parsing to `GeminiApiService` using `EventSource` parser
- Implement `OpenAiCompatibleClient` streaming via `chat/completions` with `stream=true`
- Implement `AnthropicClient` streaming via `messages` endpoint with `stream=true`
- Stream tokens to UI via `SharedFlow<String>` in `ChatRepository`
- **Impact:** Major UX improvement — users see responses token-by-token
- **Dependencies:** `okhttp-sse` or custom SSE parser; `StateFlow` → `SharedFlow` migration in ViewModel

### 15.2 Stop/Regenerate Mid-Stream [P1]
- Cancel in-flight SSE request via `call.cancel()` + coroutine cancellation
- Preserve partial response in message history
- Add "Regenerate" action on last assistant message
- **Impact:** User control during long generations

### 15.3 Token-by-Token Progress Indicator [P2]
- Replace typing indicator with actual stream progress bar
- Show tokens/sec, estimated time remaining
- Graceful fallback to non-streaming if provider doesn't support SSE

---

## Phase 16: Multi-Modal & Rich Content

### 16.1 Image Attachments & Vision [P0]
- Add `image/*` intent picker with Compose `PhotoPicker` (Android 13+) + fallback
- Store images as content URIs (no Room blobs); pass as base64 or multipart to providers
- Support Gemini Vision (`gemini-pro-vision`), GPT-4o vision, Claude 3+ vision
- Display images inline in chat bubbles with `Coil` or ` Accompanist` placeholder
- **Impact:** Massive UX upgrade — visual context in conversations

### 16.2 Image Generation Support [P1]
- Integrate provider image generation endpoints (DALL-E 3, Imagen, Stable Diffusion)
- Add "Generate Image" action in chat input toolbar
- Store generated images in app-specific cache; show inline + save to gallery option
- **Impact:** Creative AI capabilities

### 16.3 Code Execution & Sandboxing [P2]
- Build built-in Python/JS code execution tool (Docker/remote sandbox or local JSBridge)
- Display code blocks with syntax highlighting (`highlight.js` via WebView or `CodeView`)
- Add "Run Code" button on assistant code blocks
- **Impact:** Turn MCP AI Chat into an AI-powered REPL/coding assistant

---

## Phase 17: Advanced MCP Features

### 17.1 MCP Stdio Transport (Local Tools) [P1]
- Add stdio subprocess transport for local MCP servers (Node.js/Python CLIs)
- Implement process lifecycle management + stdin/stdout JSON-RPC framing
- Auto-detect local MCP servers from PATH or user-configured executables
- **Impact:** Zero-config local MCP tools (e.g., local file system, database access)

### 17.2 MCP Tool Composition & Orchestration [P1]
- Implement `ToolOrchestrator` to chain multiple MCP tools into workflows
- Allow users to define "tool macros" — reusable multi-step tool sequences
- Add conditional branching based on tool results
- **Impact:** Automation, complex task execution

### 17.3 MCP Resource Subscriptions [P2]
- Implement `resources/subscribe` and `resources/updated` notifications
- Auto-update cached resources when servers push changes
- Display resource content in chat (e.g., file contents, database rows)
- **Impact:** Real-time data integration

---

## Phase 18: Intelligence & Memory

### 18.1 Long-Term Memory with Embeddings [P1]
- Add vector store to Room (or `sqlite-vec` extension) for message embeddings
- Use provider embeddings API to embed messages + memory notes
- Implement semantic search over conversation history
- Auto-surface relevant memories when user asks related questions
- **Impact:** Context-aware AI that "remembers" past conversations

### 18.2 Smart Session Management [P1]
- Auto-tag sessions by topic using LLM classification
- Auto-pin important sessions, archive stale ones after 30 days
- Session merge/split operations
- Search across all sessions with full-text search (Room FTS5)
- **Impact:** Better organization at scale

### 18.3 Prompt Templates & Variables [P2]
- Built-in template library (coding, writing, analysis, translation)
- User-defined templates with `{{variables}}` substitution
- Template categories + favorites
- **Impact:** Productivity, consistency

---

## Phase 19: Performance & Architecture Hardening

### 19.1 Kotlin Coroutines 2.x + Flow Optimization [P1]
- Migrate to `kotlinx.coroutines` 1.9.x with structured concurrency improvements
- Replace `StateFlow` hot streams with `SharedFlow` where appropriate
- Implement `callbackFlow` for MCP SSE + AI streaming
- Add `SupervisorJob` scoping for independent parallel operations
- **Impact:** Fewer memory leaks, better cancellation, lower latency

### 19.2 Database Performance [P1]
- Add Room indexes on `chat_messages(sessionId, createdAt)`, `mcp_servers(endpoint)`
- Implement paging for message lists (`Paging 3` library)
- Add `FtsEntity` for full-text search on messages
- Optimize `ChatRepository` to use `@Query` with `LIMIT/OFFSET` for history pagination
- **Impact:** Faster scroll, instant search, lower memory footprint

### 19.3 Compose Performance Profiling [P2]
- Enable Compose compiler metrics (composable counts, stability)
- Add `@Stable` annotations to ViewModel state classes (`ChatUiState`, etc.)
- Use `derivedStateOf` for expensive UI calculations
- Profile with `Baseline Profiles` for faster startup
- **Impact:** Smoother UI, less jank

### 19.4 Network Layer Modernization [P2]
- Evaluate `Ktor` client as OkHttp replacement for multiplatform readiness
- Keep OkHttp for Android-specific features (SSE, caching)
- Add HTTP/2 + connection pooling tuning for high-throughput MCP servers
- **Impact:** Future-proofing, multiplatform compatibility

---

## Phase 20: Privacy, Security & Compliance

### 20.1 End-to-End Encryption (E2EE) [P2]
- Implement E2EE for chat messages using Signal Protocol (or AES-GCM per-session)
- Store encryption keys in Android Keystore (already have `CryptoManager` foundation)
- Add "Lock App" with biometric/PIN authentication
- **Impact:** Security, user trust, compliance readiness

### 20.2 Privacy Dashboard [P1]
- Show users what data is stored locally (sessions, tools, logs)
- One-tap "Clear All Data" with confirmation
- Show network request logs (already have `mcp_logs`; expose in UI)
- Opt-in analytics with privacy-first approach (no PII)
- **Impact:** Transparency, GDPR/CCPA readiness

### 20.3 Secure Backup & Sync [P2]
- Encrypt Room DB export for cloud backup (Google Drive, Dropbox)
- Implement E2EE sync across user's devices
- Conflict resolution for multi-device edits
- **Impact:** Data portability, multi-device support

---

## Phase 21: UI/UX Modernization

### 21.1 Adaptive UI & Large Screen Optimization [P1]
- Implement `WindowSizeClass` for responsive layouts (phone/tablet/foldable)
- Add two-pane layout on tablets: chat + servers/inspector side-by-side
- Support `WindowInsets` for foldable hinge areas
- **Impact:** Better experience on tablets and foldables

### 21.2 Material 3 Expressive Updates [P1]
- Adopt Material 3 expressive components (large top app bars, FAB variants)
- Implement `NavigationDrawer` alternative for wide screens
- Add `PullToRefresh` from Accompanist (or Material 3 stable)
- Use `ModalBottomSheet` for provider/server quick actions
- **Impact:** Modern, polished UI

### 21.3 Haptic Feedback & Animations [P2]
- Add haptic feedback on tool call start/complete (if device supports)
- Implement shared element transitions for server/inspector navigation
- Add Lottie animations for loading states
- **Impact:** Delightful micro-interactions

---

## Phase 22: Testing & Quality Evolution

### 22.1 Compose UI Testing Modernization [P1]
- Migrate from Robolectric snapshot tests to Compose `createComposeRule` + `onNode`
- Use `compose-ui-test-manifest` for test tag validation in CI
- Add accessibility testing with `semantics` assertions
- **Impact:** Faster, more reliable UI tests

### 22.2 Integration Test Suite Expansion [P1]
- Add `McpHttpClient` integration tests with `MockWebServer` SSE streaming
- Test end-to-end: MCP server → tool execution → AI response → UI state
- Add database migration CI gate (enforce `DatabaseMigrationTest` in `ci.yml`)
- **Impact:** Confidence in refactors

### 22.3 Code Coverage Target [P1]
- Raise kover minimum bound from 0% → 40% → 60% → 80%
- Add coverage reports to CI artifacts (upload to Codecov or similar)
- Exclude generated code (Hilt, Room, BuildConfig) from coverage calculation
- **Impact:** Code quality metrics, regression prevention

---

## Phase 23: Developer Experience & Ecosystem

### 23.1 Public MCP Server Registry [P2]
- Add curated list of popular MCP servers (filesystem, database, web search, etc.)
- One-tap import from registry
- Auto-configure server with sensible defaults
- **Impact:** Lower barrier to entry, discoverability

### 23.2 Plugin / Extension System [P3]
- Define extension API for third-party MCP server providers
- Allow sideloaded extensions with signature verification
- Extension marketplace or GitHub integration
- **Impact:** Ecosystem growth

### 23.3 OpenAPI / MCP Server Generator [P3]
- Auto-generate MCP server boilerplate from OpenAPI specs
- Convert REST APIs to MCP tools automatically
- **Impact:** Rapid MCP ecosystem expansion

---

## Phase 24: Platform Expansion

### 24.1 Wear OS Companion [P3]
- Compose for Wear OS app for quick chat access
- Sync conversations via encrypted Room backup
- Voice input via Wear OS mic
- **Impact:** Platform expansion

### 24.2 Desktop Support (Compose Multiplatform) [P3]
- Extend shared KMP module to desktop (Compose Multiplatform Desktop)
- Desktop-specific UI: split view, keyboard shortcuts, tray integration
- **Impact:** Cross-platform reach

---

## Technology Stack Recommendations (Current → Future)

| Layer | Current | Recommended |
|-------|---------|-------------|
| Language | Kotlin 2.x | Kotlin 2.1+ |
| UI | Jetpack Compose 1.6+ | Compose 2025.01+ / Compose Foundation 3.0 |
| DI | Hilt 2.x | Hilt 2.x + Hilt Navigation Compose stable |
| Networking | OkHttp + custom SSE | OkHttp + Ktor client (multiplatform) |
| Database | Room 2.6+ | Room 2.6+ + sqlite-vec for embeddings |
| Serialization | Manual JSON + `org.json` (tests) | kotlinx.serialization + `org.json` retained for tests |
| Async | Coroutines 1.9+ | Coroutines 1.9+ + Flow 2.0 patterns |
| Testing | MockK, Robolectric, Roborazzi | MockK + Compose Test + Turbine (flow testing) |
| CI | GitHub Actions | GitHub Actions + Build Cache + Matrix testing |
| Analytics | None | Firebase Analytics (opt-in) or custom privacy-first analytics |
| Crash Reporting | None | Firebase Crashlytics (opt-in) |

---

## Execution Priority Matrix

| Priority | Phase | Effort | User Value | Tech Risk |
|----------|-------|--------|------------|-----------|
| **P0** | 15.1 SSE Streaming | M | ★★★★★ | Medium |
| **P0** | 16.1 Image Attachments | M | ★★★★★ | Medium |
| **P1** | 14.1 Kotlin/Compose Upgrade | S | ★★★ | Low |
| **P1** | 17.1 Stdio Transport | M | ★★★★ | Medium |
| **P1** | 18.1 Long-Term Memory | L | ★★★★ | High |
| **P1** | 22.1 Compose UI Testing | M | ★★★ | Low |
| **P1** | 19.2 Database Performance | S | ★★★ | Low |
| **P1** | 21.1 Adaptive UI | M | ★★★ | Low |
| **P2** | 14.3 KMP Foundation | L | ★★★ | High |
| **P2** | 20.2 Privacy Dashboard | M | ★★★ | Low |
| **P2** | 16.3 Code Execution | L | ★★★ | High |
| **P2** | 22.3 Coverage 60% | S | ★★ | Low |
| **P3** | 23.x Plugin System | XL | ★★ | High |
| **P3** | 24.x Platform Expansion | XL | ★★ | High |

---

## Key Technology Decisions

| Decision | Rationale |
|----------|-----------|
| **Keep Jetpack Compose** | Strongly typed, Kotlin-native, first-class Material 3 support, rapid iteration |
| **Keep Hilt DI** | Production-tested, Compose integration mature, avoids manual Service Locator |
| **Keep Room** | SQLite wrapper with Flow support; sufficient for current scale; consider sqlite-vec for embeddings |
| **Add Kotlinx Serialization** | Type-safe, multiplatform, better than manual JSON + `org.json` |
| **Add Turbine for Flow Testing** | Kotlin-first flow testing library; replaces awkward MockK flow verification |
| **Consider KMP** | Enables iOS/desktop; high effort but future-proof; defer until Phase 14 validated |
| **Keep OkHttp for Android** | Industry standard, battle-tested; consider Ktor only if multiplatform becomes priority |
| **E2EE via CryptoManager** | Already have AES-GCM + Keystore foundation; extend to chat messages without new crypto primitives |
| **Vector Search via sqlite-vec** | SQLite extension, no external dependencies, works with existing Room setup |

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| **SSE streaming complexity** | Start with one provider (Gemini), abstract stream interface, fallback to non-streaming |
| **Image storage bloat** | Use content URIs (not blobs), implement cache eviction, compress before upload |
| **KMP migration effort** | Incremental: start with `data/model` and `data/tool` (pure Kotlin), keep UI Android-only |
| **Breaking API changes** | Use versioned API contracts; maintain backward-compatible `AiChatService` interface |
| **Performance regression** | Profile before/after with Baseline Profiles; gate performance in CI |
| **Encryption key loss** | User education: warn before backup; provide export key option; fallback to re-auth |

---

## Next Immediate Steps

1. **Create feature branch:** `feature/streaming-ai-responses`
2. **Prototype SSE parsing:** Add `GeminiApiService` streaming method behind feature flag
3. **UI integration:** Stream tokens to `ChatViewModel` via `SharedFlow`, display in `ChatScreen`
4. **Test:** Add `MockWebServer` SSE handshake test; verify UI renders tokens in real-time
5. **Rollout:** Beta flag in `AiProviderSettings` → "Enable streaming"; default off for stability

---

*Last updated: 2026-09-03*
*Plan owner: MCP AI Chat development team*
*Review cycle: Monthly, or after each completed phase*
