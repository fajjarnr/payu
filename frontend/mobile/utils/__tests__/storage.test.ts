import { storage } from '../storage';
import { Logger } from '../logger';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

// Mock expo-secure-store
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

describe('storage.get', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should retrieve and parse stored value', async () => {
    const mockValue = { name: 'John', age: 30 };
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(JSON.stringify(mockValue));

    const result = await storage.get('user');

    expect(SecureStore.getItemAsync).toHaveBeenCalledWith('user');
    expect(result).toEqual(mockValue);
  });

  it('should return null for non-existent key', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(null);

    const result = await storage.get('nonexistent');

    expect(result).toBeNull();
  });

  it('should return null when error occurs', async () => {
    const loggerError = jest.spyOn(Logger, 'error').mockImplementation(() => {});
    (SecureStore.getItemAsync as jest.Mock).mockRejectedValue(new Error('Storage error'));

    const result = await storage.get('user');

    expect(result).toBeNull();
    expect(loggerError).toHaveBeenCalledWith(
      'Storage',
      'Failed to read from secure storage',
      expect.any(Error),
      { key: 'user' }
    );

    loggerError.mockRestore();
  });

  it('should handle string values', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue('"test string"');

    const result = await storage.get('token');

    expect(result).toBe('test string');
  });

  it('should handle number values', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue('12345');

    const result = await storage.get('count');

    expect(result).toBe(12345);
  });

  it('should handle boolean values', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue('true');

    const result = await storage.get('isActive');

    expect(result).toBe(true);
  });

  it('should handle array values', async () => {
    const mockArray = [1, 2, 3];
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(JSON.stringify(mockArray));

    const result = await storage.get('items');

    expect(result).toEqual([1, 2, 3]);
  });

  it('should handle nested objects', async () => {
    const mockObject = { user: { profile: { name: 'John' } } };
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(JSON.stringify(mockObject));

    const result = await storage.get('nested');

    expect(result).toEqual(mockObject);
  });
});

describe('storage.set', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should store object value', async () => {
    const mockValue = { name: 'John', age: 30 };
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('user', mockValue);

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('user', JSON.stringify(mockValue));
    expect(result).toBe(true);
  });

  it('should store string value', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('token', 'abc123');

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('token', '"abc123"');
    expect(result).toBe(true);
  });

  it('should store number value', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('count', 42);

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('count', '42');
    expect(result).toBe(true);
  });

  it('should store boolean value', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('isActive', false);

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('isActive', 'false');
    expect(result).toBe(true);
  });

  it('should store null value', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('empty', null);

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('empty', 'null');
    expect(result).toBe(true);
  });

  it('should return false when error occurs', async () => {
    const loggerError = jest.spyOn(Logger, 'error').mockImplementation(() => {});
    (SecureStore.setItemAsync as jest.Mock).mockRejectedValue(new Error('Storage error'));

    const result = await storage.set('user', { name: 'John' });

    expect(result).toBe(false);
    expect(loggerError).toHaveBeenCalledWith(
      'Storage',
      'Failed to write to secure storage',
      expect.any(Error),
      { key: 'user' }
    );

    loggerError.mockRestore();
  });

  it('should store array value', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('items', [1, 2, 3]);

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('items', '[1,2,3]');
    expect(result).toBe(true);
  });

  it('should store empty object', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('empty', {});

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('empty', '{}');
    expect(result).toBe(true);
  });

  it('should store empty string', async () => {
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.set('empty', '');

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('empty', '""');
    expect(result).toBe(true);
  });
});

describe('storage.remove', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should delete item successfully', async () => {
    (SecureStore.deleteItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.remove('user');

    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('user');
    expect(result).toBe(true);
  });

  it('should return false when error occurs', async () => {
    const loggerError = jest.spyOn(Logger, 'error').mockImplementation(() => {});
    (SecureStore.deleteItemAsync as jest.Mock).mockRejectedValue(new Error('Storage error'));

    const result = await storage.remove('user');

    expect(result).toBe(false);
    expect(loggerError).toHaveBeenCalledWith(
      'Storage',
      'Failed to delete from secure storage',
      expect.any(Error),
      { key: 'user' }
    );

    loggerError.mockRestore();
  });

  it('should handle removing non-existent key', async () => {
    (SecureStore.deleteItemAsync as jest.Mock).mockResolvedValue(undefined);

    const result = await storage.remove('nonexistent');

    expect(result).toBe(true);
  });
});

describe('storage.clear', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should return true (placeholder implementation)', async () => {
    const result = await storage.clear();

    expect(result).toBe(true);
  });

  it('should handle multiple calls', async () => {
    const result1 = await storage.clear();
    const result2 = await storage.clear();

    expect(result1).toBe(true);
    expect(result2).toBe(true);
  });

  it('should return false and log when deleting a tracked key fails', async () => {
    const loggerError = jest.spyOn(Logger, 'error').mockImplementation(() => {});
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);
    await storage.set('clear-failure', { value: 'test' });
    (SecureStore.deleteItemAsync as jest.Mock).mockRejectedValue(new Error('Storage error'));

    await expect(storage.clear()).resolves.toBe(false);
    expect(loggerError).toHaveBeenCalledWith(
      'Storage',
      'Failed to clear secure storage',
      expect.any(Error)
    );

    loggerError.mockRestore();
  });
});

describe('storage on web', () => {
  const nativePlatform = Platform.OS;

  beforeEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, value: 'web' });
    jest.clearAllMocks();
  });

  afterEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, value: nativePlatform });
  });

  it('fails closed without calling SecureStore', async () => {
    expect(await storage.get('token')).toBeNull();
    expect(await storage.set('token', 'secret')).toBe(false);
    expect(await storage.remove('token')).toBe(true);
    expect(await storage.clear()).toBe(true);
    expect(SecureStore.getItemAsync).not.toHaveBeenCalled();
    expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
    expect(SecureStore.deleteItemAsync).not.toHaveBeenCalled();
  });
});

describe('storage integration', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should handle full lifecycle of data', async () => {
    const userData = { id: 1, name: 'John' };

    // Set data
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);
    await storage.set('user', userData);

    // Get data
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(JSON.stringify(userData));
    const retrieved = await storage.get('user');
    expect(retrieved).toEqual(userData);

    // Remove data
    (SecureStore.deleteItemAsync as jest.Mock).mockResolvedValue(undefined);
    await storage.remove('user');

    // Verify removal
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(null);
    const afterRemoval = await storage.get('user');
    expect(afterRemoval).toBeNull();
  });
});
