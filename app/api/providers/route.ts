import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { auth } from "@/lib/auth";
import { providerCreateSchema } from "@/lib/schemas";
import { DEFAULT_PROVIDERS } from "@/lib/mockData";
import { encrypt } from "@/lib/encryption";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  if (!isDatabaseEnabled()) return NextResponse.json({ persistence: "mock", providers: DEFAULT_PROVIDERS });
  let userId: string | undefined;
  try { const s = await auth(); userId = (s as any)?.userId || undefined; } catch {}
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const where: any = userId ? { userId } : {};
  const providers = await prisma.provider.findMany({ where, orderBy: { name: "asc" } });
  // decrypt masked? Return without raw key for security — mask
  const mapped = providers.map((p: any) => ({ ...p, apiKey: p.apiKey ? "••••" + String(p.apiKey).slice(-4) : "" }));
  return NextResponse.json({ persistence: "postgres", providers: mapped });
}

export async function POST(req: Request) {
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = providerCreateSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  let userId: string | undefined;
  try { const s = await auth(); userId = (s as any)?.userId || undefined; } catch {}
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const exists = await prisma.provider.findUnique({ where: { id: parsed.data.id } });
  if (exists) return NextResponse.json({ error: "Provider id already exists" }, { status: 409 });
  const apiKeyEnc = parsed.data.apiKey ? await encrypt(parsed.data.apiKey) : null;
  const created = await prisma.provider.create({
    data: { ...parsed.data, apiKey: apiKeyEnc as any, userId: userId || undefined } as any,
  });
  return NextResponse.json({ ok: true, provider: { ...created, apiKey: created.apiKey ? "••••" : "" } }, { status: 201 });
}
