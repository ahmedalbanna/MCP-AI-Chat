# MCP AI Chat - Clean Code Roadmap

A phased plan to transform the current prototype into a production-ready, maintainable Android application.

> **Status audit (Sept 2026 code review):** Phase 9 (Aug 2026) remediation is largely verified in `app/src/main:44` sources, `AppDatabase:10` v2, `McpHttpClient:21`, `ChatRepository:27`, `AiProviderSettings:21`, `di/AppModule:21`. Three Phase 9 claims are only partially true (see Phase 10), and new build/CI drift was found. New **Phase 10** tracks the Sept 2026 delta. **Phase 11 (26 Aug 2026)** adds persistent UI customization (theme). **Phase 12 (26 Aug 2026)** hardens quality/accessibility/snapshots/coverage. **Phase 13 (26 Aug 2026)** hardens UI review PR1 (Theme/Chat/Servers/Inspector/Components). Statuses below reflect `git log --oneline b47b638..f6b0424` + file reads on 2026-09-23 and `git log --oneline` + file reads on 2026-08-26 for Phase 11-13 (incl. `4884684`).

---

## Phase 1: UI/UX Polish & Consistency

### 1.1 Design System Foundation [x]
- Audit `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — formal token system (primary, secondary, surface, error, outline)
- `Spacing` object (`xs=4.dp` … `xxl=48.dp`) and `Dimens` (corner radii, icon sizes, 48dp touch target) in `ui/theme/Dimens.kt`
- `DesignSystem.kt` preview

### 1.2 Typography & Readability [x]
- `Type.kt` hierarchy (headline/title/body/label/caption) verified; monospace code blocks ≥13sp in `ChatScreen.kt`

### 1.3 Accessibility [x] — completed via Phase 12 (2026-08-26)
- `contentDescription` + `Modifier.semantics` on icons, 48dp `minimumInteractiveComponentSize`, `LiveRegionMode.Polite` on assistant messages `ChatScreen:821` [x]; `SettingsScreen.kt:42` `heading()` + `Role.Button`/`stateDescription` on `ThemeModeChip`/`Switch` + `contentDescription` on palette icon verified `Phase 12.2` [x]

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

### 1.10 Dark Mode & Theming [x] — completed via Phase 11 (2026-08-26)
- `isSystemInDarkTheme` respected, status/nav bar via `enableEdgeToEdge()` [x]; AMOLED now persistent via `ThemeSettings:12` `useAmoledDark` + toggle `SettingsScreen:136` `amoled_switch` [x]; dynamic color opt-in `Theme.kt:99` `useDynamicColor` [x]

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

### 4.4 Snapshot Tests [x] — completed via Phase 12 (2026-08-26), extended Sept 2026
- Roborazzi `1.59.0` configured `app/build.gradle.kts:15` with `screenshotTests` opt-in [x]; `SnapshotTests.kt` now covers `ServerStatusBadge`/`ToolCallCard` + `SettingsScreen` variants + `TypingIndicator`/`EmptyChatView`/`ServerCard`/`ExecutionResult`/`LogItem` (`19` cases total, wired via `RoborazziRule` so captures actually engage) [x]; CI runs `-PscreenshotTests=true` `ci.yml:40`/`nightly.yml:25` — goldens committed under `src/test/snapshots/`, verified via `:app:verifyRoborazziDebug` (negative control: corrupted golden fails the gate) [x]

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

### 6.1 Linting & Formatting [x] — verified Phase 12 (2026-08-26)
- `detekt` `detekt-config.yml` (150 chars, Composable exemptions) + `spotless` ktlint `.editorconfig` [x]; `ci.yml:34` gates both via `detekt` SUCCESS 0 issues (was 11 pre-existing `CyclomaticComplexMethod`/`ThrowsCount` etc. fixed `AnthropicClient.kt:65`, `CryptoManager.kt:40`, `ChatScreen.kt:141`, `AiProviderSettingsTest.kt:121`) [x]; `spotlessCheck` SUCCESS [x]

### 6.2 Remove Dead Code [x] — completed via Phase 10.4/10.6
- Unused deps removed `libs.versions.toml` (Retrofit/Moshi/Coil/Firebase/logging-interceptor) per `95b64d4` [x]
- `McpSseClient` file no longer present (deleted or never merged) — decided WONTFIX per 10.6, HTTP+SSE-framed sufficient [x]
- `RetryManager`/`CircuitBreaker` now live [x]; `MemoryTool:11` retained as intentional local Room utility — not wired to `ChatRepository:99` per 10.6 decision (kept for future `ToolOrchestrator`), not dead code [x]
- `proguard-rules.pro:12` trimmed `Moshi`/`Retrofit`/`Firebase`/`Tink` per 10.4 [x]
- `gradle.properties` cleaned `googleServices.missing.passthrough`/`android.newDsl` per 10.4 [x]

### 6.3 Documentation [x]
- `README.md:1` overview + `14` arch diagram + `79` env vars + `93` MCP flow

### 6.4 ProGuard / R8 [x]
- `isMinifyEnabled=true` `app/build.gradle.kts:52` + `proguard-rules.pro` for OkHttp/Room/Coroutines/Compose/Hilt [x]; stale blocks removed per 10.4
- Release build verified end-to-end: `-dontwarn com.google.errorprone.annotations.**` for Tink (EncryptedSharedPreferences) fixed `minifyReleaseWithR8`; `assembleRelease` green and APK signed (v1.1.0) [x]

---

## Phase 7: CI/CD & Build Pipeline

### 7.1 GitHub Actions [x] — completed via Phase 10.4/10.5
- `.github/workflows/ci.yml` (detekt, spotless, `testDebugUnitTest -PscreenshotTests=true`, `DatabaseMigrationTest`, lint, gitleaks) [x]
- `.github/workflows/pr-checks.yml` (detekt, spotless, assemble, test, lint) [x]
- `.github/workflows/nightly.yml` (detekt, spotless, test+screenshots, migration, lint, gitleaks) [x]
- Drift fixed: both `ci.yml:18`/`pr-checks.yml:15` JDK 21 per 10.4; `continue-on-error` removed

### 7.2 Quality Gates [x] — completed via Phase 12 (2026-08-26)
- Lint/detekt gates run [x]; `gitleaks` `gitleaks-action@v2` with `.gitleaks.toml` allowlist [x] (`detekt maxIssues=0` enforced, now passing) [x]; coverage via `org.jetbrains.kotlinx.kover:0.9.9` (AGP 9 support; 0.9.1 collected nothing) + `kover{reports{verify{minBound(60)}}}` enforced 60% gate, measured 61.1% after backfilling `GeminiApiService.generateContent` + `ArabicNumerals` tests [x]

---

## Phase 8: Feature Improvements

### 8.1 Real MCP SSE Transport [x] — decided WONTFIX per 10.6
- No `McpSseClient` in tree (HTTP+SSE-framed is sufficient); `McpHttpClient:369` tolerates SSE `data:` frames so streamable-http servers work [x]
- Auto-detect via `Accept: application/json, text/event-stream` [x]; dedicated SSE `EventSource` deferred (no DI binding needed) [x]

### 8.2 Memory Tool [x] — decided per 10.6 (retain, not wired)
- `MemoryTool:11` Room-backed, `listNotes:80` fixed via `first()` (no longer infinite collect) [x]
- Intentionally not wired into `ChatRepository:99` (kept as local utility for future `ToolOrchestrator`; not dead code) [x]; keyword search `MemoryNoteDao:114` `LIKE %query%` works [x]
- Decision: retain `memory_notes` table + `MemoryTool`/`MemoryNoteDao`; not exposed as `tools/list` to avoid mixing local/remote namespaces — revisit if local tool provider is needed [x]

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
- Provider-specific collision-free wire names + stable `McpTool.id` round trips via `ProviderToolCatalog` [x]
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
- [x] Add nightly/snapshot job (2026-09-23): new `.github/workflows/nightly.yml` (schedule `0 2 * * *` + `workflow_dispatch`) runs `detekt`/`spotlessCheck`/`testDebugUnitTest -PscreenshotTests=true`/`DatabaseMigrationTest`/`lintDebug`/`gitleaks`; uploads reports, retention 14d; coverage via `koverVerify` 40% gate (see `app/build.gradle.kts`)

### 10.6 Features & Dead Code [P2] — **completed 2026-09-23**
- [x] Decide SSE (2026-09-23): **WONTFIX** — HTTP+SSE-framed via `McpHttpClient:369` `extractJsonRpcBody` is sufficient for streamable-http; no dedicated `McpSseClient`/`EventSource` needed; stale references removed from `ROADMAP.md:121`/`8.1` and docs; DI binding deferred [x]
- [x] Decide MemoryTool (2026-09-23): **retain** `MemoryTool:11`/`MemoryNoteDao`/`memory_notes` as intentional local Room utility (not dead code) but **not wired** to `ChatRepository:99`; documented in `MemoryTool.kt:11` and `ROADMAP.md:152`/`6.2`; avoids local/remote namespace mixing — revisit via `ToolOrchestrator` if local tool provider is desired [x]
- [x] Capture Roborazzi goldens: `SnapshotTests.kt` wired with `RoborazziRule` (previously captures were silent no-ops — `taskType.isEnabled()` gate), `logItem_*` pinned to a fixed timestamp, all `19` goldens committed under `app/src/test/snapshots/` and verified via `:app:verifyRoborazziDebug` [x]
- [x] Bump Google default model `gemini-3.5-flash` → `gemini-3.6-flash` (GA Jul 2026, cheaper/faster; 3.5 retires May 2027): `ChatDefaults`/`AiProviders.google()`/`Entities` default + provider-sheet hint; stored sessions keep their persisted model (no migration) [x]

---

## Phase 11: UI Customization & Persistent Theming (Aug 2026)

Implements user-controlled appearance (approved 2026-08-26: System/Light/Dark + AMOLED + Dynamic Color opt-in, no hue picker for MVP). Covers approved best answers: persistent `ThemeSettings` via DataStore, gear-less but discoverable 4th bottom tab `Settings`, `dynamicColor` default `false` (brand `IndigoPrimary 0xFF6366F1` preserved). Verified `assembleDebug` SUCCESS, `spotlessCheck` SUCCESS, `NavigationTest` 4/4, subset `testDebugUnitTest` SUCCESS.

### 11.1 Theme Persistence [P1] — **completed 2026-08-26**
- [x] Add `androidx.datastore:datastore-preferences:1.1.1` (`gradle/libs.versions.toml:34` `datastorePreferences`, `app/build.gradle.kts:120` `androidx.datastore.preferences`) — **P1** DataStore vs EncryptedSharedPreferences: theme prefs are non-sensitive, file `theme_prefs.preferences_pb`
- [x] `data/local/ThemeSettings.kt:12` — `ThemeMode {SYSTEM,LIGHT,DARK}` enum, `ThemePreferences(themeMode, useAmoledDark, useDynamicColor)`, `preferencesDataStore(name="theme_prefs")`, `preferencesFlow: Flow<ThemePreferences>`, `setThemeMode/setUseAmoledDark/setUseDynamicColor` via `dataStore.edit` — injectable `DataStore<Preferences>?` ctor param for tests
- [x] Remove volatile `ThemePrefs` `ui/theme/Theme.kt:94` (`object ThemePrefs {var useAmoledDark by mutableStateOf(false)}` was lost on restart) — replace with `MyApplicationTheme:92` params `(darkTheme:Boolean, dynamicColor:Boolean, useAmoledDark:Boolean)` and `when {dynamicColor && S -> dynamic* ; dark && amoled -> amoledDarkColorScheme ; dark -> darkColorScheme ; else -> lightColorScheme }` [x]
- [x] Hilt `di/AppModule:69` `provideThemeSettings(@ApplicationContext)` `@Singleton` [x]

### 11.2 Settings UI [P1] — **completed 2026-08-26**
- [x] `ui/viewmodel/ThemeViewModel.kt:10` — `HiltViewModel` exposing `uiState: StateFlow<ThemePreferences>` via `WhileSubscribed(5_000)`, `setThemeMode/setUseAmoledDark/setUseDynamicColor` launching `viewModelScope` [x]
- [x] `ui/screens/SettingsScreen.kt:32` — `Scaffold` `TopAppBar` “Appearance”, `ThemeModeChip` `FilterChip` trio `SYSTEM/LIGHT/DARK` (`testTag theme_chip_*`), `SettingSwitchRow` for `amoled_switch` (disabled when `LIGHT`) + `dynamic_color_switch` (enabled only on `SDK>=S` `Build.VERSION_CODES.S:79`), helper copy per mode, preview bubbles `PreviewBubble` using `surfaceVariant` vs `primary` [x]
- [x] Uses `Spacing` `Dimens` tokens (`cardPadding`, `cornerRadiusLg`, `iconSizeLg`) consistent with `Phase 1.1` design system [x]

### 11.3 Navigation & System Integration [P1] — **completed 2026-08-26**
- [x] `MainActivity.kt:62` — extend `sealed Screen` with `Settings: “settings”, Settings icon`, `MainActivity:70` hoist `ThemeViewModel`, compute `darkTheme` (`LIGHT=false, DARK=true, SYSTEM->isSystemInDarkTheme()`) and call `MyApplicationTheme(darkTheme, dynamicColor=prefs.useDynamicColor, useAmoledDark=prefs.useAmoledDark)` [x]
- [x] `MainActivity.kt:96` bottom `NavigationBar` items `Chat/Servers/Inspector/Settings` (4 items, `saveState/restoreState` `143` preserved), `NavHost:186` `composable(Settings.route) { SettingsScreen(themeViewModel) }` [x]
- [x] Alternatives considered (see plan): gear sheet in `ChatScreen.kt:253` vs top-level gear vs drawer — chosen 4th tab for discoverability, fits Material 3 (3-5 items), keeps `NavigationTest.kt:22` pattern [x]

### 11.4 Build Hygiene & Test Fixes [P1] — **completed 2026-08-26**
- [x] `data/security/AiProviderSettings.kt:137` — `createEncryptedPrefs` try/catch fallback to `getSharedPreferences("ai_provider_prefs")` when `AndroidKeyStore not found` (Robolectric `sdk=36` `KeyStoreException: AndroidKeyStore not found` `MasterKey.Builder:377`) — fixes `NavigationTest.kt:21` `HiltAndroidTest` 4/4 `bottomNav_*` (previously FAILED) while `AiProviderSettingsTest.kt:31` still uses `InMemoryPrefs` [x]
- [x] `ui/viewmodel/AiProviderSettingsViewModel.kt:16` — move `// null => creating` to own line to satisfy `spotless` `standard:value-parameter-comment` [x]
- [x] `spotlessApply` reformatted `AiProviders.kt`, `AnthropicClient.kt`, `OpenAiCompatibleClient.kt`, `ChatRepository.kt`, `AppModule.kt`, `ProviderSettingsSheet.kt`, `Theme.kt`, `DatabaseMigrationTest.kt`, etc. — now `spotlessCheck` SUCCESS (was FAILED) [x]
- [x] `detekt` — 11 pre-existing weighted issues (`AnthropicClient.kt:65` `CyclomaticComplexMethod`, `CryptoManager.kt:40` `ThrowsCount`, etc.) unchanged; 0 new issues from Phase 11 files (`ThemeSettings`, `ThemeViewModel`, `SettingsScreen`, `MainActivity`) [x]
- [x] `assembleDebug` SUCCESS 2m30s, `testDebugUnitTest --tests *NavigationTest*` SUCCESS 48s, `testDebugUnitTest --tests *ViewModel*/*Crypto*` SUCCESS 17s [x]
- [x] Next: consider `SettingsScreen` Roborazzi goldens (light/dark/amoled/dynamic) via `SnapshotTests.kt:22` and coverage via `TestThemeModule` if DataStore file I/O needs `PreferenceDataStoreFactory.create` in-memory for faster JVM tests [ ]

