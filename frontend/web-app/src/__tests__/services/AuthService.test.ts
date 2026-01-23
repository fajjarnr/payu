import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AuthService, type LoginRequest, type LoginResponse } from '@/services/AuthService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('AuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be a singleton', () => {
    const instance1 = AuthService.getInstance();
    const instance2 = AuthService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('login', () => {
    it('should successfully login and store tokens', async () => {
      const mockResponse: LoginResponse = {
        access_token: 'mock_access_token',
        refresh_token: 'mock_refresh_token',
        expires_in: 3600,
        token_type: 'Bearer',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const credentials: LoginRequest = {
        username: 'testuser',
        password: 'password123',
      };

      const result = await AuthService.getInstance().login(credentials);

      expect(api.post).toHaveBeenCalledWith('/auth/login', credentials);
      expect(localStorage.getItem('token')).toBe('mock_access_token');
      expect(localStorage.getItem('refreshToken')).toBe('mock_refresh_token');
      expect(result).toEqual(mockResponse);
    });

    it('should store access token when provided', async () => {
      const mockResponse: LoginResponse = {
        access_token: 'access_token_only',
        refresh_token: '',
        expires_in: 3600,
        token_type: 'Bearer',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      await AuthService.getInstance().login({ username: 'test', password: 'pass' });

      expect(localStorage.getItem('token')).toBe('access_token_only');
    });

    it('should store refresh token when provided', async () => {
      const mockResponse: LoginResponse = {
        access_token: '',
        refresh_token: 'refresh_token_only',
        expires_in: 3600,
        token_type: 'Bearer',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      await AuthService.getInstance().login({ username: 'test', password: 'pass' });

      expect(localStorage.getItem('refreshToken')).toBe('refresh_token_only');
    });
  });

  describe('logout', () => {
    it('should clear all auth data from localStorage', () => {
      localStorage.setItem('token', 'some_token');
      localStorage.setItem('refreshToken', 'some_refresh_token');
      localStorage.setItem('user', JSON.stringify({ id: '1' }));
      localStorage.setItem('accountId', 'account_123');

      AuthService.getInstance().logout();

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(localStorage.getItem('user')).toBeNull();
      expect(localStorage.getItem('accountId')).toBeNull();
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when token exists', () => {
      localStorage.setItem('token', 'valid_token');
      expect(AuthService.getInstance().isAuthenticated()).toBe(true);
    });

    it('should return false when token does not exist', () => {
      expect(AuthService.getInstance().isAuthenticated()).toBe(false);
    });
  });

  describe('getAccessToken', () => {
    it('should return the access token when it exists', () => {
      localStorage.setItem('token', 'my_access_token');
      expect(AuthService.getInstance().getAccessToken()).toBe('my_access_token');
    });

    it('should return null when access token does not exist', () => {
      expect(AuthService.getInstance().getAccessToken()).toBeNull();
    });
  });

  describe('getRefreshToken', () => {
    it('should return the refresh token when it exists', () => {
      localStorage.setItem('refreshToken', 'my_refresh_token');
      expect(AuthService.getInstance().getRefreshToken()).toBe('my_refresh_token');
    });

    it('should return null when refresh token does not exist', () => {
      expect(AuthService.getInstance().getRefreshToken()).toBeNull();
    });
  });

  describe('refreshToken', () => {
    it('should successfully refresh the token', async () => {
      const mockResponse: LoginResponse = {
        access_token: 'new_access_token',
        refresh_token: 'new_refresh_token',
        expires_in: 3600,
        token_type: 'Bearer',
      };

      localStorage.setItem('refreshToken', 'old_refresh_token');
      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const newToken = await AuthService.getInstance().refreshToken();

      expect(api.post).toHaveBeenCalledWith('/auth/refresh', {
        refresh_token: 'old_refresh_token',
      });
      expect(localStorage.getItem('token')).toBe('new_access_token');
      expect(localStorage.getItem('refreshToken')).toBe('new_refresh_token');
      expect(newToken).toBe('new_access_token');
    });

    it('should throw error when no refresh token available', async () => {
      await expect(AuthService.getInstance().refreshToken()).rejects.toThrow(
        'No refresh token available'
      );
    });
  });
});
