---
name: project-conventions
description: MCP AI Chat architecture, Hilt/DI, state, testing, and commit rules. Use when adding features, ViewModels, repositories, Room entities, or unit tests in this repo.
---

# Project Conventions

Single `app/` module: Kotlin + Compose + Hilt + Room. Layout: `data/{local,model,network,repository,security,tool}`, `di/`, `ui/{screens,theme,viewmodel}`.

## Architecture and state

- Provider routing: `ProviderChatRouter` dispatches to `GeminiApiService` / `AnthropicClient` / `OpenAiCompatibleClient` by `AiProviderType`; all normalize to `GeminiResponse`.
- MCP flow: initialize → tools/list → tools/call, with circuit breaker + retry (`Resilience.kt`). Timeouts: MCP 15s/30s (`provideMcpOkHttpClient`), AI 60s (`provideOkHttpClient`). Per-server timeouts are WONTFIX.
- `@HiltViewModel` on ViewModels, `@Inject` constructors on repositories. All `StateFlow`s use `SharingStarted.WhileSubscribed(5000)`. `ChatViewModel.followPersistedSession` guards against snapping to stale sessions.
- `AiProviderSettings` methods are `@Synchronized` — call directly in tests, no scope needed. It accepts `prefsFactory` for in-memory JVM tests without Robolectric.

## Testing

- `mockk(relaxed = true)` for repos/services in ViewModel tests. `StandardTestDispatcher` + `advanceUntilIdle()`; `runTest` for suspend functions.
- `MockWebServer` used directly (`okhttp3.mockwebserver.MockWebServer`), never mocked. `org.json` is a test dependency.
- `isReturnDefaultValues = true`: `android.jar` calls like `Log.d` return defaults — don't mock `Log`.
- Hilt-dependent tests use `hilt.android.testing.HiltAndroidTest`.

## Style and commits

- No code comments unless explaining *why* something non-obvious. 150-char max lines.
- Before committing: inspect `git status`, `git diff`, `git log --oneline -10`; stage only intended files; never commit secrets (`.env`, `*.jks` are ignored — verify). Concise message matching repo style (`feat(ui): …`, `fix(ui): …`).
- Never update git config, skip hooks, use `-i`, force-push, or create empty commits unless explicitly asked. If hooks reject, fix and commit anew — don't amend the failed commit.
