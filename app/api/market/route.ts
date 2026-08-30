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
