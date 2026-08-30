import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  const db = !!process.env.DATABASE_URL;
  return NextResponse.json({
    status: "ok",
    uptime: process.uptime(),
    ts: Date.now(),
    version: "2.0.0",
    persistence: db ? "postgres" : "mock(localStorage)",
    auth: db ? "database(JWT fallback)" : "jwt(dev credentials)",
    streaming: "sse+mock",
    mcp: { orchestrator: "mock", transports: ["sse", "stdio", "http"] },
  });
}
