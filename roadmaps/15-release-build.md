# Phase 15 — Release Build Hardening (AAB, 16KB, Logs, R8 Mapping)

Goal: produce an upload-ready, signed **AAB** with release-safe logging and a repeatable version story.

## Evidence

- Versioning: `app/build.gradle.kts:23-28` (`versionCode 2`, `versionName 1.1.0`, `minSdk 24`, `targetSdk 36`).
- Signing: `app/build.gradle.kts:33-57` (env `KEYSTORE_PATH` default `rootDir/my-upload-key.jks`, alias `upload`); keystore gitignored; CI never signs today.
- R8: `app/build.gradle.kts:50-56` (`isMinifyEnabled=true`, `proguard-android-optimize.txt` + `app/proguard-rules.pro:1-34` covering OkHttp/Room/Coroutines/Compose/Hilt/Tink-dontwarn). No `assumenosideeffects` for `Log`.
- Logs ship in release — 41 hits, incl. `data/network/McpHttpClient.kt:198,556,559` (`Log.d`), `data/network/Resilience.kt:65` (`Log.d`), `data/network/GeminiApiService.kt:37,71,80`, `data/network/AnthropicClient.kt:55`, `data/network/OpenAiCompatibleClient.kt:56,64` (`Log.e` with code + body). No `Timber`/`isLoggable`/`BuildConfig.DEBUG` guard.
- Native libs: none seen; AGP `gradle/libs.versions.toml:2` `9.1.1` (>= 8.5.1) satisfies the 16KB page-alignment mandate for `targetSdk 35+` — still must verify the AAB.

## Tasks

### 15.1 Verify signed AAB [S]
- [ ] Run `KEYSTORE_PATH=... STORE_PASSWORD=... KEY_PASSWORD=... ./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- [ ] Confirm `assembleRelease` APK still green (R8 path already claimed in `ROADMAP.md` Phase 6.4).
- [ ] Record AAB size + `versionCode`/`versionName` in the release notes draft.

### 15.2 16KB page-alignment + 64-bit check [S]
- [ ] Run Play's `CHECK_ELF_ALIGNMENT` / `zipalign -c` equivalent on the AAB (expect trivial pass — no `.so`).
- [ ] Save the check output next to the AAB artifact; fail the phase if unaligned ELFs appear.

### 15.3 Log hygiene, SDK-free [S]
- [ ] Add to `app/proguard-rules.pro`: `-assumenosideeffects class android.util.Log { *; }` so `Log.d/v/i` (and `e/w` if desired) strip in release. Preferred: single rule over touching 41 call sites.
- [ ] Alternatively guard hot paths behind `if (BuildConfig.DEBUG)`: `McpHttpClient.kt:198,556,559`, `Resilience.kt:31,65,84`, `GeminiApiService.kt:37,71,80,194,315`.
- [ ] Audit that no API key, bearer token, or full request/response body is logged at `e/w` level in release (redact `responseBody` in `GeminiApiService.kt:80`, `AnthropicClient.kt:55`, `OpenAiCompatibleClient.kt:56` if it can carry keys).
- [ ] No new logging framework (no Timber) — keeps the SDK-free posture.

### 15.4 R8 mapping plan [S]
- [ ] Document: upload `app/build/outputs/mapping/release/mapping.txt` to Play Console on every release for deobfuscation.
- [ ] Keep `-keepattributes SourceFile,LineNumberTable` (`app/proguard-rules.pro:4-5`) so stack traces stay useful.

### 15.5 Version discipline [S]
- [ ] Freeze `versionCode 2` / `versionName 1.1.0` for first upload.
- [ ] Rule going forward: bump `versionCode` (+1 integer) on **every** Play release; `versionName` follows semver; record both in `CHANGELOG.md` + Git tag (see Phase 18).

## Acceptance gate

- [ ] Signed `app-release.aab` builds from clean checkout with env keystore.
- [ ] 16KB/64-bit check output saved and passing.
- [ ] Release AAB contains no `Log.d` output (verify via `dexdump` spot-check or R8 rule + manual smoke on release build).
- [ ] `./gradlew detekt spotlessCheck testDebugUnitTest -PscreenshotTests=true lintDebug koverVerify` green on JDK 21.

## Out of scope

- Crash/analytics SDKs, dependency bumps, i18n — pushed to Phase 19.