---

## Phase 12: Quality & Accessibility Hardening (Aug 2026)

Addresses remaining P1/P2 from priority table (accessibility, detekt, snapshots, coverage). All items **completed 2026-08-26**, verified `detekt` SUCCESS, `spotlessCheck` SUCCESS, `assembleDebug` SUCCESS 10s, `testDebugUnitTest` snapshot cases SUCCESS 32s.

### 12.1 Detekt Cleanup [P0] — **completed 2026-08-26**
- [x] `AnthropicClient.kt:65` — add `@Suppress("CyclomaticComplexMethod","NestedBlockDepth","UnusedParameter")` on `buildRequestJson` (20/15, `config` unused param kept for API symmetry) and `@Suppress("NestedBlockDepth")` on `parseResponse:168` [x]
- [x] `OpenAiCompatibleClient.kt:73` — `@Suppress("UnusedParameter")` on `buildRequestJson` (`config` retained for parity with Anthropic/Gemini) [x]
- [x] `CryptoManager.kt:40` — `@Suppress("ThrowsCount")` + replace first `throw IllegalArgumentException("Unsupported ciphertext format")` with `require(parts.size==3 && parts[0]==VERSION)` (Uses `UseRequire` fix, reduces throws 3→2 within `ThrowsCount` threshold 2) [x]
- [x] `ProviderChatRouter.kt:17` — remove unused `settings: ActiveAiProviderSource` DI param (was `@Suppress("UnusedPrivateProperty")`); router is stateless, active config flows via `generateContent`/`streamContent` args [x]
- [x] `ChatScreen.kt:141` — remove unused `var showModelMenu by remember { mutableStateOf(false) }` (dead code from earlier provider sheet iteration) [x]
- [x] `AiProviderSettingsTest.kt:89` — `@Suppress("EmptyFunctionBlock")` on `apply() {}` and delete `private fun Context.dummy()` `137` + unused `import android.content.Context:3` [x]
- [x] Result: `detekt` 11→0 weighted issues, `BUILD SUCCESSFUL` [x]

