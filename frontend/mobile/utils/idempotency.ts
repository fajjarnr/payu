/**
 * Idempotency key generation for financial operations, preventing duplicate transactions
 * during network retries. UUID v4 keys persist locally for 24h.
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
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

async function readStoredKeys(): Promise<StoredIdempotencyKey[]> {
  const raw = await AsyncStorage.getItem(IDEMPOTENCY_KEYS_STORAGE_KEY);
  if (raw) {
    return JSON.parse(raw) as StoredIdempotencyKey[];
  }

  // Migrate the old SecureStore aggregate once. AsyncStorage is used only for
  // retry metadata; auth tokens and financial payloads remain in secure storage.
  const legacyKeys = await storage.get<StoredIdempotencyKey[]>(IDEMPOTENCY_KEYS_STORAGE_KEY);
  if (legacyKeys?.length) {
    await AsyncStorage.setItem(IDEMPOTENCY_KEYS_STORAGE_KEY, JSON.stringify(legacyKeys));
    await storage.remove(IDEMPOTENCY_KEYS_STORAGE_KEY);
    return legacyKeys;
  }

  return [];
}

async function writeStoredKeys(keys: StoredIdempotencyKey[]): Promise<void> {
  await AsyncStorage.setItem(IDEMPOTENCY_KEYS_STORAGE_KEY, JSON.stringify(keys));
}

export function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const random = (crypto.getRandomValues(new Uint8Array(1))[0] / 256) * 16 | 0;
    const value = c === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

/** Builds a "{operation}::{userId}::{uuid}" idempotency key. */
export function generateIdempotencyKey(operation: string, userId?: string): string {
  const uuid = generateUUID();

  if (userId) {
    return `${operation}::${userId}::${uuid}`;
  }

  return `${operation}::${uuid}`;
}

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

export function isValidUUID(uuid: string): boolean {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  return uuidRegex.test(uuid);
}

/** Persists a key with metadata so pending operations survive an app restart. */
export async function saveIdempotencyKey(
  key: string,
  operation: string,
  userId?: string
): Promise<void> {
  try {
    const storedKeys = await readStoredKeys();

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

    await writeStoredKeys(trimmedKeys);

    Logger.debug('Idempotency', 'Key saved', { key, operation });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to save idempotency key', error, { key, operation });
    throw error;
  }
}

export async function updateIdempotencyKeyStatus(
  key: string,
  status: StoredIdempotencyKey['status'],
  retryCount?: number
): Promise<void> {
  try {
    const storedKeys = await readStoredKeys();

    const keyIndex = storedKeys.findIndex((k) => k.key === key);
    if (keyIndex === -1) return;

    storedKeys[keyIndex].status = status;
    if (retryCount !== undefined) {
      storedKeys[keyIndex].retryCount = (storedKeys[keyIndex].retryCount || 0) + retryCount;
    }

    await writeStoredKeys(storedKeys);

    Logger.debug('Idempotency', 'Key status updated', { key, status });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to update idempotency key status', error, { key, status });
  }
}

export async function removeIdempotencyKey(key: string): Promise<void> {
  try {
    const storedKeys = await readStoredKeys();

    const filteredKeys = storedKeys.filter((k) => k.key !== key);

    await writeStoredKeys(filteredKeys);

    Logger.debug('Idempotency', 'Key removed', { key });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to remove idempotency key', error, { key });
  }
}

export async function cleanupOldIdempotencyKeys(): Promise<number> {
  try {
    const storedKeys = await readStoredKeys();
    const now = Date.now();

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
      await writeStoredKeys(validKeys);
      Logger.info('Idempotency', `Cleaned up ${cleanedCount} expired keys`);
    }

    return cleanedCount;
  } catch (error) {
    Logger.error('Idempotency', 'Failed to cleanup old idempotency keys', error);
    return 0;
  }
}

export async function getPendingIdempotencyKeys(): Promise<StoredIdempotencyKey[]> {
  try {
    const storedKeys = await readStoredKeys();
    const now = Date.now();

    return storedKeys.filter((k) => {
      const isExpired = k.expiresAt ? k.expiresAt < now : false;
      return k.status === 'pending' && !isExpired;
    });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to get pending idempotency keys', error);
    return [];
  }
}

export async function hasIdempotencyKey(key: string): Promise<boolean> {
  try {
    const storedKeys = await readStoredKeys();
    const now = Date.now();

    return storedKeys.some((k) => {
      const isExpired = k.expiresAt ? k.expiresAt < now : false;
      return k.key === key && !isExpired;
    });
  } catch (error) {
    Logger.error('Idempotency', 'Failed to check idempotency key', error, { key });
    return false;
  }
}

export async function getIdempotencyKeyMetadata(
  key: string
): Promise<StoredIdempotencyKey | null> {
  try {
    const storedKeys = await readStoredKeys();
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

export async function clearAllIdempotencyKeys(): Promise<void> {
  try {
    await AsyncStorage.removeItem(IDEMPOTENCY_KEYS_STORAGE_KEY);
    await storage.remove(IDEMPOTENCY_KEYS_STORAGE_KEY);
    Logger.info('Idempotency', 'All idempotency keys cleared');
  } catch (error) {
    Logger.error('Idempotency', 'Failed to clear idempotency keys', error);
  }
}
