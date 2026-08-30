import { z } from "zod";

const envSchema = z.object({
  DATABASE_URL: z.string().min(1, "DATABASE_URL required for persistence").optional(),
  AUTH_SECRET: z.string().min(32, "AUTH_SECRET must be >=32 chars — generate with: openssl rand -base64 32").optional(),
  AUTH_URL: z.string().optional(),
  NEXT_PUBLIC_APP_URL: z.string().optional(),
  OPENAI_API_KEY: z.string().optional(),
  ANTHROPIC_API_KEY: z.string().optional(),
  GOOGLE_API_KEY: z.string().optional(),
  ENCRYPTION_KEY: z.string().refine(
    (v) => !v || /^[0-9a-fA-F]{64}$/.test(v.trim()) || (Buffer.from(v, "base64").length === 32),
    "ENCRYPTION_KEY must be 32 bytes: 64 hex chars (openssl rand -hex 32) or 32-byte base64"
  ).optional(),
  S3_ENDPOINT: z.string().optional(),
  S3_BUCKET: z.string().optional(),
  S3_ACCESS_KEY_ID: z.string().optional(),
  S3_SECRET_ACCESS_KEY: z.string().optional(),
  S3_REGION: z.string().optional(),
  MCP_HEALTH_POLL_MS: z.coerce.number().optional(),
  MCP_TIMEOUT_MS: z.coerce.number().optional(),
  NODE_ENV: z.enum(["development", "production", "test"]).optional(),
});

export type Env = z.infer<typeof envSchema>;

let _env: Env | null = null;

export function getEnv(): Env {
  if (_env) return _env;
  const parsed = envSchema.safeParse(process.env);
  if (!parsed.success) {
    console.warn("[env] validation warnings:", parsed.error.flatten().fieldErrors);
    // Don't cache invalid parse — re-parse next time so runtime env can correct build-time placeholder
    return process.env as Env;
  }
  _env = parsed.data;
  return _env;
}

export function getEnvFresh(): Env {
  const parsed = envSchema.safeParse(process.env);
  if (!parsed.success) return process.env as Env;
  return parsed.data;
}

export const env = getEnv();
export const isProd = env.NODE_ENV === "production";
// Use fresh check at call time, not cached value, for standalone builds where build-time placeholder differs
export const hasDatabase = !!process.env.DATABASE_URL;
export function hasDatabaseFresh() { return !!process.env.DATABASE_URL && !!getEnvFresh().DATABASE_URL; }
