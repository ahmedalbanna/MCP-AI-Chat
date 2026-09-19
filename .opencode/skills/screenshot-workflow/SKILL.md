---
name: screenshot-workflow
description: Manages Roborazzi snapshot goldens (record, verify, opt-in flag). Use ONLY when UI changes, goldens mismatch, or snapshot tests need re-recording.
---

# Screenshot Workflow

Roborazzi `1.59.0` (`app/build.gradle.kts`). Goldens live under `app/src/test/snapshots/`.

## Key fact

Plain `./gradlew testDebugUnitTest` runs snapshot tests **without comparing** (no-op captures). Pixel comparison only engages via the verify/record tasks below. Robolectric renders real pixels, so these are the slowest tasks in the suite.

## Commands (JDK 21, add `--no-configuration-cache` if disk fills)

```bash
./gradlew testDebugUnitTest                                   # fast; skips comparison
./gradlew testDebugUnitTest -PscreenshotTests=true           # includes snapshot cases, still no compare
./gradlew :app:verifyRoborazziDebug -PscreenshotTests=true   # real snapshot gate
./gradlew :app:recordRoborazziDebug -PscreenshotTests=true   # re-record goldens after UI changes
```

## Rules

- Re-record after **any** theme/typography/spacing change (tokens shift every golden).
- Commit goldens with the UI change; CI runs `-PscreenshotTests=true`.
- Snapshot coverage lives in `SnapshotTests.kt` (wired via `RoborazziRule`): server badges, tool cards, settings variants, typing, empty chat, server cards, execution results, log items.
- `app/build` can exceed 100GB after test runs; `./gradlew clean` periodically.
