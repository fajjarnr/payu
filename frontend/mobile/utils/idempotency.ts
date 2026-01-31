/**
 * Idempotency Utility
 *
 * Provides unique idempotency key generation for financial operations
 * to prevent duplicate transactions during network retries.
 * Uses UUID v4 for guaranteed uniqueness across distributed systems.
 *
 * Features:
 * - UUID v4 based key generation
 * - Local storage for 24h persistence
 * - Recovery support for pending operations
 * - Automatic cleanup of expired keys
 */

import { storage } from './storage';
import { Logger } from './logger';

const IDEMPOTENCY_KEYS_STORAGE_KEY = '@payu:idempotency_keys';
const IDEMPOTENCY_KEYS_TTL = 24 * 60 * 60 * 1000; // 24 hours

export interface IdempotencyKeyMetadata {
  key: string;
  operation: string;
  timestamp: number;
  userId?: string;
  retryCount?: number;
}

export interface StoredIdempotencyKey extends IdempotencyKeyMetadata {
  expiresAt: number;
  status: 'pending' | 'completed' | 'failed';
}

/**
 * UUID v4 generator
 * Generates a RFC4122 compliant UUID v4
 *
 * @returns UUID v4 string
 */
export function generateUUID(): string {
  // RFC4122 compliant UUID v4 generator
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const random = (Math.random() * 16) | 0;
    const value = c === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

/**
 * Generate a UUID-based idempotency key for a financial operation
 * Format: {operation}::{userId}::{uuid}
 *
 * This format ensures:
 * - Uniqueness across all operations (UUID v4)
 * - Readability and traceability (operation prefix)
 * - User-scoped isolation when userId is provided
 *
 * @param operation - The type of operation (e.g., 'transfer', 'topup', 'qris')
 * @param userId - Optional user ID for better tracing and scoping
 * @returns A unique idempotency key string (UUID-based)
 *
 * @example
 * ```ts
 * const key = generateIdempotencyKey('transfer', 'user-123');
 * // Returns: "transfer::user-123::550e8400-e29b-41d4-a716-446655440000"
 * ```
 */
export function generateIdempotencyKey(operation: string, userId?: string): string {
  const uuid = generateUUID();

  if (userId) {
    return `${operation}::${userId}::${uuid}`;
  }

  return `${operation}::${uuid}`;
}

/**
 * Parse an idempotency key to extract its components
 *
 * @param key - The idempotency key to parse
 * @returns Parsed components or null if invalid format
 */
export function parseIdempotencyKey(
  key: string
): { operation: string; userId?: string; uuid: string } | null {
  const parts = key.split('::');
  if (parts.length < 2) return null;

  // Format: operation::userId::uuid or operation::uuid
  if (parts.length === 2) {
    const [operation, uuid] = parts;
    return { operation, uuid };
  }

  if (parts.length === 3) {
    const [operation, userId, uuid] = parts;
    return { operation, userId, uuid };
  }

  return null;
}

/**
 * Validate if a string is a valid UUID v4
 *
 * @param uuid - The string to validate
 * @returns True if valid UUID v4
 */
export function isValidUUID(uuid: string): boolean {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  return uuidRegex.test(uuid);
}

/**
 * Save an idempotency key to persistent storage with metadata
 * This allows recovery of pending operations after app restart
 *
 * @param key - The idempotency key to save
 * @param operation - The operation type
 * @param userId - Optional user ID
 * @returns Promise that resolves when saved
 */
export async function saveIdempotencyKey(
  key: string,
  operation: string,
  userId?: string
): Promise<void> {
  try {
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    const newKey: StoredIdempotencyKey = {
      key,
      operation,
      timestamp: Date.now(),
      expiresAt: Date.now() + IDEMPOTENCY_KEYS_TTL,
      userId,
      status: 'pending',
      retryCount: 0,
    };

    storedKeys.push(newKey);

    // Keep only last 100 keys to prevent storage bloat
    const trimmedKeys = storedKeys.slice(-100);

    await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, trimmedKeys);

    Logger.debug('Idempotency', 'Key saved', { key, operation });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to save idempotency key', error, { key, operation });
  }
}

/**
 * Update the status of an idempotency key
 *
 * @param key - The idempotency key to update
 * @param status - The new status
 * @param retryCount - Optional retry count increment
 */
