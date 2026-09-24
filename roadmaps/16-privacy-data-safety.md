# Phase 16 — Privacy, Legal, Data Safety (Play Blocker)

Goal: satisfy Play's mandatory privacy-policy URL + Data Safety form with a defensible, evidence-backed disclosure. No code behavior changes expected.

## Evidence

- Secrets at rest: `data/security/AiProviderSettings.kt` (`EncryptedSharedPreferences`, `MasterKey.AES256_GCM`); `data/tool/../security/CryptoManager.kt` encrypts MCP `authHeader` at rest.
- Backup exclusions: `app/src/main/res/xml/data_extraction_rules.xml:13-25` + `app/src/main/res/xml/backup_rules.xml:13-18` exclude `mcp_chat_database{,-wal,-shm}` + `sharedpref/ai_provider_prefs`; onboarding pref (`mcp_chat_onboarding`) intentionally allowed.
- Network: `INTERNET` + `ACCESS_NETWORK_STATE` only; Android cleartext remains enabled for arbitrary LAN compatibility, while `EndpointPolicy` rejects public HTTP, allows device-local HTTP, and requires an explicit persisted grant for private-network HTTP. Credential-bearing clients do not follow redirects.
- Key injection: `.env.example` is a debug-only Gemini seed; release `BuildConfig.GEMINI_API_KEY` is always empty and verified by `verifyReleaseBuildConfig`. Real keys are entered in-app and stored encrypted.
- Missing: no `PRIVACY.md`, no hosted policy URL, no `LICENSE`, no Data Safety draft, no `fastlane/` metadata.

## Tasks

### 16.1 Privacy policy [S — mandatory URL]
- [ ] Add `PRIVACY.md` at repo root, then publish to a hosted URL (GitHub Pages / site) — Play requires a public link, not the markdown alone.
- [ ] Disclose, in plain language:
  - What is collected: user-provided AI provider keys, MCP server URLs + auth headers, chat history, tool-call audit logs, diagnostic state (latency, connection status).
  - Where it lives: on-device Room DB + `EncryptedSharedPreferences` (AndroidKeyStore); DB + `ai_provider_prefs` excluded from cloud backup/device transfer (cite both XMLs above).
  - Where it goes: only to user-configured AI endpoints (Gemini/Anthropic/OpenAI-compatible) and user-configured MCP servers; no sale, no third-party SDK sharing (SDK-free build).
  - Network: `https` for remote endpoints; device-local HTTP is supported directly; private-network HTTP requires an explicit in-app confirmation persisted with the endpoint; public cleartext HTTP is rejected.
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
- [ ] Prepare review note: Android cleartext remains enabled only because arbitrary private LAN addresses cannot be expressed in `network_security_config.xml`; `EndpointPolicy` rejects public cleartext and requires confirmation for private-network HTTP.
- [x] Reject public cleartext and disable credential-bearing redirects in the application transport layer.

### 16.4 License [S]
- [ ] Add `LICENSE` (choose: MIT / Apache-2.0 for permissive; GPL-3.0 if copyleft intended). Repo currently has none — Play + GitHub both surface this.
- [ ] If closed-source, state "All rights reserved" in the listing instead and skip `LICENSE`.

## Acceptance gate

- [ ] `PRIVACY.md` merged + publicly hosted URL live (link returns 200).
- [ ] Data Safety draft reviewed against actual permissions (`INTERNET`, `ACCESS_NETWORK_STATE` only) and backup rules.
- [ ] `LICENSE` decision recorded (file added or explicitly closed-source).

## Out of scope

- Code changes to backup rules or network config (already correct); store graphics (Phase 17); CI (Phase 18).
