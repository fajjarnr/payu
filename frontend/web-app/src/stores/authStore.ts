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
  user: User | null;
  accountId: string | null;
  /** True when user is authenticated - derived from user and accountId presence */
  isAuthenticated: boolean;
  setAuth: (user: User, accountId: string) => void;
  setUser: (user: User) => void;
  setAuthenticated: (authenticated: boolean) => void;
  logout: () => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accountId: null,
      isAuthenticated: false,

      setAuth: (user, accountId) => {
        set({
          user,
          accountId,
          isAuthenticated: true
        });
      },

      setUser: (user) => {
        set({ user });
      },

      setAuthenticated: (authenticated) => {
        if (authenticated) {
          set({ isAuthenticated: true });
        } else {
          set({ user: null, accountId: null, isAuthenticated: false });
        }
      },

      logout: () => {
        set({
          user: null,
          accountId: null,
          isAuthenticated: false
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
        user: state.user,
        accountId: state.accountId,
        isAuthenticated: state.isAuthenticated
      })
    }
  )
);

// Selector hook for derived isAuthenticated state
export const useIsAuthenticated = () => {
  return useAuthStore((state) => !!state.user && !!state.accountId);
};
