# Phase 19 — Post-Launch Ops (Deferred, Non-Blocking)

Goal: park everything that must **not** block the v1.1.0 listing, with a clear re-entry order once the app is live on an Internal track or Production.

## Evidence / debt register

- Deps stale (`gradle/libs.versions.toml`): `composeBom 2025.01.01` (~20mo old), `okhttp 4.10.0` (behind 4.12/5.x), `googleDevtoolsKsp 2.3.5` mismatched vs `kotlin 2.2.10` (KSP must track Kotlin), `roborazzi 1.59.0`, `detekt 1.23.8`; no Dependabot/Renovate in `.github/`.
- i18n: only `app/src/main/res/values/strings.xml` (`app_name`); no `values-ar/`; UI strings hardcoded EN despite `supportsRtl=true` (`AndroidManifest.xml:17`) + `docs/ARABIC_UI_GUIDE.md` + `ArabicNumerals` util. Violates own `docs/AGENTS.md` `stringResource` rule — accepted for EN-only v1.1.0.
- Docs duplication: `docs/` ~23k lines (`docs/AGENTS.md:4138`, `AGENTS-BOTTOMSHEET&DRAWER.md:4359`, `AGENTS-BOTTOMBAR.md:3344`, `ARABIC_UI_GUIDE.md:1059`) vs root `AGENTS.md:88`; RTL/AutoMirrored tables triplicated; `docs/UX-ROADMAP.md:43` gate copy stale.
- No crash/analytics SDK (intentional SDK-free): no `crashlytics`/`sentry`/`firebase` in `*.kts` or `*.kt`; `proguard-rules.pro:12` Firebase block removed per `ROADMAP.md:123,227`.
- No in-app updates: no `play:app-update` in `*.kts`/`*.kt`.
- Coverage: Kover 60% gate (`app/build.gradle.kts:169-191`, `nightly.yml:52-53`); roadmap claims 61.1% — thin margin, brittle to new code.

## Tasks (in re-entry order)

### 19.1 Dependency + toolchain refresh [M]
- [ ] Add Dependabot/Renovate for Gradle + AGP.
- [ ] Bump `composeBom`, `okhttp` (4.12+), align KSP ↔ Kotlin, then `roborazzi`, `detekt`, `coil 2.7.0`, `room 2.7.0` as needed — one bump PR per group with full gate.
- [ ] Re-run 16KB check after any native dep appears.

### 19.2 i18n + RTL completion [M]
- [ ] Extract hardcoded EN strings to `values/strings.xml` first (satisfy `stringResource` rule).
- [ ] Then add `values-ar/` + RTL QA (AutoMirrored directionals, `textDirection=Content`, Eastern Arabic numerals via `ArabicNumerals` where appropriate per `ARABIC_UI_GUIDE.md`).
- [ ] Refresh store screenshots per locale once Arabic ships.

### 19.3 Observability decision [M]
- [ ] Decide: stay SDK-free (privacy differentiator; rely on Play vitals + user reports) vs add Sentry self-hosted vs Firebase Crashlytics. Do not add silently — each changes the privacy policy + Data Safety form (Phase 16).
- [ ] If added: update `PRIVACY.md`, Data Safety, `proguard-rules.pro`, and `release.yml` (mapping upload becomes mandatory, not advisory).

### 19.4 Update mechanism + support loop [S]
- [ ] Evaluate Play in-app updates (`play:app-update`) flexible vs immediate for post-launch fixes.
- [ ] Stand up support triage: Play reviews → GitHub issues template, ANR/vitals dashboard review cadence, hotfix branch (`hotfix/*`) + expedited `release.yml` run.

### 19.5 Docs + coverage health [S]
- [ ] Dedupe `docs/` (single canonical theming/RTL/bottom-bar sources; fix `THEME_GUIDE` spec-vs-code drift; refresh `UX-ROADMAP.md` gate).
- [ ] Raise Kover margin above 60% (target 70%) before major feature work; keep `total{minBound(0)}` vacuous rule honest or remove it.

## Acceptance gate (per re-entry)

- [ ] Each 19.x item ships as its own PR with the standard gate (`detekt → spotlessCheck → test -PscreenshotTests=true → lintDebug → koverVerify`, JDK 21) + updated `CHANGELOG.md` + privacy/data-safety review if SDKs or data handling changed.

## Explicitly not in v1.1.0

- No new SDKs, no locale expansion, no docs rewrite, no auto-update enforcement — launch first, harden after traction.
