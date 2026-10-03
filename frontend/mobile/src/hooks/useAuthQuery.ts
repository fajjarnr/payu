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
 * Auth query hooks. Security policy P2-C2: tokens go only to SecureStore and are
 * never stored in the React Query cache; session state is memory-only.
 */

export const authKeys = {
  all: ['auth'] as const,
  user: () => [...authKeys.all, 'user'] as const,
  session: () => [...authKeys.all, 'session'] as const,
  // Security: no 'tokens' key - tokens are never in the React Query cache
};

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
}

/** Login; tokens are written to SecureStore, never the query cache. */
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
      // Security: store tokens only in SecureStore (encrypted)
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
      // Security: store tokens only in SecureStore (encrypted)
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

/** Logout; clears the React Query cache. */
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
      // Security: store refreshed tokens only in SecureStore (encrypted)
      // NEVER store tokens in React Query cache
      await storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens);

      // Note: No cache update for tokens - they stay in SecureStore only
    },
    ...options,
  });
}

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
 * Reads the current auth state from cache. getTokens() reads SecureStore directly;
 * tokens are never stored in the React Query cache.
 */
export function useAuthState() {
  const queryClient = useQueryClient();

  return {
    getUser: () => queryClient.getQueryData<User>(authKeys.user()),
    /**
     * Get tokens from SecureStore (not from React Query cache)
     * Security: tokens are never in the React Query cache
     */
    getTokens: async () => {
      return await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);
    },
    getSession: () =>
      queryClient.getQueryData<AuthState>(authKeys.session()),
    /**
     * Set auth state after login/register
     * Security: tokens go to SecureStore only, never the React Query cache
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
 * Initializes auth state from storage. Only non-sensitive data enters the cache;
 * tokens stay in SecureStore.
 */
export function useInitializeAuth() {
  const queryClient = useQueryClient();

  return {
    initialize: async (): Promise<AuthState | null> => {
      try {
        // Security: only user data enters the cache
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
          // Security: no tokens in cache - they stay in SecureStore

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
