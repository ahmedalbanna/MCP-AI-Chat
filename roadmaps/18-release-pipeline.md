# Phase 18 — Release CI + Versioning

Goal: make signed releases repeatable — tag in, AAB + mapping out — without ever exposing the upload key.

## Evidence

- Existing workflows (`.github/workflows/`): `ci.yml` (detekt, spotless, `testDebugUnitTest -PscreenshotTests=true`, `DatabaseMigrationTest`, `lintDebug`, gitleaks), `pr-checks.yml` (+ `assembleDebug`), `nightly.yml` (+ `koverVerify`, artifact retention 14d). None builds `assembleRelease`/`bundleRelease` or touches signing.
- Signing inputs: `app/build.gradle.kts:33-57` (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`, alias `upload`). `my-upload-key.jks` gitignored — correct, must stay that way.
- Versioning: hardcoded `versionCode 2` / `versionName 1.1.0` (`app/build.gradle.kts:27-28`); `CHANGELOG.md:1-13` hand-edited (1.1.0 + 1.0 only); no `release.yml`, no tag automation.
- Env: CI copies `.env.example → .env` (`ci.yml:30-31`); placeholder `MY_GEMINI_API_KEY` ignored at runtime — safe for release builds.

## Tasks

### 18.1 Release workflow [S]
- [ ] Add `.github/workflows/release.yml`: trigger on tag `v*` + manual `workflow_dispatch`.
- [ ] Steps: checkout (`fetch-depth: 0`) → JDK 21 (Temurin) → Gradle setup → `cp .env.example .env` → `detekt` → `spotlessCheck` → `testDebugUnitTest -PscreenshotTests=true` → `DatabaseMigrationTest` → `lintDebug` → `koverVerify` → decode keystore from `PLAY_UPLOAD_KEYSTORE_B64` secret to file → `bundleRelease` (+ `assembleRelease` smoke) with `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` from secrets.
- [ ] Upload artifacts: `app/build/outputs/bundle/release/app-release.aab` + `app/build/outputs/mapping/release/mapping.txt` (+ `app-release.apk` for sideload testing), retention ≥ 90d.
- [ ] Optional (only after service-account key exists): Play Internal upload step (e.g. `r0adkll/upload-google-play`) gated to tags; keep manual-promote-to-production.

### 18.2 Secrets + key safety [S]
- [ ] Required repo secrets: `PLAY_UPLOAD_KEYSTORE_B64`, `PLAY_KEYSTORE_PASSWORD`, `PLAY_KEY_PASSWORD` (alias `upload` documented). Never log them; mask in workflow.
- [ ] Document Play App Signing enrollment (upload key registered once in Play Console; Play manages app-signing key; rotation procedure linked).
- [ ] Fail closed: workflow errors if any signing secret is missing (no unsigned "release" artifact).

### 18.3 Version + notes discipline [S]
- [ ] Rule: every Play release bumps `versionCode` (+1) and sets `versionName` (semver); tag `v<versionName>` (e.g. `v1.1.0` → `versionCode 2` for first upload).
- [ ] Require a `CHANGELOG.md` entry per release (Keep-a-Changelog style: Added/Fixed/Security); generate the GitHub Release from the tag + changelog section.
- [ ] Add a PR checklist item: "version bumped + changelog updated?" for any `release/*` branch.

## Acceptance gate

- [ ] Pushing tag `v1.1.0` produces a signed AAB + `mapping.txt` artifact with zero manual Gradle invocation.
- [ ] Secrets absent from logs; unsigned fallback impossible.
- [ ] `CHANGELOG.md` + GitHub Release match `versionCode 2` / `1.1.0`.

## Out of scope

- Auto-bump bots, staged-rollout automation, dependency updates (Phase 19).
