"use client";
import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type { Provider, MCPServer, Chat, AgentMode, MarketItem } from "./types";
import { DEFAULT_PROVIDERS, DEFAULT_SERVERS, DEFAULT_CHATS, MARKET, DEFAULT_MODES } from "./mockData";
import { uid, timeNow } from "./utils";

type CurrentUser = { id:string; name:string; email:string; avatar:string; role:string; isGuest:boolean };
type AppState = {
  providers: Provider[];
  selectedProvider: string;
  selectedModel: string;
  selectedMode: string;
  servers: MCPServer[];
  chats: Chat[];
  activeChatId: string | null;
  modes: AgentMode[];
  market: MarketItem[];
  showArchived: boolean;
  sidebarOpen: boolean;
  // drawers
  drawer: null | "servers" | "providers" | "marketplace" | "prompts" | "profile" | "modes" | "onboarding" | "auth";
  marketTab: string;
  serverTab: string;
  promptRecent: string[];
  dbHydrated: boolean;
  currentUser: CurrentUser | null;
  onboarded: boolean;
  // actions
  setProvider: (id: string) => void;
  setModel: (m: string) => void;
  setMode: (id: string) => void;
  toggleServer: (id: string) => void;
  addServer: (s: MCPServer) => void;
  updateServer: (id: string, patch: Partial<MCPServer>) => void;
  removeServer: (id: string) => void;
  newChat: () => void;
  selectChat: (id: string) => void;
  archiveChat: (id: string) => void;
  deleteChat: (id: string) => void;
  renameChat: (id: string, title: string) => void;
  hydrateFromDb: () => Promise<void>;
  sendMessage: (text: string, attachments?: { name: string; size: number; type: string }[]) => void;
  editMessage: (msgId: string, text: string) => void;
  deleteMessage: (msgId: string) => void;
  retryLast: () => void;
  setDrawer: (d: AppState["drawer"]) => void;
  setShowArchived: (v: boolean) => void;
  toggleSidebar: () => void;
  setMarketTab: (t: string) => void;
  setServerTab: (t: string) => void;
  installMarket: (id: string) => void;
  addMode: (m: AgentMode) => void;
  updateMode: (id: string, patch: Partial<AgentMode>) => void;
  deleteMode: (id: string) => void;
  updateProvider: (id: string, patch: Partial<Provider>) => void;
  addProvider: (p: Provider) => void;
  deleteProvider: (id: string) => void;
  recordPromptUse: (prompt: string) => void;
  setCurrentUser: (u: CurrentUser | null) => void;
  setOnboarded: (v: boolean) => void;
};

