import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { chatPatchSchema } from "@/lib/schemas";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// GET /api/chats/:id
export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const chat = await prisma.chat.findUnique({ where: { id }, include: { messages: { orderBy: { createdAt: "asc" } } } });
  if (!chat) return NextResponse.json({ error: "Not found" }, { status: 404 });
  return NextResponse.json({
    id: chat.id,
    title: chat.title,
    created: new Date(chat.createdAt).getTime(),
    archived: chat.archived,
    messages: (chat.messages || []).map((m: any) => ({
      id: m.id,
      role: m.role,
      text: m.content,
      html: m.html || undefined,
      time: new Date(m.createdAt).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
      status: m.status || "done",
      attachments: m.attachments || undefined,
    })),
  });
}

// PATCH /api/chats/:id  { title?, archived? }
export async function PATCH(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = chatPatchSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const exists = await prisma.chat.findUnique({ where: { id } });
  if (!exists) return NextResponse.json({ error: "Not found" }, { status: 404 });
  const updated = await prisma.chat.update({ where: { id }, data: parsed.data });
  return NextResponse.json({ id: updated.id, title: updated.title, archived: updated.archived });
}

// DELETE /api/chats/:id
export async function DELETE(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const exists = await prisma.chat.findUnique({ where: { id } });
  if (!exists) return NextResponse.json({ error: "Not found" }, { status: 404 });
  await prisma.chat.delete({ where: { id } });
  return NextResponse.json({ ok: true });
}
