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
    const r = Math.random() * 16 | 0;
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
