import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { providerPatchSchema } from "@/lib/schemas";
import { encrypt } from "@/lib/encryption";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function PATCH(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = providerPatchSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const exists = await prisma.provider.findUnique({ where: { id } });
  if (!exists) return NextResponse.json({ error: "Not found" }, { status: 404 });
  const data: any = { ...parsed.data };
  if (data.apiKey) data.apiKey = await encrypt(data.apiKey);
  const updated = await prisma.provider.update({ where: { id }, data });
  return NextResponse.json({ ok: true, provider: { ...updated, apiKey: updated.apiKey ? "••••" : "" } });
}

export async function DELETE(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const exists = await prisma.provider.findUnique({ where: { id } });
  if (!exists) return NextResponse.json({ error: "Not found" }, { status: 404 });
  await prisma.provider.delete({ where: { id } });
  return NextResponse.json({ ok: true });
}
