import { env } from "./env";

type PrismaClientType = any;

let _prisma: PrismaClientType | null = null;
let _warned = false;

function createClient(): PrismaClientType | null {
  if (!process.env.DATABASE_URL) {
    if (!_warned && process.env.NODE_ENV !== "production") {
      console.warn("[db] DATABASE_URL not set — running in mock mode (no persistence). Set DATABASE_URL to enable Postgres.");
      _warned = true;
    }
    return null;
  }
  if (_prisma) return _prisma;
  try {
    let PrismaClient: any;
    try {
      // eslint-disable-next-line
      PrismaClient = require("@prisma/client").PrismaClient;
    } catch {
      const req = Function("return typeof require !== 'undefined' ? require : null")() as any;
      if (!req) return null;
      PrismaClient = req("@prisma/client").PrismaClient;
    }
    const globalForPrisma = globalThis as unknown as { __prisma?: PrismaClientType };
    if (globalForPrisma.__prisma) {
      _prisma = globalForPrisma.__prisma;
      return _prisma;
    }
    _prisma = new PrismaClient({
      log: env.NODE_ENV === "development" ? ["warn", "error"] : ["error"],
    });
    if (process.env.NODE_ENV !== "production") globalForPrisma.__prisma = _prisma;
    return _prisma;
  } catch (e) {
    console.warn("[db] PrismaClient not available (run `npx prisma generate`):", (e as Error).message);
    return null;
  }
}

export const prisma: PrismaClientType | null = createClient();

export function getPrisma(): PrismaClientType | null {
  return createClient();
}

export function isDatabaseEnabled(): boolean {
  return !!process.env.DATABASE_URL && !!getPrisma();
}
