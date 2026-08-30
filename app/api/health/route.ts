import { NextResponse } from "next/server";

export async function GET() {
  return NextResponse.json({ status: "ok", uptime: process.uptime(), ts: Date.now(), version: "2.0.0" });
}
