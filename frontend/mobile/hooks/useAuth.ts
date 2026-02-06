/**
 * useAuth Hook - Unified Authentication Hook
 *
 * This hook provides a unified interface for authentication using TanStack Query.
 * It replaces the previous Zustand-based auth store with React Query for server state.
 *
 * MIGRATION:
 * - Previous: useAuthStore from '@/store/authStore'
 * - Current: useAuth from '@/hooks/useAuth' (uses TanStack Query internally)
 *
 * STATE MANAGEMENT:
 * - Server State (user, session): Managed by TanStack Query (useAuthState, useLogin, etc.)
 * - UI State (biometricPromptEnabled): Managed by useAuthStore (Zustand for UI only)
 * - Tokens: Stored ONLY in SecureStore (encrypted), never in React state
 *
 * SECURITY:
 * - Tokens are NEVER stored in React state or React Query cache
 * - Tokens are read directly from SecureStore when needed
 * - Token refresh is handled automatically by the API layer
 */
import { useEffect, useCallback, useRef } from 'react';
import { useRouter } from 'expo-router';
import {
  useAuthState,
  useLogin,
  useRegister,
  useLogout,
  useInitializeAuth,
} from '@/src/hooks/useAuthQuery';
import { useAuthStore } from '@/store/authStore';
import { storage } from '@/utils/storage';
import { AUTH_CONFIG } from '@/constants/config';
import { AuthTokens, User } from '@/types';

/**
 * useAuth Hook
 *
 * Provides authentication state and actions with unified state management:
 * - Server state via TanStack Query
 * - UI state via Zustand (minimal)
 * - Token management via SecureStore
 */
export const useAuth = () => {
  const router = useRouter();
  const isMountedRef = useRef(true);
  const tokenCheckTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Server state from TanStack Query
  const { getUser, getSession, getTokens, setAuth, clearAuth } = useAuthState();
  const { initialize } = useInitializeAuth();

  // UI state from Zustand (minimal - only UI preferences)
  const { biometricPromptEnabled, setBiometricPromptEnabled } = useAuthStore();

  // Mutations
  const loginMutation = useLogin({
    onSuccess: () => {
      router.replace('/(tabs)');
    },
  });

  const registerMutation = useRegister({
    onSuccess: () => {
      router.replace('/(tabs)');
    },
  });

  const logoutMutation = useLogout({
    onSuccess: () => {
      router.replace('/(auth)/login');
    },
  });

  // Derived state
  const user = getUser();
  const session = getSession();
  const isAuthenticated = session?.isAuthenticated ?? false;

  // Token refresh check
  useEffect(() => {
    isMountedRef.current = true;

    const checkTokenExpiry = async () => {
      if (!isMountedRef.current || !isAuthenticated) return;

      const tokens = await getTokens();

      if (tokens?.expiresIn) {
        const expiryTime = new Date(tokens.expiresIn).getTime();
        const now = new Date().getTime();
        const timeUntilExpiry = expiryTime - now;

        if (timeUntilExpiry < AUTH_CONFIG.REFRESH_THRESHOLD) {
          try {
            // Token refresh is handled by the API interceptor
            // This is just for additional safety
            console.log('Token nearing expiry, refresh will be handled by API layer');
          } catch (error) {
            if (isMountedRef.current) {
              console.error('Token check failed:', error);
            }
          }
        }
      }
    };

    if (isAuthenticated) {
      tokenCheckTimeoutRef.current = setInterval(() => {
        checkTokenExpiry();
      }, 60000) as unknown as NodeJS.Timeout;

      checkTokenExpiry();
    }

    return () => {
      isMountedRef.current = false;
      if (tokenCheckTimeoutRef.current) {
        clearInterval(tokenCheckTimeoutRef.current);
        tokenCheckTimeoutRef.current = null;
      }
    };
  }, [isAuthenticated, getTokens]);

  // Actions
  const login = useCallback(
    async (identifier: string, password: string) => {
      await loginMutation.mutateAsync({ identifier, password });
    },
    [loginMutation]
  );

  const register = useCallback(
    async (data: {
      email: string;
      phoneNumber: string;
      fullName: string;
      password: string;
      confirmPassword?: string;
    }) => {
      const registerData = {
        ...data,
        confirmPassword: data.confirmPassword || data.password,
      };
      await registerMutation.mutateAsync(registerData);
    },
    [registerMutation]
  );

  const logout = useCallback(async () => {
    isMountedRef.current = false;
    if (tokenCheckTimeoutRef.current) {
      clearInterval(tokenCheckTimeoutRef.current);
      tokenCheckTimeoutRef.current = null;
    }
    await logoutMutation.mutateAsync();
  }, [logoutMutation]);

  const updateUser = useCallback(
    async (updatedUser: User) => {
      // Update in SecureStore
      await storage.set(AUTH_CONFIG.USER_KEY, updatedUser);
      // Update in React Query cache
      setAuth({ user: updatedUser, tokens: await getTokens() || { accessToken: '', refreshToken: '' } });
    },
    [setAuth, getTokens]
  );

  return {
    // State
    user,
    isAuthenticated,
    isLoading: loginMutation.isPending || registerMutation.isPending || logoutMutation.isPending,
    error: loginMutation.error?.message || registerMutation.error?.message || null,

    // Actions
    login,
    register,
    logout,
    updateUser,
    clearError: () => {
      // Errors are automatically cleared by React Query
    },

    // UI State
    biometricPromptEnabled,
    setBiometricPromptEnabled,

    // Initialization
    initialize: initialize,
  };
};
