import { NextResponse } from "next/server";
import { z } from "zod";
import { orchestrate, streamText } from "@/lib/mcp";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { auth } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const BodySchema = z.object({
  messages: z.array(z.object({ role: z.enum(["user", "assistant", "system"]), content: z.string() })).min(1),
  provider: z.string().optional(),
  model: z.string().optional(),
  mode: z.string().optional(),
  chatId: z.string().optional(),
  stream: z.boolean().optional(),
  attachments: z.array(z.object({ name: z.string(), size: z.number(), type: z.string() })).optional(),
});

function wantsStream(req: Request, bodyStream?: boolean) {
  const url = new URL(req.url);
  if (url.searchParams.get("stream") === "1" || url.searchParams.get("stream") === "true") return true;
  if (bodyStream) return true;
  const accept = req.headers.get("accept") || "";
  if (accept.includes("text/event-stream")) return true;
  return false;
}

async function persistIfEnabled({
  userId,
  chatId,
  userContent,
  assistantContent,
  assistantHtml,
  title,
}: {
  userId?: string | null;
  chatId?: string | null;
  userContent: string;
  assistantContent: string;
  assistantHtml?: string;
  title: string;
}) {
  if (!isDatabaseEnabled()) return null;
  try {
    const prisma = getPrisma();
    if (!prisma) return null;
    let chat = null as any;
    if (chatId) {
      chat = await prisma.chat.findUnique({ where: { id: chatId } });
    }
    if (!chat) {
      chat = await prisma.chat.create({
        data: {
          title: title.slice(0, 80) || "New conversation",
          userId: userId || undefined,
        },
      });
    }
    await prisma.message.create({
      data: { chatId: chat.id, role: "user", content: userContent, status: "done", attachments: undefined as any },
    });
    const assistant = await prisma.message.create({
      data: {
        chatId: chat.id,
        role: "assistant",
        content: assistantContent,
        html: assistantHtml as any,
        status: "done",
      },
    });
    await prisma.chat.update({ where: { id: chat.id }, data: { updatedAt: new Date() } });
    return { chatId: chat.id, assistantId: assistant.id };
  } catch (e) {
    console.warn("[api/chat] persist failed:", (e as Error).message);
    return null;
  }
}

export async function POST(req: Request) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const parsed = BodySchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid body", details: parsed.error.flatten() }, { status: 400 });
  }

  const { messages, provider, model, mode, chatId, stream: bodyStream, attachments } = parsed.data;
  const last = messages[messages.length - 1]?.content || "";
  const prompt = String(last).slice(0, 4000);

  // Optional auth — never blocks chat if unauthenticated (public MVP)
  let userId: string | null = null;
  try {
    const session = await auth();
    userId = (session as any)?.userId || (session as any)?.user?.id || null;
    if ((session as any)?.user?.email && !userId) userId = (session as any).user.email;
  } catch {
    // ignore auth errors (e.g. no DATABASE_URL)
  }

  const orchestrated = await orchestrate(prompt, { mode, servers: [] });
  const fullText = orchestrated.text;
  const html = orchestrated.html;
  const tools = orchestrated.tools;

  // Persist non-streaming immediately (best-effort)
  const shouldPersist = !!prompt.trim();
  const persistTitle = prompt.slice(0, 48);

  if (!wantsStream(req, bodyStream)) {
    if (shouldPersist) {
      const persisted = await persistIfEnabled({
        userId,
        chatId,
        userContent: prompt + (attachments?.length ? `\n[Attachments: ${attachments.map((a) => a.name).join(", ")}]` : ""),
        assistantContent: fullText,
        assistantHtml: html,
        title: persistTitle,
      });
      return NextResponse.json(
        {
          id: persisted?.assistantId || Date.now().toString(36),
          chatId: persisted?.chatId || chatId || null,
          role: "assistant",
          content: fullText,
          html,
          tools,
          provider: provider || "anthropic",
          model: model || "claude-3.5-sonnet",
          mode: mode || "Chat",
          created: Date.now(),
          persisted: !!persisted,
        },
        { headers: { "x-mcp-latency": String(tools[0]?.latencyMs || 142), "x-mcp-persisted": persisted ? "1" : "0" } }
      );
    }
    return NextResponse.json(
      {
        id: Date.now().toString(36),
        role: "assistant",
        content: fullText,
        html,
        tools,
        provider: provider || "anthropic",
        model: model || "claude-3.5-sonnet",
        mode: mode || "Chat",
        created: Date.now(),
        persisted: false,
      },
      { headers: { "x-mcp-latency": String(tools[0]?.latencyMs || 142) } }
    );
  }

  // ── Streaming SSE ──
  const encoder = new TextEncoder();
  let persistedChatId: string | null = chatId || null;

  // Persist user message eagerly (non-blocking)
  let persistDone: Promise<void> | null = null;
  if (shouldPersist && isDatabaseEnabled()) {
    persistDone = (async () => {
      const r = await persistIfEnabled({
        userId,
        chatId,
        userContent: prompt + (attachments?.length ? `\n[Attachments: ${attachments.map((a) => a.name).join(", ")}]` : ""),
        assistantContent: fullText,
        assistantHtml: html,
        title: persistTitle,
      });
      if (r?.chatId) persistedChatId = r.chatId;
    })();
  }

  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      const send = (event: string, data: unknown) => {
        controller.enqueue(encoder.encode(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`));
      };
      const sendData = (data: unknown) => {
        controller.enqueue(encoder.encode(`data: ${JSON.stringify(data)}\n\n`));
      };

      // meta
      send("meta", { provider: provider || "anthropic", model: model || "claude-3.5-sonnet", mode: mode || "Chat", tools, html, chatId: persistedChatId });

      // chunked text
      for await (const chunk of streamText(fullText, 24)) {
        sendData({ delta: chunk, done: false });
        // mimic network latency ~42ms as in client sim (lib/store.ts:163)
        await new Promise((r) => setTimeout(r, 42));
      }
      // ensure persistence finished before final
      if (persistDone) await persistDone.catch(() => {});
      send("done", { content: fullText, html, tools, chatId: persistedChatId, persisted: !!persistedChatId });
      controller.enqueue(encoder.encode(`data: [DONE]\n\n`));
      controller.close();
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache, no-transform",
      Connection: "keep-alive",
      "X-Accel-Buffering": "no",
      "x-mcp-latency": String(tools[0]?.latencyMs || 142),
    },
  });
}

export async function GET() {
  const db = isDatabaseEnabled();
  return NextResponse.json({
    providers: ["openai", "anthropic", "google", "local"],
    modes: ["Chat", "Code", "Research", "Planning", "Write", "Debug", "Shield", "Creative"],
    streaming: true,
    mcp: { healthPollMs: 30000, timeoutMs: 10000 },
    persistence: db ? "postgres" : "mock(localStorage)",
    auth: { enabled: true, strategy: db ? "database" : "jwt" },
  });
}
