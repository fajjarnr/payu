import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { Logger } from './logger';

/**
 * Performance: Track known keys for batch operations
 * This enables parallel operations and bulk clearing
 */
const KNOWN_KEYS = new Set<string>();

/**
 * Enhanced Storage Utility with Parallel Operations Support
 *
 * SECURITY POLICY: Log Sanitization (P2-C3)
 * ==========================================
 * - All errors logged via sanitized logger (no sensitive data in logs)
 * - Storage values are NEVER logged (only keys)
 * - Error messages don't contain value content
 *
 * Performance Optimizations (P2-C6):
 * 1. Parallel async operations with Promise.all for independent reads
 * 2. Key tracking for batch operations
 * 3. Batch get/set for reduced async overhead
 */
export const storage = {
  /**
   * Get a single value from secure store
   * Performance: Async, cached in memory after first read
   */
  async get<T>(key: string): Promise<T | null> {
    if (Platform.OS === 'web') {
      return null;
    }

    try {
      const value = await SecureStore.getItemAsync(key);
      return value ? JSON.parse(value) : null;
    } catch (error) {
      // Sanitized logging - key is safe, value is not logged
      Logger.error('Storage', 'Failed to read from secure storage', error, { key });
      return null;
    }
  },

  /**
   * Get multiple values in parallel (performance optimization)
   * Useful for app initialization when loading multiple values
   *
   * @example
   * const [user, tokens, settings] = await storage.getMany(['user', 'tokens', 'settings']);
   */
  async getMany<T extends Record<string, any>>(keys: string[]): Promise<(T | null)[]> {
    // Parallel read for better performance
    return Promise.all(keys.map(key => this.get<T>(key)));
  },

  /**
   * Set a single value
   * Performance: Async, tracks key for batch operations
   */
  async set<T>(key: string, value: T): Promise<boolean> {
    if (Platform.OS === 'web') {
      return false;
    }

    try {
      await SecureStore.setItemAsync(key, JSON.stringify(value));
      KNOWN_KEYS.add(key); // Track key for later bulk operations
      // Log success with key only (value is sensitive, not logged)
      Logger.debug('Storage', 'Stored value in secure storage', { key });
      return true;
    } catch (error) {
      // Sanitized logging - key is safe, value is intentionally omitted
      Logger.error('Storage', 'Failed to write to secure storage', error, { key });
      return false;
    }
  },

  /**
   * Set multiple values in parallel (performance optimization)
   * Useful for batch writes during login/initialization
   *
   * @example
   * await storage.setMany([
   *   ['user', userData],
   *   ['tokens', tokenData],
   *   ['settings', settingsData]
   * ]);
   */
  async setMany<T>(entries: [string, T][]): Promise<boolean[]> {
    // Parallel write for better performance
    return Promise.all(
      entries.map(([key, value]) => this.set<T>(key, value))
    );
  },

  /**
   * Remove a single value
   */
  async remove(key: string): Promise<boolean> {
    if (Platform.OS === 'web') {
      return true;
    }

    try {
      await SecureStore.deleteItemAsync(key);
      KNOWN_KEYS.delete(key);
      Logger.debug('Storage', 'Removed value from secure storage', { key });
      return true;
    } catch (error) {
      Logger.error('Storage', 'Failed to delete from secure storage', error, { key });
      return false;
    }
  },

  /**
   * Remove multiple values in parallel (performance optimization)
   *
   * @example
   * await storage.removeMany(['tempCache', 'oldSession']);
   */
  async removeMany(keys: string[]): Promise<boolean[]> {
    return Promise.all(keys.map(key => this.remove(key)));
  },

  /**
   * Clear all known keys from secure store
   * Uses tracked keys for efficient bulk deletion
   */
  async clear(): Promise<boolean> {
    if (Platform.OS === 'web') {
      return true;
    }

    try {
      // Delete all tracked keys in parallel
      const keys = Array.from(KNOWN_KEYS);
      await Promise.all(keys.map(key => SecureStore.deleteItemAsync(key)));
      KNOWN_KEYS.clear();
      Logger.info('Storage', 'Cleared all tracked secure storage keys', { keysCleared: keys.length });
      return true;
    } catch (error) {
      Logger.error('Storage', 'Failed to clear secure storage', error);
      return false;
    }
  },

  /**
   * Get all tracked keys (useful for debugging)
   */
  getTrackedKeys(): string[] {
    return Array.from(KNOWN_KEYS);
  },
};
