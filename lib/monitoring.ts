// Minimal monitoring stub — replace with @sentry/nextjs in prod
// import * as Sentry from "@sentry/nextjs";
// Sentry.init({ dsn: process.env.SENTRY_DSN, tracesSampleRate: 0.1 });

export function captureError(err: unknown, context?: Record<string, unknown>) {
  console.error("[monitor]", err, context);
  // if (process.env.SENTRY_DSN) Sentry.captureException(err);
}

export function captureMessage(msg: string, level: "info"|"warning"|"error" = "info") {
  console.warn(`[monitor:${level}]`, msg);
}
