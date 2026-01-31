import { act, renderHook } from '@testing-library/react-native';
import { useAuthStore } from '../authStore';
import { authService } from '@/services/auth.service';
import { storage } from '@/utils/storage';
import { AUTH_CONFIG } from '@/constants/config';
import { User, AuthTokens } from '@/types';

// Get the mocked storage functions
const mockStorage = storage as jest.Mocked<typeof storage>;

// Mock dependencies
jest.mock('@/services/auth.service');
jest.mock('@/utils/storage');

const mockUser: User = {
  id: 'user-123',
  email: 'test@example.com',
  phoneNumber: '+6281234567890',
  fullName: 'Test User',
  kycVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
};

const mockTokens: AuthTokens = {
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-456',
  expiresIn: 3600,
};

describe('authStore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset store state
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = useAuthStore.getState();

      expect(state.user).toBeNull();
      expect(state.isAuthenticated).toBe(false);
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
    });

    it('should not have tokens in state (SECURITY: tokens only in SecureStore)', () => {
      const state = useAuthStore.getState();

      // SECURITY: Tokens should never be in Zustand state
      expect('tokens' in state).toBe(false);
    });
  });

  describe('login', () => {
    it('should set loading state when login starts', async () => {
      (authService.login as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.login('test@example.com', 'password123');
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should update state on successful login', async () => {
      (authService.login as jest.Mock).mockResolvedValue({
        user: mockUser,
        tokens: mockTokens,
      });
      (mockStorage.set as jest.Mock).mockResolvedValue(true);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.login('test@example.com', 'password123');
      });

      expect(authService.login).toHaveBeenCalledWith({
        identifier: 'test@example.com',
        password: 'password123', // pragma: allowlist secret
      });
      expect(mockStorage.set).toHaveBeenCalledWith(AUTH_CONFIG.TOKEN_KEY, mockTokens);
      expect(mockStorage.set).toHaveBeenCalledWith(AUTH_CONFIG.USER_KEY, mockUser);
      expect(result.current.user).toEqual(mockUser);
      // SECURITY: Tokens are not in state, only in SecureStore
      expect('tokens' in result.current).toBe(false);
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle login error', async () => {
      const errorMessage = 'Invalid credentials';
      (authService.login as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.login('test@example.com', 'wrongpassword');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
    });

    it('should handle generic error message when response is undefined', async () => {
      (authService.login as jest.Mock).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.login('test@example.com', 'password123');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Login failed');
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('register', () => {
    const registerData = {
      email: 'new@example.com',
      phoneNumber: '+6281234567890',
      fullName: 'New User',
      password: 'password123',
    };

    it('should set loading state when registration starts', () => {
      (authService.register as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.register(registerData);
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should update state on successful registration', async () => {
      (authService.register as jest.Mock).mockResolvedValue({
        user: mockUser,
        tokens: mockTokens,
      });
      (mockStorage.set as jest.Mock).mockResolvedValue(true);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.register(registerData);
      });

      expect(authService.register).toHaveBeenCalledWith({
        ...registerData,
        confirmPassword: registerData.password,
      });
      expect(mockStorage.set).toHaveBeenCalledWith(AUTH_CONFIG.TOKEN_KEY, mockTokens);
      expect(mockStorage.set).toHaveBeenCalledWith(AUTH_CONFIG.USER_KEY, mockUser);
      expect(result.current.user).toEqual(mockUser);
      // SECURITY: Tokens are not in state, only in SecureStore
      expect('tokens' in result.current).toBe(false);
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.isLoading).toBe(false);
    });

    it('should use provided confirmPassword if available', async () => {
      (authService.register as jest.Mock).mockResolvedValue({
        user: mockUser,
        tokens: mockTokens,
      });
      (mockStorage.set as jest.Mock).mockResolvedValue(true);

      const { result } = renderHook(() => useAuthStore());

      const dataWithConfirm = {
        ...registerData,
        confirmPassword: 'confirm123', // pragma: allowlist secret
      };

      await act(async () => {
        await result.current.register(dataWithConfirm);
      });

      expect(authService.register).toHaveBeenCalledWith(dataWithConfirm);
    });

    it('should handle registration error', async () => {
      const errorMessage = 'Email already exists';
      (authService.register as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.register(registerData);
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('logout', () => {
    it('should clear state and storage on logout', async () => {
      (authService.logout as jest.Mock).mockResolvedValue(undefined);
      (mockStorage.remove as jest.Mock).mockResolvedValue(true);

      // Set initial authenticated state
      useAuthStore.setState({
        user: mockUser,
        isAuthenticated: true,
      });

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.logout();
      });

      expect(authService.logout).toHaveBeenCalled();
      expect(mockStorage.remove).toHaveBeenCalledWith(AUTH_CONFIG.TOKEN_KEY);
      expect(mockStorage.remove).toHaveBeenCalledWith(AUTH_CONFIG.USER_KEY);
      expect(result.current.user).toBeNull();
      // SECURITY: Tokens are not in state, only in SecureStore
      expect('tokens' in result.current).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('should clear state even when logout API fails', async () => {
      (authService.logout as jest.Mock).mockRejectedValue(new Error('Network error'));
      (mockStorage.remove as jest.Mock).mockResolvedValue(true);

      // Set initial authenticated state
      useAuthStore.setState({
        user: mockUser,
        isAuthenticated: true,
      });

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.logout();
      });

      expect(result.current.user).toBeNull();
      // SECURITY: Tokens are not in state, only in SecureStore
      expect('tokens' in result.current).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('refreshToken', () => {
    it('should update tokens in SecureStore on successful refresh', async () => {
      const newTokens: AuthTokens = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600,
      };

      (authService.refreshToken as jest.Mock).mockResolvedValue({
        user: mockUser,
        tokens: newTokens,
      });
      (mockStorage.set as jest.Mock).mockResolvedValue(true);
      (mockStorage.get as jest.Mock).mockResolvedValue(mockTokens);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.refreshToken();
      });

      expect(authService.refreshToken).toHaveBeenCalledWith(mockTokens.refreshToken);
      expect(mockStorage.set).toHaveBeenCalledWith(AUTH_CONFIG.TOKEN_KEY, newTokens);
      // SECURITY: Tokens are not in state, only in SecureStore
      expect('tokens' in result.current).toBe(false);
    });

    it('should throw error when no refresh token exists', async () => {
      (mockStorage.get as jest.Mock).mockResolvedValue(null);

      const { result } = renderHook(() => useAuthStore());

      await expect(
        act(async () => {
          await result.current.refreshToken();
        })
      ).rejects.toThrow('No refresh token available');
    });

    it('should logout when refresh fails', async () => {
      (authService.refreshToken as jest.Mock).mockRejectedValue(new Error('Invalid token'));
      (authService.logout as jest.Mock).mockResolvedValue(undefined);
      (mockStorage.remove as jest.Mock).mockResolvedValue(true);
      (mockStorage.get as jest.Mock).mockResolvedValue(mockTokens);

      useAuthStore.setState({
        user: mockUser,
        isAuthenticated: true,
      });

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.refreshToken();
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.user).toBeNull();
      // SECURITY: Tokens are not in state, only in SecureStore
      expect('tokens' in result.current).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('getTokens', () => {
    it('should return tokens from SecureStore', async () => {
      (mockStorage.get as jest.Mock).mockResolvedValue(mockTokens);

      const { result } = renderHook(() => useAuthStore());

      let tokens: AuthTokens | null = null;
      await act(async () => {
        tokens = await result.current.getTokens();
      });

      expect(tokens).toEqual(mockTokens);
      expect(mockStorage.get).toHaveBeenCalledWith(AUTH_CONFIG.TOKEN_KEY);
    });

    it('should return null when no tokens exist', async () => {
      (mockStorage.get as jest.Mock).mockResolvedValue(null);

      const { result } = renderHook(() => useAuthStore());

      let tokens: AuthTokens | null = null;
      await act(async () => {
        tokens = await result.current.getTokens();
      });

      expect(tokens).toBeNull();
    });
  });

  describe('checkAuthStatus', () => {
    it('should return true when tokens and user exist', async () => {
      (mockStorage.get as jest.Mock)
        .mockResolvedValueOnce(mockTokens)  // First call for tokens
        .mockResolvedValueOnce(mockUser);   // Second call for user

      const { result } = renderHook(() => useAuthStore());

      let isAuthenticated = false;
      await act(async () => {
        isAuthenticated = await result.current.checkAuthStatus();
      });

      expect(isAuthenticated).toBe(true);
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).toEqual(mockUser);
    });

    it('should return false when tokens do not exist', async () => {
      (mockStorage.get as jest.Mock)
        .mockResolvedValueOnce(null)        // First call for tokens
        .mockResolvedValueOnce(mockUser);   // Second call for user

      const { result } = renderHook(() => useAuthStore());

      let isAuthenticated = true;
      await act(async () => {
        isAuthenticated = await result.current.checkAuthStatus();
      });

      expect(isAuthenticated).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('should return false when user does not exist', async () => {
      (mockStorage.get as jest.Mock)
        .mockResolvedValueOnce(mockTokens)  // First call for tokens
        .mockResolvedValueOnce(null);       // Second call for user

      const { result } = renderHook(() => useAuthStore());

      let isAuthenticated = true;
      await act(async () => {
        isAuthenticated = await result.current.checkAuthStatus();
      });

      expect(isAuthenticated).toBe(false);
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('clearError', () => {
    it('should clear error state', () => {
      useAuthStore.setState({ error: 'Some error' });

      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('updateUser', () => {
    it('should update user state', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.updateUser(mockUser);
      });

      expect(result.current.user).toEqual(mockUser);
    });

    it('should update existing user data', () => {
      useAuthStore.setState({ user: mockUser });

      const { result } = renderHook(() => useAuthStore());

      const updatedUser = { ...mockUser, fullName: 'Updated Name' };

      act(() => {
        result.current.updateUser(updatedUser);
      });

      expect(result.current.user?.fullName).toBe('Updated Name');
    });
  });
});