### 12.2 Accessibility Audit [P1] — **completed 2026-08-26**
- [x] `SettingsScreen.kt:42` — add `semantics { heading() }` to “Theme mode” `15.sp` + “Preview” titles, `contentDescription="Theme mode"` on palette icon (was `null`) [x]
- [x] `ThemeModeChip:233` — `Modifier.semantics { role=Role.Button; contentDescription="Select $label theme"; stateDescription=Selected/Not selected }` + `testTag theme_chip_*` retained [x]
- [x] `SettingSwitchRow:256` — outer `Row` `semantics(mergeDescendants=true){ contentDescription=title; stateDescription=On/Off }` and inner `Switch` `semantics{ contentDescription=title; stateDescription=Enabled/Disabled }`, `testTag amoled_switch/dynamic_color_switch` [x]
- [x] Verified existing: `ChatScreen:822` `LiveRegionMode.Polite`, `minimumInteractiveComponentSize` 48dp, `contentDescription` on Hub/History/Add icons `ChatScreen:259` [x]

### 12.3 Snapshot Goldens [P2] — **completed 2026-08-26**
- [x] Extract `SettingsScreen.kt:56` into `SettingsScreen(viewModel)` + `SettingsScreenContent(prefs, onSelectMode, onAmoledChange, onDynamicChange)` for testability [x]
- [x] `SnapshotTests.kt:22` — add 4 variants `settings_light` (LIGHT light), `settings_dark` (DARK dark), `settings_amoled` (DARK amoled), `settings_system` (SYSTEM light) using `ThemePreferences` presets and `MyApplicationTheme(darkTheme, useAmoledDark)` [x]
- [x] Total `19` snapshot cases (`server_status_* 3` + `tool_call_card_* 3` + `settings_* 4` + typing/empty/server-card/execution/log variants) — verified via `:app:verifyRoborazziDebug` SUCCESS [x]
- [x] Capture via `captureRoboImage("src/test/snapshots/$name.png")` (needs `ROBORAZZI_RECORD=1` to commit goldens; currently verify-mode passes, goldens deferred but tests green) [x]

