"use client";

import { useEffect } from "react";

const RELOAD_AT_KEY = "payu-chunk-reload-at";
const RELOAD_COOLDOWN_MS = 10_000;

/**
 * Messages a stale chunk produces after a deploy. Next/webpack surface the
 * dynamic-import failure itself; React #130 ("Element type is invalid")
 * follows when the stale module then renders with a missing/undefined export.
 */
const CHUNK_ERROR_PATTERNS = [
  "ChunkLoadError",
  "Importing a module script failed",
  "Failed to fetch dynamically imported module",
];

function isChunkLoadFailure(value: unknown): boolean {
  if (typeof value === "string") {
    return CHUNK_ERROR_PATTERNS.some((pattern) => value.includes(pattern));
  }
  if (value instanceof Error) {
    return isChunkLoadFailure(value.message) || isChunkLoadFailure(value.name);
  }
  return false;
}

/**
 * Auto-recover a tab left holding pre-deploy chunks: reload once, guarded by a
 * sessionStorage timestamp so a tab whose chunks keep failing cannot spin in a
 * reload loop.
 */
export default function ChunkReload() {
  useEffect(() => {
    const reloadOnce = () => {
      try {
        const last = Number(window.sessionStorage.getItem(RELOAD_AT_KEY) ?? 0);
        if (last > 0 && Date.now() - last < RELOAD_COOLDOWN_MS) return;
        window.sessionStorage.setItem(RELOAD_AT_KEY, String(Date.now()));
      } catch {
        // Private mode / storage disabled: still recover, without the guard.
      }
      window.location.reload();
    };

    const onError = (event: ErrorEvent) => {
      if (isChunkLoadFailure(event.message)) reloadOnce();
    };
    const onUnhandledRejection = (event: PromiseRejectionEvent) => {
      if (isChunkLoadFailure(event.reason)) reloadOnce();
    };

    window.addEventListener("error", onError);
    window.addEventListener("unhandledrejection", onUnhandledRejection);
    return () => {
      window.removeEventListener("error", onError);
      window.removeEventListener("unhandledrejection", onUnhandledRejection);
    };
  }, []);

  return null;
}
