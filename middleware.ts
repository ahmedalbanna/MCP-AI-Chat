import { NextResponse, NextRequest } from "next/server";

// In-memory rate limit (per instance, resets on redeploy — sufficient for MVP, use Redis in prod)
const buckets = new Map<string, { count: number; reset: number }>();

function rateLimit(ip: string, limit: number, windowMs: number): { ok: boolean; remaining: number; reset: number } {
  const now = Date.now();
  const key = ip;
  const cur = buckets.get(key);
  if (!cur || now > cur.reset) {
    buckets.set(key, { count: 1, reset: now + windowMs });
    return { ok: true, remaining: limit - 1, reset: now + windowMs };
  }
  if (cur.count >= limit) return { ok: false, remaining: 0, reset: cur.reset };
  cur.count++;
  return { ok: true, remaining: limit - cur.count, reset: cur.reset };
}

export function middleware(req: NextRequest) {
  const res = NextResponse.next();

  // Security headers
  res.headers.set("X-Frame-Options", "DENY");
  res.headers.set("X-Content-Type-Options", "nosniff");
  res.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  res.headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  if (process.env.NODE_ENV === "production") {
    res.headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  }

  const path = req.nextUrl.pathname;
  const ip = (req as any).ip || req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "127.0.0.1";

  // Rate limit: /api/chat is expensive (AI + MCP)
  if (path.startsWith("/api/chat")) {
    const rl = rateLimit(`chat:${ip}`, 12, 60_000); // 12/min
    res.headers.set("X-RateLimit-Limit", "12");
    res.headers.set("X-RateLimit-Remaining", String(rl.remaining));
    res.headers.set("X-RateLimit-Reset", String(Math.ceil(rl.reset / 1000)));
    if (!rl.ok) {
      return NextResponse.json({ error: "Rate limited — try again in a few seconds" }, { status: 429, headers: res.headers });
    }
  } else if (path.startsWith("/api/")) {
    const rl = rateLimit(`api:${ip}`, 60, 60_000);
    res.headers.set("X-RateLimit-Limit", "60");
    res.headers.set("X-RateLimit-Remaining", String(rl.remaining));
    if (!rl.ok) return NextResponse.json({ error: "Too many requests" }, { status: 429, headers: res.headers });
  }

  // Auth gate for API (allow public MVP but warn if no session in production)
  // Real gate is in lib/auth — middleware only sets header
  res.headers.set("X-MCP-Terminal", "2.0");

  return res;
}

export const config = {
  matcher: ["/api/:path*", "/((?!_next/static|_next/image|favicon.svg).*)"],
};