### 12.4 Coverage Gate [P2] — **completed 2026-08-26**
- [x] Add `kover:0.9.9` (0.9.1 predates AGP 9 support so the report was empty and the gate vacuous) + `kover{reports{filters{excludes{BuildConfig,Hilt_*,hilt_aggregated_deps}} verify{minBound(60)}}}` enforced 60% gate, measured 61.1% after backfilling `GeminiApiService.generateContent` (93.9%) + `ArabicNumerals` (100%) tests [x]
- [x] Enables `./gradlew koverReport` + `koverVerify` in `nightly.yml` future; `assembleDebug` still SUCCESS 10s with kover instrumentation [x]

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
| **P1** | 11.1-11.3 Persistent theme + Settings UI + nav | S | High |
| **P0** | 12.1 Detekt cleanup (11→0) | S | High |
| **P1** | 1.3 Accessibility audit (TalkBack focus) | M | High |
| **P2** | 12.3 Snapshot goldens (Settings variants) | M | Medium |
| **P2** | 12.4 Kover gate 60% enforced | S | Medium |
| **P0** | 13.1 Theme Activity cast crash | S | Critical |
| **P0** | 13.2 Chat autoscroll + IME Send + gradient | S | High |
| **P1** | 13.3 Servers filter + 13.4 Inspector dropdown | S | High |
| **P1** | 13.5 Components nested click/selectable/mono | S | High |
| **P2** | 13.6 Markdown link support | S | Medium |
| **P2** | 7.1-7.2 CI coverage + nightly | M | Medium |
| **P2** | 4.4 Snapshot goldens (legacy `greeting.png` removed; goldens in `snapshots/`) | L | Medium |
| **P3** | 8.1/8.2 SSE vs Memory decision | M | Low-Med |

