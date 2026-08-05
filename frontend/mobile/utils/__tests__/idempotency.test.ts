/**
 * Unit tests for idempotency utility.
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  cleanupOldIdempotencyKeys,
  generateIdempotencyKey,
  getPendingIdempotencyKeys,
  hasIdempotencyKey,
  parseIdempotencyKey,
  removeIdempotencyKey,
  saveIdempotencyKey,
} from '../idempotency';
import { storage } from '../storage';

jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
}));

jest.mock('../storage', () => ({
  storage: {
    get: jest.fn(),
    set: jest.fn(),
    remove: jest.fn(),
  },
}));

const asyncStorageGetItem = AsyncStorage.getItem as jest.Mock;
const asyncStorageSetItem = AsyncStorage.setItem as jest.Mock;

describe('Idempotency Utility', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asyncStorageGetItem.mockResolvedValue(null);
    asyncStorageSetItem.mockResolvedValue(undefined);
    (AsyncStorage.removeItem as jest.Mock).mockResolvedValue(undefined);
  });

  describe('generateIdempotencyKey', () => {
    it('generates unique, parseable keys', () => {
      const key1 = generateIdempotencyKey('transfer', 'user123');
      const key2 = generateIdempotencyKey('transfer', 'user123');

      expect(key1).not.toBe(key2);
      expect(parseIdempotencyKey(key1)).toEqual(
        expect.objectContaining({ operation: 'transfer', userId: 'user123' })
      );
    });

    it('supports keys without a user id', () => {
      const key = generateIdempotencyKey('topup');

      expect(parseIdempotencyKey(key)).toEqual(expect.objectContaining({ operation: 'topup' }));
    });
  });

  describe('saveIdempotencyKey', () => {
    it('persists metadata through AsyncStorage instead of the SecureStore aggregate', async () => {
      await saveIdempotencyKey('test-key', 'transfer', 'user123');

      expect(asyncStorageSetItem).toHaveBeenCalledWith(
        '@payu:idempotency_keys',
        expect.stringContaining('test-key')
      );
      expect(storage.set).not.toHaveBeenCalled();
    });

    it('keeps the bounded metadata list', async () => {
      const existingKeys = Array.from({ length: 100 }, (_, index) => ({
        key: `key-${index}`,
        operation: 'transfer',
        timestamp: Date.now(),
        expiresAt: Date.now() + 86_400_000,
        status: 'pending' as const,
      }));
      asyncStorageGetItem.mockResolvedValue(JSON.stringify(existingKeys));

      await saveIdempotencyKey('new-key', 'transfer', 'user123');

      const savedKeys = JSON.parse(asyncStorageSetItem.mock.calls[0][1]);
      expect(savedKeys).toHaveLength(100);
      expect(savedKeys.at(-1)).toEqual(expect.objectContaining({ key: 'new-key' }));
    });

    it('migrates the legacy SecureStore aggregate once', async () => {
      const legacyKeys = [
        {
          key: 'legacy-key',
          operation: 'transfer',
          timestamp: Date.now(),
          expiresAt: Date.now() + 86_400_000,
          status: 'pending' as const,
        },
      ];
      asyncStorageGetItem.mockResolvedValue(null);
      (storage.get as jest.Mock).mockResolvedValue(legacyKeys);

      await saveIdempotencyKey('new-key', 'transfer');

      expect(storage.remove).toHaveBeenCalledWith('@payu:idempotency_keys');
      expect(asyncStorageSetItem).toHaveBeenCalledWith(
        '@payu:idempotency_keys',
        expect.stringContaining('legacy-key')
      );
    });

    it('rejects when metadata cannot be persisted', async () => {
      const error = new Error('metadata storage unavailable');
      asyncStorageSetItem.mockRejectedValue(error);

      await expect(saveIdempotencyKey('test-key', 'transfer')).rejects.toThrow(
        'metadata storage unavailable'
      );
    });
  });

  describe('removeIdempotencyKey', () => {
    it('removes only the requested key', async () => {
      const keys = [
        {
          key: 'key-1',
          operation: 'transfer',
          timestamp: Date.now(),
          expiresAt: Date.now() + 86_400_000,
          status: 'pending' as const,
        },
        {
          key: 'key-2',
          operation: 'transfer',
          timestamp: Date.now(),
          expiresAt: Date.now() + 86_400_000,
          status: 'pending' as const,
        },
      ];
      asyncStorageGetItem.mockResolvedValue(JSON.stringify(keys));

      await removeIdempotencyKey('key-1');

      expect(JSON.parse(asyncStorageSetItem.mock.calls[0][1])).toEqual([
        expect.objectContaining({ key: 'key-2' }),
      ]);
    });
  });

  describe('cleanupOldIdempotencyKeys', () => {
    it('removes expired keys', async () => {
      const keys = [
        {
          key: 'old-key',
          operation: 'transfer',
          timestamp: Date.now() - 25 * 60 * 60 * 1000,
          expiresAt: Date.now() - 1,
          status: 'pending' as const,
        },
        {
          key: 'recent-key',
          operation: 'transfer',
          timestamp: Date.now(),
          expiresAt: Date.now() + 86_400_000,
          status: 'pending' as const,
        },
      ];
      asyncStorageGetItem.mockResolvedValue(JSON.stringify(keys));

      await cleanupOldIdempotencyKeys();

      expect(JSON.parse(asyncStorageSetItem.mock.calls[0][1])).toEqual([
        expect.objectContaining({ key: 'recent-key' }),
      ]);
    });

    it('does not write when there is nothing to clean', async () => {
      asyncStorageGetItem.mockResolvedValue(JSON.stringify([]));

      await cleanupOldIdempotencyKeys();

      expect(asyncStorageSetItem).not.toHaveBeenCalled();
    });
  });

  describe('getPendingIdempotencyKeys', () => {
    it('returns only non-expired pending keys', async () => {
      const keys = [
        {
          key: 'pending-key',
          operation: 'transfer',
          timestamp: Date.now(),
          expiresAt: Date.now() + 86_400_000,
          status: 'pending' as const,
        },
        {
          key: 'expired-key',
          operation: 'transfer',
          timestamp: Date.now(),
          expiresAt: Date.now() - 1,
          status: 'pending' as const,
        },
      ];
      asyncStorageGetItem.mockResolvedValue(JSON.stringify(keys));

      await expect(getPendingIdempotencyKeys()).resolves.toEqual([
        expect.objectContaining({ key: 'pending-key' }),
      ]);
    });
  });

  describe('hasIdempotencyKey', () => {
    it('returns true for an existing non-expired key', async () => {
      asyncStorageGetItem.mockResolvedValue(JSON.stringify([{
        key: 'existing-key',
        operation: 'transfer',
        timestamp: Date.now(),
        expiresAt: Date.now() + 86_400_000,
        status: 'pending',
      }]));

      await expect(hasIdempotencyKey('existing-key')).resolves.toBe(true);
    });

    it('returns false for a missing key', async () => {
      await expect(hasIdempotencyKey('missing-key')).resolves.toBe(false);
    });
  });
});
