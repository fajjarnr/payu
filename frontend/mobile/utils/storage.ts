import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { Logger } from './logger';

/** Tracks known keys to enable batch operations and bulk clearing. */
const KNOWN_KEYS = new Set<string>();

/**
 * Secure storage wrapper. Security P2-C3: values are never logged, only keys.
 * Performance P2-C6: parallel batch get/set/remove.
 */
export const storage = {
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

  async getMany<T extends Record<string, any>>(keys: string[]): Promise<(T | null)[]> {
    // Parallel read for better performance
    return Promise.all(keys.map(key => this.get<T>(key)));
  },

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

  async setMany<T>(entries: [string, T][]): Promise<boolean[]> {
    // Parallel write for better performance
    return Promise.all(
      entries.map(([key, value]) => this.set<T>(key, value))
    );
  },

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

  async removeMany(keys: string[]): Promise<boolean[]> {
    return Promise.all(keys.map(key => this.remove(key)));
  },

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

  getTrackedKeys(): string[] {
    return Array.from(KNOWN_KEYS);
  },
};
