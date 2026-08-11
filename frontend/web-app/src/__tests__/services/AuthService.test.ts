import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthService } from '@/services/AuthService';
import api from '@/lib/api';

const mockFetch = vi.fn();
global.fetch = mockFetch;

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
    AuthService.getInstance()['authenticated'] = false;
    mockFetch.mockReset();
  });

  it('should be a singleton', () => {
    const instance1 = AuthService.getInstance();
    const instance2 = AuthService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('logout', () => {
    it('should clear session state without touching localStorage', async () => {
      // Set authenticated state first
      AuthService.getInstance()['authenticated'] = true;

      mockFetch.mockResolvedValue({ ok: true });
      await AuthService.getInstance().logout();

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
      mockFetch.mockResolvedValue({ ok: true, json: async () => ({ expiresIn: 900 }) });

      await AuthService.getInstance().refreshToken();

      // SECURITY: Backend manages refresh token via httpOnly cookie
      expect(mockFetch).toHaveBeenCalledWith('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      });
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