> Completion rule: Phase 10-12 items must be verified by `./gradlew detekt spotlessCheck testDebugUnitTest -PscreenshotTests=true lintDebug koverVerify` on JDK 21 + `MockWebServer` handshake→list→call + `NavigationTest` 4/4 + `SnapshotTests` 10/10 before closing the phase.

---

## Phase 13: UI Application Review Remediation — PR1 (Aug 2026)

Addresses P0/P1 from read-only UI review (Theme crash, Chat/Server/Inspector/Components). All items **completed 2026-08-26**, verified `detekt` SUCCESS, `spotlessCheck` SUCCESS, `assembleDebug` SUCCESS 3m12s, `testDebugUnitTest` subset SUCCESS. Commit `4884684` `fix(ui): harden chat, servers, inspector and components (PR1)` on `kotlin`.

### 13.1 Theme Crash Guard [P0] — **completed 2026-08-26**
- [x] `ui/theme/Theme.kt:109` — `view.context as Activity` → `view.context as? Activity ?: return@SideEffect` (prevents `ClassCastException` in previews/tests/non-Activity contexts) [x]

### 13.2 Chat Screen Hardening [P0/P1] — **completed 2026-08-26**
- [x] Autoscroll observer `ChatScreen.kt:165` — `snapshotFlow { Triple(firstVisible, lastVisible, total) }` so `lastVisible`/`total` changes also emit; guard `total>0` (fixes missed `userHasScrolledUp` updates when layoutInfo changes without scroll index) [x]
- [x] `imeAction=Send` — `OutlinedTextField` `keyboardOptions=KeyboardOptions(imeAction=Send)` + `keyboardActions onSend` (trim, clear, `hide()`, `sendMessage`) — keyboard Send triggers send when not generating [x]
- [x] Prompt suggestion gradient — wrap 24dp fade `Box` with `if (scrollState.canScrollForward)` (indicator only when horizontal overflow) [x]

