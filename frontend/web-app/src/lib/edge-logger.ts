/**
 * Lightweight structured logger for Edge Runtime (e.g., Next.js middleware).
 *
 * Winston requires Node.js APIs (fs, stream, etc.) that are NOT available in
 * the Edge Runtime. This module provides the same structured JSON output
 * using only Web-standard APIs so it can be safely imported in middleware.
 */

const SERVICE_NAME = "payu-web-app";

function format(
  level: string,
  message: string,
  meta?: Record<string, unknown>,
) {
  return JSON.stringify({
    timestamp: new Date().toISOString(),
    level,
    service: SERVICE_NAME,
    message,
    ...meta,
  });
}

export const edgeLogger = {
  info(message: string, meta?: Record<string, unknown>) {
    console.log(format("info", message, meta));
  },
  warn(message: string, meta?: Record<string, unknown>) {
    console.warn(format("warn", message, meta));
  },
  error(message: string, meta?: Record<string, unknown>) {
    console.error(format("error", message, meta));
  },
  debug(message: string, meta?: Record<string, unknown>) {
    if (process.env.LOG_LEVEL === "debug") {
      console.debug(format("debug", message, meta));
    }
  },
};
