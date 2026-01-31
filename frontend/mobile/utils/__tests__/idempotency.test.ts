/**
 * Unit tests for idempotency utility
 */

import {
  generateIdempotencyKey,
  saveIdempotencyKey,
  removeIdempotencyKey,
  cleanupOldIdempotencyKeys,
  getPendingIdempotencyKeys,
  hasIdempotencyKey,
} from '../idempotency';
import { storage } from '../storage';

// Mock storage
jest.mock('../storage', () => ({
  storage: {
    get: jest.fn(),
    set: jest.fn(),
    remove: jest.fn(),
  },
}));

describe('Idempotency Utility', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('generateIdempotencyKey', () => {
    it('should generate unique keys for each call', () => {
      const key1 = generateIdempotencyKey('transfer', 'user123');
      const key2 = generateIdempotencyKey('transfer', 'user123');

      expect(key1).not.toBe(key2);
    });

    it('should include operation type in the key', () => {
      const key = generateIdempotencyKey('transfer', 'user123');
      expect(key).toContain('transfer');
    });

    it('should include userId in the key when provided', () => {
      const key = generateIdempotencyKey('transfer', 'user123');
      expect(key).toContain('user123');
    });

    it('should generate keys without userId', () => {
      const key = generateIdempotencyKey('topup');
      expect(key).toContain('topup');
      expect(key).toMatch(/^topup-\d+-[a-z0-9]+$/);
    });

    it('should generate keys in correct format', () => {
      const key = generateIdempotencyKey('transfer', 'user123');
      expect(key).toMatch(/^transfer-user123-\d+-[a-z0-9]+$/);
    });
  });

  describe('saveIdempotencyKey', () => {
    it('should save idempotency key to storage', async () => {
      (storage.get as jest.Mock).mockResolvedValue([]);

      await saveIdempotencyKey('test-key', 'transfer', 'user123');

      expect(storage.set).toHaveBeenCalledWith(
        '@payu:idempotency_keys',
        expect.arrayContaining([
          expect.objectContaining({
            key: 'test-key',
            operation: 'transfer',
            userId: 'user123',
          }),
        ])
      );
    });

    it('should append to existing keys', async () => {
      const existingKeys = [
        { key: 'existing-key', operation: 'topup', timestamp: Date.now() },
      ];
      (storage.get as jest.Mock).mockResolvedValue(existingKeys);

      await saveIdempotencyKey('new-key', 'transfer', 'user123');

      expect(storage.set).toHaveBeenCalledWith(
        '@payu:idempotency_keys',
        expect.arrayContaining([
          expect.objectContaining({ key: 'existing-key' }),
          expect.objectContaining({ key: 'new-key' }),
        ])
      );
    });

    it('should limit keys to 100 entries', async () => {
      const existingKeys = Array.from({ length: 100 }, (_, i) => ({
        key: `key-${i}`,
        operation: 'transfer',
        timestamp: Date.now(),
      }));
      (storage.get as jest.Mock).mockResolvedValue(existingKeys);

      await saveIdempotencyKey('new-key', 'transfer', 'user123');

      const savedKeys = (storage.set as jest.Mock).mock.calls[0][1];
      expect(savedKeys).toHaveLength(100);
    });

    it('should handle storage errors gracefully', async () => {
      (storage.get as jest.Mock).mockRejectedValue(new Error('Storage error'));

      await expect(saveIdempotencyKey('test-key', 'transfer')).resolves.not.toThrow();
    });
  });

  describe('removeIdempotencyKey', () => {
    it('should remove specific key from storage', async () => {
      const existingKeys = [
        { key: 'key-1', operation: 'transfer', timestamp: Date.now() },
        { key: 'key-2', operation: 'transfer', timestamp: Date.now() },
      ];
      (storage.get as jest.Mock).mockResolvedValue(existingKeys);

      await removeIdempotencyKey('key-1');

      expect(storage.set).toHaveBeenCalledWith(
        '@payu:idempotency_keys',
        expect.arrayContaining([
          expect.objectContaining({ key: 'key-2' }),
        ])
      );

      const savedKeys = (storage.set as jest.Mock).mock.calls[0][1];
      expect(savedKeys).not.toContainEqual(expect.objectContaining({ key: 'key-1' }));
    });

    it('should handle storage errors gracefully', async () => {
      (storage.get as jest.Mock).mockRejectedValue(new Error('Storage error'));

      await expect(removeIdempotencyKey('test-key')).resolves.not.toThrow();
    });
  });

  describe('cleanupOldIdempotencyKeys', () => {
    it('should remove keys older than 24 hours', async () => {
      const oldTimestamp = Date.now() - 25 * 60 * 60 * 1000; // 25 hours ago
      const recentTimestamp = Date.now() - 1 * 60 * 60 * 1000; // 1 hour ago

      const keys = [
        { key: 'old-key', operation: 'transfer', timestamp: oldTimestamp },
        { key: 'recent-key', operation: 'transfer', timestamp: recentTimestamp },
      ];
      (storage.get as jest.Mock).mockResolvedValue(keys);

      await cleanupOldIdempotencyKeys();

      const savedKeys = (storage.set as jest.Mock).mock.calls[0][1];
      expect(savedKeys).toHaveLength(1);
      expect(savedKeys[0].key).toBe('recent-key');
    });

    it('should handle empty storage', async () => {
      (storage.get as jest.Mock).mockResolvedValue([]);

      await cleanupOldIdempotencyKeys();

      expect(storage.set).toHaveBeenCalledWith('@payu:idempotency_keys', []);
    });

    it('should handle storage errors gracefully', async () => {
      (storage.get as jest.Mock).mockRejectedValue(new Error('Storage error'));

      await expect(cleanupOldIdempotencyKeys()).resolves.not.toThrow();
    });
  });

  describe('getPendingIdempotencyKeys', () => {
    it('should return keys from the last hour', () => {
      const recentKey = {
        key: 'recent-key',
        operation: 'transfer',
        timestamp: Date.now() - 30 * 60 * 1000, // 30 minutes ago
      };
      const oldKey = {
        key: 'old-key',
        operation: 'transfer',
        timestamp: Date.now() - 2 * 60 * 60 * 1000, // 2 hours ago
      };

      (storage.get as jest.Mock).mockResolvedValue([recentKey, oldKey]);

      const promise = getPendingIdempotencyKeys();
      expect(promise).resolves.toEqual([recentKey]);
    });

    it('should return empty array on error', () => {
      (storage.get as jest.Mock).mockRejectedValue(new Error('Storage error'));

      const promise = getPendingIdempotencyKeys();
      expect(promise).resolves.toEqual([]);
    });
  });

  describe('hasIdempotencyKey', () => {
    it('should return true when key exists', async () => {
      const keys = [
        { key: 'existing-key', operation: 'transfer', timestamp: Date.now() },
      ];
      (storage.get as jest.Mock).mockResolvedValue(keys);

      const result = await hasIdempotencyKey('existing-key');
      expect(result).toBe(true);
    });

    it('should return false when key does not exist', async () => {
      const keys = [
        { key: 'existing-key', operation: 'transfer', timestamp: Date.now() },
      ];
      (storage.get as jest.Mock).mockResolvedValue(keys);

      const result = await hasIdempotencyKey('non-existent-key');
      expect(result).toBe(false);
    });

    it('should return false on storage error', async () => {
      (storage.get as jest.Mock).mockRejectedValue(new Error('Storage error'));

      const result = await hasIdempotencyKey('test-key');
      expect(result).toBe(false);
    });
  });
});
