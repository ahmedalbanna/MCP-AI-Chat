// Middleware is intentionally light — no DB access at edge.
// Auth session is resolved server-side via `auth()` in API routes / Server Components (`lib/auth.ts`).
// This passthrough ensures edge compatibility (Prisma cannot run in edge runtime).
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(_req: NextRequest) {
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.svg|.*\\.png).*)"],
};
