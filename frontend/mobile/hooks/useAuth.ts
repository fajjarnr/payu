/**
 * Unified auth hook using TanStack Query for server state, Zustand for UI state.
 * Tokens are stored only in SecureStore, never in React state or Query cache.
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

export const useAuth = () => {
  const router = useRouter();
  const isMountedRef = useRef(true);
  const tokenCheckTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const { getUser, getSession, getTokens, setAuth, clearAuth } = useAuthState();
  const { initialize } = useInitializeAuth();

  // UI state from Zustand (minimal - only UI preferences)
  const { biometricPromptEnabled, setBiometricPromptEnabled } = useAuthStore();

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

  const user = getUser();
  const session = getSession();
  const isAuthenticated = session?.isAuthenticated ?? false;

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
      await storage.set(AUTH_CONFIG.USER_KEY, updatedUser);
      setAuth({ user: updatedUser, tokens: await getTokens() || { accessToken: '', refreshToken: '' } });
    },
    [setAuth, getTokens]
  );

  return {
    user,
    isAuthenticated,
    isLoading: loginMutation.isPending || registerMutation.isPending || logoutMutation.isPending,
    error: loginMutation.error?.message || registerMutation.error?.message || null,

    login,
    register,
    logout,
    updateUser,
    clearError: () => {
      // Errors are automatically cleared by React Query
    },

    biometricPromptEnabled,
    setBiometricPromptEnabled,

    initialize: initialize,
  };
};
