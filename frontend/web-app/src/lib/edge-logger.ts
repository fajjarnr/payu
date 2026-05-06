/**
 * Lightweight structured logger for Edge Runtime (e.g., Next.js middleware).
 *
 * Winston requires Node.js APIs (fs, stream, etc.) that are NOT available in
 * the Edge Runtime. This module provides the same structured JSON output
 * using only Web-standard APIs so it can be safely imported in middleware.
 */

const SERVICE_NAME = "web-app";
const SERVICE_VERSION = "1.5.0";
const ENVIRONMENT = process.env.NODE_ENV || "dev";

function format(
  level: string,
  message: string,
  meta?: Record<string, unknown>,
) {
  return JSON.stringify({
    timestamp: new Date().toISOString(),
    level,
    service_name: SERVICE_NAME,
    service_version: SERVICE_VERSION,
    environment: ENVIRONMENT,
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
