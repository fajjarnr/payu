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
 * SECURITY: Tokens are managed via httpOnly cookies, not localStorage.
 * This prevents XSS attacks from stealing tokens (OWASP ASVS 2.7.1, PCI-DSS 8.2.4).
 * Only user profile data and auth state are held in memory.
 */
interface AuthState {
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

export const useIsAuthenticated = () => {
  return useAuthStore((state) => !!state.user && !!state.accountId);
};
