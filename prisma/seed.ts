import { PrismaClient } from "@prisma/client";
import { DEFAULT_PROVIDERS, DEFAULT_SERVERS, DEFAULT_MODES, MARKET } from "../lib/mockData";

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

  console.log("[seed] done");
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(async () => { await prisma.$disconnect(); });
