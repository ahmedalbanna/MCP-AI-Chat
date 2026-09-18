# AGENTS.md — MCP AI Chat

Android app (Kotlin + Jetpack Compose + Hilt + Room) with a single `app/` module.

## Build & Test Commands

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK | `KEYSTORE_PATH=... STORE_PASSWORD=... KEY_PASSWORD=... ./gradlew assembleRelease` |
| Run unit tests (fast, skips snapshots) | `./gradlew testDebugUnitTest` |
| Run unit tests incl. snapshots | `./gradlew testDebugUnitTest -PscreenshotTests=true` |
| Record Roborazzi goldens | `./gradlew :app:recordRoborazziDebug -PscreenshotTests=true` |
| Detekt static analysis (0 issues allowed) | `./gradlew detekt` |
| Spotless / ktlint formatting check | `./gradlew spotlessCheck` |
| Apply formatting | `./gradlew spotlessApply` |
| Lint | `./gradlew lintDebug` |
| Coverage gate (60% minBound) | `./gradlew koverVerify` |

**Order matters**: `detekt` → `spotlessCheck` → `testDebugUnitTest`.

## Prerequisites

- **JDK 21** (Temurin).
- **`.env` file**: Copy `.env.example` to `.env` before building (`cp .env.example .env`). Without it the secrets Gradle plugin fails. The key is optional in-app; `GEMINI_API_KEY=MY_GEMINI_API_KEY` placeholder is ignored at runtime.
- **Keystore** for release: `my-upload-key.jks` (gitignored; generate with `keytool`).
- **Configuration cache**: Enabled in `gradle.properties`. If disk fills, pass `--no-configuration-cache`.
- **Disk space**: `app/build` can reach 100+GB after test runs. Run `./gradlew clean` periodically.

## Project Layout

```
app/src/main/java/com/aistudio/mcpchat/
├── data/
│   ├── local/          # Room entities, DAOs, AppDatabase
│   ├── model/          # AiProviders presets, GeminiModels, McpModels
│   ├── network/        # McpHttpClient, ProviderChatRouter, GeminiApiService,
│   │   │               #   AnthropicClient, OpenAiCompatibleClient, Resilience
│   │   └── OkHttpExtensions.kt  # suspend Call.await() (cancellable HTTP)
│   ├── repository/      # ChatRepository (tool-execution loop), McpRepository
│   ├── security/        # AiProviderSettings, CryptoManager
│   └── tool/            # Built-in tools, LocalFallbackHandler
├── di/                  # Hilt AppModule (timeout config, client injection)
├── ui/
│   ├── screens/         # Feature screens (ChatScreen + ChatMessageItem/ChatEmptyState/ChatStreamingWidgets, etc.)
│   ├── theme/           # Material 3 theme
│   └── viewmodel/       # ViewModels (ChatViewModel, etc.)
├── McpChatApplication.kt
└── MainActivity.kt
app/src/test/              # JVM unit + Robolectric tests
  ├── assets/            # Room schemas (KSP-generated at build)
  └── snapshots/         # Roborazzi golden PNGs
app/src/androidTest/       # Instrumented tests
```

## Testing Conventions

- **MockK**: Use `mockk(relaxed = true)` for repositories/services in ViewModel tests.
- **Coroutines**: `StandardTestDispatcher` + `advanceUntilIdle()` for ViewModels; `runTest` for suspending functions.
- **MockWebServer**: Use `okhttp3.mockwebserver.MockWebServer` directly (not MockK for the server).
- **`org.json`** (`json:20240303`) is a test dependency — `JSONObject`/`JSONArray` available in JVM tests.
- **`isReturnDefaultValues = true`** (`app/build.gradle.kts:72`): android.jar methods like `Log.d` return defaults instead of throwing. Don't mock `Log`.
- **`@Synchronized`** on `AiProviderSettings` methods — tests can call them directly, no coroutine scope needed.
- **StateFlow pattern**: All ViewModel StateFlows use `SharingStarted.WhileSubscribed(5000)`.
- Snapshot tests excluded unless `-PscreenshotTests=true` is passed.
- `AiProviderSettings` accepts a `prefsFactory: (Context) -> SharedPreferences` for in-memory JVM testing without Robolectric.

## Architecture Notes

- **Provider routing**: `ProviderChatRouter` dispatches to `GeminiApiService`, `AnthropicClient`, or `OpenAiCompatibleClient` based on `AiProviderType`. All normalize to `GeminiResponse`.
- **MCP flow**: Initialize → tools/list → tools/call, with circuit breaker + retry (`RetryManager` in `Resilience.kt`).
- **Timeouts**: MCP = 15s connect / 30s read-write (`AppModule#provideMcpOkHttpClient`). AI providers = 60s (`provideOkHttpClient`). Per-server timeouts are WONTFIX (documented in ROADMAP).
- **Circuit breaker**: `CircuitBreaker.canExecute()` gates `McpHttpClient` calls. `shouldSkipDueToCircuit` wraps it for readability.
- **Session management**: `ChatViewModel.followPersistedSession` guards against snapping back to a stale session when the user has navigated elsewhere mid-request.
- **Hilt**: `@HiltViewModel` on ViewModels. `@Inject` constructors for repositories. Use `hilt.android.testing.HiltAndroidTest` for Hilt-dependent tests.

## Code Style

- **Line length**: 150 chars max (spotless + detekt `MaxLineLength` in `detekt-config.yml`).
- **Detekt**: `maxIssues = 0` (any violation fails build). Cyclomatic complexity threshold 15; LongMethod threshold 80.
- Use `@Suppress("CyclomaticComplexMethod", "NestedBlockDepth")` on complex request-building methods rather than refactoring (see `AnthropicClient.buildRequestJson`).
- **No code comments** unless explaining *why* something is non-obvious.

## CI Workflows

- `ci.yml` — PR checks: detekt, spotless, tests (with screenshots), Room migration tests, lint, gitleaks.
- `nightly.yml` — Daily: same checks + `koverVerify` coverage gate (60%) + artifact upload.
- Branch conventions: PRs target `main` or `kotlin`.
