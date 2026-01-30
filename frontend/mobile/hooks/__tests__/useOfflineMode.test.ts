import { renderHook, act } from '@testing-library/react-native';
import { useOfflineMode } from '../useOfflineMode';
import NetInfo from '@react-native-community/netinfo';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Mock dependencies
jest.mock('@react-native-community/netinfo', () => ({
  addEventListener: jest.fn(),
  fetch: jest.fn(),
}));

jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
}));

describe('useOfflineMode', () => {
  const mockUnsubscribe = jest.fn();
  let netInfoCallback: ((state: any) => void) | null = null;

  beforeEach(() => {
    jest.clearAllMocks();

    // Capture the callback passed to addEventListener
    (NetInfo.addEventListener as jest.Mock).mockImplementation((callback) => {
      netInfoCallback = callback;
      return mockUnsubscribe;
    });

    (AsyncStorage.getItem as jest.Mock).mockResolvedValue(null);
    (AsyncStorage.setItem as jest.Mock).mockResolvedValue(undefined);
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useOfflineMode());

    expect(result.current.isOnline).toBe(true);
    expect(result.current.offlineQueue).toEqual([]);
    expect(result.current.hasPendingActions).toBe(false);
  });

  it('should subscribe to network status on mount', () => {
    renderHook(() => useOfflineMode());

    expect(NetInfo.addEventListener).toHaveBeenCalled();
  });

  it('should update online status when network changes', () => {
    const { result } = renderHook(() => useOfflineMode());

    // Simulate going offline
    act(() => {
      netInfoCallback?.({ isConnected: false });
    });

    expect(result.current.isOnline).toBe(false);

    // Simulate coming back online
    act(() => {
      netInfoCallback?.({ isConnected: true });
    });

    expect(result.current.isOnline).toBe(true);
  });

  it('should add item to offline queue', async () => {
    const { result } = renderHook(() => useOfflineMode());

    let itemId: string | undefined;
    await act(async () => {
      itemId = await result.current.addToOfflineQueue('transfer', { amount: 50000, toAccount: '123456' });
    });

    expect(itemId).toBeDefined();
    expect(AsyncStorage.setItem).toHaveBeenCalled();
  });

  it('should not process queue when going offline', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

    renderHook(() => useOfflineMode());

    act(() => {
      netInfoCallback?.({ isConnected: false });
    });

    expect(consoleSpy).not.toHaveBeenCalledWith('Processing offline item:', expect.any(Object));

    consoleSpy.mockRestore();
  });

  it('should cache data for offline use', async () => {
    const { result } = renderHook(() => useOfflineMode());

    const cacheData = { transactions: [{ id: 'tx-1', amount: 100000 }] };
    await act(async () => {
      await result.current.cacheForOffline('transactions', cacheData);
    });

    expect(AsyncStorage.setItem).toHaveBeenCalledWith(
      '@payu:offline_cache',
      expect.stringContaining('transactions')
    );
  });

  it('should retrieve cached data', async () => {
    const cacheData = {
      transactions: {
        data: [{ id: 'tx-1', amount: 100000 }],
        timestamp: Date.now(),
      },
    };
    (AsyncStorage.getItem as jest.Mock).mockImplementation((key: string) => {
      if (key === '@payu:offline_cache') {
        return Promise.resolve(JSON.stringify(cacheData));
      }
      return Promise.resolve(null);
    });

    const { result } = renderHook(() => useOfflineMode());

    let cached: any = null;
    await act(async () => {
      cached = await result.current.getCachedData('transactions');
    });

    expect(cached).not.toBeNull();
    expect(cached?.data).toEqual([{ id: 'tx-1', amount: 100000 }]);
    expect(cached?.isStale).toBe(false);
  });

  it('should return null for non-existent cache key', async () => {
    (AsyncStorage.getItem as jest.Mock).mockResolvedValue(JSON.stringify({}));

    const { result } = renderHook(() => useOfflineMode());

    let cached = null;
    await act(async () => {
      cached = await result.current.getCachedData('nonexistent');
    });

    expect(cached).toBeNull();
  });

  it('should mark cache as stale if older than 1 hour', async () => {
    const oldCacheData = {
      transactions: {
        data: [{ id: 'tx-1', amount: 100000 }],
        timestamp: Date.now() - 2 * 60 * 60 * 1000, // 2 hours ago
      },
    };
    (AsyncStorage.getItem as jest.Mock).mockImplementation((key: string) => {
      if (key === '@payu:offline_cache') {
        return Promise.resolve(JSON.stringify(oldCacheData));
      }
      return Promise.resolve(null);
    });

    const { result } = renderHook(() => useOfflineMode());

    let cached: any = null;
    await act(async () => {
      cached = await result.current.getCachedData('transactions');
    });

    expect(cached).not.toBeNull();
    expect(cached?.isStale).toBe(true);
  });

  it('should clear offline queue', async () => {
    const { result } = renderHook(() => useOfflineMode());

    await act(async () => {
      await result.current.clearOfflineQueue();
    });

    expect(AsyncStorage.setItem).toHaveBeenCalledWith('@payu:offline_queue', '[]');
  });

  it('should unsubscribe from NetInfo on unmount', () => {
    const { unmount } = renderHook(() => useOfflineMode());

    unmount();

    expect(mockUnsubscribe).toHaveBeenCalled();
  });
});
