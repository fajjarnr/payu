import {
  useMutation,
  useQueryClient,
  UseMutationOptions,
} from '@tanstack/react-query';
import { authService } from '@/services/auth.service';
import { storage } from '@/utils/storage';
import { AUTH_CONFIG } from '@/constants/config';
import {
  LoginCredentials,
  RegisterData,
  AuthResponse,
  User,
  AuthTokens,
} from '@/types';

/**
 * Auth Query Hooks - React Query Integration for Authentication
 *
 * SECURITY POLICY: Token Storage (P2-C2)
 * ========================================
 *
 * CRITICAL: Tokens are NEVER stored in React Query cache.
 * Tokens are stored ONLY in SecureStore (encrypted).
 *
 * This file provides hooks for auth operations while ensuring:
 * - Tokens go directly to SecureStore (never React Query cache)
 * - User data can be cached (non-sensitive)
 * - Session state is memory-only (not persisted)
 *
 * @module useAuthQuery
 * @version 2.0.0 - Secure token handling
 */

// Query keys
export const authKeys = {
  all: ['auth'] as const,
  user: () => [...authKeys.all, 'user'] as const,
  session: () => [...authKeys.all, 'session'] as const,
  // SECURITY: No 'tokens' key - tokens are NEVER in React Query cache
};

// Types for auth state
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
}

/**
 * Hook for login with token storage
 */
export function useLogin(
  options?: UseMutationOptions<AuthResponse, Error, LoginCredentials>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['login'],
    mutationFn: async (credentials: LoginCredentials) => {
      const response = await authService.login(credentials);
      return response;
    },
    onSuccess: async (data) => {
      // SECURITY: Store tokens ONLY in SecureStore (encrypted)
      // NEVER store tokens in React Query cache
      // Performance: Parallel write operations for better response time
      await Promise.all([
        storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens),
        storage.set(AUTH_CONFIG.USER_KEY, data.user),
      ]);

      // Update non-sensitive auth state in cache (user only, no tokens)
      queryClient.setQueryData(authKeys.user(), data.user);
      queryClient.setQueryData(authKeys.session(), {
        user: data.user,
        isAuthenticated: true,
      });

      // Invalidate all queries to refetch with new auth
      queryClient.invalidateQueries({
        predicate: (query) => {
          // Don't invalidate auth queries
          return !query.queryKey[0]?.toString().startsWith('auth');
        },
      });
    },
    ...options,
  });
}

/**
 * Hook for registration
 */
export function useRegister(
  options?: UseMutationOptions<AuthResponse, Error, RegisterData>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['register'],
    mutationFn: async (data: RegisterData) => {
      const response = await authService.register(data);
      return response;
    },
    onSuccess: async (data) => {
      // SECURITY: Store tokens ONLY in SecureStore (encrypted)
      // NEVER store tokens in React Query cache
      // Performance: Parallel write operations for better response time
      await Promise.all([
        storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens),
        storage.set(AUTH_CONFIG.USER_KEY, data.user),
      ]);

      // Update non-sensitive auth state in cache (user only, no tokens)
      queryClient.setQueryData(authKeys.user(), data.user);
      queryClient.setQueryData(authKeys.session(), {
        user: data.user,
        isAuthenticated: true,
      });
    },
    ...options,
  });
}

/**
 * Hook for logout with cache clearing
 */
export function useLogout(
  options?: UseMutationOptions<void, Error, void>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['logout'],
    mutationFn: async () => {
      try {
        // Call logout endpoint
        await authService.logout();
      } catch (error) {
        // Ignore logout endpoint errors
        console.log('Logout endpoint error:', error);
      }
    },
    onSuccess: async () => {
      // Clear secure storage
      // Performance: Parallel delete operations
      await Promise.all([
        storage.remove(AUTH_CONFIG.TOKEN_KEY),
        storage.remove(AUTH_CONFIG.USER_KEY),
      ]);

      // Clear all queries from cache
      queryClient.clear();
    },
    onError: async () => {
      // Even if logout fails, clear local data
      // Performance: Parallel delete operations
      await Promise.all([
        storage.remove(AUTH_CONFIG.TOKEN_KEY),
        storage.remove(AUTH_CONFIG.USER_KEY),
      ]);
      queryClient.clear();
    },
    ...options,
  });
}

/**
 * Hook for token refresh
 */
