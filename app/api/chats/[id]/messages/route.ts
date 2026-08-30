import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { messageCreateSchema } from "@/lib/schemas";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// GET /api/chats/:id/messages
export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const chat = await prisma.chat.findUnique({ where: { id } });
  if (!chat) return NextResponse.json({ error: "Chat not found" }, { status: 404 });
  const messages = await prisma.message.findMany({ where: { chatId: id }, orderBy: { createdAt: "asc" } });
  return NextResponse.json({
    chatId: id,
    messages: messages.map((m: any) => ({
      id: m.id,
      role: m.role,
      text: m.content,
      html: m.html || undefined,
      time: new Date(m.createdAt).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
      status: m.status || "done",
      tasks: m.tasks || undefined,
      todos: m.todos || undefined,
      attachments: m.attachments || undefined,
      createdAt: m.createdAt,
    })),
  });
}

// POST /api/chats/:id/messages  { role, content, html?, attachments? }
export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isDatabaseEnabled()) return NextResponse.json({ error: "DB not configured" }, { status: 503 });
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = messageCreateSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  const chat = await prisma.chat.findUnique({ where: { id } });
  if (!chat) return NextResponse.json({ error: "Chat not found" }, { status: 404 });
  const msg = await prisma.message.create({
    data: {
      chatId: id,
      role: parsed.data.role as any,
      content: parsed.data.content,
      html: parsed.data.html as any,
      status: parsed.data.status || "done",
      tasks: (parsed.data as any).tasks as any,
      todos: (parsed.data as any).todos as any,
      attachments: parsed.data.attachments as any,
    },
  });
  await prisma.chat.update({ where: { id }, data: { updatedAt: new Date() } });
  return NextResponse.json({
    id: msg.id,
    role: msg.role,
    text: msg.content,
    html: (msg as any).html || undefined,
    status: (msg as any).status || "done",
    tasks: (msg as any).tasks || undefined,
    todos: (msg as any).todos || undefined,
    attachments: (msg as any).attachments || undefined,
    time: new Date(msg.createdAt).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
  }, { status: 201 });
}
