import { NextResponse } from "next/server";
import { z } from "zod";
import { orchestrate, streamText } from "@/lib/mcp";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { auth } from "@/lib/auth";
import { DEFAULT_MODES, DEFAULT_PROVIDERS } from "@/lib/mockData";

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
  tasks,
  todos,
  attachments,
}: {
  userId?: string | null;
  chatId?: string | null;
  userContent: string;
  assistantContent: string;
  assistantHtml?: string;
  title: string;
  tasks?: any;
  todos?: any;
  attachments?: any;
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
      data: { chatId: chat.id, role: "user", content: userContent, status: "done", attachments: (attachments as any) || undefined },
    });
    const assistant = await prisma.message.create({
      data: {
        chatId: chat.id,
        role: "assistant",
        content: assistantContent,
        html: assistantHtml as any,
        status: "done",
        tasks: (tasks as any) || undefined,
        todos: (todos as any) || undefined,
      },
    });
    await prisma.chat.update({ where: { id: chat.id }, data: { updatedAt: new Date() } });
    return { chatId: chat.id, assistantId: assistant.id };
  } catch (e) {
    console.warn("[api/chat] persist failed:", (e as Error).message);
    return null;
  }
}

async function getProviderRecord(providerId: string): Promise<any | null> {
  if (!isDatabaseEnabled()) {
    return DEFAULT_PROVIDERS.find((p) => p.id === providerId) || null;
  }
  try {
    const prisma = getPrisma();
    if (!prisma) return DEFAULT_PROVIDERS.find((p) => p.id === providerId) || null;
    const rec = await prisma.provider.findUnique({ where: { id: providerId } });
    if (rec) return rec;
    return DEFAULT_PROVIDERS.find((p) => p.id === providerId) || null;
  } catch {
    return DEFAULT_PROVIDERS.find((p) => p.id === providerId) || null;
  }
}

async function getModeRecord(modeId: string): Promise<any | null> {
  if (!isDatabaseEnabled()) return DEFAULT_MODES.find((m) => m.id === modeId) || DEFAULT_MODES[0];
  try {
    const prisma = getPrisma();
    if (!prisma) return DEFAULT_MODES.find((m) => m.id === modeId) || null;
    const rec = await prisma.agentMode.findUnique({ where: { id: modeId } });
    if (rec) return rec;
    return DEFAULT_MODES.find((m) => m.id === modeId) || null;
  } catch {
    return DEFAULT_MODES.find((m) => m.id === modeId) || null;
  }
}