export async function updateIdempotencyKeyStatus(
  key: string,
  status: StoredIdempotencyKey['status'],
  retryCount?: number
): Promise<void> {
  try {
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    const keyIndex = storedKeys.findIndex((k) => k.key === key);
    if (keyIndex === -1) return;

    storedKeys[keyIndex].status = status;
    if (retryCount !== undefined) {
      storedKeys[keyIndex].retryCount = (storedKeys[keyIndex].retryCount || 0) + retryCount;
    }

    await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, storedKeys);

    Logger.debug('Idempotency', 'Key status updated', { key, status });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to update idempotency key status', error, { key, status });
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
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];

    const filteredKeys = storedKeys.filter((k) => k.key !== key);

    await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, filteredKeys);

    Logger.debug('Idempotency', 'Key removed', { key });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to remove idempotency key', error, { key });
  }
}

/**
 * Clean up expired idempotency keys (older than 24 hours)
 * Should be called periodically or on app startup
 *
 * @returns Number of keys cleaned up
 */
export async function cleanupOldIdempotencyKeys(): Promise<number> {
  try {
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];
    const now = Date.now();

    // Filter out expired keys
    const validKeys = storedKeys.filter((k) => {
      if (k.expiresAt) {
        return k.expiresAt > now;
      }
      // Fallback to timestamp check for backward compatibility
      const oneDayAgo = now - IDEMPOTENCY_KEYS_TTL;
      return k.timestamp > oneDayAgo;
    });

    const cleanedCount = storedKeys.length - validKeys.length;

    if (cleanedCount > 0) {
      await storage.set(IDEMPOTENCY_KEYS_STORAGE_KEY, validKeys);
      Logger.info('Idempotency', `Cleaned up ${cleanedCount} expired keys`);
    }

    return cleanedCount;
  } catch (error) {
    Logger.error('Idempotency', 'Failed to cleanup old idempotency keys', error);
    return 0;
  }
}

/**
 * Get all pending idempotency keys
 * Useful for recovery scenarios
 *
 * @returns Array of pending idempotency key metadata
 */
export async function getPendingIdempotencyKeys(): Promise<StoredIdempotencyKey[]> {
  try {
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];
    const now = Date.now();

    // Only return non-expired pending keys
    return storedKeys.filter((k) => {
      const isExpired = k.expiresAt ? k.expiresAt < now : false;
      return k.status === 'pending' && !isExpired;
    });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to get pending idempotency keys', error);
    return [];
  }
}

/**
 * Check if a specific idempotency key exists in storage
 * Useful for preventing duplicate operations in the same session
 *
 * @param key - The idempotency key to check
 * @returns True if the key exists in storage
 */
export async function hasIdempotencyKey(key: string): Promise<boolean> {
  try {
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];
    const now = Date.now();

    // Check if key exists and is not expired
    return storedKeys.some((k) => {
      const isExpired = k.expiresAt ? k.expiresAt < now : false;
      return k.key === key && !isExpired;
    });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to check idempotency key', error, { key });
    return false;
  }
}

/**
 * Get metadata for a specific idempotency key
 *
 * @param key - The idempotency key to look up
 * @returns The stored metadata or null if not found
 */
export async function getIdempotencyKeyMetadata(
  key: string
): Promise<StoredIdempotencyKey | null> {
  try {
    const storedKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY) || [];
    const now = Date.now();

    const foundKey = storedKeys.find((k) => {
      const isExpired = k.expiresAt ? k.expiresAt < now : false;
      return k.key === key && !isExpired;
    });

    return foundKey || null;
  } catch (error) {
    Logger.error('Idempotency', 'Failed to get idempotency key metadata', error, { key });
    return null;
  }
}

/**
 * Clear all idempotency keys from storage
 * Useful for logout or testing purposes
 *
 * @returns Promise that resolves when cleared
 */
export async function clearAllIdempotencyKeys(): Promise<void> {
  try {
    await storage.remove(IDEMPOTENCY_KEYS_STORAGE_KEY);
    Logger.info('Idempotency', 'All idempotency keys cleared');
  } catch (error) {
    Logger.error('Idempotency', 'Failed to clear idempotency keys', error);
  }
}
