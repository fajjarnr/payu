import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useLogin, useLogout, useRefreshToken, useAuth } from '@/hooks/useAuth';
import AuthService from '@/services/AuthService';
import { useAuthStore } from '@/stores';

// Mock AuthService
vi.mock('@/services/AuthService');

// Mock auth store
vi.mock('@/stores', () => ({
  useAuthStore: vi.fn()
}));

const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

// Mock window.location
const mockLocation = { href: '' };
Object.defineProperty(window, 'location', {
  writable: true,
  value: mockLocation
});

describe('useLogin hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockSetAuth = vi.fn();
  const mockUser = {
    id: 'user-123',
    externalId: 'ext-123',
    username: 'testuser',
    email: 'test@example.com',
    fullName: 'Test User',
    nik: '1234567890123456',
    kycStatus: 'PENDING' as const,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
    mockLocation.href = '';

    mockUseAuthStore.mockReturnValue({
      setAuth: mockSetAuth,
      logout: vi.fn(),
      setToken: vi.fn()
    });
  });

  it('should be defined', () => {
    expect(useLogin).toBeDefined();
  });

  it('should login successfully with valid credentials', async () => {
    const mockLoginResponse = {
      access_token: 'access-token-123',
      refresh_token: 'refresh-token-123',
      expires_in: 3600,
      token_type: 'Bearer'
    };

    vi.mocked(AuthService.login).mockResolvedValue(mockLoginResponse);

    const { result } = renderHook(() => useLogin(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({
        username: 'testuser',
        password: 'password123'
      });
    });

    expect(AuthService.login).toHaveBeenCalledWith({
      username: 'testuser',
      password: 'password123'
    });
    expect(mockSetAuth).toHaveBeenCalledWith(
      mockLoginResponse.access_token,
      mockLoginResponse.refresh_token,
      expect.any(Object),
      ''
    );
  });

  it('should invalidate auth queries on successful login', async () => {
    const mockLoginResponse = {
      access_token: 'access-token-123',
      refresh_token: 'refresh-token-123',
      expires_in: 3600,
      token_type: 'Bearer'
    };

    vi.mocked(AuthService.login).mockResolvedValue(mockLoginResponse);

    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useLogin(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({
        username: 'testuser',
        password: 'password123'
      });
    });

    expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['auth'] });
  });

  it('should handle login error', async () => {
    const mockError = new Error('Invalid credentials');
    vi.mocked(AuthService.login).mockRejectedValue(mockError);

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const { result } = renderHook(() => useLogin(), { wrapper });

    let error: Error | null = null;
    try {
      await act(async () => {
        await result.current.mutateAsync({
          username: 'wronguser',
          password: 'wrongpassword'
        });
      });
    } catch (e) {
      error = e as Error;
    }

    expect(error).toBeTruthy();
    expect(consoleErrorSpy).toHaveBeenCalledWith('Login failed:', mockError);

    consoleErrorSpy.mockRestore();
  });

  it('should have isLoading state during login mutation', async () => {
    vi.mocked(AuthService.login).mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              access_token: 'access-token-123',
              refresh_token: 'refresh-token-123',
              expires_in: 3600,
              token_type: 'Bearer'
            });
          }, 100);
        })
    );

    const { result } = renderHook(() => useLogin(), { wrapper });

    act(() => {
      result.current.mutate({
        username: 'testuser',
        password: 'password123'
      });
    });

    expect(result.current.isPending).toBe(true);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });
});

describe('useLogout hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockLogout = vi.fn();

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
    mockLocation.href = '';

    mockUseAuthStore.mockReturnValue({
      logout: mockLogout,
      setAuth: vi.fn(),
      setToken: vi.fn()
    });
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
    expect(queryClient.clear).toHaveBeenCalled();
  });

  it('should redirect to login page after logout', async () => {
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(mockLocation.href).toBe('/login');
  });

  it('should handle logout mutation states', async () => {
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(result.current.isSuccess).toBe(true);
  });
});

describe('useRefreshToken hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockSetToken = vi.fn();

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();

    mockUseAuthStore.mockReturnValue({
      setToken: mockSetToken,
      logout: vi.fn(),
      setAuth: vi.fn()
    });
  });

  it('should be defined', () => {
    expect(useRefreshToken).toBeDefined();
  });

  it('should refresh token successfully', async () => {
    const newToken = 'new-access-token-456';
    vi.mocked(AuthService.refreshToken).mockResolvedValue(newToken);

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(AuthService.refreshToken).toHaveBeenCalled();
    expect(mockSetToken).toHaveBeenCalledWith(newToken);
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

  it('should update token in store on successful refresh', async () => {
    const newToken = 'updated-access-token-789';
    vi.mocked(AuthService.refreshToken).mockResolvedValue(newToken);

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(mockSetToken).toHaveBeenCalledTimes(1);
    expect(mockSetToken).toHaveBeenCalledWith(newToken);
  });
});

describe('useAuth hook', () => {
  it('should be defined', () => {
    expect(useAuth).toBeDefined();
  });

  it('should return auth store state', () => {
    const mockAuthState = {
      token: 'test-token',
      refreshToken: 'test-refresh-token',
      user: {
        id: 'user-123',
        externalId: 'ext-123',
        username: 'testuser',
        email: 'test@example.com',
        fullName: 'Test User',
        nik: '1234567890123456',
        kycStatus: 'PENDING' as const,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      },
      accountId: 'account-123',
      isAuthenticated: true,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setToken: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn()
    };

    mockUseAuthStore.mockReturnValue(mockAuthState);

    const { result } = renderHook(() => useAuth());

    expect(result.current).toEqual(mockAuthState);
  });

  it('should update when auth store changes', () => {
    const initialState = {
      token: null,
      refreshToken: null,
      user: null,
      accountId: null,
      isAuthenticated: false,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setToken: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn()
    };

    mockUseAuthStore.mockReturnValue(initialState);

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAuthenticated).toBe(false);

    const loggedInState = {
      ...initialState,
      token: 'new-token',
      isAuthenticated: true
    };

    mockUseAuthStore.mockReturnValue(loggedInState);

    // Rerender to get updated state
    const { result: newResult } = renderHook(() => useAuth());

    expect(newResult.current.isAuthenticated).toBe(true);
    expect(newResult.current.token).toBe('new-token');
  });
});

describe('useAuth integration', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
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

    mockUseAuthStore.mockReturnValue({
      setAuth: vi.fn(),
      logout: vi.fn(),
      setToken: vi.fn()
    });
  });

  it('should handle complete auth flow: login -> logout', async () => {
    const mockLoginResponse = {
      access_token: 'access-token-123',
      refresh_token: 'refresh-token-123',
      expires_in: 3600,
      token_type: 'Bearer'
    };

    vi.mocked(AuthService.login).mockResolvedValue(mockLoginResponse);

    // Login
    const { result: loginResult } = renderHook(() => useLogin(), { wrapper });

    await act(async () => {
      await loginResult.current.mutateAsync({
        username: 'testuser',
        password: 'password123'
      });
    });

    expect(loginResult.current.isSuccess).toBe(true);

    // Logout
    const { result: logoutResult } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await logoutResult.current.mutateAsync();
    });

    expect(logoutResult.current.isSuccess).toBe(true);
    expect(mockLocation.href).toBe('/login');
  });

  it('should handle token refresh during authenticated session', async () => {
    const newToken = 'refreshed-token-456';
    vi.mocked(AuthService.refreshToken).mockResolvedValue(newToken);

    const { result } = renderHook(() => useRefreshToken(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(result.current.isSuccess).toBe(true);
  });
});
