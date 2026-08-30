import { z } from "zod";

export const paginationSchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  limit: z.coerce.number().int().min(1).max(100).default(20),
  q: z.string().optional(),
  archived: z.coerce.boolean().optional(),
});

export const chatCreateSchema = z.object({
  title: z.string().min(1).max(80).default("New conversation"),
  archived: z.boolean().optional(),
});

export const chatPatchSchema = z.object({
  title: z.string().min(1).max(80).optional(),
  archived: z.boolean().optional(),
});

export const messageCreateSchema = z.object({
  role: z.enum(["user", "assistant", "system"]).default("user"),
  content: z.string().min(1).max(20000),
  html: z.enum(["pg", "aws", "fs", "generic"]).optional(),
  status: z.enum(["done", "streaming"]).optional(),
  tasks: z.array(z.object({ id: z.string(), text: z.string(), done: z.boolean() })).optional(),
  todos: z.array(z.object({ text: z.string(), done: z.boolean() })).optional(),
  attachments: z.array(z.object({ id: z.string(), name: z.string(), size: z.number(), type: z.string(), kind: z.enum(["image","video","audio","file"]).optional(), url: z.string().optional() })).optional(),
});
export const messagePatchSchema = messageCreateSchema.partial().extend({ content: z.string().min(1).max(20000).optional() });

export const serverCreateSchema = z.object({
  name: z.string().min(2).max(48).regex(/^[a-z0-9-]+$/),
  transport: z.enum(["sse","stdio","http"]),
  endpoint: z.string().min(4).max(500),
  tools: z.string().max(500).default(""),
  color: z.string().default("custom"),
  desc: z.string().max(500).optional(),
  timeout: z.number().int().min(1000).max(60000).default(10000),
  retries: z.number().int().min(0).max(5).default(2),
  env: z.array(z.object({ k: z.string(), v: z.string() })).default([]),
  autoConnect: z.boolean().default(false),
  healthCheck: z.boolean().default(true),
});

export const serverPatchSchema = serverCreateSchema.partial().extend({
  status: z.enum(["connected","disconnected","connecting"]).optional(),
  latency: z.number().optional(),
  health: z.enum(["ok","off","warn"]).optional(),
});

export const providerCreateSchema = z.object({
  id: z.string().min(2).max(32).regex(/^[a-z0-9-]+$/),
  name: z.string().min(2).max(48),
  baseUrl: z.string().url().or(z.string().min(4)),
  apiKey: z.string().max(500).optional(),
  models: z.array(z.string()).min(1),
  temp: z.number().min(0).max(2).default(0.7),
  maxTokens: z.number().int().min(256).max(128000).default(4096),
  enabled: z.boolean().default(true),
  streaming: z.boolean().default(true),
  latency: z.number().int().default(120),
});

export const providerPatchSchema = providerCreateSchema.partial();

export const modeCreateSchema = z.object({
  id: z.string().min(2).max(32).regex(/^[A-Za-z0-9-]+$/),
  name: z.string().min(2).max(32),
  icon: z.string().default("bot"),
  color: z.string().default("#2563eb"),
  desc: z.string().max(500).default(""),
  prompt: z.string().max(4000).default(""),
  temp: z.number().min(0).max(2).default(0.7),
  tokens: z.number().int().min(256).max(128000).default(4096),
  provider: z.string().optional(),
  model: z.string().optional(),
  servers: z.array(z.string()).default([]),
  tools: z.object({ sql: z.boolean(), fs: z.boolean(), cloud: z.boolean(), web: z.boolean() }).default({ sql: true, fs: true, cloud: true, web: false }),
  builtin: z.boolean().default(false),
});

export const modePatchSchema = modeCreateSchema.partial();

export type PaginationInput = z.infer<typeof paginationSchema>;
