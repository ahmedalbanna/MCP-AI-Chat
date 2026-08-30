import { NextResponse } from "next/server";
import { hasDatabase } from "@/lib/env";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  return NextResponse.json({
    status: "ok",
    uptime: process.uptime(),
    ts: Date.now(),
    version: "2.0.0",
    persistence: hasDatabase ? "postgres" : "mock(localStorage)",
    auth: hasDatabase ? "database(JWT fallback)" : "jwt(dev credentials)",
    streaming: "sse+mock",
    mcp: { orchestrator: "mock", transports: ["sse", "stdio", "http"] },
  });
}
