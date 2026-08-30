import { NextResponse } from "next/server";

// Mock chat completions — will be replaced by Vercel AI SDK + MCP orchestrator
export async function POST(req: Request) {
  const { messages, provider, model, mode } = await req.json().catch(() => ({ messages: [] }));
  const last = messages?.[messages.length - 1]?.content || "";
  // Simple echo with metadata; streaming will be implemented via Edge runtime
  const content = `[${provider || "anthropic"}/${model || "claude-3.5-sonnet"} • ${mode || "Chat"}] Echo: ${String(last).slice(0, 800)}`;
  // Simulate latency header for client
  return NextResponse.json({ id: Date.now().toString(36), role: "assistant", content, created: Date.now() }, { headers: { "x-mcp-latency": "142" } });
}

export async function GET() {
  return NextResponse.json({ providers: ["openai", "anthropic", "google", "local"], modes: ["Chat", "Code", "Research", "Planning", "Write", "Debug", "Shield", "Creative"] });
}
