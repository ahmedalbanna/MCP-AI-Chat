# Productization Roadmaps — v1.1.0 → Public Play Listing

Target: **Public Play listing**, SDK-free (no Firebase/Sentry) to preserve the current privacy posture.

| Phase | Document | Effort | Blocker? |
|-------|----------|--------|----------|
| 15 | [Release build hardening](15-release-build.md) — AAB, 16KB, log hygiene, R8 mapping, version discipline | S | Yes — upload requires AAB |
| 16 | [Privacy, legal, data safety](16-privacy-data-safety.md) — policy URL, Data Safety form, LICENSE, cleartext justification | S | Yes — policy URL mandatory |
| 17 | [Store listing assets](17-store-listing.md) — fastlane metadata, graphics, screenshots, rating | S | Yes — listing incomplete without |
| 18 | [Release CI + versioning](18-release-pipeline.md) — tag-triggered signed AAB, mapping upload, CHANGELOG discipline | S | No, but required for repeatability |
| 19 | [Post-launch ops](19-post-launch.md) — deps, i18n, docs dedupe, in-app updates, crash SDK decision | M | No — after first listing |

## Execution order

```
15 (AAB + logs + mapping) → 16 (privacy + safety + license) → 17 (listing assets)
  → 18 (release.yml + tag v1.1.0) → Play Internal → Production → 19 (post-launch)
```

## Baseline (Sept 2026, verified read-only)

- `app/build.gradle.kts:23-28`: `applicationId com.aistudio.mcpchat`, `versionCode 2`, `versionName 1.1.0`, `minSdk 24`, `targetSdk 36` — compliant with the Aug-2026 36 mandate.
- Signing exists `app/build.gradle.kts:33-57` (`KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD`, `my-upload-key.jks` gitignored) + `isMinifyEnabled=true` + `app/proguard-rules.pro:1-34`.
- Manifest `app/src/main/AndroidManifest.xml:1-33`: only `INTERNET` + `ACCESS_NETWORK_STATE`, `supportsRtl=true`, adaptive icons present, backup exclusions correct.
- Quality gates green per `ROADMAP.md`: detekt 0, spotless, 450+ tests, 23 Roborazzi goldens, Kover 60%, gitleaks.

## Conventions

- Each phase doc lists **evidence** (`file:line`), **tasks** (`[ ]`), **acceptance gate**, and **out of scope**.
- Do not widen scope mid-phase; push follow-ups to Phase 19.
- Quality gate for any code change: `./gradlew detekt spotlessCheck testDebugUnitTest -PscreenshotTests=true lintDebug koverVerify` on JDK 21.