export const useStore = create<AppState>()(
  persist(
    (set, get) => ({
      providers: DEFAULT_PROVIDERS,
      selectedProvider: "anthropic",
      selectedModel: "claude-3.5-sonnet",
      selectedMode: "Chat",
      servers: DEFAULT_SERVERS,
      chats: DEFAULT_CHATS,
      activeChatId: DEFAULT_CHATS[0]?.id || null,
      modes: DEFAULT_MODES,
      market: MARKET,
      showArchived: false,
      sidebarOpen: false,
      drawer: null,
      marketTab: "all",
      serverTab: "all",
      promptRecent: [],
      dbHydrated: false,
      currentUser: null,
      onboarded: false,

      setProvider: (id) => {
        const prov = get().providers.find((p) => p.id === id);
        if (!prov || !prov.enabled) return;
        set({ selectedProvider: id, selectedModel: prov.models[0] || get().selectedModel });
      },
      setModel: (m) => set({ selectedModel: m }),
      setMode: (id) => set({ selectedMode: id }),

      toggleServer: (id) => {
        const srv = get().servers.find((x) => x.id === id);
        const next = srv ? (srv.status === "connected" ? "disconnected" : "connected") : "connected";
        const health = next === "connected" ? "ok" : "off";
        const latency = Math.floor(Math.random() * 120 + 40);
        set((s) => ({
          servers: s.servers.map((x) => x.id === id ? { ...x, status: next as any, health: health as any, lastSeen: Date.now(), latency } as MCPServer : x)
        }));
        (async () => { try { await fetch(`/api/servers/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ status: next, health, latency }) }); } catch {} })();
      },
      addServer: (srv) => {
        set((s) => ({ servers: [...s.servers, srv] }));
        (async () => { try { await fetch(`/api/servers`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ name: srv.name, transport: srv.transport, endpoint: srv.endpoint, tools: srv.tools, color: srv.color, desc: srv.desc, timeout: srv.timeout, retries: srv.retries, env: srv.env, autoConnect: srv.autoConnect, healthCheck: srv.healthCheck }) }); } catch {} })();
      },
      updateServer: (id, patch) => {
        set((s) => ({ servers: s.servers.map((x) => x.id === id ? { ...x, ...patch } : x) }));
        (async () => { try { await fetch(`/api/servers/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(patch) }); } catch {} })();
      },
      removeServer: (id) => {
        set((s) => ({ servers: s.servers.filter((x) => x.id !== id) }));
        (async () => { try { await fetch(`/api/servers/${id}`, { method: "DELETE" }); } catch {} })();
      },

      newChat: () => {
        // optimistic local — sync to DB best-effort
        const c: Chat = { id: uid(), title: "New conversation", created: Date.now(), archived: false, messages: [] };
        set((s) => ({ chats: [c, ...s.chats], activeChatId: c.id }));
        // try remote create; on success replace temp id with DB id
        (async () => {
          try {
            const res = await fetch("/api/chats", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ title: c.title }) });
            if (!res.ok) return;
            const data: any = await res.json();
            if (data?.id && data.id !== c.id) {
              set((s) => ({
                chats: s.chats.map((ch) => ch.id === c.id ? { ...ch, id: data.id } : ch),
                activeChatId: s.activeChatId === c.id ? data.id : s.activeChatId,
              }));
            }
          } catch {}
        })();
      },
      selectChat: (id) => set({ activeChatId: id, sidebarOpen: false }),
      archiveChat: (id) => {
        const prev = get().chats.find((c) => c.id === id);
        set((s) => ({ chats: s.chats.map((c) => c.id === id ? { ...c, archived: !c.archived } : c) }));
        (async () => {
          try {
            const nextArchived = !prev?.archived;
            await fetch(`/api/chats/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ archived: nextArchived }) });
          } catch {}
        })();
      },
      deleteChat: (id) => {
        set((s) => {
          const chats = s.chats.filter((c) => c.id !== id);
          let activeChatId = s.activeChatId;
          if (activeChatId === id) activeChatId = chats[0]?.id || null;
          return { chats, activeChatId };
        });
        (async () => { try { await fetch(`/api/chats/${id}`, { method: "DELETE" }); } catch {} })();
      },
      renameChat: (id, title) => {
        set((s) => ({ chats: s.chats.map((c) => c.id === id ? { ...c, title: title.slice(0, 80) } : c) }));
        (async () => { try { await fetch(`/api/chats/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ title: title.slice(0, 80) }) }); } catch {} })();
      },
      sendMessage: (text, attachments) => {
        const activeId = get().activeChatId;
        if (!activeId) return;
        const trimmed = text.trim();
        if (!trimmed && !attachments?.length) return;
        const userMsg = { id: uid(), role: "user" as const, text: trimmed, time: timeNow(), attachments: attachments?.map(a => ({ id: uid(), name: a.name, size: a.size, type: a.type, kind: a.type.startsWith("image/") ? "image" as const : a.type.startsWith("video/") ? "video" as const : a.type.startsWith("audio/") ? "audio" as const : "file" as const })) };
        // handle slash commands locally
        if (trimmed.startsWith("/")) {
          const cmd = trimmed.split(" ")[0].toLowerCase();
          if (cmd === "/clear") {
            set((s) => ({ chats: s.chats.map((c) => c.id === activeId ? { ...c, messages: [] } : c) }));
            return;
          }
          if (cmd === "/new") { get().newChat(); return; }
        }
        set((s) => ({
          chats: s.chats.map((c) => c.id === activeId ? {
            ...c,
            title: c.messages.length === 0 && trimmed ? trimmed.slice(0, 48) : c.title,
            messages: [...c.messages, userMsg]
          } : c)
        }));
        // streaming assistant placeholder
        const assistId = uid();
        const streamingMsg = { id: assistId, role: "assistant" as const, text: "", time: timeNow(), streaming: true as const };
        set((s) => ({ chats: s.chats.map((c) => c.id === activeId ? { ...c, messages: [...c.messages, streamingMsg] } : c) }));

        const lower = trimmed.toLowerCase();
        const fallbackSimulate = () => {
          let full = "";
          if (lower.includes("postgres") || lower.includes("sql") || lower.includes("users")) {
            full = "Here are the last 5 user registrations from **remote_db.users** (via `postgres-mcp`):\n\n| id | email | created_at |\n|---|---|---|\n| 1042 | alice@example.com | 2024-10-20 |\n| 1041 | bob@example.com | 2024-10-19 |\n| 1040 | carol@example.com | 2024-10-18 |\n| 1039 | dave@example.com | 2024-10-17 |\n| 1038 | eve@example.com | 2024-10-16 |\n\nTLS 1.3 • latency 68ms • `SELECT * FROM users ORDER BY created_at DESC LIMIT 5`";
          } else if (lower.includes("s3") || lower.includes("bucket")) {
            full = "S3 buckets in **aws-us-east** (via `aws-us-east`):\n\n- `app-prod-assets` — 1.2 GB • 342 objects\n- `logs-archive` — 842 MB • 12k objects\n- `ml-datasets` — 4.1 GB • 89 objects\n\nTotal 3 buckets • 6.1 GB";
          } else if (lower.includes("/help")) {
            full = "Available commands: `/sql`, `/s3`, `/fs`, `/github`, `/marketplace`, `/servers`, `/new`, `/clear`, `/export`, `/help`, `/todo`, `/mode`, `/model`, `/connect`, `/retry`.\n\nTry: `Query the remote PostgreSQL database for the last 5 user registrations`";
          } else {
            full = `Got it — **${trimmed.slice(0, 120)}**\n\nI'm running in **${get().selectedMode}** mode via **${get().selectedProvider} / ${get().selectedModel}** with ${get().servers.filter(s => s.status === "connected").length} MCP server(s) connected.\n\nHow would you like to proceed? You can ask me to query Postgres, list S3 buckets, or browse the marketplace.`;
          }
          let i = 0;
          const mockTasks = lower.includes("postgres") ? [{ id: "t1", text: "Verify table schema", done: false }, { id: "t2", text: "Run SELECT on remote_db.users", done: true }, { id: "t3", text: "Format as markdown table", done: true }] : undefined;
          const mockTodos = lower.includes("postgres") ? [{ text: "Export to CSV", done: false }, { text: "Share with team", done: false }] : lower.includes("s3") ? [{ text: "Review lifecycle policies", done: false }, { text: "Enable versioning", done: true }] : undefined;
          const mockHtml = lower.includes("postgres") ? "pg" as const : lower.includes("s3") ? "aws" as const : undefined;
          const iv = setInterval(() => {
            i += Math.ceil(full.length / 24);
            if (i >= full.length) {
              clearInterval(iv);
              set((s) => ({
                chats: s.chats.map((c) => c.id === activeId ? {
                  ...c,
                  messages: c.messages.map((m) => m.id === assistId ? { ...m, text: full, streaming: false, html: mockHtml, tasks: mockTasks as any, todos: mockTodos as any } : m)
                } : c)
              }));
            } else {
              const chunk = full.slice(0, i);
              set((s) => ({
                chats: s.chats.map((c) => c.id === activeId ? {
                  ...c,
                  messages: c.messages.map((m) => m.id === assistId ? { ...m, text: chunk } : m)
                } : c)
              }));
            }
          }, 42);
        };

        // Try real API streaming (SSE) — falls back to local simulate on failure/offline
        const useApi = typeof window !== "undefined" && !trimmed.startsWith("/") && typeof fetch !== "undefined";
        if (useApi) {
          (async () => {
            const syncChatId = (serverChatId: string | null) => {
              if (serverChatId && serverChatId !== activeId) {
                set((s) => {
                  const exists = s.chats.some((c) => c.id === serverChatId);
                  if (exists) return { activeChatId: serverChatId };
                  // server created new chat — replace temp id
                  return {
                    chats: s.chats.map((c) => c.id === activeId ? { ...c, id: serverChatId } : c),
                    activeChatId: s.activeChatId === activeId ? serverChatId : s.activeChatId,
                  };
                });
              }
            };
            try {
              const res = await fetch("/api/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
                body: JSON.stringify({
                  messages: [{ role: "user", content: trimmed }],
                  provider: get().selectedProvider,
                  model: get().selectedModel,
                  mode: get().selectedMode,
                  stream: true,
                  chatId: activeId,
                  attachments,
                }),
              });
              if (!res.ok || !res.body) throw new Error(`api ${res.status}`);
              const ct = res.headers.get("content-type") || "";
              if (!ct.includes("text/event-stream")) {
                const data: any = await res.json();
                const finalText: string = data.content || data.text || "";
                const html = data.html as any;
                if (data.chatId) syncChatId(data.chatId);
                set((s) => ({
                  chats: s.chats.map((c) => (c.id === activeId || c.id === data.chatId) ? {
                    ...c,
                    title: c.messages.length <= 1 && trimmed ? trimmed.slice(0, 48) : c.title,
                    messages: c.messages.map((m) => m.id === assistId ? { ...m, text: finalText, streaming: false, html: html || (lower.includes("postgres") ? "pg" : lower.includes("s3") ? "aws" : undefined), tasks: data.tasks, todos: data.todos } : m)
                  } : c)
                }));
                return;
              }
              const reader = res.body.getReader();
              const decoder = new TextDecoder();
              let buffer = "";
              let finalHtml: string | undefined = undefined;
              let finalText = "";
              let lastEvent = "";
              let finalTasks: any = undefined;
              let finalTodos: any = undefined;
              while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                let sep: number;
                while ((sep = buffer.indexOf("\n\n")) !== -1) {
                  const block = buffer.slice(0, sep);
                  buffer = buffer.slice(sep + 2);
                  const lines = block.split("\n");
                  let eventType = "";
                  let dataPayload = "";
                  for (const line of lines) {
                    if (line.startsWith("event:")) eventType = line.slice(6).trim();
                    else if (line.startsWith("data:")) dataPayload += (dataPayload ? "\n" : "") + line.slice(5).trim();
                  }
                  if (!dataPayload) continue;
                  if (dataPayload === "[DONE]") break;
                  // track last event for context
                  if (eventType) lastEvent = eventType;
                  try {
                    const obj = JSON.parse(dataPayload);
                    // chatId sync from meta/done
                    const maybeChatId = (obj as any).chatId || (obj as any).chat_id;
                    if (maybeChatId) syncChatId(maybeChatId);
                    if (typeof obj.delta === "string") {
                      finalText = obj.delta;
                      if (obj.html) finalHtml = obj.html;
                      if ((obj as any).tasks) finalTasks = (obj as any).tasks;
                      if ((obj as any).todos) finalTodos = (obj as any).todos;
                      set((s) => ({
                        chats: s.chats.map((c) => c.id === activeId ? {
                          ...c,
                          messages: c.messages.map((m) => m.id === assistId ? { ...m, text: obj.delta, html: (obj.html as any) || m.html } : m)
                        } : c)
                      }));
                    } else if (obj.content && !obj.delta) {
                      finalText = obj.content;
                      if (obj.html) finalHtml = obj.html;
                      if ((obj as any).tasks) finalTasks = (obj as any).tasks;
                      if ((obj as any).todos) finalTodos = (obj as any).todos;
                      set((s) => ({
                        chats: s.chats.map((c) => c.id === activeId ? {
                          ...c,
                          messages: c.messages.map((m) => m.id === assistId ? { ...m, text: obj.content, streaming: false, html: (obj.html as any) || finalHtml as any, tasks: (obj as any).tasks || finalTasks, todos: (obj as any).todos || finalTodos } : m)
                        } : c)
                      }));
                    } else if (obj.html && !obj.delta) {
                      finalHtml = obj.html;
                    }
                    if ((obj as any).tasks) finalTasks = (obj as any).tasks;
                    if ((obj as any).todos) finalTodos = (obj as any).todos;
                    // handle explicit done event with streaming marker
                    if (lastEvent === "done" || obj.done === true) {
                      finalText = obj.content || obj.delta || finalText;
                      if ((obj as any).tasks) finalTasks = (obj as any).tasks;
                      if ((obj as any).todos) finalTodos = (obj as any).todos;
                      set((s) => ({
                        chats: s.chats.map((c) => c.id === activeId ? {
                          ...c,
                          messages: c.messages.map((m) => m.id === assistId ? { ...m, text: finalText || m.text, streaming: false, html: (finalHtml as any) || m.html, tasks: finalTasks || (m as any).tasks, todos: finalTodos || (m as any).todos } : m)
                        } : c)
                      }));
                    }
                  } catch {
                    // ignore json parse errors per chunk
                  }
                }
              }
              // finalize (if not already finalized via done event)
              set((s) => ({
                chats: s.chats.map((c) => c.id === activeId ? {
                  ...c,
                  messages: c.messages.map((m) => m.id === assistId ? { ...m, text: finalText || (m as any).text, streaming: false, html: (finalHtml as any) || (m as any).html || (lower.includes("postgres") ? "pg" as const : lower.includes("s3") ? "aws" as const : undefined), tasks: finalTasks || (m as any).tasks, todos: finalTodos || (m as any).todos } : m)
                } : c)
              }));
            } catch {
              fallbackSimulate();
            }
          })();
          return;
        }
        fallbackSimulate();
      },
      editMessage: (msgId, text) => {
        const aid = get().activeChatId;
        if (!aid) return;
        set((s) => ({ chats: s.chats.map((c) => c.id === aid ? { ...c, messages: c.messages.map((m) => m.id === msgId ? { ...m, text } : m) } : c) }));
        (async () => {
          try { await fetch(`/api/chats/${aid}/messages/${msgId}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ content: text, text }) }); } catch {}
        })();
      },
      deleteMessage: (msgId) => {
        const aid = get().activeChatId;
        if (!aid) return;
        set((s) => ({ chats: s.chats.map((c) => c.id === aid ? { ...c, messages: c.messages.filter((m) => m.id !== msgId) } : c) }));
        (async () => {
          try { await fetch(`/api/chats/${aid}/messages/${msgId}`, { method: "DELETE" }); } catch {}
        })();
      },
      retryLast: () => {
        const aid = get().activeChatId;
        if (!aid) return;
        const chat = get().chats.find((c) => c.id === aid);
        const lastUser = [...(chat?.messages || [])].reverse().find((m) => m.role === "user");
        if (lastUser?.text) get().sendMessage(lastUser.text);
      },
      setDrawer: (d) => set({ drawer: d }),
      setShowArchived: (v) => set({ showArchived: v }),
      toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
      setMarketTab: (t) => set({ marketTab: t }),
      setServerTab: (t) => set({ serverTab: t }),
      installMarket: (id) => {
        const item = get().market.find((m) => m.id === id);
        const willInstall = item ? !item.installed : true;
        set((s) => ({ market: s.market.map((m) => m.id === id ? { ...m, installed: willInstall } : m) }));
        (async () => {
          try {
            if (willInstall && item) {
              // map market → server; e.g., Postgres Pro → postgres-mcp
              const name = item.name.toLowerCase().replace(/\s+/g, "-").replace(/[^a-z0-9-]/g, "");
              const tools = (item.tools || []).join(", ");
              const transport = (item.cat === "db" || item.icon === "database") ? "stdio" : item.cat === "fs" ? "stdio" : "sse";
              const endpoint = transport === "stdio"
                ? `npx @modelcontextprotocol/server-${item.icon === "database" ? "postgres" : item.icon === "folder" ? "filesystem /data" : item.id}`
                : `https://${item.id}.mcp.example.com/sse`;
              const srv:any = {
                name: name.slice(0, 48),
                transport,
                endpoint,
                tools,
                color: item.color?.includes("ff9900") ? "aws" : item.icon==="database" ? "pg" : item.icon==="folder" ? "fs" : "custom",
                desc: item.desc.slice(0, 80),
                timeout: 10000, retries: 2, env: [], autoConnect: true, healthCheck: true,
              };
              const res = await fetch(`/api/servers`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(srv) });
              if (res.ok) {
                const data:any = await res.json();
                // add to servers locally with returned id if available
                if (data?.id) {
                  set((s) => ({ servers: [...s.servers, { ...srv, id: data.id, status:"disconnected", latency:40, health:"off", lastSeen:Date.now() } as any] }));
                } else {
                  // hydrates via /api/servers next poll
                  const sRes = await fetch(`/api/servers`, { cache:"no-store" });
                  if (sRes.ok) { const j:any = await sRes.json(); if(Array.isArray(j.servers)) set({ servers: j.servers }); }
                }
              }
            } else if (!willInstall && item) {
              // uninstall: remove servers matching market item name
              const name = item.name.toLowerCase().replace(/\s+/g, "-").replace(/[^a-z0-9-]/g, "");
              const local = get().servers.find((s) => s.name === name || s.name.includes(item.id));
              if (local) {
                set((s) => ({ servers: s.servers.filter((x) => x.id !== local.id) }));
                try { await fetch(`/api/servers/${local.id}`, { method:"DELETE" }); } catch {}
              }
            }
            // persist installed flag to DB if endpoint exists (optional)
            try { await fetch(`/api/market`, { method:"POST", headers:{ "Content-Type":"application/json"}, body: JSON.stringify({ id, installed: willInstall }) }); } catch {}
          } catch {}
        })();
      },
      addMode: (m) => {
        set((s) => ({ modes: [...s.modes, m] }));
        (async () => { try { await fetch(`/api/modes`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(m) }); } catch {} })();
      },
      updateMode: (id, patch) => {
        set((s) => ({ modes: s.modes.map((m) => m.id === id ? { ...m, ...patch } : m) }));
        (async () => { try { await fetch(`/api/modes/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(patch) }); } catch {} })();
      },
      deleteMode: (id) => {
        set((s) => ({ modes: s.modes.filter((m) => m.id !== id), selectedMode: s.selectedMode === id ? "Chat" : s.selectedMode }));
        (async () => { try { await fetch(`/api/modes/${id}`, { method: "DELETE" }); } catch {} })();
      },
      updateProvider: (id, patch) => {
        set((s) => ({ providers: s.providers.map((p) => p.id === id ? { ...p, ...patch } : p) }));
        (async () => { try { await fetch(`/api/providers/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(patch) }); } catch {} })();
      },
      addProvider: (p) => {
        set((s) => ({ providers: [...s.providers, p] }));
        (async () => { try { await fetch(`/api/providers`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(p) }); } catch {} })();
      },
      deleteProvider: (id) => {
        set((s) => ({ providers: s.providers.filter((p) => p.id !== id) }));
        (async () => { try { await fetch(`/api/providers/${id}`, { method: "DELETE" }); } catch {} })();
      },
      recordPromptUse: (prompt) => set((s) => ({ promptRecent: [prompt, ...s.promptRecent.filter((p) => p !== prompt)].slice(0, 8) })),
      setCurrentUser: (u) => {
        set({ currentUser: u });
        try { if (u) localStorage.setItem("mcp_currentUser", JSON.stringify(u)); else localStorage.removeItem("mcp_currentUser"); } catch {}
        try { if (u) localStorage.setItem("mcp_currentUserId", u.id); else localStorage.removeItem("mcp_currentUserId"); } catch {}
      },
      setOnboarded: (v) => {
        set({ onboarded: v });
        try { if(v) localStorage.setItem("mcp_onboarded","1"); else localStorage.removeItem("mcp_onboarded"); } catch {}
      },
      hydrateFromDb: async () => {
        if (get().dbHydrated) return;
        const refreshMcpHealth = async () => {
          try {
            const r = await fetch("/api/mcp/health", { cache: "no-store" });
            if (!r.ok) return;
            const data: any = await r.json();
            const servers = data?.mcp?.servers as any[] | undefined;
            if (!Array.isArray(servers) || servers.length === 0) return;
            set((s) => ({
              servers: s.servers.map((local) => {
                const remote = servers.find((x: any) => x.id === local.id || x.name === local.name);
                if (!remote) return local;
                return {
                  ...local,
                  status: remote.status || local.status,
                  health: remote.health || local.health,
                  latency: typeof remote.latency === "number" ? remote.latency : local.latency,
                  lastSeen: remote.lastSeen || Date.now(),
                  tools: typeof remote.tools === "string" ? remote.tools : Array.isArray(remote.tools) ? remote.tools.join(", ") : local.tools,
                } as MCPServer;
              }),
            }));
          } catch {}
        };
        // start polling immediately (works in mock + postgres)
        if (typeof window !== "undefined") {
          const g = globalThis as unknown as { __mcpPoll?: ReturnType<typeof setInterval> };
          if (!g.__mcpPoll) {
            refreshMcpHealth();
            g.__mcpPoll = setInterval(refreshMcpHealth, 30000);
          }
        }
        try {
          const h = await fetch("/api/health", { cache: "no-store" }).then((r) => r.json()).catch(() => null);
          if (!h || h.persistence !== "postgres") { set({ dbHydrated: true }); return; }
          // chats
          try {
            const cRes = await fetch("/api/chats?limit=50", { cache: "no-store" });
            if (cRes.ok) {
              const cData: any = await cRes.json();
              if (Array.isArray(cData.chats) && cData.chats.length > 0) {
                const chats: Chat[] = cData.chats;
                set((s) => {
                  const activeStillExists = chats.some((ch) => ch.id === s.activeChatId);
                  return { chats, activeChatId: activeStillExists ? s.activeChatId : chats[0]?.id || null };
                });
              }
            }
          } catch {}
          // servers
          try {
            const sRes = await fetch("/api/servers", { cache: "no-store" });
            if (sRes.ok) {
              const sData: any = await sRes.json();
              if (Array.isArray(sData.servers) && sData.servers.length > 0) set({ servers: sData.servers });
            }
          } catch {}
          // providers
          try {
            const pRes = await fetch("/api/providers", { cache: "no-store" });
            if (pRes.ok) {
              const pData: any = await pRes.json();
              if (Array.isArray(pData.providers) && pData.providers.length > 0) {
                const providers: Provider[] = pData.providers.map((p: any) => ({ ...p, apiKey: p.apiKey || "" }));
                set({ providers });
              }
            }
          } catch {}
          // modes
          try {
            const mRes = await fetch("/api/modes", { cache: "no-store" });
            if (mRes.ok) {
              const mData: any = await mRes.json();
              if (Array.isArray(mData.modes) && mData.modes.length > 0) set({ modes: mData.modes });
            }
          } catch {}
          // market
          try {
            const mkRes = await fetch("/api/market", { cache: "no-store" });
            if (mkRes.ok) {
              const mkData:any = await mkRes.json();
              const items = mkData.items || mkData.market;
              if (Array.isArray(items) && items.length>0) set({ market: items });
            }
          } catch {}
          set({ dbHydrated: true });
          // refresh health after DB servers loaded to get latency
          refreshMcpHealth();
        } catch {
          set({ dbHydrated: true });
        }
      },
    }),
    {
      name: "mcp-ai-chat-v3",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        providers: state.providers,
        selectedProvider: state.selectedProvider,
        selectedModel: state.selectedModel,
        selectedMode: state.selectedMode,
        servers: state.servers,
        chats: state.chats,
        activeChatId: state.activeChatId,
        modes: state.modes,
        market: state.market,
        showArchived: state.showArchived,
        promptRecent: state.promptRecent,
        marketTab: state.marketTab,
        serverTab: state.serverTab,
        currentUser: state.currentUser,
        onboarded: state.onboarded,
      }),
    }
  )
);
