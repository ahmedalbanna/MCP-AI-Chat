import { NextResponse } from "next/server";
import { getPrisma, isDatabaseEnabled } from "@/lib/db";
import { MARKET } from "@/lib/mockData";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: Request) {
  const url = new URL(req.url);
  const cat = url.searchParams.get("cat") || "all";
  const q = (url.searchParams.get("q") || "").toLowerCase();
  if (!isDatabaseEnabled()) {
    let items = MARKET;
    if (cat !== "all") items = items.filter((m) => m.cat === cat);
    if (q) items = items.filter((m) => m.name.toLowerCase().includes(q) || m.desc.toLowerCase().includes(q) || m.tags.some((t) => t.includes(q)));
    return NextResponse.json({ persistence: "mock", items });
  }
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ persistence: "mock", items: MARKET });
  const where: any = {};
  if (cat !== "all") where.cat = cat;
  if (q) where.OR = [{ name: { contains: q, mode: "insensitive" } }, { desc: { contains: q, mode: "insensitive" } }];
  const items = await prisma.marketItem.findMany({ where, orderBy: [{ trending: "desc" }, { stars: "desc" }] });
  if (items.length === 0) return NextResponse.json({ persistence: "postgres", items: MARKET, seeded: false });
  return NextResponse.json({ persistence: "postgres", items });
}

export async function POST(req: Request) {
  let body: any;
  try { body = await req.json(); } catch { return NextResponse.json({ error: "Invalid JSON" }, { status: 400 }); }
  const { id, installed } = body || {};
  if (!id) return NextResponse.json({ error: "id required" }, { status: 400 });
  if (!isDatabaseEnabled()) return NextResponse.json({ ok: true, persistence: "mock" });
  const prisma = getPrisma();
  if (!prisma) return NextResponse.json({ error: "DB unavailable" }, { status: 503 });
  try {
    const updated = await prisma.marketItem.update({ where: { id }, data: { installed: !!installed } });
    return NextResponse.json({ ok: true, item: updated });
  } catch { return NextResponse.json({ error: "not found" }, { status: 404 }); }
}
