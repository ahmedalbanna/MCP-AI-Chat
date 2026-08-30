import { PrismaClient } from "@prisma/client";
import { DEFAULT_PROVIDERS, DEFAULT_SERVERS, DEFAULT_MODES, MARKET, DEFAULT_CHATS } from "../lib/mockData";

const prisma = new PrismaClient();

async function main() {
  console.log("[seed] start");

  // Providers
  for (const p of DEFAULT_PROVIDERS) {
    await prisma.provider.upsert({
      where: { id: p.id },
      update: { name: p.name, baseUrl: p.baseUrl, models: p.models, temp: p.temp, maxTokens: p.maxTokens, enabled: p.enabled, streaming: p.streaming, latency: p.latency },
      create: { id: p.id, name: p.name, baseUrl: p.baseUrl, models: p.models, temp: p.temp, maxTokens: p.maxTokens, enabled: p.enabled, streaming: p.streaming, latency: p.latency } as any,
    });
  }
  console.log(`[seed] providers: ${DEFAULT_PROVIDERS.length}`);

  // Servers
  for (const s of DEFAULT_SERVERS) {
    await prisma.server.upsert({
      where: { name: s.name },
      update: { transport: s.transport as any, endpoint: s.endpoint, tools: s.tools, status: s.status as any, color: s.color, desc: s.desc, timeout: s.timeout, retries: s.retries, env: s.env as any, latency: s.latency, health: s.health, autoConnect: s.autoConnect, healthCheck: s.healthCheck },
      create: { name: s.name, transport: s.transport as any, endpoint: s.endpoint, tools: s.tools, status: s.status as any, color: s.color, desc: s.desc, timeout: s.timeout, retries: s.retries, env: s.env as any, latency: s.latency, health: s.health, autoConnect: s.autoConnect, healthCheck: s.healthCheck } as any,
    });
  }
  console.log(`[seed] servers: ${DEFAULT_SERVERS.length}`);

  // Modes
  for (const m of DEFAULT_MODES) {
    await prisma.agentMode.upsert({
      where: { id: m.id },
      update: { name: m.name, icon: m.icon, color: m.color, desc: m.desc, prompt: m.prompt, temp: m.temp, tokens: m.tokens, provider: m.provider || null, model: m.model || null, servers: m.servers, tools: m.tools as any, builtin: m.builtin },
      create: { id: m.id, name: m.name, icon: m.icon, color: m.color, desc: m.desc, prompt: m.prompt, temp: m.temp, tokens: m.tokens, provider: m.provider || null, model: m.model || null, servers: m.servers, tools: m.tools as any, builtin: m.builtin } as any,
    });
  }
  console.log(`[seed] modes: ${DEFAULT_MODES.length}`);

  // Market
  for (const it of MARKET) {
    await prisma.marketItem.upsert({
      where: { id: it.id },
      update: { name: it.name, cat: it.cat, icon: it.icon, color: it.color, desc: it.desc, author: it.author, version: it.version, stars: it.stars, installs: it.installs, installed: it.installed, featured: it.featured, trending: it.trending, isNew: it.isNew, tags: it.tags, tools: it.tools, verified: it.verified },
      create: { id: it.id, name: it.name, cat: it.cat, icon: it.icon, color: it.color, desc: it.desc, author: it.author, version: it.version, stars: it.stars, installs: it.installs, installed: it.installed, featured: it.featured, trending: it.trending, isNew: it.isNew, tags: it.tags, tools: it.tools, verified: it.verified } as any,
    });
  }
  console.log(`[seed] market: ${MARKET.length}`);

  // Chats + Messages — ensure demo conversations have full content (including tasks/todos)
  for (const ch of DEFAULT_CHATS) {
    const existing = await prisma.chat.findUnique({ where: { id: ch.id } });
    if (!existing) {
      await prisma.chat.create({
        data: {
          id: ch.id,
          title: ch.title,
          archived: ch.archived,
          createdAt: new Date(ch.created),
          updatedAt: new Date(ch.created),
          messages: {
            create: ch.messages.map((m: any) => ({
              id: m.id,
              role: m.role,
              content: m.text || "",
              html: m.html || null,
              status: m.status || "done",
              tasks: m.tasks ? (m.tasks as any) : undefined,
              todos: m.todos ? (m.todos as any) : undefined,
            })),
          },
        } as any,
      });
    } else {
      await prisma.chat.update({ where: { id: ch.id }, data: { title: ch.title, archived: ch.archived } });
      for (const m of ch.messages as any[]) {
        const em = await prisma.message.findUnique({ where: { id: m.id } });
        if (!em) {
          await prisma.message.create({
            data: { id: m.id, chatId: ch.id, role: m.role, content: m.text || "", html: m.html || null, status: m.status || "done", tasks: m.tasks as any, todos: m.todos as any } as any,
          });
        } else {
          const patch: any = {};
          if (!em.content || em.content.length < 10) patch.content = m.text || em.content;
          if (m.html && (em as any).html !== m.html) patch.html = m.html;
          if (m.tasks && JSON.stringify((em as any).tasks) !== JSON.stringify(m.tasks)) patch.tasks = m.tasks;
          if (m.todos && JSON.stringify((em as any).todos) !== JSON.stringify(m.todos)) patch.todos = m.todos;
          if (Object.keys(patch).length) await prisma.message.update({ where: { id: m.id }, data: patch });
        }
      }
    }
  }
  console.log(`[seed] chats: ${DEFAULT_CHATS.length}`);

  console.log("[seed] done");
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(async () => { await prisma.$disconnect(); });
