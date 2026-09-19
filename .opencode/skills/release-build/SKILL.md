---
name: release-build
description: Builds the signed release APK/AAB with R8, keystore env passwords, and apksigner verification. Use ONLY when cutting a release, signing, or checking release outputs.
---

# Release Build

## Prerequisites

- JDK 21. `.env` must exist (`cp .env.example .env`).
- Keystore at repo root `my-upload-key.jks` (gitignored, alias `upload`). Signing config: `app/build.gradle.kts:33-57`.
- Passwords come from env: `STORE_PASSWORD`, `KEY_PASSWORD`. Never commit or log them.
- `KEYSTORE_PATH` must be an **absolute path**: relative paths resolve against `app/`, not the repo root, and fail `validateSigningRelease`.

## Build

```bash
KEYSTORE_PATH=/content/MCP-AI-Chat/my-upload-key.jks STORE_PASSWORD='...' KEY_PASSWORD='...' ./gradlew assembleRelease --no-configuration-cache
# AAB: ... ./gradlew bundleRelease ...
```

- R8 minify is on (`isMinifyEnabled=true`); the build takes ~15-20 min on a slow box, mostly in `minifyReleaseWithR8`. Run detached (`setsid … & disown`) and poll the log.
- Outputs: `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/bundle/release/app-release.aab`.
- Current: `versionCode 2`, `versionName 1.1.0` (`app/build.gradle.kts`).

## Verify

```bash
/opt/android-sdk/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Expect v1+v2 signature lines and the upload cert DN/SHA-256.

## New keystore (only if unpublished or rotating)

```bash
keytool -genkeypair -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storepass '...' -keypass '...' -dname "CN=MCP AI Chat, OU=Dev, O=MCPChat, L=Unknown, ST=Unknown, C=US"
```

Back up the old file first. A new key changes the signing identity and cannot update a Play listing signed with the old key.
