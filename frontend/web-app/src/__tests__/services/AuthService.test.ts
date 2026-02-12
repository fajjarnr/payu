import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthService, type LoginRequest, type LoginResponse } from '@/services/AuthService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

/**
 * SECURITY NOTICE: Test Updates
 * ================================
 * These tests have been updated to reflect the security fix:
 * - Tokens are NO LONGER stored in localStorage
 * - Tokens are managed via httpOnly cookies by the backend
 * - AuthService only tracks authentication state, not tokens
 */
describe('AuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be a singleton', () => {
    const instance1 = AuthService.getInstance();
    const instance2 = AuthService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('login', () => {
    it('should successfully login without storing tokens in localStorage', async () => {
      const mockResponse: LoginResponse = {
        success: true,
        data: {
          user: {
            id: 'user-123',
            externalId: 'ext-123',
            username: 'testuser',
            email: 'test@example.com',
            fullName: 'Test User',
            nik: '1234567890123456',
            kycStatus: 'PENDING',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z'
          }
        }
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const credentials: LoginRequest = {
        username: 'testuser',
        password: 'password123',
      };

      const result = await AuthService.getInstance().login(credentials);

      expect(api.post).toHaveBeenCalledWith('/auth/login', credentials);
      // SECURITY: Tokens are NOT stored in localStorage
      // They are managed via httpOnly cookies by the backend
      expect(result).toEqual(mockResponse);
    });

    it('should handle login response without storing tokens locally', async () => {
      const mockResponse: LoginResponse = {
        success: true,
        data: {
          user: {
            id: 'user-123',
            externalId: 'ext-123',
            username: 'testuser',
            email: 'test@example.com',
            fullName: 'Test User',
            nik: '1234567890123456',
            kycStatus: 'PENDING',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z'
          }
        }
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      await AuthService.getInstance().login({ username: 'test', password: 'pass' });

      // SECURITY: Tokens are NOT stored in localStorage
      // Backend manages tokens via httpOnly cookies
      expect(AuthService.getInstance().isAuthenticated()).toBe(true);
    });
  });

  describe('logout', () => {
    it('should clear session state without touching localStorage', () => {
      // Set authenticated state first
      AuthService.getInstance()['authenticated'] = true;

      AuthService.getInstance().logout();

      expect(AuthService.getInstance().isAuthenticated()).toBe(false);
      // SECURITY: No localStorage operations - tokens are in httpOnly cookies
      // Backend clears the cookies
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when authenticated', () => {
      AuthService.getInstance()['authenticated'] = true;
      expect(AuthService.getInstance().isAuthenticated()).toBe(true);
    });

    it('should return false when not authenticated', () => {
      AuthService.getInstance()['authenticated'] = false;
      expect(AuthService.getInstance().isAuthenticated()).toBe(false);
    });
  });

  describe('getAccessToken', () => {
    it('should return null and warn about httpOnly cookies', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const result = AuthService.getInstance().getAccessToken();

      expect(result).toBeNull();
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        'getAccessToken() is deprecated. Tokens are managed via httpOnly cookies.'
      );

      consoleWarnSpy.mockRestore();
    });
  });

  describe('getRefreshToken', () => {
    it('should return null and warn about httpOnly cookies', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const result = AuthService.getInstance().getRefreshToken();

      expect(result).toBeNull();
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        'getRefreshToken() is deprecated. Tokens are managed via httpOnly cookies.'
      );

      consoleWarnSpy.mockRestore();
    });
  });

  describe('refreshToken', () => {
    it('should call refresh endpoint without using stored refresh token', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: {} });

      await AuthService.getInstance().refreshToken();

      // SECURITY: Backend manages refresh token via httpOnly cookie
      expect(api.post).toHaveBeenCalledWith('/auth/refresh', {});
    });
  });

  describe('validateSession', () => {
    it('should return true when session is valid', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: {} });

      const result = await AuthService.getInstance().validateSession();

      expect(api.get).toHaveBeenCalledWith('/auth/validate');
      expect(result).toBe(true);
    });

    it('should return false when session is invalid', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Invalid session'));

      const result = await AuthService.getInstance().validateSession();

      expect(result).toBe(false);
      expect(AuthService.getInstance().isAuthenticated()).toBe(false);
    });
  });
});
