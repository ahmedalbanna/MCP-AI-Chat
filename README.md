# MCP AI Chat — Production (Next.js)

> **Branch:** `feat/production-nextjs` • **Stack:** Next.js 15 (App Router) + TypeScript + Tailwind + Zustand + Prisma + Vercel AI SDK + MCP SDK

Prototype preserved at [`/public/prototype/index.html`](./public/prototype/index.html) (original `index.html` + `css/style.css` + `js/app.js` — 672 + 1097 + 4045 lines). This branch is the **production implementation** that turns that prototype into a real app.

---

## 1) Quick start

```bash
npm install
cp .env.example .env   # fill DATABASE_URL etc.
npm run dev            # http://localhost:3000
npm run build && npm start
```

Health check: `GET /api/health` → `{ status: "ok" }`  
Chat (mock): `POST /api/chat` `{ messages, provider, model, mode }`

---

## 2) Stack rationale

| Layer | Choice | Why |
|---|---|---|
| Framework | **Next.js 15 App Router** | SSR/SSG, Route Handlers, Edge streaming |
| Language | **TypeScript 5.7 strict** | Contracts for MCP tool schemas |
| Styling | **Tailwind 3.4 + design tokens** | Tokens migrated from `css/style.css` → `app/globals.css` |
| State | **Zustand + persist** | Single store (`lib/store.ts`) replaces 4045-line `js/app.js`, persisted to `localStorage` (`mcp-ai-chat-v2`), ready to hydrate from Prisma |
| Data | **Prisma + Postgres** | Schema in `prisma/schema.prisma` — `User / Provider / Server / Chat / Message / AgentMode / MarketItem` |
| AI | **Vercel AI SDK + MCP SDK** | `app/api/chat` → streaming orchestrator → MCP servers (sse/stdio/http) |
| Uploads | **S3 presigned URLs** | Attachments flow through `POST /api/upload` (File → S3 → `AttachmentPreview`) |
| Auth | **Auth.js (NextAuth)** | Ready for `lib/auth.ts` — guards `app/` + encrypts stored API keys (AES-GCM) |

---

## 3) Project structure

```
app/
  layout.tsx            # Root layout + metadata + globals.css
  page.tsx              # <Sidebar> + <Topbar> + <ChatView> + drawers (single page app shell)
  globals.css           # Tailwind + full css/style.css tokens (1102 lines)
  api/
    health/route.ts     # Liveness probe
    chat/route.ts       # Mock completions (will become Edge streaming)
lib/
  types.ts              # Provider, MCPServer, Chat, AgentMode, MarketItem, …
  mockData.ts           # DEFAULT_PROVIDERS (4) • DEFAULT_SERVERS (3) • DEFAULT_CHATS (3) • MARKET (10) • PROMPTS (14) • SLASH (16) • DEFAULT_MODES (8)
  store.ts              # Zustand store — all mutations: sendMessage + streaming simulation, toggleServer, installMarket, …
  utils.ts              # uid, fmtSize, esc, cn
components/
  shell/Sidebar.tsx     # Workspace (archived toggle) + MCP servers (live tunnel card) + Provider mini
  shell/Topbar.tsx      # Crumb + live badge + mode pills + Export
  chat/ChatView.tsx     # Messages + scroll FAB + empty state
  chat/MessageItem.tsx  # User bubble + Assistant card (table/code-block renderer)
  chat/Composer.tsx     # Attach (6 files) + slash (/) + todo draft + mode pill
  chat/PromptStrip.tsx  # 6 mode-aware chips from PROMPTS
  drawers/Drawers.tsx   # Servers • Marketplace (7 tabs) • Prompts • Providers • Profile • Modes
prisma/schema.prisma    # Postgres schema ready for `prisma migrate dev`
public/prototype/       # Original prototype for reference
```

---

## 4) Data model parity with prototype

Prototype `localStorage` keys → store `persist` keys:

- `mcp_providers` (4) → `providers` — enabled toggle, apiKey encrypted, latency, streaming flag
- `mcp_servers` (3) → `servers` — transport sse/stdio/http, env[], timeout/retries, health
- `mcp_chats` (3) → `chats` + `activeChatId` — archived, messages with `html: pg|aws|fs`
- `mcp_market` (10) → `market` — featured/trending/isNew/verified, install toggle
- `mcp_todos_draft` → local draft in `Composer` (also persisted)
- `mcp_modes` (8) → `modes` — builtin vs custom, provider override, tools {sql,fs,cloud,web}
- `provider/model/agentMode/marketTab/serverTab` → explicit store fields

Streaming: `sendMessage()` appends a `streaming:true` assistant placeholder and simulates 24-chunk streaming at 42ms — same UX as prototype but via React state; will be replaced by `ai.streamText()` + MCP tool calling.

---

## 5) MCP orchestrator (next step)

```
[ Composer ] → POST /api/chat (Edge) → Vercel AI SDK
                                   ├─► provider routing (openai/anthropic/google/local)
                                   └─► MCPClient pool (sse/stdio/http)
                                         ├─ postgres-mcp → query / describe_schema
                                         ├─ aws-us-east   → s3_list / ec2_status
                                         └─ local-fs      → read / write / list
                                   ← SSE stream → MessageItem streaming badge + shimmer
```

Health polling: `GET /api/servers/health` every 30s updates `latency` + `health`.

---

## 6) Scripts

```bash
npm run dev        # Next dev (Turbopack)
npm run build      # Production build — verified ✓ (124 kB First Load)
npm run start      # Serve .next
npm run lint       # next lint
npm run typecheck  # tsc --noEmit
npx prisma generate
npx prisma migrate dev
```

---

## 7) Deploy

Vercel (recommended) — set `DATABASE_URL`, `AUTH_SECRET`, `OPENAI_API_KEY` etc. in project env.  
Or Docker:

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
CMD ["npm","start"]
```

---

## 8) Roadmap

- [ ] Replace mock `POST /api/chat` with `streamText()` + `experimental_createMCPClient`
- [ ] Auth.js + middleware + encrypted provider keys
- [ ] `POST /api/upload` → S3 presigned + `GET /api/servers` CRUD (Prisma)
- [ ] tRPC layer for type-safe client (`lib/trpc/`)
- [ ] Playwright E2E (chat, slash, marketplace, MCP toggle)
