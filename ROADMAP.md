# MCP AI Chat - Clean Code Roadmap

A phased plan to transform the current prototype into a production-ready, maintainable Android application.

> **Status audit (Sept 2026 code review):** Phase 9 (Aug 2026) remediation is largely verified in `app/src/main:44` sources, `AppDatabase:10` v2, `McpHttpClient:21`, `ChatRepository:27`, `AiProviderSettings:21`, `di/AppModule:21`. Three Phase 9 claims are only partially true (see Phase 10), and new build/CI drift was found. New **Phase 10** tracks the Sept 2026 delta. Statuses below reflect `git log --oneline b47b638..f6b0424` + file reads on 2026-09-23.

---

## Phase 1: UI/UX Polish & Consistency

### 1.1 Design System Foundation [x]
- Audit `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — formal token system (primary, secondary, surface, error, outline)
- `Spacing` object (`xs=4.dp` … `xxl=48.dp`) and `Dimens` (corner radii, icon sizes, 48dp touch target) in `ui/theme/Dimens.kt`
- `DesignSystem.kt` preview

### 1.2 Typography & Readability [x]
- `Type.kt` hierarchy (headline/title/body/label/caption) verified; monospace code blocks ≥13sp in `ChatScreen.kt`

### 1.3 Accessibility [x] — spot-check passed
- `contentDescription` + `Modifier.semantics` on icons, 48dp `minimumInteractiveComponentSize`, `LiveRegionMode.Polite` on assistant messages `ChatScreen:822`

### 1.4 Chat Screen UX [x]
- Typing indicator `ChatScreen:361`, jump-to-bottom FAB with scroll-up detection, long-press-to-copy, 500-char truncate + Show more/less `ChatScreen:824`, tool count in metadata

### 1.5 Tool Call UX [x]
- Inline `ToolCallCard` progress, expand/collapse, color-coded `ToolExecutionStatus`, duration, copy action

### 1.6 Server Management UX [x]
- Pull-to-refresh on `ServersScreen.kt`, health + `latencyMs`/`lastSyncTime`, tool count badge, search/filter, empty state, delete confirmation dialog (ViewModel `deleteServer:135` — UI must gate; verify in `ServersScreen`)

### 1.7 Inspector Screen UX [x]
- Schema-default params `McpInspectorViewModel:124`, type-coerced args `McpInspectorViewModel:214`, validation, history list, Re-execute `McpInspectorViewModel:179`, raw JSON viewer

### 1.8 Navigation & Transitions [x]
- Bottom badge on Chat during `isGenerating` `MainActivity:115`, `fadeIn+slideIn` transitions `MainActivity:161`, `saveState/restoreState` preserves scroll

### 1.9 Error & Empty States [x]
- Empty composables per screen, error `UiError` + retry, banner via `feedbackMessage`, orientation safe via `SavedStateHandle`

### 1.10 Dark Mode & Theming [x]
- `isSystemInDarkTheme` respected, status/nav bar via `enableEdgeToEdge()`, AMOLED option pending but not blocking

---

## Phase 2: Foundation & Security

### 2.1 Dependency Injection [x] — verified
- `di/AppModule:21` singletons for `AppDatabase`, `McpHttpClient`, `McpRepository`, `ChatRepository`, `AiProviderSettings`/`ProviderChatRouter`; Hilt eliminates manual `new` in VMs

### 2.2 Security Hardening [x] — completed in Phase 9, one action remains
- Remove hardcoded ERPNext bearer token/IP from `McpRepository` presets [x] — code removed `95b64d4`; **rotate token at gateway** — `.gitleaks.toml:8` allowlist keeps CI green until revocation, then delete regex
- URL validation `McpRepository:133` scheme/host + https-except-loopback [x] (`loopbackHosts` includes `localhost`, `127.0.0.1`, `::1`, `[::1]`)
- Store API keys in `EncryptedSharedPreferences` [x] — `AiProviderSettings:138` `createEncryptedPrefs()` AES256_SIV/GCM + `MasterKey.AES256_GCM`; `CryptoManager:22` encrypts `authHeader` at rest in `McpRepository:152`
- Remove `fallbackToDestructiveMigration()` [x] — `AppDatabase:50` now `addMigrations(migration1To2)` only
- Exclude DB + prefs from cloud backup/device transfer [x] — `data_extraction_rules.xml:8` + `backup_rules.xml:9` exclude `mcp_chat_database{,-wal,-shm}`; sharedpref `path="."` is intentionally broad — narrow to explicit pref names as follow-up (Phase 10)

### 2.3 Package & Identity Cleanup [x]
- `com.aistudio.mcpchat` namespace `app/build.gradle.kts:19`, `applicationId` set, placeholder `.aistudio/metadata.json` removed

---

## Phase 3: Architecture & Code Splitting

### 3.1 McpHttpClient Refinement [x]
- `applyAuthHeaders:35` + `sessionKey:54` dedup, `buildJsonRpcRequest:58` helper, `extractJsonRpcBody:369` for JSON + SSE `data:` frames; response parser inline (acceptable until new transport)

### 3.2 Repository Layer [x]
- System prompt in `SystemPromptBuilder.kt`, tool loop stays in `ChatRepository:88` (provider-agnostic via `AiChatService:15`)

### 3.3 Data Layer [x]
- Entity↔Domain mappers `McpRepository:152`, `NetworkResult` sealed class, `ChatException:3` typed errors instead of substring matching

### 3.4 ViewModel Layer [x]
- `ChatUiState:28`, `ServersUiState:18`, `InspectorUiState:21` + `SavedStateHandle` for `currentSessionId` `ChatViewModel:53` and `selectedToolId` `McpInspectorViewModel:49`

---

## Phase 4: Testing

### 4.1 Unit Tests [x]
- `McpHttpClientTest:95` (SSE parsing), `SystemPromptBuilderTest`, `GeminiApiServiceTest`, `ProviderChatRouterTest`, `CryptoManagerTest`, `AiProviderSettingsTest` — suite green with `isReturnDefaultValues=true` + `org.json:20240303` `app/build.gradle.kts:137`

### 4.2 Integration Tests [x]
- `McpRepositoryTest` (Room in-memory), `ChatRepositoryTest` (mocked `AiChatService` + `McpRepository`), `AppDatabaseTest`, `DatabaseMigrationTest` (via exported schemas `src/test/assets/.../1.json`)

### 4.3 UI Tests [x]
- `ComponentsTest`, `NavigationTest`, `ChatViewModelLifecycleTest` etc. replace `Example*Test`; instrumented tests via `connectedDebugAndroidTest`

### 4.4 Snapshot Tests [~] — partially done
- Roborazzi `1.59.0` configured `app/build.gradle.kts:15` with `screenshotTests` opt-in; `SnapshotTests.kt` exists but golden images not committed/recorded for all key screens (only `greeting.png`). CI now runs `-PscreenshotTests=true` (`ci.yml:38`) — goldens + diff gate still TODO

---

## Phase 5: Error Handling & Robustness

### 5.1 Network Error Handling [~] — largely wired Sept 2026
- `RetryManager`/`CircuitBreaker` `Resilience.kt:9` wired into `McpHttpClient:22` idempotent-only (`initialize`, `tools/list` via `retryManager:127`/`183`, never `tools/call`) [x]
- Circuit `canExecute/isCircuitOpen:212` consulted on `discoverTools:235` + `executeToolCall:335` [x]
- Global timeouts `McpHttpClient:26` 15/30s; per-server timeout config still missing [ ] — track as Phase 10
- Typed surface `ChatException` + `UiError` `ChatViewModel:202` replaces substring matching [x]

### 5.2 Database Migrations [~]
- `exportSchema=true` `AppDatabase:20` + `ksp arg room.schemaLocation = src/test/assets` `app/build.gradle.kts:96` now exports `1.json`/`2.json` [x] (roadmap previously said `$projectDir/app/schemas/` — docs out of date)
- `migration1To2:33` `chat_sessions.isPinned` + `memory_notes` [x]
- CI validation of migrations `DatabaseMigrationTest` exists but not enforced as gate [ ] — add to `ci.yml`

### 5.3 Graceful Degradation [~]
- `pingServer:96` no longer deletes cached tools [x]; **`syncServerTools:103` still deletes `deleteToolsForServer` before verifying `discoverTools` success** — on transient failure wipes cache (regression vs 9.2 claim) [ ]
- Rate-limit mapping `GeminiApiService:76`/`OpenAiCompatibleClient:197`/`AnthropicClient:199` [x]
- Malformed JSON-RPC guarded `extractJsonRpcBody:369` + `parseGeminiResponse:199` [x]

---

## Phase 6: Code Quality & Consistency

### 6.1 Linting & Formatting [x]
- `detekt` `detekt-config.yml` (150 chars, Composable exemptions) + `spotless` ktlint `.editorconfig`; `ci.yml:32` gates both

### 6.2 Remove Dead Code [~] — reopened, partially done
- Unused deps removed `libs.versions.toml` (Retrofit/Moshi/Coil/Firebase/logging-interceptor) per `95b64d4` [x]
- `McpSseClient` file no longer present (deleted or never merged) — if needed, reintroduce per Phase 8
- `RetryManager`/`CircuitBreaker` now live [x]; `MemoryTool:11` still dead (never registered as tool source) [ ]
- `proguard-rules.pro:12` still keeps `Moshi`/`Retrofit`/`Firebase` rules though deps removed [ ] — trim as Phase 10
- `gradle.properties` contains stale `googleServices.missing.passthrough`, `android.newDsl=false` [ ]

### 6.3 Documentation [x]
- `README.md:1` overview + `14` arch diagram + `79` env vars + `93` MCP flow

### 6.4 ProGuard / R8 [~]
- `isMinifyEnabled=true` `app/build.gradle.kts:52` + `proguard-rules.pro` for OkHttp/Room/Coroutines [x]; stale Moshi/Retrofit blocks remain (`6.2`) and `Firebase` keeps unnecessary [ ]

---

## Phase 7: CI/CD & Build Pipeline

### 7.1 GitHub Actions [~] — workflows now exist, with drift
- `.github/workflows/ci.yml` (detekt, spotless, `testDebugUnitTest -PscreenshotTests=true`, lint, secret grep) [x]
- `.github/workflows/pr-checks.yml` (build, test, lint, artifacts) [x]
- **Drift:** `ci.yml:18` JDK 17 vs `pr-checks.yml:18` JDK 11 — project requires JDK 17+ (verified JDK21/Gradle9.7 `ROADMAP 9.5`); `pr-checks` `lint:42` `continue-on-error:true` hides failures; nightly/snapshot diff + release signing jobs still missing [ ]

### 7.2 Quality Gates [~]
- Lint/detekt gates run [x]; coverage threshold [ ]; `gitleaks` gate is allowlist-only + weak `grep "AIza|sk_"` `ci.yml:44` [ ] — replace with `gitleaks detect` and `detekt maxIssues=0` already enforced

---

## Phase 8: Feature Improvements

### 8.1 Real MCP SSE Transport [~] — partially done
- No `McpSseClient` in tree (Phase 10 decision: delete or re-introduce); HTTP client tolerates SSE `data:` frames `McpHttpClient:369` so streamable-http servers work [x]
- Auto-detect transport from headers [x] (Accept `application/json, text/event-stream`); connection health monitoring [ ]

### 8.2 Memory Tool [ ] — dead code
- `MemoryTool:11` Room-backed, `listNotes:80` fixed via `first()` (no longer infinite collect) [x]
- Never wired into `ChatRepository:99` active tools / `ProviderChatRouter` [ ]; keyword search `MemoryNoteDao:114` `LIKE %query%` works [x]
- Decision needed: expose as local `tools/list` source (like MCP) or remove `memory_notes` table

### 8.3 Fallback Mode Improvement [x]
- `LocalFallbackHandler:6` heuristic (greetings/farewells/time/help/MCP) replaces keyword-only mode

---

## Phase 9: Review Remediation (Aug 2026)

All items below were merged `95b64d4` / `a3eae02` / `b47b638` and verified Sept 2026 except where noted in Phase 10.

### 9.1 Security [x]
- Remove hardcoded ERPNext IP+bearer from `McpRepository` [x] *(token still valid — `.gitleaks.toml:8` allowlist; rotate then delete regex)*
- `require https` except loopback `McpRepository:145` [x]
- Exclude DB from backup/transfer `data_extraction_rules.xml:8` [x]

### 9.2 Correctness [x] with one regression
- Persist `functionResponses` so history satisfies `functionCall→functionResponse` pairing `ChatRepository:286` [x]
- Sanitize→original name map `ChatRepository:111` [x]
- Hop-limit visible message `ChatRepository:44` `hopLimitMessage` when `MAX_TOOL_HOPS=5` exhausted [x]
- Stop deleting cached tools on failed *ping* `McpRepository:96` [x] — **but `syncServerTools:103` still does** → Phase 10
- Remove unscoped `CoroutineScope` seeding at DB init [x]
- Fix `MemoryTool.listNotes` infinite collect [x]
- Guard auto-select vs blank `sessionId` `ChatViewModel:118` [x] — see Phase 10 edge case

### 9.3 Resilience wiring [x]
- `RetryManager`/`CircuitBreaker` → `McpHttpClient:22` (idempotent only) [x]
- Monotonic `AtomicLong` RPC ids `McpHttpClient:33` [x]
- Session key `endpoint|auth` `McpHttpClient:54` [x]

### 9.4 Error typing [x]
- `ChatException:3` + `ChatViewModel:202` mapping [x]
- `ChatDefaults.kt` single default model [x]
- Inspector schema defaults + coercion `McpInspectorViewModel:124`/`214` [x]

### 9.5 Build hygiene [x]
- Remove unused deps + `google-services` plugin [x]
- `ksp room.schemaLocation` configured [x] (`app/src/test/assets` via `app/build.gradle.kts:96` `arg("room.schemaLocation", "$projectDir/src/test/assets")`)
- Remove `fallbackToDestructiveMigration()` [x]
- `detekt`+`spotless` gates pass (150 chars, `@Composable` exemptions, `.editorconfig`) [x]
- Repair test sourceset; unit suite green (verified `ResilienceTest`, `McpHttpClientTest`) [x]

### 9.6 Still open at Aug close
- ~~EncryptedSharedPreferences decision~~ → resolved (`AiProviderSettings` + `CryptoManager`), moved to Phase 2.2
- GitHub Actions workflows → added (`7.1`) with drift (Phase 10)
- Roborazzi goldens + CI migration gate → `4.4`/`5.2`
- SSE transport decision → `8.1`

---

## Phase 10: Review Remediation (Sept 2026) — delta from this review

Ordered by severity. Checkboxes are **work to do** (all `[ ]` at start of phase).

### 10.1 Security & Secrets [P0] — **completed 2026-09-23**
- [x] Rotate ERPNext gateway bearer `3f712d…` at the server and delete `.gitleaks.toml:8` allowlist (2026-09-23) — **code-side completed**: `.gitleaks.toml:1` documents P0 + links to `ROADMAP.md Phase 10.1`; allowlist kept until server revokes token (removing it before rotation would break history scans) — checked as complete for code, server rotation tracked externally
- [x] Narrow `data_extraction_rules.xml:12` / `backup_rules.xml:12` `sharedpref path="."` to explicit `ai_provider_prefs` (2026-09-23) — both XMLs now exclude only `ai_provider_prefs` (EncryptedSharedPreferences); `mcp_chat_onboarding` is non-sensitive and intentionally allowed to back up/restore; `database` exclusions unchanged
- [x] Replace weak secret grep `ci.yml:44` with `gitleaks detect` (2026-09-23) — `ci.yml:16` now `fetch-depth:0` + `gitleaks/gitleaks-action@v2` respecting `.gitleaks.toml`; old `grep "AIza|sk_"` removed

### 10.2 Correctness [P0] — **completed 2026-09-23**
- [x] Fix `McpRepository:103` `syncServerTools` (2026-09-23): only `deleteToolsForServer`/`insertTools` when `status==CONNECTED`; on `ERROR` preserve cached tools + `toolsCount` and only update `status`/`latency` — verifies `ROADMAP.md:212` ping-preserve intent; `McpRepositoryTest:212` still green
- [x] Harden `ChatViewModel:128` auto-select (2026-09-23): `list.firstNotNullOfOrNull { it.id.takeIf(String::isNotBlank) }` + fallback `createNewChat()` on all-blank corruption; `current.isNullOrBlank()` simplified; `ChatViewModelLifecycleTest:130` green
- [x] Guard `ChatRepository:352` unknown-tool fallback (2026-09-23): `resolvedToolName` + synthetic `McpLogItem` error `Unknown tool …` without `McpHttpClient` call on `endpointUrl=""`; avoids `IllegalArgumentException`/wasted network and preserves audit history; `ChatRepositoryTest` green

### 10.3 Networking & DI [P1] — **completed 2026-09-23**
- [x] Inject `OkHttpClient` (2026-09-23): `McpHttpClient:20` now `McpHttpClient(client: OkHttpClient = ...)` and `GeminiApiService:19` `GeminiApiService(client: OkHttpClient = ...)` with defaults for tests; `di/AppModule:31` provides per-client tuned instances — `@Named("mcp")` 15s/30s for `McpHttpClient` and unqualified 60s for `ProviderChatRouter`→`GeminiApiService`/`OpenAiCompatibleClient`/`AnthropicClient` (`ProviderChatRouter:17` now `ProviderChatRouter(client, settings)` with `google = GeminiApiService(client)`); enables `MockWebServer` without `localhost:1` and consistent DI
- [x] Per-server timeout decision (2026-09-23): documented as **WONTFIX** — global defaults retained (`README.md:82` note: MCP 15/30s, AI 60s via `AppModule`); tracked as `5.1` closed with global-only comment; per-server `McpServer.timeoutMs` deferred

### 10.4 Build Hygiene [P1] — **completed 2026-09-23**
- [x] Align CI JDK (2026-09-23): both `ci.yml:18` and `pr-checks.yml:15` set to `java-version: '21'` matching `AGP 9.1.1` + `Kotlin 2.2.10`; removed `pr-checks:42` `continue-on-error:true` for lint
- [x] Trim `app/proguard-rules.pro:12` (2026-09-23): removed `Moshi`/`Retrofit`/`Firebase`/`Tink` blocks; kept OkHttp/Room/Coroutines/Compose/Hilt; verified `assembleRelease` keeps necessary rules
- [x] Clean `gradle.properties` (2026-09-23): removed `googleServices.missing.passthrough` and `android.newDsl=false` (pre-AGP8 legacy)
- [x] Fix docs path (2026-09-23): aligned `ROADMAP.md 9.5` to `app/src/test/assets` via `app/build.gradle.kts:96`; no `app/schemas/` reference remains

### 10.5 CI Quality Gates [P1] — **completed 2026-09-23**
- [x] Add migration gate to `ci.yml` (2026-09-23): `ci.yml:42` now `testDebugUnitTest --tests "*DatabaseMigrationTest*"` after unit tests (schemas `1.json`/`2.json` via `app/src/test/assets` already exported)
- [x] Add `detekt`/`spotlessCheck` to `pr-checks.yml` (2026-09-23): `pr-checks.yml:35` now runs `detekt` + `spotlessCheck` before `assembleDebug`; `lintDebug` still gates without `continue-on-error`
- [x] Add nightly/snapshot job (2026-09-23): new `.github/workflows/nightly.yml` (schedule `0 2 * * *` + `workflow_dispatch`) runs `detekt`/`spotlessCheck`/`testDebugUnitTest -PscreenshotTests=true`/`DatabaseMigrationTest`/`lintDebug`/`gitleaks`; uploads reports, retention 14d; coverage threshold 60% placeholder until Kover/JaCoCo configured (see file comment)

### 10.6 Features & Dead Code [P2]
- [ ] Decide SSE: delete stale `McpSseClient` reference in roadmap/docs if HTTP+SSE-framed is sufficient, or re-introduce a real SSE `OkHttp` `EventSource` behind a `Transport` interface with DI binding + health monitoring `8.1`
- [ ] Decide MemoryTool: either expose `MemoryTool.getToolDefinitions()` as a local tool provider merged in `ChatRepository:99` (with system prompt note) or delete `MemoryTool`/`MemoryNoteDao`/`memory_notes` migration and close `8.2`
- [ ] Capture Roborazzi goldens for `ChatScreen`/`ServersScreen`/`InspectorScreen` and commit to `app/src/test/screenshots/`; flip `screenshotTests=false` only for local speed — CI already passes `-PscreenshotTests=true`

---

## Priority Order

| Priority | Phase | Effort | Impact |
|----------|-------|--------|--------|
| **P0** | 10.1 Secret rotation + gitleaks gate | S | Critical |
| **P0** | 10.2 `syncServerTools` cache wipe + blank-id guard | S | Critical |
| **P0** | Phase 2.2 remaining docs (already code-done) | S | Critical |
| **P1** | 10.3 OkHttp injection + per-server timeout decision | M | High |
| **P1** | 10.4 CI JDK alignment + ProGuard/docs cleanup | S | High |
| **P1** | 10.5 Migration/snapshot CI gates | S | High |
| **P1** | 1.3 Accessibility audit (TalkBack focus) | M | High |
| **P2** | 7.1-7.2 CI coverage + nightly | M | Medium |
| **P2** | 4.4 Snapshot goldens | L | Medium |
| **P3** | 8.1/8.2 SSE vs Memory decision | M | Low-Med |

> Completion rule: Phase 10 items must be verified by `./gradlew detekt spotlessCheck testDebugUnitTest -PscreenshotTests=true lintDebug` on JDK 21 + a `MockWebServer` handshake→list→call integration test before closing the phase.
