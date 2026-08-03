import pino from "pino";

/**
 * Structured Pino logger for PayU Web-App (BFF layer).
 *
 * - JSON output by default — ready for ELK / Loki / CloudWatch
 * - Correlation-ID aware via child loggers
 * - ~5-10x faster than Winston (async, low-overhead)
 * - Safe for Node.js API routes (NOT Edge runtime — see edge-logger.ts)
 */

const isProduction = process.env.NODE_ENV === "production" || process.env.SPRING_PROFILES_ACTIVE === "container";

const logger = pino({
  level: process.env.LOG_LEVEL || (isProduction ? "info" : "debug"),
  ...(isProduction ? {} : {
    transport: {
      target: "pino-pretty",
      options: { colorize: true }
    }
  }),
  base: {
    service_name: "web-app",
    service_version: process.env.APP_VERSION || "unknown",
    environment: process.env.NODE_ENV || "dev",
  },
  timestamp: pino.stdTimeFunctions.isoTime,
});

export default logger;

/* ------------------------------------------------------------------ */
/*  Helper: build child logger with correlation-id bound              */
/* ------------------------------------------------------------------ */
export function withCorrelation(correlationId: string) {
  return logger.child({ correlationId });
}

/* ------------------------------------------------------------------ */
/*  Helper: extract or generate correlation-id from a request         */
/* ------------------------------------------------------------------ */
export function getCorrelationId(request: Request): string {
  return (
    request.headers.get("x-correlation-id") ??
    request.headers.get("x-request-id") ??
    crypto.randomUUID()
  );
}

/* ------------------------------------------------------------------ */
/*  Re-export edge logger for backward compat (do NOT import this in  */
/*  Edge Runtime — use @/lib/edge-logger directly instead)            */
/* ------------------------------------------------------------------ */
export { edgeLogger } from "./edge-logger";
