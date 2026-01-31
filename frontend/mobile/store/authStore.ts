import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { User, AuthTokens } from '@/types';
import { storage } from '@/utils/storage';
import { authService } from '@/services/auth.service';
import { Logger } from '@/utils/logger';
import { AUTH_CONFIG } from '@/constants/config';

/**
 * No-op storage for Zustand persistence
 *
 * SECURITY P2-C2: We use a no-op storage to prevent Zustand from persisting
 * any auth state to AsyncStorage. All sensitive data (tokens, user)
 * is stored ONLY in SecureStore (encrypted).
 *
 * This ensures:
 * - Tokens are never in AsyncStorage (unencrypted)
 * - Tokens are never in React Query cache
 * - Tokens are never in Zustand persistence layer
 *
 * Logging: All operations use sanitized logger to prevent token leakage
 */
const noOpStorage = {
  getItem: async (_name: string): Promise<string | null> => {
    return null;
  },
  setItem: async (_name: string, _value: string): Promise<void> => {
    // No-op: We don't persist auth state to AsyncStorage
  },
  removeItem: async (_name: string): Promise<void> => {
    // No-op
  },
};

/**
 * AuthState Interface
 *
 * SECURITY P2-C2: Tokens are NOT stored in Zustand state.
 * Tokens are stored ONLY in SecureStore (encrypted storage).
 * This prevents token exposure in:
 * - Zustand state snapshots
 * - React Query cache
 * - Memory dumps
 * - Redux DevTools (if enabled)
 *
 * The `isAuthenticated` flag is computed based on token existence
 * in SecureStore during initialization and login/logout operations.
 *
 * Logging P2-C3: All operations use sanitized logger to prevent token leakage
 */
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (identifier: string, password: string) => Promise<void>;
  register: (data: {
    email: string;
    phoneNumber: string;
    fullName: string;
    password: string;
    confirmPassword?: string;
  }) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
  clearError: () => void;
  updateUser: (user: User) => void;
  getTokens: () => Promise<AuthTokens | null>;
  checkAuthStatus: () => Promise<boolean>;
}

/**
 * Secure Token Storage Helper Functions
 *
 * These functions ensure tokens are ONLY stored in SecureStore
 * and never in Zustand state or any other storage mechanism.
 */
const tokenStorage = {
  async saveTokens(tokens: AuthTokens): Promise<void> {
    await storage.set(AUTH_CONFIG.TOKEN_KEY, tokens);
  },

  async getTokens(): Promise<AuthTokens | null> {
    return await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);
  },

  async removeTokens(): Promise<void> {
    await storage.remove(AUTH_CONFIG.TOKEN_KEY);
  },

  async saveUser(user: User): Promise<void> {
    await storage.set(AUTH_CONFIG.USER_KEY, user);
  },

  async getUser(): Promise<User | null> {
    return await storage.get<User>(AUTH_CONFIG.USER_KEY);
  },

  async removeUser(): Promise<void> {
    await storage.remove(AUTH_CONFIG.USER_KEY);
  },
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      /**
       * Get tokens from SecureStore
       * SECURITY: Tokens are never stored in Zustand state
       */
      getTokens: async () => {
        return await tokenStorage.getTokens();
      },

      /**
       * Check authentication status
       * Returns true if valid tokens exist in SecureStore
       */
      checkAuthStatus: async () => {
        const tokens = await tokenStorage.getTokens();
        const user = await tokenStorage.getUser();
        const isAuthenticated = !!tokens?.accessToken && !!user;

        set({ isAuthenticated, user });
        return isAuthenticated;
      },

      login: async (identifier: string, password: string) => {
        set({ isLoading: true, error: null });

        try {
          const response = await authService.login({
            identifier,
            password,
          });

          // SECURITY: Store tokens ONLY in SecureStore (encrypted)
          // Never store tokens in Zustand state
          await tokenStorage.saveTokens(response.tokens);
          await tokenStorage.saveUser(response.user);

          set({
            user: response.user,
            isAuthenticated: true,
            isLoading: false,
          });
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Login failed',
            isLoading: false,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      register: async (data) => {
        set({ isLoading: true, error: null });

        try {
          // Ensure confirmPassword is present for RegisterData type
          const registerData = {
            ...data,
            confirmPassword: data.confirmPassword || data.password,
          };

          const response = await authService.register(registerData);

          // SECURITY: Store tokens ONLY in SecureStore (encrypted)
          await tokenStorage.saveTokens(response.tokens);
          await tokenStorage.saveUser(response.user);

          set({
            user: response.user,
            isAuthenticated: true,
            isLoading: false,
          });
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Registration failed',
            isLoading: false,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      logout: async () => {
        try {
          await authService.logout();
        } catch (error) {
          // Sanitized logging - error details are logged but tokens are not
          Logger.error('AuthStore', 'Logout API call failed', error);
        } finally {
          // SECURITY P2-C2: Remove tokens from SecureStore only
          // Execute sequentially to ensure both operations complete
          await tokenStorage.removeTokens();
          await tokenStorage.removeUser();

          set({
            user: null,
            isAuthenticated: false,
          });
        }
      },

      refreshToken: async () => {
        const tokens = await tokenStorage.getTokens();

        if (!tokens?.refreshToken) {
          throw new Error('No refresh token available');
        }

        try {
          const response = await authService.refreshToken(tokens.refreshToken);

          // SECURITY: Store refreshed tokens ONLY in SecureStore
          await tokenStorage.saveTokens(response.tokens);

          // Note: We don't update Zustand state with tokens
          // Tokens remain only in SecureStore
        } catch (error) {
          await get().logout();
          throw error;
        }
      },

      clearError: () => set({ error: null }),

      updateUser: (user: User) => {
        set({ user });
        // Also update user in SecureStore for persistence
        // This is a fire-and-forget operation, but we log errors appropriately
        tokenStorage.saveUser(user).catch((error) => {
          // Sanitized logging - user data is NOT logged (may contain PII)
          Logger.error('AuthStore', 'Failed to update user in SecureStore', error);
        });
      },
    }),
    {
      name: 'auth-storage',
      // SECURITY: Use no-op storage to prevent any persistence to AsyncStorage
      // All auth data (tokens, user) is stored ONLY in SecureStore (encrypted)
      storage: createJSONStorage(() => noOpStorage),
      partialize: (_state) => ({
        // SECURITY: We don't persist anything through Zustand
        // All sensitive data stays in SecureStore only
        // This prevents tokens from appearing in:
        // - AsyncStorage (unencrypted)
        // - React Query cache
        // - Zustand persistence layer
      }),
    }
  )
);
