import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useLogout, useRefreshToken, useAuth } from '@/hooks/useAuth';
import AuthService from '@/services/AuthService';
import { NextIntlClientProvider } from 'next-intl';
import messages from '../../../messages/id.json';

// Mock AuthService
vi.mock('@/services/AuthService');

// Mock the auth store module
const mockSetAuth = vi.fn();
const mockLogout = vi.fn();
const mockSetAuthenticated = vi.fn();
const mockSetUser = vi.fn();
const mockClearAuth = vi.fn();
const mockSetTokenExpiry = vi.fn();

vi.mock('@/stores/authStore', () => ({
  useAuthStore: vi.fn((selector: (state: unknown) => unknown) => {
    const state = {
      user: null,
      accountId: null,
      isAuthenticated: false,
      tokenExpiresAt: null,
      setAuth: mockSetAuth,
      setUser: mockSetUser,
      setAuthenticated: mockSetAuthenticated,
      setTokenExpiry: mockSetTokenExpiry,
      logout: mockLogout,
      clearAuth: mockClearAuth
    };
    return selector ? selector(state) : state;
  })
}));

// Mock window.location
const mockLocation = { href: '' };
Object.defineProperty(window, 'location', {
  writable: true,
  value: mockLocation
});

describe('useLogout hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale="id" messages={messages}>{children}</NextIntlClientProvider>
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
    mockLocation.href = '';
  });

  it('should be defined', () => {
    expect(useLogout).toBeDefined();
  });

  it('should logout successfully and clear queries', async () => {
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(mockLogout).toHaveBeenCalled();
  });

  it('should redirect to login page after logout', async () => {
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(mockLocation.href).toBe('/id/login');
  });

  it('should handle logout mutation states', async () => {
    const { result } = renderHook(() => useLogout(), { wrapper });

    // Execute logout and wait for completion
    await result.current.mutateAsync();

    // Verify logout was called
    expect(mockLogout).toHaveBeenCalled();
  });
});

describe('useRefreshToken hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale="id" messages={messages}>{children}</NextIntlClientProvider>
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useRefreshToken).toBeDefined();
  });

  it('should refresh token successfully', async () => {
    vi.mocked(AuthService.refreshToken).mockResolvedValue({ expiresIn: 900 });

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(AuthService.refreshToken).toHaveBeenCalled();
    // setAuthenticated is called with true after successful refresh
    expect(mockSetAuthenticated).toHaveBeenCalledWith(true);
  });

  it('should handle refresh token error', async () => {
    const mockError = new Error('Refresh token expired');
    vi.mocked(AuthService.refreshToken).mockRejectedValue(mockError);

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    let error: Error | null = null;
    try {
      await act(async () => {
        await result.current.mutateAsync();
      });
    } catch (e) {
      error = e as Error;
    }

    expect(error).toBeTruthy();
    expect(error?.message).toContain('Refresh token expired');
  });

  it('should update authenticated state in store on successful refresh', async () => {
    vi.mocked(AuthService.refreshToken).mockResolvedValue({ expiresIn: 900 });

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(mockSetAuthenticated).toHaveBeenCalledTimes(1);
    expect(mockSetAuthenticated).toHaveBeenCalledWith(true);
  });
});

describe('useAuth hook', () => {
  it('should be defined', () => {
    expect(useAuth).toBeDefined();
  });
});

describe('useAuth integration', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale="id" messages={messages}>{children}</NextIntlClientProvider>
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
    mockLocation.href = '';
  });

  it('should handle logout', async () => {
    // Logout
    const { result: logoutResult } = renderHook(() => useLogout(), { wrapper });

    await logoutResult.current.mutateAsync();

    // Verify logout was called and redirect happened
    expect(mockLogout).toHaveBeenCalled();
    expect(mockLocation.href).toBe('/id/login');
  });

  it('should handle token refresh during authenticated session', async () => {
    vi.mocked(AuthService.refreshToken).mockResolvedValue({ expiresIn: 900 });

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    await result.current.mutateAsync();

    // Verify setAuthenticated was called after refresh
    expect(mockSetAuthenticated).toHaveBeenCalledWith(true);
  });
});
