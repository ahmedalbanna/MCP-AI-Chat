---
name: verify-gates
description: Runs the MCP AI Chat quality gates in the required order (detekt, spotless, unit tests, lint, kover). Use ONLY when verifying a build, running checks, or before committing in this Android project.
---

# Verify Gates

Run gates in this order. Stop at the first failure.

## Prerequisites

- JDK 21 (Temurin).
- `.env` must exist: `cp .env.example .env` (secrets plugin fails without it).
- Configuration cache is on; if disk fills, add `--no-configuration-cache`.
- `app/build` can exceed 100GB after test runs; `./gradlew clean` periodically.

## Gate order

| # | Command |
|---|---------|
| 1 | `./gradlew detekt` (0 issues allowed, `maxIssues = 0`) |
| 2 | `./gradlew spotlessCheck` (fix with `spotlessApply`) |
| 3 | `./gradlew testDebugUnitTest` (add `-PscreenshotTests=true` to include Roborazzi snapshots) |
| 4 | `./gradlew lintDebug` |
| 5 | `./gradlew koverVerify` (60% minBound) |

Other useful commands: `./gradlew assembleDebug`, `:app:recordRoborazziDebug -PscreenshotTests=true` (re-record goldens after any theme/typography change), `testDebugUnitTest --tests "<Pattern>"` for a subset.

## Rules

- Detekt: 150-char max line, complexity 15, LongMethod 80. Prefer `@Suppress` on generated/complex parsers over refactors that hurt readability (see `AnthropicClient.buildRequestJson`).
- Snapshot tests are excluded unless `-PscreenshotTests=true` is passed.
- Never commit with failing gates; never skip hooks or force-push.