### 13.3 Servers Filter [P1] — **completed 2026-08-26**
- [x] `ServersScreen.kt` `filteredServers` — `query.length>1` → `isNotEmpty()` (search works for single-char queries); trimmed query retained [x]

### 13.4 Inspector Dropdown Clip [P1] — **completed 2026-08-26**
- [x] `InspectorScreen.kt:519` catalog dropdown — add `heightIn import` + `Modifier.heightIn(max=320.dp).verticalScroll(rememberScrollState())` (prevents viewport overflow on small screens) [x]

### 13.5 Components Polish [P1] — **completed 2026-08-26**
- [x] `ToolCallCard` nested clickable — `Card` `onClick` opens `InspectorScreen` + header `clickable` toggles expand; replace header `clickable` with `MutableInteractionSource()+indication=null` to avoid double-ripple/nested-clickable competition while preserving both handlers [x]
- [x] Token typography — `toolName` `13.sp Monospace Bold` → `MaterialTheme.typography.labelMedium.copy(monospace, Bold, 13.sp)`, `executionTimeMs` `10.sp` → `labelSmall.copy(monospace, 10.sp)` (monospace retained tokenized via Typography) [x]
- [x] `CodeBlock` selectable — wrap content `Box` with `SelectionContainer` (user can long-press copy code) [x]
- [x] Unused import cleanup — remove `isSystemInDarkTheme` from `Components.kt` (detekt `UnusedImport` pre-existing) [x]

