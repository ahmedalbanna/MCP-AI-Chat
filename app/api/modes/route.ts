import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { modeCreateSchema } from "@/lib/schemas";
import { DEFAULT_MODES } from "@/lib/mockData";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  if (!isDatabaseEnabled()) return NextResponse.json({ persistence: "mock", modes: DEFAULT_MODES });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const modes = await prisma.agentMode.findMany({ orderBy: { name: "asc" } });
  if (modes.length === 0) return NextResponse.json({ persistence: "postgres", modes: DEFAULT_MODES, seeded: false });
  const mapped = modes.map((m: any) => ({
    id: m.id,
    name: m.name,
    icon: m.icon,
    color: m.color,
    desc: m.desc,
    prompt: m.prompt,
    temp: m.temp,
    tokens: m.tokens,
    provider: m.provider || "",
    model: m.model || "",
    servers: m.servers || [],
    tools: m.tools || { sql: true, fs: true, cloud: true, web: false },
    builtin: m.builtin,
  }));
  return NextResponse.json({ persistence: "postgres", modes: mapped });
}

export async function POST(req: Request) {
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = modeCreateSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const exists = await prisma.agentMode.findUnique({ where: { id: parsed.data.id } });
  if (exists) return NextResponse.json({ error: "Mode id already exists" }, { status: 409 });
  const created = await prisma.agentMode.create({ data: parsed.data as any });
  return NextResponse.json({ ok: true, mode: created }, { status: 201 });
}
