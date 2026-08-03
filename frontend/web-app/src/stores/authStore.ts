import { create } from 'zustand';
import type { User } from '@/types';

const LEGACY_AUTH_STORAGE_KEY = 'payu-auth-storage';

if (typeof window !== 'undefined') {
  try {
    window.localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY);
  } catch {
    // Browser storage may be disabled; auth remains in memory.
  }
}

/**
 * Authentication State Management Store
 *
 * SECURITY NOTICE: Token Storage
 * ================================
 * This store does NOT persist JWT tokens (access_token, refresh_token).
 * Tokens are managed exclusively via httpOnly cookies from the backend.
 *
 * What IS held in memory:
 * - User profile data for the current render session
 * - Account ID for the current render session
 * - Authentication state (boolean)
 *
 * What is NOT stored here:
 * - access_token (managed via httpOnly cookie)
 * - refresh_token (managed via httpOnly cookie)
 *
 * Why this approach?
 * - Prevents XSS attacks from stealing tokens from localStorage
 * - httpOnly cookies are inaccessible to JavaScript
 * - Complies with PCI-DSS and OWASP security standards
 *
 * References:
 * - OWASP ASVS 2.7.1: Verify the application does not expose session tokens
 * - PCI-DSS Requirement 8.2.4: Secure authentication handling
 */
interface AuthState {
  // NOTE: token and refreshToken are REMOVED for security
  // Tokens are now managed via httpOnly cookies by the backend
  user: User | Partial<User> | null;
  accountId: string | null;
  /** True when user is authenticated - derived from user and accountId presence */
  isAuthenticated: boolean;
  /**
   * Timestamp (ms) when the accessToken cookie will expire.
   * Stored in-memory only (not persisted) to schedule proactive token refresh.
   * Populated after login or refresh. Does NOT contain the token itself.
   */
  tokenExpiresAt: number | null;
  setAuth: (user: User | Partial<User>, accountId: string) => void;
  setUser: (user: User | Partial<User>) => void;
  setAuthenticated: (authenticated: boolean) => void;
  setTokenExpiry: (expiresAt: number) => void;
  logout: () => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  accountId: null,
  isAuthenticated: false,
  tokenExpiresAt: null,

  setAuth: (user, accountId) => {
    set({
      user,
      accountId,
      isAuthenticated: true
    });
  },

  setTokenExpiry: (expiresAt) => {
    set({ tokenExpiresAt: expiresAt });
  },

  setUser: (user) => {
    set({ user });
  },

  setAuthenticated: (authenticated) => {
    if (authenticated) {
      set({ isAuthenticated: true });
    } else {
      set({ user: null, accountId: null, isAuthenticated: false, tokenExpiresAt: null });
    }
  },

  logout: () => {
    set({
      user: null,
      accountId: null,
      isAuthenticated: false,
      tokenExpiresAt: null
    });
  },

  clearAuth: () => {
    get().logout();
  }
}));

// Selector hook for derived isAuthenticated state
export const useIsAuthenticated = () => {
  return useAuthStore((state) => !!state.user && !!state.accountId);
};