export function useRefreshToken(
  options?: UseMutationOptions<AuthResponse, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['refreshToken'],
    mutationFn: async (refreshToken: string) => {
      const response = await authService.refreshToken(refreshToken);
      return response;
    },
    onSuccess: async (data) => {
      // SECURITY: Store refreshed tokens ONLY in SecureStore (encrypted)
      // NEVER store tokens in React Query cache
      await storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens);

      // Note: No cache update for tokens - they stay in SecureStore only
    },
    ...options,
  });
}

/**
 * Hook for password reset request
 */
export function useRequestPasswordReset(
  options?: UseMutationOptions<void, Error, string>
) {
  return useMutation({
    mutationKey: ['requestPasswordReset'],
    mutationFn: async (email: string) => {
      await authService.requestPasswordReset(email);
    },
    ...options,
  });
}

/**
 * Hook for password reset confirmation
 */
export function useResetPassword(
  options?: UseMutationOptions<void, Error, { token: string; password: string }>
) {
  return useMutation({
    mutationKey: ['resetPassword'],
    mutationFn: async ({ token, password }: { token: string; password: string }) => {
      await authService.resetPassword(token, password);
    },
    ...options,
  });
}

/**
 * Hook for changing password
 */
export function useChangePassword(
  options?: UseMutationOptions<void, Error, { oldPassword: string; newPassword: string }>
) {
  return useMutation({
    mutationKey: ['changePassword'],
    mutationFn: async ({
      oldPassword,
      newPassword,
    }: {
      oldPassword: string;
      newPassword: string;
    }) => {
      await authService.changePassword(oldPassword, newPassword);
    },
    ...options,
  });
}

/**
 * Hook for email verification
 */
export function useVerifyEmail(
  options?: UseMutationOptions<void, Error, string>
) {
  return useMutation({
    mutationKey: ['verifyEmail'],
    mutationFn: async (token: string) => {
      await authService.verifyEmail(token);
    },
    ...options,
  });
}

/**
 * Hook to get current auth state from cache
 *
 * SECURITY: getTokens() reads directly from SecureStore
 * Tokens are NEVER stored in React Query cache
 */
export function useAuthState() {
  const queryClient = useQueryClient();

  return {
    getUser: () => queryClient.getQueryData<User>(authKeys.user()),
    /**
     * Get tokens from SecureStore (not from React Query cache)
     * SECURITY: Tokens are never in React Query cache
     */
    getTokens: async () => {
      return await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);
    },
    getSession: () =>
      queryClient.getQueryData<AuthState>(authKeys.session()),
    /**
     * Set auth state after login/register
     * SECURITY: Tokens go to SecureStore only, never React Query cache
     */
    setAuth: async (data: AuthResponse) => {
      // Store tokens in SecureStore (encrypted)
      // Performance: Parallel write operations
      await Promise.all([
        storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens),
        storage.set(AUTH_CONFIG.USER_KEY, data.user),
      ]);

      // Store only non-sensitive data in React Query cache
      queryClient.setQueryData(authKeys.user(), data.user);
      queryClient.setQueryData(authKeys.session(), {
        user: data.user,
        isAuthenticated: true,
      });
    },
    clearAuth: async () => {
      // Clear SecureStore
      // Performance: Parallel delete operations
      await Promise.all([
        storage.remove(AUTH_CONFIG.TOKEN_KEY),
        storage.remove(AUTH_CONFIG.USER_KEY),
      ]);

      // Clear React Query cache
      queryClient.removeQueries({ queryKey: authKeys.all });
    },
  };
}

/**
 * Hook to initialize auth state from storage
 * Call this in your app initialization
 *
 * SECURITY: Only loads non-sensitive data into React Query cache.
 * Tokens stay in SecureStore and are never loaded into cache.
 */
export function useInitializeAuth() {
  const queryClient = useQueryClient();

  return {
    initialize: async (): Promise<AuthState | null> => {
      try {
        // SECURITY: Only load user data into cache
        // Tokens remain in SecureStore only
        // Performance: Parallel read operations for faster initialization
        const [user, tokens] = await Promise.all([
          storage.get<User>(AUTH_CONFIG.USER_KEY),
          storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY),
        ]);

        if (tokens && user) {
          const authState: AuthState = {
            user,
            isAuthenticated: true,
          };

          // Set only non-sensitive data in cache
          queryClient.setQueryData(authKeys.user(), user);
          queryClient.setQueryData(authKeys.session(), authState);
          // SECURITY: No tokens in cache - they stay in SecureStore

          return authState;
        }

        return null;
      } catch (error) {
        console.error('Error initializing auth:', error);
        return null;
      }
    },
  };
}
