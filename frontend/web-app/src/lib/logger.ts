import pino from "pino";

/**
 * Structured Pino logger for PayU Web-App (BFF layer).
 *
 * - JSON output by default — ready for ELK / Loki / CloudWatch
 * - Correlation-ID aware via child loggers
 * - ~5-10x faster than Winston (async, low-overhead)
 * - Safe for Node.js API routes (NOT Edge runtime — see edge-logger.ts)
 */

const LOG_LEVEL = process.env.LOG_LEVEL || "info";
const SERVICE_NAME = "payu-web-app";

const logger = pino({
  level: LOG_LEVEL,
  name: SERVICE_NAME,
  // In production: pure JSON to stdout (for log aggregators)
  // In development: pretty-print for readability
  ...(process.env.NODE_ENV !== "production" && {
    transport: {
      target: "pino-pretty",
      options: {
        colorize: true,
        translateTime: "HH:MM:ss.l",
        ignore: "pid,hostname",
      },
    },
  }),
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
