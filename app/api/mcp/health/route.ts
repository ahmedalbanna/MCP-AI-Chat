import { NextResponse } from "next/server";
import { isDatabaseEnabled } from "@/lib/db";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  let live: any = null;
  let error: string | null = null;
  try {
    const { getHealth, listAllTools } = await import("@/lib/mcp/registry");
    const [health, tools] = await Promise.all([getHealth().catch(() => []), listAllTools().catch(() => [])]);
    live = {
      servers: health.map((h: any) => ({
        id: h.server.id,
        name: h.server.name,
        transport: h.server.transport,
        endpoint: h.server.endpoint,
        status: h.status,
        health: h.health,
        latency: h.latency,
        lastSeen: h.lastSeen,
        tools: h.tools.map((t: any) => t.name),
        error: h.error || null,
      })),
      tools,
    };
  } catch (e) {
    error = (e as Error).message;
  }

  // Fallback to mock if no live data
  if (!live || !live.servers || live.servers.length === 0) {
    try {
      const { DEFAULT_SERVERS } = await import("@/lib/mockData");
      live = {
        servers: DEFAULT_SERVERS.map((s) => ({
          id: s.id,
          name: s.name,
          transport: s.transport,
          endpoint: s.endpoint,
          status: s.status,
          health: s.health,
          latency: s.latency,
          lastSeen: s.lastSeen,
          tools: s.tools.split(",").map((t) => t.trim()),
          error: "mock",
        })),
        tools: [],
        fallback: "mock",
      };
    } catch {}
  }

  return NextResponse.json({
    status: "ok",
    persistence: isDatabaseEnabled() ? "postgres" : "mock",
    mcp: live,
    ts: Date.now(),
    error,
  });
}

export async function POST(req: Request) {
  // Trigger health check for a specific server: { serverId }
  let body: any = {};
  try { body = await req.json(); } catch {}
  const serverId = body.serverId || body.id;
  if (!serverId) return NextResponse.json({ error: "serverId required" }, { status: 400 });
  try {
    const { healthCheck } = await import("@/lib/mcp/registry");
    const entry = await healthCheck(serverId);
    if (!entry) return NextResponse.json({ error: "not found" }, { status: 404 });
    return NextResponse.json({ ok: true, server: { id: entry.server.id, name: entry.server.name, status: entry.status, health: entry.health, latency: entry.latency, tools: entry.tools } });
  } catch (e) {
    return NextResponse.json({ error: (e as Error).message }, { status: 500 });
  }
}