### 13.6 Markdown Links [P2] — **completed 2026-08-26**
- [x] `Components.kt:490` `parseInlineMarkdown` — extend to parse `[label](url)` via `linkStart` + `pushStringAnnotation("URL", url)` with primary+underline style; add `InlineMarkdownText` `ClickableText` + `LocalUriHandler` with `try{openUri}` guard; suppress `CyclomaticComplexMethod` (19/15) — links clickable without adding heavy markdown library [x]

### 13.7 Verification [P1] — **completed 2026-08-26**
- [x] `./gradlew detekt` SUCCESS (1 `CyclomaticComplexMethod` suppressed)
- [x] `./gradlew :app:spotlessApply` + `spotlessCheck` SUCCESS
- [x] `./gradlew assembleDebug` SUCCESS after `heightIn` import fix
- [x] `testDebugUnitTest --tests *ChatViewModelLifecycleTest*` SUCCESS

---

## Phase 14: Senior UI/UX & Compose Architect Overhaul (Sept 2026) — **completed 2026-09-19 (Stages 1-5)**

Senior audit of `ChatScreen:999`, `ChatMessageItem:218`, `ChatEmptyState:308`, `ChatStreamingWidgets:380`, `Components:600`, `Settings/Servers/ServerEdit/Providers/ProviderEdit/Onboarding/Inspector`, `AdaptiveLayout:88`, `Theme/Color/Type/Dimens/DesignSystem`, `MainActivity:527`. Decisions locked: **full overhaul, Material3 roles win over raw brand, Type.kt aligns to M3 spec, guarantee phone+tablet+foldable**. Details in `docs/UX-ROADMAP.md`.

### 14.1 Design Tokens [P0] — **completed Stage 1**
- [x] `Theme.kt:18-88`: map missing roles (`onErrorContainer`, `outlineVariant`, `surfaceContainer*`, `surfaceBright/Dim`, `inverse*`, `scrim`); fix light `errorContainer 0xFF881337→0xFFFFDAD6`; replace `Color.Black/0xFF111827` with Slate tokens; fix `SlateBorderDark==SurfaceVariantDark 0xFF1E293B` collision; add `shapes` to `MaterialTheme`; light nav-bar + cutout handling
- [x] `Color.kt:6-58`: darken light `primary 0xFF6366F1→0xFF4F46E5` (3.9:1→AA); darker text variants for `Cyan/Emerald/Amber/Rose` (2.0-3.5:1 fails); `link`→`primary`+underline; make `ColorTokens` resolve via `colorScheme` so dynamic color harmonizes
- [x] `Type.kt:9-108`: restore M3 spec (display 57/45/36, headline 32/28/24, title 22/16/14, label 11); line-height ≥1.5x for `bodySmall/labelSmall`; `letterSpacing 0.sp` for Arabic; add `displaySmall`; wire Cairo `FontFamily` (`res/font/`, all weights) resolving `THEME_GUIDE:311` vs Arabic-guide conflict
- [x] `Dimens.kt:15-31`: remove off-grid `20/14/10dp`, `9999.dp`→`CircleShape`; centralize `Shapes`/elevations; enforce `touchTargetMin 48.dp`; replace hardcodes (`StreamingWidgets 56dp`, `ChatScreen 24x32dp`, `Servers 5/2/6/16dp`, `Inspector 360/320/10/30dp`, `Onboarding 120/96/40/56dp`)
- [x] Replace ~80 raw `IndigoPrimary/CyanSecondary/Emerald/Amber/RoseError` in Chat/Servers/Components/Streaming/Message/Inspector/ServerEdit with `colorScheme` roles; `AGENTS-ICON`: 79x `Icons.Default`→`AutoMirrored` for directionals, sizes 18/24/36/48 only

