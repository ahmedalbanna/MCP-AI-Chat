import { env, hasDatabase } from "./env";

// Lazy singleton — avoids hard failure when DATABASE_URL is absent or client not generated (e.g. CI/build without DB).
// Consumers must check `hasDatabase` and handle `prisma === null`.

type PrismaClientType = any; // avoid hard import at build time when client not yet generated

let _prisma: PrismaClientType | null = null;
let _warned = false;

function createClient(): PrismaClientType | null {
  if (!hasDatabase) {
    if (!_warned && process.env.NODE_ENV !== "production") {
      console.warn("[db] DATABASE_URL not set — running in mock mode (no persistence). Set DATABASE_URL to enable Postgres.");
      _warned = true;
    }
    return null;
  }
  if (_prisma) return _prisma;
  try {
    // dynamic require to avoid build-time hard failure if `prisma generate` hasn't run yet
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { PrismaClient } = require("@prisma/client") as { PrismaClient: new (opts?: any) => PrismaClientType };
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
  return hasDatabase && !!getPrisma();
}
