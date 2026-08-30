import { env } from "../env";
import { decrypt } from "../encryption";

type ProviderId = "openai" | "anthropic" | "google" | "local" | string;

async function resolveKey(providerId: ProviderId, dbKeyEncrypted?: string | null): Promise<string | undefined> {
  if (dbKeyEncrypted) {
    try {
      const dec = await decrypt(dbKeyEncrypted);
      if (dec && dec !== "••••" && dec.length > 8) return dec;
    } catch {}
  }
  if (providerId === "openai") return env.OPENAI_API_KEY || process.env.OPENAI_API_KEY;
  if (providerId === "anthropic") return env.ANTHROPIC_API_KEY || process.env.ANTHROPIC_API_KEY;
  if (providerId === "google") return env.GOOGLE_API_KEY || process.env.GOOGLE_API_KEY;
  return undefined;
}

// Returns an AI SDK LanguageModelV1 or null if not configured
export async function getAIModel(providerId: string, modelId: string, opts: { apiKeyEncrypted?: string | null; baseUrl?: string } = {}): Promise<any | null> {
  const apiKey = await resolveKey(providerId, opts.apiKeyEncrypted || null);
  const baseUrl = opts.baseUrl;

  try {
    if (providerId === "openai") {
      if (!apiKey) return null;
      const { createOpenAI } = await import("@ai-sdk/openai");
      const openai = createOpenAI({ apiKey, baseURL: baseUrl || undefined });
      return openai(modelId || "gpt-4o-mini");
    }
    if (providerId === "anthropic") {
      if (!apiKey) return null;
      const { createAnthropic } = await import("@ai-sdk/anthropic");
      const anthropic = createAnthropic({ apiKey, baseURL: baseUrl || undefined });
      return anthropic(modelId || "claude-3-5-sonnet-20241022");
    }
    if (providerId === "google") {
      if (!apiKey) return null;
      const { createGoogleGenerativeAI } = await import("@ai-sdk/google");
      const google = createGoogleGenerativeAI({ apiKey, baseURL: baseUrl || undefined });
      return google(modelId || "gemini-1.5-pro");
    }
    if (providerId === "local") {
      // Local Ollama via OpenAI-compatible endpoint (e.g. http://localhost:11434/v1)
      const { createOpenAI } = await import("@ai-sdk/openai");
      const localBase = baseUrl || "http://localhost:11434/v1";
      const local = createOpenAI({ apiKey: apiKey || "ollama", baseURL: localBase });
      return local(modelId || "llama-3.1-70B");
    }
  } catch (e) {
    console.warn(`[ai] getAIModel failed for ${providerId}/${modelId}:`, (e as Error).message);
    return null;
  }
  return null;
}

export async function hasAnyProviderKey(): Promise<boolean> {
  return !!(env.OPENAI_API_KEY || env.ANTHROPIC_API_KEY || env.GOOGLE_API_KEY || process.env.OPENAI_API_KEY || process.env.ANTHROPIC_API_KEY || process.env.GOOGLE_API_KEY);
}