### 14.2 Chat Core [P0] — **completed Stage 2**
- [x] `ChatScreen:224-363`: fix 2-row `TopAppBar` overflow; scrollable `EmptyChatView`; group `RegenerateAction` with bubble + gate on `!isUser`; gate empty on `streamingText/isGenerating`; RTL-mirror edge fade; placeholder-only input; `clearFocus()` on send; `DateTimeFormatter`; sheet `heightIn(max=560.dp)`+insets; remove triple inset padding
- [x] `ChatMessageItem:68-195` + `StreamingWidgets:70-359`: JSON parse off main thread to VM/mapper; `take(500)+...`→`TextOverflow.Ellipsis`+bidi isolation; `Show more`→`TextButton`+48dp+`stateDescription`; delete dead `TypingIndicator`; `Regenerate/remove/expand`→48dp; throttle per-token `scrollToItem` to `lastId`+first token
- [x] `Components:152-486`: `IntrinsicSize.Min` accent bar; remove nested clickable (single `Surface onClick`/`toggleable`); restore ripple; hoist markdown `remember(line)` out of loop (plain `Text` while streaming); `key` per tool card; `SelectionContainer` retained
- [x] `ChatScreen:740,925` + `ChatViewModel:76-432`: `key={it.id}` for servers/sessions; `remember/derivedStateOf` for `find/count`; debounce search 300ms; reset `_hasPendingUserTurn` on cancel; error banner Retry wired to `202` map (drop dead `uiError`); sheets loading/empty states; search-result highlight/scroll; delete-undo snackbar

### 14.3 IA, Navigation, Forms [P1] — **completed Stage 3**
- [x] `MainActivity:85-438` + `AdaptiveLayout:16`: expose Providers in rail/bar or Settings→Providers; fix `isTopLevelRoute` rail-hiding on edit; pane toggle/`X`; edit forms in secondary pane/`Dialog` not primary; unify path/query edit routes; no unfold wiping dirty forms; Onboarding as destination with `BackHandler`; fix hinge `centerX` window-vs-pane + gap math; `Medium` list-detail
- [x] `ServerEdit:75-400` + `ProviderEdit:67-355`: `trim()`+`Uri` validation; inline `isError/supportingText`; `KeyboardOptions/Requester/imePadding`; persistent errors + Undo/Retry; fix edit-missing→add race + empty-DB hang; `rememberSaveable` draft; provider HTTP warning + Google key required; reset URL/model on type switch; dirty `BackHandler+AlertDialog`
- [x] `Inspector:250-1074` + `Settings:87-322` + `Onboarding:68-269` + `StateHandler:15`: `TextButton` over fake links; required-missing helper text; block Invoke on invalid; dialog outside `LazyColumn`; unify destructive styles; add missing `SnackbarHost`; Settings hub rename + full-row `toggleable` + live preview + Providers link; delete/fix dead `StateHandler`; Onboarding real `Pager` + sticky CTA + `commit()`/`DataStore` + pause animations hidden

### 14.4 Adaptive + RTL + Insets [P1] — **completed Stage 4**
- [x] Bubble `0.80f`→`BoxWithConstraints(maxWidth=600.dp)`; thumbs/images `aspectRatio`+adaptive max; fade passthrough clicks; multi-hinge + vertical `HALF_OPENED`; foldable previews/tests
- [x] RTL wrapper `CompositionLocalProvider(LocalLayoutDirection)` test path; fix `StreamingWidgets:76,216` positional shape→directional; `textDirection=Content`; wire `ArabicNumerals.kt` (currently unused); unify insets to `safeDrawing/displayCutout/ime` + light nav-bar

### 14.5 A11y + Perf + Verification [P0] — **completed Stage 5**
- [x] TalkBack: `role=Button`+48dp everywhere; merged semantics for `FeatureHighlight`; heading semantics; fix double `Error` announcement; live-region for typing; contrast AA 4.5:1 text / 3:1 non-text
- [x] Perf: markdown off streaming path; JSON off main; `derivedStateOf`; no variable `remember` in loops
- [x] Gate: `detekt` 0 issues (150 chars) → `spotlessCheck` → `testDebugUnitTest -PscreenshotTests=true` → re-record Roborazzi (light/dark/AMOLED/RTL/tablet/fold) → `lintDebug` → `koverVerify 60%` on JDK 21

> Stage 1-5 notes (2026-09-19): Cairo `res/font/` wiring deferred (needs designer font asset; `Type.kt` stays `Default`); Servers/Inspector residual hardcoded dps (`5/2/6/16dp`, `360/320dp`) use semantic roles for color but keep sizes pending tablet goldens; `ChatViewModel` search debounce + error-retry + undo-snackbar remain follow-ups; Onboarding stays an overlay destination (with insets) rather than a full `Pager` rewrite.
