/**
 * Idempotency Utility
 *
 * Provides unique idempotency key generation for financial operations
 * to prevent duplicate transactions during network retries.
 */

import { storage } from './storage';

const IDEMPOTENCY_KEYS_STORAGE_KEY = '@payu:idempotency_keys';

export interface IdempotencyKeyMetadata {
  key: string;
  operation: string;
  timestamp: number;
  userId?: string;
}

/**
 * Generate a unique idempotency key for a financial operation
 * Format: {operation}-{userId}-{timestamp}-{random}
 *
 * @param operation - The type of operation (e.g., 'transfer', 'topup', 'qris')
 * @param userId - Optional user ID for better tracing
 * @returns A unique idempotency key string
 */
export function generateIdempotencyKey(operation: string, userId?: string): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 15);

  if (userId) {
    return `${operation}-${userId}-${timestamp}-${random}`;
  }

  return `${operation}-${timestamp}-${random}`;
}

/**
 * Save an idempotency key to persistent storage
 * This allows recovery of pending operations after app restart
 *
 * @param key - The idempotency key to save
 * @param operation - The operation type
 * @param userId - Optional user ID
 */
export async function saveIdempotencyKey(
  key: string,
  operation: string,
  userId?: string
): Promise<void> {
  try {
    const keys = await storage.get<IdempotencyKeyMetadata[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    keys.push({
      key,
      operation,
      timestamp: Date.now(),
      userId,
    });

    // Keep only last 100 keys to prevent storage bloat
    const trimmedKeys = keys.slice(-100);

    await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, trimmedKeys);
  } catch (error) {
    console.warn('Failed to save idempotency key:', error);
  }
}

/**
 * Remove an idempotency key from storage
 * Called after an operation completes successfully
 *
 * @param key - The idempotency key to remove
 */
export async function removeIdempotencyKey(key: string): Promise<void> {
  try {
    const keys = await storage.get<IdempotencyKeyMetadata[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    const filteredKeys = keys.filter((k) => k.key !== key);

    await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, filteredKeys);
  } catch (error) {
    console.warn('Failed to remove idempotency key:', error);
  }
}

/**
 * Clean up old idempotency keys (older than 24 hours)
 * Should be called periodically or on app startup
 */
export async function cleanupOldIdempotencyKeys(): Promise<void> {
  try {
    const keys = await storage.get<IdempotencyKeyMetadata[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;

    const validKeys = keys.filter((k) => k.timestamp > oneDayAgo);

    await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, validKeys);
  } catch (error) {
    console.warn('Failed to cleanup old idempotency keys:', error);
  }
}

/**
 * Get all pending idempotency keys
 * Useful for recovery scenarios
 */
export async function getPendingIdempotencyKeys(): Promise<IdempotencyKeyMetadata[]> {
  try {
    const keys = await storage.get<IdempotencyKeyMetadata[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    const oneHourAgo = Date.now() - 60 * 60 * 1000;

    // Only return keys from the last hour (likely still pending)
    return keys.filter((k) => k.timestamp > oneHourAgo);
  } catch (error) {
    console.warn('Failed to get pending idempotency keys:', error);
    return [];
  }
}

/**
 * Check if a specific idempotency key exists in storage
 * Useful for preventing duplicate operations in the same session
 */
export async function hasIdempotencyKey(key: string): Promise<boolean> {
  try {
    const keys = await storage.get<IdempotencyKeyMetadata[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    return keys.some((k) => k.key === key);
  } catch (error) {
    return false;
  }
}
