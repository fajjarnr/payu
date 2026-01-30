import { renderHook, act } from '@testing-library/react';
import { useAuthStore } from '@/stores';

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  test('should initialize with empty auth state', () => {
    const { result } = renderHook(() => useAuthStore());

    // Tokens are managed via httpOnly cookies - not stored in state
    expect(result.current.user).toBeNull();
    expect(result.current.accountId).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  test('should set auth state correctly', () => {
    const { result } = renderHook(() => useAuthStore());
    const mockUser = {
      id: '123',
      externalId: 'ext123',
      username: 'testuser',
      email: 'test@example.com',
      fullName: 'Test User',
      nik: '1234567890123456',
      kycStatus: 'PENDING' as const,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    };

    act(() => {
      // setAuth now only takes (user, accountId) - tokens are in httpOnly cookies
      result.current.setAuth(mockUser, 'acc123');
    });

    // Tokens are managed via httpOnly cookies - not stored in state
    expect(result.current.user).toEqual(mockUser);
    expect(result.current.accountId).toBe('acc123');
    expect(result.current.isAuthenticated).toBe(true);
  });

  test('should set user correctly', () => {
    const { result } = renderHook(() => useAuthStore());
    const mockUser = {
      id: '123',
      externalId: 'ext123',
      username: 'testuser',
      email: 'test@example.com',
      fullName: 'Test User',
      nik: '1234567890123456',
      kycStatus: 'PENDING' as const,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    };

    act(() => {
      result.current.setUser(mockUser);
    });

    expect(result.current.user).toEqual(mockUser);
  });

  test('should set authenticated state correctly', () => {
    const { result } = renderHook(() => useAuthStore());

    act(() => {
      result.current.setAuthenticated(true);
    });

    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.setAuthenticated(false);
    });

    expect(result.current.isAuthenticated).toBe(false);
  });

  test('should logout correctly', () => {
    const { result } = renderHook(() => useAuthStore());
    const mockUser = {
      id: '123',
      externalId: 'ext123',
      username: 'testuser',
      email: 'test@example.com',
      fullName: 'Test User',
      nik: '1234567890123456',
      kycStatus: 'PENDING' as const,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    };

    act(() => {
      // setAuth now only takes (user, accountId) - tokens are in httpOnly cookies
      result.current.setAuth(mockUser, 'acc123');
    });

    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.logout();
    });

    // Tokens are managed via httpOnly cookies - not stored in state
    expect(result.current.user).toBeNull();
    expect(result.current.accountId).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  test('clearAuth should call logout', () => {
    const { result } = renderHook(() => useAuthStore());
    const mockUser = {
      id: '123',
      externalId: 'ext123',
      username: 'testuser',
      email: 'test@example.com',
      fullName: 'Test User',
      nik: '1234567890123456',
      kycStatus: 'PENDING' as const,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    };

    act(() => {
      // setAuth now only takes (user, accountId) - tokens are in httpOnly cookies
      result.current.setAuth(mockUser, 'acc123');
    });

    act(() => {
      result.current.clearAuth();
    });

    expect(result.current.isAuthenticated).toBe(false);
  });
});
