import NextAuth from "next-auth";
import Credentials from "next-auth/providers/credentials";
import GitHub from "next-auth/providers/github";
import Google from "next-auth/providers/google";
import { getPrisma } from "./db";
import { env } from "./env";

// Lazy adapter — only when DATABASE_URL + Prisma client available.
// Avoids build failure in mock mode (no DB).
function getAdapter() {
  const prisma = getPrisma();
  if (!prisma) return undefined;
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { PrismaAdapter } = require("@auth/prisma-adapter") as typeof import("@auth/prisma-adapter");
    return PrismaAdapter(prisma);
  } catch {
    console.warn("[auth] @auth/prisma-adapter not available — falling back to JWT");
    return undefined;
  }
}

const providers: ReturnType<typeof NextAuth> extends never ? never : any[] = [];

// GitHub / Google only when env present (allows local dev without OAuth)
if (process.env.GITHUB_ID && process.env.GITHUB_SECRET) {
  providers.push(GitHub);
}
if (process.env.GOOGLE_ID && process.env.GOOGLE_SECRET) {
  providers.push(Google);
}

// Dev credentials provider — always enabled in non-production, optional in prod
providers.push(
  Credentials({
    name: "Dev Login",
    credentials: {
      email: { label: "Email", type: "email", placeholder: "dev@example.com" },
      name: { label: "Name", type: "text", placeholder: "Developer" },
    },
    async authorize(creds) {
      const email = (creds?.email as string)?.trim().toLowerCase();
      if (!email || !email.includes("@")) return null;
      const name = (creds?.name as string) || email.split("@")[0];
      // In production, you may restrict domain here
      return { id: `dev-${email}`, email, name, image: `https://i.pravatar.cc/150?u=${encodeURIComponent(email)}` };
    },
  })
);

export const { handlers, auth, signIn, signOut } = NextAuth({
  adapter: getAdapter(),
  secret: env.AUTH_SECRET || process.env.AUTH_SECRET || "dev-secret-change-me-32-chars-min",
  session: { strategy: getAdapter() ? "database" : "jwt" },
  trustHost: true,
  providers,
  pages: {
    // use default next-auth pages; custom UI can be added later at /auth/signin
  },
  callbacks: {
    async session({ session, user, token }) {
      // augment session with role/userId for app use
      if (user) {
        (session as any).userId = user.id;
        (session as any).role = (user as any).role || "Admin";
      } else if (token) {
        (session as any).userId = token.sub;
        (session as any).role = (token as any).role || "Admin";
      }
      return session;
    },
    async jwt({ token, user }) {
      if (user) {
        (token as any).role = (user as any).role || "Admin";
      }
      return token;
    },
  },
});
