# syntax=docker/dockerfile:1
# Multi-stage Next.js 15 standalone build — Node 20 alpine
FROM node:20-alpine AS base
RUN apk add --no-cache libc6-compat openssl

FROM base AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# Generate Prisma client before build (no DB needed at build time — hasDatabase guards runtime)
RUN npx prisma generate --schema=prisma/schema.prisma || true
ENV NEXT_TELEMETRY_DISABLED=1
# Build-time env fallbacks so build succeeds without real secrets (runtime uses real env)
ENV DATABASE_URL="postgresql://user:pass@localhost:5432/db?schema=public"
ENV AUTH_SECRET="build-time-placeholder-32-chars-min"
ENV ENCRYPTION_KEY="0123456789abcdef0123456789abcdef"
RUN npm run build

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
RUN addgroup --system --gid 1001 nodejs \
 && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/prisma ./prisma
COPY --from=builder --chown=nextjs:nodejs /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder --chown=nextjs:nodejs /app/node_modules/@prisma ./node_modules/@prisma

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
# Healthcheck hits /api/health (mock-safe)
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 CMD wget -qO- http://localhost:3000/api/health || exit 1
CMD ["node", "server.js"]
