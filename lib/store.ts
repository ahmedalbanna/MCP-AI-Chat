"use client";
import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type { Provider, MCPServer, Chat, AgentMode, MarketItem } from "./types";
import { DEFAULT_PROVIDERS, DEFAULT_SERVERS, DEFAULT_CHATS, MARKET, DEFAULT_MODES } from "./mockData";
import { uid, timeNow } from "./utils";

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
  drawer: null | "servers" | "providers" | "marketplace" | "prompts" | "profile" | "modes";
  marketTab: string;
  serverTab: string;
  promptRecent: string[];
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

      setProvider: (id) => {
        const prov = get().providers.find((p) => p.id === id);
        if (!prov || !prov.enabled) return;
        set({ selectedProvider: id, selectedModel: prov.models[0] || get().selectedModel });
      },
      setModel: (m) => set({ selectedModel: m }),
      setMode: (id) => set({ selectedMode: id }),

      toggleServer: (id) => set((s) => ({
        servers: s.servers.map((srv) => srv.id === id ? { ...srv, status: srv.status === "connected" ? "disconnected" : "connected", health: srv.status === "connected" ? "off" : "ok", lastSeen: Date.now(), latency: Math.floor(Math.random() * 120 + 40) } as MCPServer : srv)
      })),
      addServer: (srv) => set((s) => ({ servers: [...s.servers, srv] })),
      updateServer: (id, patch) => set((s) => ({ servers: s.servers.map((x) => x.id === id ? { ...x, ...patch } : x) })),
      removeServer: (id) => set((s) => ({ servers: s.servers.filter((x) => x.id !== id) })),

      newChat: () => {
        const c: Chat = { id: uid(), title: "New conversation", created: Date.now(), archived: false, messages: [] };
        set((s) => ({ chats: [c, ...s.chats], activeChatId: c.id }));
      },
      selectChat: (id) => set({ activeChatId: id, sidebarOpen: false }),
      archiveChat: (id) => set((s) => ({ chats: s.chats.map((c) => c.id === id ? { ...c, archived: !c.archived } : c) })),
      deleteChat: (id) => set((s) => {
        const chats = s.chats.filter((c) => c.id !== id);
        let activeChatId = s.activeChatId;
        if (activeChatId === id) activeChatId = chats[0]?.id || null;
        return { chats, activeChatId };
      }),
      renameChat: (id, title) => set((s) => ({ chats: s.chats.map((c) => c.id === id ? { ...c, title: title.slice(0, 80) } : c) })),
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
        // simulate streaming via interval then finalize with mock html table
        let full = "";
        // choose response based on keyword
        const lower = trimmed.toLowerCase();
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
        const iv = setInterval(() => {
          i += Math.ceil(full.length / 24);
          if (i >= full.length) {
            clearInterval(iv);
            set((s) => ({
              chats: s.chats.map((c) => c.id === activeId ? {
                ...c,
                messages: c.messages.map((m) => m.id === assistId ? { ...m, text: full, streaming: false, html: lower.includes("postgres") ? "pg" as const : lower.includes("s3") ? "aws" as const : undefined } : m)
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
      },
      editMessage: (msgId, text) => {
        const aid = get().activeChatId;
        if (!aid) return;
        set((s) => ({ chats: s.chats.map((c) => c.id === aid ? { ...c, messages: c.messages.map((m) => m.id === msgId ? { ...m, text } : m) } : c) }));
      },
      deleteMessage: (msgId) => {
        const aid = get().activeChatId;
        if (!aid) return;
        set((s) => ({ chats: s.chats.map((c) => c.id === aid ? { ...c, messages: c.messages.filter((m) => m.id !== msgId) } : c) }));
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
      installMarket: (id) => set((s) => ({ market: s.market.map((m) => m.id === id ? { ...m, installed: !m.installed } : m) })),
      addMode: (m) => set((s) => ({ modes: [...s.modes, m] })),
      updateMode: (id, patch) => set((s) => ({ modes: s.modes.map((m) => m.id === id ? { ...m, ...patch } : m) })),
      deleteMode: (id) => set((s) => ({ modes: s.modes.filter((m) => m.id !== id), selectedMode: s.selectedMode === id ? "Chat" : s.selectedMode })),
      updateProvider: (id, patch) => set((s) => ({ providers: s.providers.map((p) => p.id === id ? { ...p, ...patch } : p) })),
      addProvider: (p) => set((s) => ({ providers: [...s.providers, p] })),
      deleteProvider: (id) => set((s) => ({ providers: s.providers.filter((p) => p.id !== id) })),
      recordPromptUse: (prompt) => set((s) => ({ promptRecent: [prompt, ...s.promptRecent.filter((p) => p !== prompt)].slice(0, 8) })),
    }),
    {
      name: "mcp-ai-chat-v2",
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
      }),
    }
  )
);
