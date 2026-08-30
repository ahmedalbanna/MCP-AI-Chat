import type { MCPServer } from "../types";
import { env } from "../env";

// Registry is server-only (nodejs) — never bundle to client.
// Uses MCP SDK clients for real transports, with mock fallback.

type ToolDef = { name: string; description?: string; inputSchema?: unknown };

type Entry = {
  server: MCPServer;
  status: "connected" | "disconnected" | "connecting" | "error";
  latency: number;
  health: "ok" | "off" | "warn";
  lastSeen: number;
  tools: ToolDef[];
  error?: string;
};

const globalForRegistry = globalThis as unknown as {
  __mcpRegistry?: Map<string, Entry>;
  __mcpClients?: Map<string, any>;
};

function getMaps() {
  if (!globalForRegistry.__mcpRegistry) globalForRegistry.__mcpRegistry = new Map();
  if (!globalForRegistry.__mcpClients) globalForRegistry.__mcpClients = new Map();
  return { registry: globalForRegistry.__mcpRegistry!, clients: globalForRegistry.__mcpClients! };
}

function parseStdio(endpoint: string): { command: string; args: string[]; env?: Record<string, string> } {
  // endpoint examples: "npx @modelcontextprotocol/server-postgres" or "npx @modelcontextprotocol/server-filesystem /data"
  const parts = endpoint.trim().split(/\s+/);
  if (parts.length === 0) return { command: "npx", args: [] };
  const command = parts[0];
  const args = parts.slice(1);
  return { command, args };
}

async function createRealClient(server: MCPServer): Promise<{ client: any; tools: ToolDef[]; latency: number } | null> {
  try {
    // Dynamic imports — keep edge bundle clean and allow graceful fallback when SDK not available
    const { Client } = await import("@modelcontextprotocol/sdk/client/index.js");
    const start = Date.now();

    let transport: any = null;

    if (server.transport === "stdio") {
      const { StdioClientTransport } = await import("@modelcontextprotocol/sdk/client/stdio.js");
      const { command, args } = parseStdio(server.endpoint);
      // Merge env from server.env + process.env
      const envMap: Record<string, string> = {};
      for (const kv of server.env || []) if (kv.k) envMap[kv.k] = kv.v;
      transport = new StdioClientTransport({
        command,
        args,
        env: { ...process.env as Record<string, string>, ...envMap },
        stderr: "pipe",
      } as any);
    } else if (server.transport === "sse") {
      const { SSEClientTransport } = await import("@modelcontextprotocol/sdk/client/sse.js");
      // SSEClientTransport expects URL; handle wss -> https conversion
      let url = server.endpoint;
      if (url.startsWith("wss://")) url = url.replace("wss://", "https://");
      if (url.startsWith("ws://")) url = url.replace("ws://", "http://");
      transport = new SSEClientTransport(new URL(url));
    } else if (server.transport === "http") {
      const { StreamableHTTPClientTransport } = await import("@modelcontextprotocol/sdk/client/streamableHttp.js");
      transport = new StreamableHTTPClientTransport(new URL(server.endpoint));
    } else {
      return null;
    }

    const client = new Client({ name: "mcp-ai-chat", version: "2.0.0" }, { capabilities: {} });
    // Add timeout via Promise.race
    const timeoutMs = server.timeout || Number(env.MCP_TIMEOUT_MS) || 10000;
    const connectPromise = client.connect(transport);
    const timeout = new Promise((_, rej) => setTimeout(() => rej(new Error(`connect timeout ${timeoutMs}ms`)), timeoutMs));
    await Promise.race([connectPromise, timeout]);

    // list tools with timeout
    const listStart = Date.now();
    const toolsRes: any = await Promise.race([
      client.listTools(),
      new Promise((_, rej) => setTimeout(() => rej(new Error("listTools timeout")), 6000)),
    ]);
    const latency = Date.now() - listStart;
    const tools: ToolDef[] = (toolsRes?.tools || []).map((t: any) => ({ name: t.name, description: t.description, inputSchema: t.inputSchema }));

    const totalLatency = Date.now() - start;
    return { client, tools, latency: latency || totalLatency };
  } catch (e) {
    console.warn(`[mcp] real connect failed for ${server.name} (${server.transport} ${server.endpoint}):`, (e as Error).message);
    return null;
  }
}

