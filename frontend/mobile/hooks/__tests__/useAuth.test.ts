import { renderHook, waitFor, act } from '@testing-library/react-native';
import { useAuth } from '../useAuth';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'expo-router';

// Mock dependencies
jest.mock('expo-router', () => ({
  useRouter: jest.fn(),
}));

jest.mock('@/store/authStore', () => ({
  useAuthStore: jest.fn(),
}));

jest.mock('@/utils/storage', () => ({
  storage: {
    get: jest.fn(),
    set: jest.fn(),
    remove: jest.fn(),
  },
}));

describe('useAuth', () => {
  const mockReplace = jest.fn();
  const mockLogin = jest.fn();
  const mockRegister = jest.fn();
  const mockLogout = jest.fn();
  const mockRefreshToken = jest.fn();
  const mockClearError = jest.fn();
  const mockUpdateUser = jest.fn();

  const mockUser = {
    id: 'user-123',
    email: 'test@example.com',
    phoneNumber: '+6281234567890',
    fullName: 'Test User',
    kycVerified: true,
    createdAt: '2024-01-01T00:00:00Z',
  };

  const mockTokens = {
    accessToken: 'access-token-123',
    refreshToken: 'refresh-token-456',
    expiresIn: Date.now() + 3600000, // 1 hour from now
  };

  beforeEach(() => {
    jest.clearAllMocks();

    (useRouter as jest.Mock).mockReturnValue({
      replace: mockReplace,
    });
  });

  it('should return auth state from store', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: mockUser,
      tokens: mockTokens,
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toEqual(mockUser);
    expect(result.current.tokens).toEqual(mockTokens);
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('should return loading state', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: true,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('should return error state', () => {
    const errorMessage = 'Invalid credentials';
    (useAuthStore as jest.Mock).mockReturnValue({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: errorMessage,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.error).toBe(errorMessage);
  });

  it('should call login from store', async () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.login('test@example.com', 'password123');
    });

    expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
  });

  it('should call register from store', async () => {
    const registerData = {
      email: 'new@example.com',
      phoneNumber: '+6281234567890',
      fullName: 'New User',
      password: 'password123',
      confirmPassword: 'password123',
    };

    (useAuthStore as jest.Mock).mockReturnValue({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register(registerData);
    });

    expect(mockRegister).toHaveBeenCalledWith(registerData);
  });

  it('should call logout and redirect to login', async () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: mockUser,
      tokens: mockTokens,
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.logout();
    });

    expect(mockLogout).toHaveBeenCalled();
    expect(mockReplace).toHaveBeenCalledWith('/(auth)/login');
  });

  it('should call clearError from store', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: 'Some error',
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    act(() => {
      result.current.clearError();
    });

    expect(mockClearError).toHaveBeenCalled();
  });

  it('should call updateUser from store', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: mockUser,
      tokens: mockTokens,
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());
    const updatedUser = { ...mockUser, fullName: 'Updated Name' };

    act(() => {
      result.current.updateUser(updatedUser);
    });

    expect(mockUpdateUser).toHaveBeenCalledWith(updatedUser);
  });

  it('should call refreshToken from store', async () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: mockUser,
      tokens: mockTokens,
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.refreshToken();
    });

    expect(mockRefreshToken).toHaveBeenCalled();
  });

  it('should not refresh token when not authenticated', () => {
    (useAuthStore as jest.Mock).mockReturnValue({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    renderHook(() => useAuth());

    expect(mockRefreshToken).not.toHaveBeenCalled();
  });

  it('should not refresh token when token has plenty of time left', () => {
    const freshTokens = {
      accessToken: 'access-token-123',
      refreshToken: 'refresh-token-456',
      expiresIn: Date.now() + 3600000, // 1 hour from now
    };

    (useAuthStore as jest.Mock).mockReturnValue({
      user: mockUser,
      tokens: freshTokens,
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: mockLogin,
      register: mockRegister,
      logout: mockLogout,
      refreshToken: mockRefreshToken,
      clearError: mockClearError,
      updateUser: mockUpdateUser,
    });

    renderHook(() => useAuth());

    expect(mockRefreshToken).not.toHaveBeenCalled();
  });
});
