import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { auth } from "@/lib/auth";
import { serverCreateSchema } from "@/lib/schemas";
import { DEFAULT_SERVERS } from "@/lib/mockData";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  if (!isDatabaseEnabled()) {
    return NextResponse.json({ persistence: "mock", servers: DEFAULT_SERVERS });
  }
  let userId: string | undefined;
  try { const s = await auth(); userId = (s as any)?.userId || undefined; } catch {}
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const where: any = userId ? { userId } : {};
  const servers = await prisma.server.findMany({ where, orderBy: { createdAt: "asc" } });
  const mapped = servers.map((s: any) => ({
    id: s.id,
    name: s.name,
    transport: s.transport,
    endpoint: s.endpoint,
    tools: s.tools,
    status: s.status,
    color: s.color,
    desc: s.desc || "",
    timeout: s.timeout,
    retries: s.retries,
    env: s.env || [],
    latency: s.latency || 0,
    health: s.health || "off",
    lastSeen: s.updatedAt ? new Date(s.updatedAt).getTime() : Date.now(),
    autoConnect: s.autoConnect,
    healthCheck: s.healthCheck,
  }));
  return NextResponse.json({ persistence: "postgres", servers: mapped });
}

export async function POST(req: Request) {
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = serverCreateSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  let userId: string | undefined;
  try { const s = await auth(); userId = (s as any)?.userId || undefined; } catch {}
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  try {
    const created = await prisma.server.create({ data: { ...parsed.data, tools: parsed.data.tools || "", userId: userId || undefined } as any });
    return NextResponse.json({ id: created.id, ...parsed.data }, { status: 201 });
  } catch (e: any) {
    if (String(e.message).includes("Unique")) return NextResponse.json({ error: "Server name already exists" }, { status: 409 });
    return NextResponse.json({ error: "Create failed", details: e.message }, { status: 500 });
  }
}
