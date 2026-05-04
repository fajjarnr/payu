import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '@/types';

/**
 * Authentication State Management Store
 *
 * SECURITY NOTICE: Token Storage
 * ================================
 * This store does NOT persist JWT tokens (access_token, refresh_token).
 * Tokens are managed exclusively via httpOnly cookies from the backend.
 *
 * What IS stored in this store:
 * - User profile data (non-sensitive)
 * - Account ID
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

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
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
    }),
    {
      name: 'payu-auth-storage',
      // Only persist non-sensitive data (user profile, account ID)
      // Tokens are NEVER persisted - they're in httpOnly cookies
      partialize: (state) => ({
        user: state.user ? {
          id: state.user.id,
          username: state.user.username,
          roles: state.user.roles,
          accountId: state.user.accountId
        } as Partial<User> as User : null,
        accountId: state.accountId,
        isAuthenticated: state.isAuthenticated
        // tokenExpiresAt intentionally NOT persisted:
        // cookie is the source of truth; useSilentRefresh re-estimates on mount
      })
    }
  )
);

// Selector hook for derived isAuthenticated state
export const useIsAuthenticated = () => {
  return useAuthStore((state) => !!state.user && !!state.accountId);
};
