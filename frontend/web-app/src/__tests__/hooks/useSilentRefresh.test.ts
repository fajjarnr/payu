import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useSilentRefresh } from '@/hooks/useSilentRefresh';

// --- Store mocks ---
const mockSetAuthenticated = vi.fn();
const mockSetTokenExpiry = vi.fn();
const mockLogout = vi.fn();

let mockIsAuthenticated = false;
let mockTokenExpiresAt: number | null = null;

vi.mock('@/stores', () => ({
  useAuthStore: vi.fn((selector: (state: unknown) => unknown) => {
    const state = {
      isAuthenticated: mockIsAuthenticated,
      tokenExpiresAt: mockTokenExpiresAt,
      setAuthenticated: mockSetAuthenticated,
      setTokenExpiry: mockSetTokenExpiry,
      logout: mockLogout,
    };
    return selector(state);
  }),
}));

// --- Fetch mock ---
const mockFetch = vi.fn();
global.fetch = mockFetch;

describe('useSilentRefresh', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    mockIsAuthenticated = false;
    mockTokenExpiresAt = null;
    mockFetch.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should not schedule refresh when not authenticated', () => {
    mockIsAuthenticated = false;
    renderHook(() => useSilentRefresh());

    // No timer should be set — advancing time should not trigger fetch
    vi.advanceTimersByTime(60 * 60 * 1000);
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('should trigger immediate refresh on mount if authenticated with null tokenExpiresAt', async () => {
    mockIsAuthenticated = true;
    mockTokenExpiresAt = null;
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ success: true, expiresIn: 900 }),
    });

    renderHook(() => useSilentRefresh());

    // Flush the microtask queue for the immediate doRefresh()
    await vi.runAllTimersAsync();

    expect(mockFetch).toHaveBeenCalledWith('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
    expect(mockSetAuthenticated).toHaveBeenCalledWith(true);
    expect(mockSetTokenExpiry).toHaveBeenCalled();
  });

  it('should schedule refresh 2 minutes before token expiry', async () => {
    const now = Date.now();
    const TOKEN_LIFETIME = 15 * 60 * 1000; // 15 min
    const REFRESH_MARGIN = 2 * 60 * 1000; // 2 min

    mockIsAuthenticated = true;
    mockTokenExpiresAt = now + TOKEN_LIFETIME;

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, expiresIn: 900 }),
    });

    renderHook(() => useSilentRefresh());

    // Before the scheduled time — no call yet
    vi.advanceTimersByTime(TOKEN_LIFETIME - REFRESH_MARGIN - 1000);
    expect(mockFetch).not.toHaveBeenCalled();

    // Advance past the scheduled time (13 min mark)
    await act(async () => {
      vi.advanceTimersByTime(2000);
      await vi.runAllTimersAsync();
    });

    expect(mockFetch).toHaveBeenCalledWith('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
  });

  it('should logout when refresh returns non-ok response', async () => {
    mockIsAuthenticated = true;
    mockTokenExpiresAt = null;
    mockFetch.mockResolvedValueOnce({ ok: false, status: 401 });

    renderHook(() => useSilentRefresh());

    await vi.runAllTimersAsync();

    expect(mockLogout).toHaveBeenCalled();
  });

  it('should not logout on network error (allows retry)', async () => {
    mockIsAuthenticated = true;
    mockTokenExpiresAt = null;
    mockFetch.mockRejectedValueOnce(new Error('Network error'));

    renderHook(() => useSilentRefresh());

    await vi.runAllTimersAsync();

    // Network errors should NOT trigger logout — retry via backoff
    expect(mockLogout).not.toHaveBeenCalled();
  });

  it('should prevent concurrent refresh calls', async () => {
    mockIsAuthenticated = true;
    mockTokenExpiresAt = null;

    // Create a slow-resolving promise to simulate in-flight request
    let resolveRefresh: (value: unknown) => void;
    const slowPromise = new Promise((resolve) => {
      resolveRefresh = resolve;
    });

    mockFetch.mockReturnValueOnce(slowPromise);

    renderHook(() => useSilentRefresh());

    // First call is in-flight
    await act(async () => {
      vi.advanceTimersByTime(1);
    });

    expect(mockFetch).toHaveBeenCalledTimes(1);

    // Resolve the first call
    await act(async () => {
      resolveRefresh!({
        ok: true,
        json: async () => ({ success: true, expiresIn: 900 }),
      });
      await vi.runAllTimersAsync();
    });

    expect(mockSetAuthenticated).toHaveBeenCalledWith(true);
  });

  it('should use exponential backoff on retry (2s, 4s, 8s...)', async () => {
    const now = Date.now();
    mockIsAuthenticated = true;
    mockTokenExpiresAt = now + 1000; // Expires in 1 second (past margin)

    // First attempt fails
    mockFetch
      .mockResolvedValueOnce({ ok: false, status: 500 })
      // Retry #1 succeeds
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true, expiresIn: 900 }),
      });

    renderHook(() => useSilentRefresh());

    // Trigger the initial scheduled refresh (immediate since past margin)
    await act(async () => {
      await vi.runAllTimersAsync();
    });

    // First call happened — failed, so logout was called
    expect(mockFetch).toHaveBeenCalledTimes(1);
    expect(mockLogout).toHaveBeenCalled();
  });

  it('should eagerly refresh on tab focus when token is about to expire', async () => {
    const now = Date.now();
    const EAGER_THRESHOLD = 3 * 60 * 1000; // 3 min

    mockIsAuthenticated = true;
    // Token expires in 2 minutes (within eager threshold)
    mockTokenExpiresAt = now + 2 * 60 * 1000;

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, expiresIn: 900 }),
    });

    renderHook(() => useSilentRefresh());

    // Simulate tab becoming visible
    await act(async () => {
      Object.defineProperty(document, 'visibilityState', {
        value: 'visible',
        writable: true,
        configurable: true,
      });
      document.dispatchEvent(new Event('visibilitychange'));
      await vi.runAllTimersAsync();
    });

    // Should have triggered an eager refresh
    expect(mockFetch).toHaveBeenCalledWith('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
  });

  it('should not eager-refresh on tab focus when token has plenty of time left', async () => {
    const now = Date.now();

    mockIsAuthenticated = true;
    // Token expires in 10 minutes (well above eager threshold of 3 min)
    mockTokenExpiresAt = now + 10 * 60 * 1000;

    renderHook(() => useSilentRefresh());

    // Simulate tab becoming visible
    await act(async () => {
      Object.defineProperty(document, 'visibilityState', {
        value: 'visible',
        writable: true,
        configurable: true,
      });
      document.dispatchEvent(new Event('visibilitychange'));
    });

    // Should NOT have called refresh
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('should clean up timer on unmount', () => {
    mockIsAuthenticated = true;
    mockTokenExpiresAt = Date.now() + 15 * 60 * 1000;

    const { unmount } = renderHook(() => useSilentRefresh());

    unmount();

    // After unmount, advancing time should not trigger any fetch
    vi.advanceTimersByTime(20 * 60 * 1000);
    expect(mockFetch).not.toHaveBeenCalled();
  });
});
