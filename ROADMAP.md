# MCP Terminal — Production Roadmap

**Vision:** One chat, many tools. TLS 1.3 secure tunnel → AI providers (BYOK) → MCP nodes (Postgres, S3, FS, GitHub, …) → verifiable, local-first, high-design. No mocks in prod.

Prototype: `index.html:672` `css/style.css:1097` `js/app.js:4045` — high-fidelity vanilla. Prod: `Next.js 15 + Zustand v3 + Prisma Postgres + MCP SDK + AI SDK` `app/page.tsx:53` `globals.css:1102`.

---

## Shipped — Stages 0-7 (ce12478)

| Stage | Scope | Key Files | Verify |
|-------|-------|-----------|--------|
| **0 Foundation** | Env `64hex`, ESM-safe DB, Docker healthy | `lib/env.ts:22` `lib/db.ts:8` `Dockerfile:3` `docker-compose.yml:22` `prisma/migrations/20260830*` | `curl /api/health → postgres` `docker ps healthy` |
| **1 Data & Design** | Tasks/todos, table, polish | `mockData.ts:16` `prisma/schema 134` `MessageItem.tsx:88` `Sidebar 26 Archived` `Composer 8 drag/paste` | screenshot `table 1042-1038 + Tasks 2/3` |
| **2 Chat Real** | Persist + streaming tasks, edit/delete | `app/api/chats/[id]/messages/[messageId]/route.ts:1` `chat/route.ts:30` `store.ts:270` `finalTasks` | `PATCH/DELETE` survive reload, SSE `done tasks` |
| **3 Providers** | Encrypt `AES-GCM`, sync | `app/api/providers/route.ts:6` `store 381` `Drawers.tsx:165` `masked ••••` `Test latency` | `curl PATCH → encrypted Xlpk → GET masked` |
| **4 MCP** | CRUD sse/stdio/http, health | `Drawers.tsx:25 ServersDrawer CRUD ^[a-z0-9-]{2,48}$` `store 87 toggle sync` `registry.ts:39` `health poll 30s` | `POST /api/servers` `tunnel-card avg` |
| **5 Marketplace** | DB sync, install→server | `app/api/market/route.ts:26 POST` `store 368 hydrate market` `installMarket → server` | `curl /api/market 10` `Postgres Pro:True` |
| **6 Auth/Onboarding** | Splash, 4 steps, guest | `page.tsx:12` `Onboarding.tsx 4 steps` `AuthModal.tsx login/signup/guest Demo` `store currentUser/onboarded v3` | playwright fresh clear → onboarding → auth |
| **7 Hardening** | Rate-limit, headers, tests | `middleware.ts:4 12/min` `X-Frame-Options DENY` `lib/monitoring.ts` `tests/visual.spec.ts` | `curl -i /api/chat X-RateLimit 12` |

Build: `30.6kB First Load 132kB Middleware 32.9kB` `healthy postgres`.

---

## Next — Stages 8-12 (High-Design, Real Features)

### 8 Live Speech & TTS (2w)
- `WebSpeech API` `SpeechRecognition interim + MediaRecorder opus` `js/app.js:291` parity → `components/chat/LiveOverlay.tsx`
- `speechBar wave/timer` `hold-to-talk` `interrupt`, `TTS` `speechSynthesis` `voice select`
- `lib/ai/voice.ts` `whisper` fallback, `POST /api/voice/transcribe`

### 9 Files & Attachments Real (1.5w)
- `S3 presign POST /api/uploads` `s3_list` `multipart`, `FileChip` `pdf/csv/json` real parse `Papaparse` `pdfjs`
- `Composer` `URL.createObjectURL revoke` `6 files` `preview--file` `progress`, `MessageItem user-media` `lightbox`

### 10 Agent Modes Pro (1w)
- `Mode modal` `icon/color/desc/prompt/temp/tokens/provider/model/servers/tools` `index.html:382` parity
- `confirmModeSwitch` provider override, `per-mode MCP` filter in `buildMCPTools`

### 11 Observability & Scale (1w)
- `Sentry @sentry/nextjs` `lib/monitoring.ts` `captureError`, `Prometheus /api/metrics`, `pino` logs, `Redis` rate-limit (replace Map)
- `Playwright` visual regression `8765 vs 3000` CI `GitHub Actions`, `Lighthouse ≥95`

### 12 Multi-Workspace & Sync (2w)
- `Workspace` model `prisma Workspace { id, ownerId, name }`, `Chat.workspaceId`, `Sidebar workspace switcher`
- `Sync` `WebSocket` `realtime` `Yjs` for shared chats, `guest → account` migration `mcp_users` → `User`

---

## Design System — Non-Negotiable

Tokens `css/style.css:7-32` `—primary #2563eb —shadow-sm/md/lg —ease .2,.8,.2,1` `max-w 1440 glass` `sidebar 352 blur 12px` `messages 780` `card hover shadow-md` `user bubble eff6ff→dbeafe`. Keep `globals.css` 1:1. No Tailwind redesign.

Motion: `pulse 2s`, `shimmer 1.4s`, `msgIn .32s`, `blink 1.4s`. A11y: `trapFocus 367`, `skip-link`, `kbd`.

---

## Tech

`Next 15 app/` `Zustand persist v3` `Prisma 16-alpine` `MCP SDK @modelcontextprotocol/sdk` `AI SDK ai @ai-sdk/openai/anthropic/google` `Lucide`, `Middleware` `JWT` `AES-GCM 32B`.

Env prod: `DATABASE_URL AUTH_SECRET(64) ENCRYPTION_KEY(64hex) SENTRY_DSN S3_*`.

---

## Milestones

- **M1 (done):** 0-7 ship, `feat/production-nextjs` `ce12478`, `docker healthy`
- **M2 (4w):** 8-10 Live+Files+Modes
- **M3 (2w):** 11-12 scale + workspaces → `v2.0 GA`

Contribute: `npm run dev` `python -m http.server 8765` (prototype) + `docker compose up` `npx playwright test`.
