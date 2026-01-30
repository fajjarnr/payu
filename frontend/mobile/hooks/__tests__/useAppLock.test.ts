import { renderHook, act } from '@testing-library/react-native';
import { useAppLock } from '../useAppLock';
import * as SecureStore from 'expo-secure-store';
import { AppState } from 'react-native';
import { useBiometrics } from '../useBiometrics';

// Mock dependencies
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

jest.mock('react-native', () => ({
  AppState: {
    currentState: 'active',
    addEventListener: jest.fn(),
  },
}));

jest.mock('../useBiometrics', () => ({
  useBiometrics: jest.fn(),
}));

describe('useAppLock', () => {
  const mockAuthenticate = jest.fn();
  const mockCheckAvailability = jest.fn();
  const mockRemove = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    (useBiometrics as jest.Mock).mockReturnValue({
      authenticate: mockAuthenticate,
      checkAvailability: mockCheckAvailability,
    });

    (AppState.addEventListener as jest.Mock).mockReturnValue({ remove: mockRemove });
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(null);
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useAppLock());

    expect(result.current.isLocked).toBe(false);
    expect(result.current.lockEnabled).toBe(true);
    expect(result.current.sessionTimeout).toBe(5);
  });

  it('should subscribe to AppState changes on mount', () => {
    renderHook(() => useAppLock());

    expect(AppState.addEventListener).toHaveBeenCalledWith('change', expect.any(Function));
  });

  it('should unlock with biometrics when available', async () => {
    mockCheckAvailability.mockResolvedValue(true);
    mockAuthenticate.mockResolvedValue(true);

    const { result } = renderHook(() => useAppLock());

    // First lock the app
    act(() => {
      result.current.lockImmediately();
    });

    expect(result.current.isLocked).toBe(true);

    const unlockResult = await act(async () => {
      return await result.current.unlock();
    });

    expect(unlockResult).toBe(true);
    expect(mockCheckAvailability).toHaveBeenCalled();
    expect(mockAuthenticate).toHaveBeenCalledWith('Unlock PayU');
  });

  it('should return false when biometric authentication fails', async () => {
    mockCheckAvailability.mockResolvedValue(true);
    mockAuthenticate.mockResolvedValue(false);

    const { result } = renderHook(() => useAppLock());

    act(() => {
      result.current.lockImmediately();
    });

    const unlockResult = await act(async () => {
      return await result.current.unlock();
    });

    expect(unlockResult).toBe(false);
    expect(result.current.isLocked).toBe(true);
  });

  it('should fallback to PIN when biometrics not available', async () => {
    mockCheckAvailability.mockResolvedValue(false);

    const { result } = renderHook(() => useAppLock());

    act(() => {
      result.current.lockImmediately();
    });

    const unlockResult = await act(async () => {
      return await result.current.unlock();
    });

    expect(unlockResult).toBe(true);
    expect(result.current.isLocked).toBe(false);
  });

  it('should toggle app lock', async () => {
    const { result } = renderHook(() => useAppLock());

    await act(async () => {
      await result.current.toggleAppLock(false);
    });

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('@payu:app_lock_enabled', 'false');
  });

  it('should update session timeout', async () => {
    const { result } = renderHook(() => useAppLock());

    await act(async () => {
      await result.current.setSessionTimeout(30);
    });

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('@payu:session_timeout', '30');
  });

  it('should lock immediately', () => {
    const { result } = renderHook(() => useAppLock());

    act(() => {
      result.current.lockImmediately();
    });

    expect(result.current.isLocked).toBe(true);
  });

  it('should handle toggle lock errors gracefully', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
    (SecureStore.setItemAsync as jest.Mock).mockRejectedValue(new Error('Storage error'));

    const { result } = renderHook(() => useAppLock());

    await act(async () => {
      await result.current.toggleAppLock(false);
    });

    expect(consoleSpy).toHaveBeenCalledWith('Failed to toggle app lock:', expect.any(Error));

    consoleSpy.mockRestore();
  });

  it('should handle set timeout errors gracefully', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
    (SecureStore.setItemAsync as jest.Mock).mockRejectedValue(new Error('Storage error'));

    const { result } = renderHook(() => useAppLock());

    await act(async () => {
      await result.current.setSessionTimeout(30);
    });

    expect(consoleSpy).toHaveBeenCalledWith('Failed to set session timeout:', expect.any(Error));

    consoleSpy.mockRestore();
  });

  it('should unsubscribe from AppState on unmount', () => {
    const { unmount } = renderHook(() => useAppLock());

    unmount();

    expect(mockRemove).toHaveBeenCalled();
  });
});

describe('useScreenshotPrevention', () => {
  it('should initialize without errors', () => {
    const { useScreenshotPrevention } = require('../useAppLock');
    const { result } = renderHook(() => useScreenshotPrevention());
    expect(result.current).toBeUndefined();
  });
});

describe('useSessionTimeout', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with session not expired', () => {
    const { useSessionTimeout } = require('../useAppLock');
    const { result } = renderHook(() => useSessionTimeout(30));

    expect(result.current.isSessionExpired).toBe(false);
  });

  it('should mark session as expired after timeout', () => {
    const { useSessionTimeout } = require('../useAppLock');
    const { result } = renderHook(() => useSessionTimeout(1)); // 1 minute timeout

    expect(result.current.isSessionExpired).toBe(false);

    // Fast forward past the timeout
    act(() => {
      jest.advanceTimersByTime(61 * 1000); // 61 seconds
    });

    expect(result.current.isSessionExpired).toBe(true);
  });

  it('should reset session when resetSession is called', () => {
    const { useSessionTimeout } = require('../useAppLock');
    const { result } = renderHook(() => useSessionTimeout(1));

    act(() => {
      jest.advanceTimersByTime(61 * 1000);
    });

    expect(result.current.isSessionExpired).toBe(true);

    act(() => {
      result.current.resetSession();
    });

    expect(result.current.isSessionExpired).toBe(false);
  });

  it('should handle different timeout values', () => {
    const { useSessionTimeout } = require('../useAppLock');
    const { result, rerender } = renderHook(
      ({ timeout }) => useSessionTimeout(timeout),
      { initialProps: { timeout: 5 } }
    );

    expect(result.current.isSessionExpired).toBe(false);

    rerender({ timeout: 10 });

    // Should still not be expired
    expect(result.current.isSessionExpired).toBe(false);
  });
});
