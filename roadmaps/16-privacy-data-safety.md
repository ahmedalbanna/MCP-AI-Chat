# Phase 16 — Privacy, Legal, Data Safety (Play Blocker)

Goal: satisfy Play's mandatory privacy-policy URL + Data Safety form with a defensible, evidence-backed disclosure. No code behavior changes expected.

## Evidence

- Secrets at rest: `data/security/AiProviderSettings.kt` (`EncryptedSharedPreferences`, `MasterKey.AES256_GCM`); `data/tool/../security/CryptoManager.kt` encrypts MCP `authHeader` at rest.
- Backup exclusions: `app/src/main/res/xml/data_extraction_rules.xml:13-25` + `app/src/main/res/xml/backup_rules.xml:13-18` exclude `mcp_chat_database{,-wal,-shm}` + `sharedpref/ai_provider_prefs`; onboarding pref (`mcp_chat_onboarding`) intentionally allowed.
- Network: `app/src/main/AndroidManifest.xml:5-6` (`INTERNET`, `ACCESS_NETWORK_STATE` only); `app/src/main/res/xml/network_security_config.xml:11` (`cleartextTrafficPermitted=true` app-wide for LAN runtimes like Ollama/vLLM + `http://localhost:4096` OpenCode preset); UI flags non-loopback `http` via `McpRepository.isInsecureEndpoint` (warns, still allows).
- Key injection: `.env.example:7` (`GEMINI_API_KEY=MY_GEMINI_API_KEY` placeholder, ignored at runtime); secrets plugin `app/build.gradle.kts:88-93`; real keys entered in-app and stored encrypted.
- Missing: no `PRIVACY.md`, no hosted policy URL, no `LICENSE`, no Data Safety draft, no `fastlane/` metadata.

## Tasks

### 16.1 Privacy policy [S — mandatory URL]
- [ ] Add `PRIVACY.md` at repo root, then publish to a hosted URL (GitHub Pages / site) — Play requires a public link, not the markdown alone.
- [ ] Disclose, in plain language:
  - What is collected: user-provided AI provider keys, MCP server URLs + auth headers, chat history, tool-call audit logs, diagnostic state (latency, connection status).
  - Where it lives: on-device Room DB + `EncryptedSharedPreferences` (AndroidKeyStore); DB + `ai_provider_prefs` excluded from cloud backup/device transfer (cite both XMLs above).
  - Where it goes: only to user-configured AI endpoints (Gemini/Anthropic/OpenAI-compatible) and user-configured MCP servers; no sale, no third-party SDK sharing (SDK-free build).
  - Network: `https` by default; plain `http` only when the user explicitly configures a LAN/local endpoint (Ollama/vLLM/localhost); non-loopback `http` flagged insecure in-app.
  - User control: delete servers/chats in-app; Clear app data removes everything; backup restore does not resurrect secrets.
  - Contact email for privacy requests + data-deletion procedure.

### 16.2 Data Safety form draft [S]
- [ ] Draft answers in-repo (e.g. `docs/play-data-safety.md` or inside Phase 17 metadata) so re-submits are repeatable:
  - No location, contacts, photos, files beyond user-pasted chat/attachments.
  - Collected: credentials (app functionality, user-provided, ephemeral to user endpoints), chat + app activity (functionality), diagnostics (functionality).
  - Encrypted in transit (`https`; user-opted LAN `http` disclosed) + at rest (EncryptedSharedPreferences/AndroidKeyStore, Room excluded from backup).
  - Sharing: no third-party sharing except the user's own chosen AI/MCP endpoints; no advertising.
- [ ] Reconcile with the cleartext justification below before submitting.

### 16.3 Cleartext justification [S]
- [ ] Prepare review note: app-wide cleartext exists solely for user-configured LAN/local runtimes; remote use over `http` is flagged in-app (`isInsecureEndpoint`); prefer `https` (already documented in `README.md`).
- [ ] Do not narrow `network_security_config.xml` now — that would break the LAN use-case; revisit post-launch if Play pushes back (scoped per-domain config).

### 16.4 License [S]
- [ ] Add `LICENSE` (choose: MIT / Apache-2.0 for permissive; GPL-3.0 if copyleft intended). Repo currently has none — Play + GitHub both surface this.
- [ ] If closed-source, state "All rights reserved" in the listing instead and skip `LICENSE`.

## Acceptance gate

- [ ] `PRIVACY.md` merged + publicly hosted URL live (link returns 200).
- [ ] Data Safety draft reviewed against actual permissions (`INTERNET`, `ACCESS_NETWORK_STATE` only) and backup rules.
- [ ] `LICENSE` decision recorded (file added or explicitly closed-source).

## Out of scope

- Code changes to backup rules or network config (already correct); store graphics (Phase 17); CI (Phase 18).