async function loadServers(): Promise<any[]> {
  if (!isDatabaseEnabled()) {
    const { DEFAULT_SERVERS } = await import("@/lib/mockData");
    return DEFAULT_SERVERS;
  }
  try {
    const prisma = getPrisma();
    if (!prisma) {
      const { DEFAULT_SERVERS } = await import("@/lib/mockData");
      return DEFAULT_SERVERS;
    }
    const servers = await prisma.server.findMany({ orderBy: { createdAt: "asc" } });
    if (servers.length === 0) {
      const { DEFAULT_SERVERS } = await import("@/lib/mockData");
      return DEFAULT_SERVERS;
    }
    return servers.map((s: any) => ({
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
  } catch {
    const { DEFAULT_SERVERS } = await import("@/lib/mockData");
    return DEFAULT_SERVERS;
  }
}

// ── AI path helpers ──
async function tryAIStreaming({
  prompt,
  messages,
  providerId,
  modelId,
  modeId,
  providerRecord,
  modeRecord,
}: {
  prompt: string;
  messages: { role: string; content: string }[];
  providerId: string;
  modelId: string;
  modeId: string;
  providerRecord: any;
  modeRecord: any;
}): Promise<{ stream: ReadableStream<Uint8Array>; headers: Record<string, string>; fullTextPromise: Promise<{ text: string; html?: string; tools: any[] }> } | null> {
  try {
    const { getAIModel } = await import("@/lib/ai/providers");
    const apiKeyEnc = providerRecord?.apiKey && !String(providerRecord.apiKey).startsWith("•") ? providerRecord.apiKey : null;
    const baseUrl = providerRecord?.baseUrl || undefined;
    const aiModel = await getAIModel(providerId, modelId, { apiKeyEncrypted: apiKeyEnc, baseUrl });
    if (!aiModel) return null;

    // MCP tools
    let mcpTools: Record<string, any> = {};
    let registryTools: any[] = [];
    try {
      const servers = await loadServers();
      // Best-effort connect all (parallel, no throw)
      const { connectAll, listAllTools } = await import("@/lib/mcp/registry");
      await connectAll(servers as any).catch(() => {});
      registryTools = await listAllTools().catch(() => []);
      const { buildMCPTools } = await import("@/lib/ai/mcp-tools");
      mcpTools = await buildMCPTools(registryTools).catch(() => ({}));
      if (Object.keys(mcpTools).length === 0) {
        const { buildMockToolsFallback } = await import("@/lib/ai/mcp-tools");
        mcpTools = buildMockToolsFallback();
      }
    } catch {
      const { buildMockToolsFallback } = await import("@/lib/ai/mcp-tools");
      mcpTools = buildMockToolsFallback();
    }

    const { streamText: aiStreamText } = await import("ai");

    const system = modeRecord?.prompt || `You are a helpful assistant in ${modeId} mode. Use MCP tools when relevant.`;
    const temp = typeof modeRecord?.temp === "number" ? modeRecord.temp : 0.7;

    const result = aiStreamText({
      model: aiModel,
      system,
      messages: messages as any,
      tools: mcpTools,
      maxSteps: 4,
      temperature: temp,
    } as any);

    let fullText = "";
    let html: string | undefined = undefined;
    const toolsUsed: any[] = [];

    // We need to capture tool calls for metadata
    // result.toolCalls is a promise that resolves after streaming; we can poll via result.steps
    const encoder = new TextEncoder();
    const stream = new ReadableStream<Uint8Array>({
      async start(controller) {
        const send = (event: string, data: unknown) => {
          controller.enqueue(encoder.encode(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`));
        };
        const sendData = (data: unknown) => {
          controller.enqueue(encoder.encode(`data: ${JSON.stringify(data)}\n\n`));
        };

        // Determine html hint based on prompt/tools
        const lower = prompt.toLowerCase();
        if (lower.includes("postgres") || lower.includes("sql")) html = "pg";
        else if (lower.includes("s3") || lower.includes("bucket") || lower.includes("aws")) html = "aws";

        send("meta", { provider: providerId, model: modelId, mode: modeId, tools: registryTools, html, streaming: "ai" });

        try {
          for await (const delta of result.textStream) {
            fullText += delta;
            sendData({ delta: fullText, done: false });
          }
          // After textStream, check for tool results to enrich response
          try {
            const steps = await result.steps;
            for (const step of steps || []) {
              for (const tc of (step as any).toolCalls || []) toolsUsed.push({ server: tc.toolName, tool: tc.toolName });
              for (const tr of (step as any).toolResults || []) toolsUsed.push({ result: tr.result });
            }
          } catch {}
          // Fallback html detection from tool usage
          if (!html && toolsUsed.some((t) => String(t.tool || t.server).includes("postgres"))) html = "pg";
          if (!html && toolsUsed.some((t) => String(t.tool || t.server).includes("s3"))) html = "aws";

          send("done", { content: fullText, html, tools: toolsUsed.length ? toolsUsed : registryTools, streaming: "ai" });
          controller.enqueue(encoder.encode(`data: [DONE]\n\n`));
          controller.close();
        } catch (e) {
          console.warn("[api/chat] AI stream error:", (e as Error).message);
          // Fallback: send what we have and close
          send("done", { content: fullText || "AI streaming interrupted", html, tools: toolsUsed });
          controller.enqueue(encoder.encode(`data: [DONE]\n\n`));
          controller.close();
        }
      },
    });

    const fullTextPromise = (async () => {
      try {
        // Wait for full completion (result.text is a promise)
        const text = await result.text;
        fullText = text;
        return { text, html, tools: toolsUsed.length ? toolsUsed : registryTools };
      } catch {
        return { text: fullText, html, tools: toolsUsed };
      }
    })();

    return { stream, headers: { "x-mcp-ai": "1", "x-mcp-provider": providerId }, fullTextPromise };
  } catch (e) {
    console.warn("[api/chat] tryAIStreaming failed:", (e as Error).message);
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
  const providerId = provider || "anthropic";
  const modelId = model || "claude-3.5-sonnet";
  const modeId = mode || "Chat";

  // Optional auth — never blocks chat if unauthenticated (public MVP)
  let userId: string | null = null;
  try {
    const session = await auth();
    userId = (session as any)?.userId || (session as any)?.user?.id || null;
    if ((session as any)?.user?.email && !userId) userId = (session as any).user.email;
  } catch {
    // ignore auth errors (e.g. no DATABASE_URL)
  }

  const shouldStream = wantsStream(req, bodyStream);
  const shouldPersist = !!prompt.trim();
  const persistTitle = prompt.slice(0, 48);

  // ── Try AI path first when streaming or non-streaming ──
  if (shouldStream) {
    // Attempt AI streaming; on success, return SSE with persistence handling
    const providerRecord = await getProviderRecord(providerId);
    const modeRecord = await getModeRecord(modeId);
    const aiAttempt = await tryAIStreaming({ prompt, messages, providerId, modelId, modeId, providerRecord, modeRecord });

    if (aiAttempt) {
      let persistedChatId: string | null = chatId || null;
      let persistDone: Promise<any> | null = null;
      // We don't know fullText yet; persist after AI completes
      const { stream, headers, fullTextPromise } = aiAttempt;

      if (shouldPersist && isDatabaseEnabled()) {
        persistDone = (async () => {
          const { text: full, html } = await fullTextPromise.catch(() => ({ text: prompt, html: undefined }));
          const r = await persistIfEnabled({
            userId,
            chatId,
            userContent: prompt + (attachments?.length ? `\n[Attachments: ${attachments.map((a) => a.name).join(", ")}]` : ""),
            assistantContent: full || prompt,
            assistantHtml: html,
            title: persistTitle,
            attachments,
          });
          if (r?.chatId) persistedChatId = r.chatId;
        })();
      }

      // Wrap original AI stream to ensure persistence completes before close
      const encoder = new TextEncoder();
      const wrapped = new ReadableStream<Uint8Array>({
        async start(controller) {
          const reader = stream.getReader();
          try {
            while (true) {
              const { done, value } = await reader.read();
              if (done) break;
              controller.enqueue(value);
            }
            if (persistDone) await persistDone.catch(() => {});
            controller.close();
          } catch (e) {
            controller.error(e);
          }
        },
      });

      return new Response(wrapped, {
        headers: {
          "Content-Type": "text/event-stream",
          "Cache-Control": "no-cache, no-transform",
          Connection: "keep-alive",
          "X-Accel-Buffering": "no",
          ...headers,
        },
      });
    }
    // else fall through to mock streaming
  } else {
    // Non-streaming: try AI generateText
    try {
      const providerRecord = await getProviderRecord(providerId);
      const modeRecord = await getModeRecord(modeId);
      const { getAIModel } = await import("@/lib/ai/providers");
      const apiKeyEnc = providerRecord?.apiKey && !String(providerRecord.apiKey).startsWith("•") ? providerRecord.apiKey : null;
      const aiModel = await getAIModel(providerId, modelId, { apiKeyEncrypted: apiKeyEnc, baseUrl: providerRecord?.baseUrl });
      if (aiModel) {
        const { generateText } = await import("ai");
        const servers = await loadServers();
        let mcpTools: Record<string, any> = {};
        try {
          const { connectAll, listAllTools } = await import("@/lib/mcp/registry");
          await connectAll(servers as any).catch(() => {});
          const regTools = await listAllTools().catch(() => []);
          const { buildMCPTools, buildMockToolsFallback } = await import("@/lib/ai/mcp-tools");
          mcpTools = await buildMCPTools(regTools).catch(() => ({}));
          if (Object.keys(mcpTools).length === 0) mcpTools = buildMockToolsFallback();
        } catch {
          const { buildMockToolsFallback } = await import("@/lib/ai/mcp-tools");
          mcpTools = buildMockToolsFallback();
        }
        const system = modeRecord?.prompt || `You are a helpful assistant in ${modeId} mode.`;
        const result: any = await generateText({
          model: aiModel,
          system,
          messages: messages as any,
          tools: mcpTools,
          maxSteps: 4,
          temperature: modeRecord?.temp ?? 0.7,
        } as any);
        const full = result.text || "";
        const lower = prompt.toLowerCase();
        let html: string | undefined;
        if (lower.includes("postgres") || lower.includes("sql")) html = "pg";
        else if (lower.includes("s3") || lower.includes("bucket")) html = "aws";
        const tools = (result.toolResults || result.steps || []) as any[];

        if (shouldPersist) {
          const persisted = await persistIfEnabled({
            userId,
            chatId,
            userContent: prompt + (attachments?.length ? `\n[Attachments: ${attachments.map((a) => a.name).join(", ")}]` : ""),
            assistantContent: full,
            assistantHtml: html,
            title: persistTitle,
            attachments,
          });
          return NextResponse.json(
            {
              id: persisted?.assistantId || Date.now().toString(36),
              chatId: persisted?.chatId || chatId || null,
              role: "assistant",
              content: full,
              html,
              tools,
              provider: providerId,
              model: modelId,
              mode: modeId,
              created: Date.now(),
              persisted: !!persisted,
              streaming: "ai",
            },
            { headers: { "x-mcp-latency": "ai", "x-mcp-ai": "1" } }
          );
        }
        return NextResponse.json({
          id: Date.now().toString(36),
          role: "assistant",
          content: full,
          html,
          tools,
          provider: providerId,
          model: modelId,
          mode: modeId,
          created: Date.now(),
          persisted: false,
          streaming: "ai",
        });
      }
    } catch (e) {
      console.warn("[api/chat] AI generate fallback:", (e as Error).message);
    }
  }

  // ── Mock fallback (deterministic, no API keys needed) ──
  const orchestrated = await orchestrate(prompt, { mode: modeId, servers: [] });
  const fullText = orchestrated.text;
  const html = orchestrated.html;
  const tools = orchestrated.tools;
  // tasks/todos for rich demo parity (prototype had them for pg)
  const mockTasks = html === "pg"
    ? [{ id: "t1", text: "Verify table schema", done: false }, { id: "t2", text: "Run SELECT on remote_db.users", done: true }, { id: "t3", text: "Format as markdown table", done: true }]
    : undefined;
  const mockTodos = html === "pg"
    ? [{ text: "Export to CSV", done: false }, { text: "Share with team", done: false }]
    : html === "aws"
      ? [{ text: "Review lifecycle policies", done: false }, { text: "Enable versioning", done: true }]
      : undefined;

  if (!shouldStream) {
    if (shouldPersist) {
      const persisted = await persistIfEnabled({
        userId,
        chatId,
        userContent: prompt + (attachments?.length ? `\n[Attachments: ${attachments.map((a) => a.name).join(", ")}]` : ""),
        assistantContent: fullText,
        assistantHtml: html,
        title: persistTitle,
        tasks: mockTasks,
        todos: mockTodos,
        attachments,
      });
      return NextResponse.json(
        {
          id: persisted?.assistantId || Date.now().toString(36),
          chatId: persisted?.chatId || chatId || null,
          role: "assistant",
          content: fullText,
          html,
          tools,
          provider: providerId,
          model: modelId,
          mode: modeId,
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
        provider: providerId,
        model: modelId,
        mode: modeId,
        created: Date.now(),
        persisted: false,
      },
      { headers: { "x-mcp-latency": String(tools[0]?.latencyMs || 142) } }
    );
  }

  // ── Streaming SSE mock ──
  const encoder = new TextEncoder();
  let persistedChatId: string | null = chatId || null;

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
        tasks: mockTasks,
        todos: mockTodos,
        attachments,
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

      send("meta", { provider: providerId, model: modelId, mode: modeId, tools, html, chatId: persistedChatId });

      for await (const chunk of streamText(fullText, 24)) {
        sendData({ delta: chunk, done: false });
        await new Promise((r) => setTimeout(r, 42));
      }
      if (persistDone) await persistDone.catch(() => {});
      send("done", { content: fullText, html, tools, chatId: persistedChatId, persisted: !!persistedChatId, tasks: mockTasks, todos: mockTodos });
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
  // probe MCP registry for live tools (best-effort)
  let mcpLive: any = null;
  try {
    const { listAllTools, getHealth } = await import("@/lib/mcp/registry");
    const [tools, health] = await Promise.all([listAllTools().catch(() => []), getHealth().catch(() => [])]);
    mcpLive = { tools, health: health.map((h: any) => ({ server: h.server.name, status: h.status, latency: h.latency, health: h.health })) };
  } catch {}
  return NextResponse.json({
    providers: ["openai", "anthropic", "google", "local"],
    modes: ["Chat", "Code", "Research", "Planning", "Write", "Debug", "Shield", "Creative"],
    streaming: "ai+mock(sse)",
    mcp: { healthPollMs: 30000, timeoutMs: 10000, live: mcpLive },
    persistence: db ? "postgres" : "mock(localStorage)",
    auth: { enabled: true, strategy: db ? "database" : "jwt" },
  });
}