export async function ensureConnected(server: MCPServer): Promise<Entry> {
  const { registry, clients } = getMaps();
  const existing = registry.get(server.id);
  if (existing && existing.status === "connected" && clients.has(server.id)) {
    // health staleness check — if lastSeen > pollMs, re-list
    const pollMs = Number(env.MCP_HEALTH_POLL_MS) || 30000;
    if (Date.now() - existing.lastSeen < pollMs) return existing;
  }

  // mark connecting
  registry.set(server.id, {
    server,
    status: "connecting",
    latency: existing?.latency || 0,
    health: "warn",
    lastSeen: Date.now(),
    tools: existing?.tools || [],
  });

  const real = await createRealClient(server);
  if (real) {
    clients.set(server.id, real.client);
    const entry: Entry = {
      server,
      status: "connected",
      latency: real.latency,
      health: "ok",
      lastSeen: Date.now(),
      tools: real.tools,
    };
    registry.set(server.id, entry);
    return entry;
  }

  // fallback: mock tools based on server.name/tools string
  const mockTools: ToolDef[] = (server.tools || "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
    .map((name) => ({ name }));
  const entry: Entry = {
    server,
    status: server.status === "connected" ? "connected" : "disconnected",
    latency: server.latency || 0,
    health: server.health as any || "off",
    lastSeen: Date.now(),
    tools: mockTools.length ? mockTools : [{ name: "mock_tool" }],
    error: "mock fallback",
  };
  // do not store real client
  registry.set(server.id, entry);
  return entry;
}

export async function callTool(serverName: string, toolName: string, args: Record<string, unknown> = {}): Promise<{ ok: boolean; data: unknown; latency: number; error?: string }> {
  const { registry, clients } = getMaps();
  // find server by name
  let entry: Entry | undefined;
  for (const e of registry.values()) if (e.server.name === serverName) entry = e;
  if (!entry) return { ok: false, data: null, latency: 0, error: "server not found" };

  const client = clients.get(entry.server.id);
  if (!client) {
    // mock tool call
    const latency = Math.floor(Math.random() * 80 + 20);
    await new Promise((r) => setTimeout(r, latency));
    return { ok: true, data: { mock: true, tool: toolName, args }, latency };
  }

  const start = Date.now();
  try {
    const res: any = await Promise.race([
      client.callTool({ name: toolName, arguments: args }),
      new Promise((_, rej) => setTimeout(() => rej(new Error("callTool timeout")), entry.server.timeout || 10000)),
    ]);
    return { ok: true, data: res, latency: Date.now() - start };
  } catch (e) {
    return { ok: false, data: null, latency: Date.now() - start, error: (e as Error).message };
  }
}

export async function listAllTools(): Promise<{ server: string; tools: ToolDef[]; status: string; latency: number }[]> {
  const { registry } = getMaps();
  return [...registry.values()].map((e) => ({ server: e.server.name, tools: e.tools, status: e.status, latency: e.latency }));
}

export async function getHealth(): Promise<Entry[]> {
  const { registry } = getMaps();
  return [...registry.values()];
}

export async function disconnect(serverId: string): Promise<void> {
  const { registry, clients } = getMaps();
  const client = clients.get(serverId);
  if (client) {
    try { await client.close(); } catch {}
    clients.delete(serverId);
  }
  const entry = registry.get(serverId);
  if (entry) registry.set(serverId, { ...entry, status: "disconnected", health: "off", lastSeen: Date.now() });
}

export async function connectAll(servers: MCPServer[]): Promise<Entry[]> {
  const results: Entry[] = [];
  for (const s of servers) {
    const e = await ensureConnected(s);
    results.push(e);
  }
  return results;
}

export async function healthCheck(serverId: string): Promise<Entry | null> {
  const { registry } = getMaps();
  const entry = registry.get(serverId);
  if (!entry) return null;
  // re-ensure connection to measure latency
  return ensureConnected(entry.server);
}
