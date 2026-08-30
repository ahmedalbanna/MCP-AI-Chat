import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { messagePatchSchema } from "@/lib/schemas";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// PATCH /api/chats/:id/messages/:messageId  { content?, tasks?, todos?, html? }
export async function PATCH(req: Request, { params }: { params: Promise<{ id: string; messageId: string }> }) {
  const { id, messageId } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = messagePatchSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const msg = await prisma.message.findUnique({ where: { id: messageId } });
  if (!msg || (msg as any).chatId !== id) return NextResponse.json({ error: "Not found" }, { status: 404 });
  const data: any = {};
  if (parsed.data.content !== undefined) data.content = parsed.data.content;
  if ((parsed.data as any).html !== undefined) data.html = (parsed.data as any).html;
  if ((parsed.data as any).tasks !== undefined) data.tasks = (parsed.data as any).tasks;
  if ((parsed.data as any).todos !== undefined) data.todos = (parsed.data as any).todos;
  if ((parsed.data as any).attachments !== undefined) data.attachments = (parsed.data as any).attachments;
  if ((parsed.data as any).status !== undefined) data.status = (parsed.data as any).status;
  // compat: allow `text` alias for content (client sends text)
  const raw: any = body;
  if (raw.text !== undefined && data.content === undefined) data.content = raw.text;
  const updated = await prisma.message.update({ where: { id: messageId }, data });
  await prisma.chat.update({ where: { id }, data: { updatedAt: new Date() } });
  return NextResponse.json({
    id: updated.id,
    role: (updated as any).role,
    text: (updated as any).content,
    html: (updated as any).html || undefined,
    tasks: (updated as any).tasks || undefined,
    todos: (updated as any).todos || undefined,
    attachments: (updated as any).attachments || undefined,
    time: new Date((updated as any).createdAt).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
  });
}

// DELETE /api/chats/:id/messages/:messageId
export async function DELETE(_req: Request, { params }: { params: Promise<{ id: string; messageId: string }> }) {
  const { id, messageId } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const msg = await prisma.message.findUnique({ where: { id: messageId } });
  if (!msg || (msg as any).chatId !== id) return NextResponse.json({ error: "Not found" }, { status: 404 });
  await prisma.message.delete({ where: { id: messageId } });
  await prisma.chat.update({ where: { id }, data: { updatedAt: new Date() } });
  return NextResponse.json({ ok: true });
}
