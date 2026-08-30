import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { auth } from "@/lib/auth";
import { chatCreateSchema, paginationSchema } from "@/lib/schemas";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// GET /api/chats?page=&limit=&q=&archived=
export async function GET(req: Request) {
  if (!isDatabaseEnabled()) {
    return NextResponse.json({ persistence: "mock", chats: [], total: 0, page: 1, limit: 20, note: "Set DATABASE_URL to enable Postgres persistence" });
  }
  const url = new URL(req.url);
  const parsed = paginationSchema.safeParse({
    page: url.searchParams.get("page") || undefined,
    limit: url.searchParams.get("limit") || undefined,
    q: url.searchParams.get("q") || undefined,
    archived: url.searchParams.get("archived") || undefined,
  });
  const { page, limit, q, archived } = parsed.success ? parsed.data : { page: 1, limit: 20, q: undefined, archived: undefined };

  let userId: string | undefined;
  try { const s = await auth(); userId = (s as any)?.userId || (s as any)?.user?.id || undefined; } catch {}

  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });

  const where: any = {};
  if (typeof archived === "boolean") where.archived = archived;
  if (q) where.title = { contains: q, mode: "insensitive" };
  if (userId) where.userId = userId;

  const [total, chats] = await Promise.all([
    prisma.chat.count({ where }),
    prisma.chat.findMany({
      where,
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * limit,
      take: limit,
      include: { messages: { orderBy: { createdAt: "asc" } } },
    }),
  ]);

  // map to client Chat shape
  const mapped = chats.map((c: any) => ({
    id: c.id,
    title: c.title,
    created: new Date(c.createdAt).getTime(),
    archived: c.archived,
    messages: (c.messages || []).map((m: any) => ({
      id: m.id,
      role: m.role,
      text: m.content,
      html: m.html || undefined,
      time: new Date(m.createdAt).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
      status: m.status || "done",
      attachments: m.attachments || undefined,
    })),
  }));

  return NextResponse.json({ persistence: "postgres", chats: mapped, total, page, limit });
}

// POST /api/chats  { title }
export async function POST(req: Request) {
  if (!isDatabaseEnabled()) {
    return NextResponse.json({ error: "DB not configured — chat created locally only", persistence: "mock" }, { status: 503 });
  }
  let body: unknown;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const parsed = chatCreateSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });

  let userId: string | undefined;
  try { const s = await auth(); userId = (s as any)?.userId || (s as any)?.user?.id || undefined; } catch {}

  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });

  const chat = await prisma.chat.create({
    data: { title: parsed.data.title, archived: parsed.data.archived ?? false, userId: userId || undefined },
  });

  return NextResponse.json({
    id: chat.id,
    title: chat.title,
    created: new Date(chat.createdAt).getTime(),
    archived: chat.archived,
    messages: [],
  }, { status: 201 });
}
