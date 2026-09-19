# Phase 17 — Store Listing Assets

Goal: complete public-listing metadata + graphics so the Play Console listing can be submitted without back-and-forth.

## Evidence

- Identity: `app_name=MCP AI Chat` (`app/src/main/res/values/strings.xml:1-3`); `applicationId com.aistudio.mcpchat` (`app/build.gradle.kts:24`).
- Icons: adaptive `mipmap-mdpi..xxxhdpi/ic_launcher[.round].webp` + `mipmap-anydpi-v26` XML (background `#0D1117`, foreground logo, monochrome) — Play adaptive/themed-icon ready.
- Theme: Compose `Theme.kt` + `MainActivity enableEdgeToEdge()` — satisfies Android 15 edge-to-edge enforcement for screenshots.
- Snapshots: 23 Roborazzi goldens under `app/src/test/snapshots/` (device-frame-free; good as screenshot *drafts*, not final store shots).
- Missing: no `fastlane/metadata/`, no `store/` dir, no feature graphic, no device screenshots, no rating/audience answers.

## Tasks

### 17.1 Metadata skeleton [S]
- [ ] Create `fastlane/metadata/android/en-US/` with: `title.txt` (≤30ch), `short_description.txt` (≤80ch), `full_description.txt` (≤4000ch), plus `contact email`, `category` (Productivity/Tools), `content-rating` answers draft.
- [ ] Full description must cover: multi-provider chat (Gemini default `gemini-3.6-flash`, Anthropic, OpenAI-compatible incl. Ollama/vLLM/LM Studio), MCP initialize → tools/list → tools/call loop, streaming with stop/regenerate, inspector + activity logs, encrypted on-device keys, Material 3 dynamic/AMOLED theming, tablet/foldable layouts.
- [ ] Disclose LAN/`http` support + encrypted-at-rest posture (mirrors Phase 16 policy) so review and users see it upfront.

### 17.2 Graphics [S]
- [ ] `512x512` high-res icon (export from adaptive foreground/background, no transparency issues).
- [ ] `1024x500` feature graphic (no critical text in safe-zone edges).
- [ ] Screenshots: phone (min 2), 7" tablet, 10" tablet — capture light + dark (+ AMOLED hero) on real device/emulator at Play's required sizes; show Chat, Servers, Inspector, Settings. Roborazzi goldens are layout refs only — retake on device.
- [ ] Optional promo video URL (defer unless available).

### 17.3 Rating, audience, questionnaires [S]
- [ ] Complete content-rating questionnaire draft in-repo (chat app with user-generated content + unfiltered AI output → expect Teen/Mature guidance; no gambling/ads).
- [ ] Target audience + news/app declarations; contact email monitored for Play correspondence.
- [ ] Confirm `supportsRtl=true` + EN-only v1.1.0 (`values/strings.xml` has only `app_name`; all UI strings hardcoded EN) — list English as the launch language; Arabic localization is Phase 19.

## Acceptance gate

- [ ] `fastlane/metadata/android/en-US/` complete + graphics at Play sizes + screenshot set (phone + 7" + 10").
- [ ] Rating/audience drafts reviewed; contact email verified.
- [ ] Listing preview passes Play Console's completeness checklist (no missing-graphic warnings).

## Out of scope

- Translations, promo video production, post-launch screenshot refresh cadence (Phase 19).
