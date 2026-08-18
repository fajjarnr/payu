import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Generate a UUID v4 for idempotency keys
 * Uses crypto.randomUUID() if available, falls back to manual generation
 */
export function generateUUID(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  // Fallback for environments without crypto.randomUUID
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (crypto.getRandomValues(new Uint8Array(1))[0] / 256) * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/**
 * Get default headers for financial mutations including idempotency key
 * Generates a new UUID for each call to ensure unique idempotency keys per request
 */
export function getFinancialMutationHeaders(): Record<string, string> {
  return {
    'X-Idempotency-Key': generateUUID(),
  };
}

/**
 * FE-IDM-002: deterministic idempotency key for a financial mutation so that
 * safe retries of the same logical operation reuse the same key (preventing
 * duplicate mutations). Use this for mutations that can be retried.
 */
export function idempotencyKeyFor(operation: string, resourceId: string): string {
  return generateUUIDFrom(`${operation}:${resourceId}`);
}

function generateUUIDFrom(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash << 5) - hash + seed.charCodeAt(i);
    hash |= 0;
  }
  const bytes = new Uint8Array(16);
  for (let i = 0; i < 16; i++) {
    bytes[i] = (hash + (i * 31)) & 0xff;
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes).map((b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
