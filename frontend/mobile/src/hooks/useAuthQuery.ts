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

// Query keys
export const authKeys = {
  all: ['auth'] as const,
  user: () => [...authKeys.all, 'user'] as const,
  tokens: () => [...authKeys.all, 'tokens'] as const,
  session: () => [...authKeys.all, 'session'] as const,
};

// Types for auth state
interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
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
      // Store tokens securely
      await storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens);
      await storage.set(AUTH_CONFIG.USER_KEY, data.user);

      // Update auth state in cache
      queryClient.setQueryData(authKeys.user(), data.user);
      queryClient.setQueryData(authKeys.tokens(), data.tokens);
      queryClient.setQueryData(authKeys.session(), {
        user: data.user,
        tokens: data.tokens,
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
      // Store tokens securely
      await storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens);
      await storage.set(AUTH_CONFIG.USER_KEY, data.user);

      // Update auth state in cache
      queryClient.setQueryData(authKeys.user(), data.user);
      queryClient.setQueryData(authKeys.tokens(), data.tokens);
      queryClient.setQueryData(authKeys.session(), {
        user: data.user,
        tokens: data.tokens,
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
      await storage.remove(AUTH_CONFIG.TOKEN_KEY);
      await storage.remove(AUTH_CONFIG.USER_KEY);

      // Clear all queries from cache
      queryClient.clear();
    },
    onError: async () => {
      // Even if logout fails, clear local data
      await storage.remove(AUTH_CONFIG.TOKEN_KEY);
      await storage.remove(AUTH_CONFIG.USER_KEY);
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
      // Update stored tokens
      await storage.set(AUTH_CONFIG.TOKEN_KEY, data.tokens);

      // Update cache
      queryClient.setQueryData(authKeys.tokens(), data.tokens);
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
 */
export function useAuthState() {
  const queryClient = useQueryClient();

  return {
    getUser: () => queryClient.getQueryData<User>(authKeys.user()),
    getTokens: () => queryClient.getQueryData<AuthTokens>(authKeys.tokens()),
    getSession: () =>
      queryClient.getQueryData<AuthState>(authKeys.session()),
    setAuth: (data: AuthResponse) => {
      queryClient.setQueryData(authKeys.user(), data.user);
      queryClient.setQueryData(authKeys.tokens(), data.tokens);
      queryClient.setQueryData(authKeys.session(), {
        user: data.user,
        tokens: data.tokens,
        isAuthenticated: true,
      });
    },
    clearAuth: () => {
      queryClient.removeQueries({ queryKey: authKeys.all });
    },
  };
}

/**
 * Hook to initialize auth state from storage
 * Call this in your app initialization
 */
export function useInitializeAuth() {
  const queryClient = useQueryClient();

  return {
    initialize: async (): Promise<AuthState | null> => {
      try {
        const [tokens, user] = await Promise.all([
          storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY),
          storage.get<User>(AUTH_CONFIG.USER_KEY),
        ]);

        if (tokens && user) {
          const authState: AuthState = {
            user,
            tokens,
            isAuthenticated: true,
          };

          // Set in cache
          queryClient.setQueryData(authKeys.user(), user);
          queryClient.setQueryData(authKeys.tokens(), tokens);
          queryClient.setQueryData(authKeys.session(), authState);

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
