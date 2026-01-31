import { useEffect, useCallback, useRef, useState } from 'react';
import { useRouter } from 'expo-router';
import { useAuthStore } from '@/store/authStore';
import { AUTH_CONFIG } from '@/constants/config';
import { AuthTokens } from '@/types';

/**
 * useAuth Hook
 *
 * SECURITY: This hook provides authentication state and actions.
 * Tokens are NEVER stored in React state - they are read from SecureStore
 * only when needed (token expiry checks).
 */
export const useAuth = () => {
  const router = useRouter();
  const isMountedRef = useRef(true);
  const tokenCheckTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [tokens, setTokens] = useState<AuthTokens | null>(null);

  const {
    user,
    isAuthenticated,
    isLoading,
    error,
    login,
    register,
    logout,
    refreshToken,
    clearError,
    updateUser,
    getTokens,
  } = useAuthStore();

  // Load tokens from SecureStore when authenticated
  useEffect(() => {
    const loadTokens = async () => {
      if (isAuthenticated) {
        const storedTokens = await getTokens();
        if (isMountedRef.current) {
          setTokens(storedTokens);
        }
      } else {
        setTokens(null);
      }
    };

    loadTokens();
  }, [isAuthenticated, getTokens]);

  // Check token expiry periodically
  useEffect(() => {
    isMountedRef.current = true;

    const checkTokenExpiry = async () => {
      if (!isMountedRef.current) return;

      // Get fresh tokens from SecureStore
      const currentTokens = await getTokens();

      if (currentTokens?.expiresIn) {
        const expiryTime = new Date(currentTokens.expiresIn).getTime();
        const now = new Date().getTime();
        const timeUntilExpiry = expiryTime - now;

        if (timeUntilExpiry < AUTH_CONFIG.REFRESH_THRESHOLD) {
          try {
            await refreshToken();
            // Refresh local tokens after successful refresh
            const newTokens = await getTokens();
            if (isMountedRef.current) {
              setTokens(newTokens);
            }
          } catch (error) {
            if (isMountedRef.current) {
              console.error('Failed to refresh token:', error);
            }
          }
        }
      }
    };

    if (isAuthenticated) {
      // Use a timeout to check token expiry periodically
      tokenCheckTimeoutRef.current = setInterval(() => {
        checkTokenExpiry();
      }, 60000) as unknown as NodeJS.Timeout; // Check every minute

      // Initial check
      checkTokenExpiry();
    }

    return () => {
      isMountedRef.current = false;
      if (tokenCheckTimeoutRef.current) {
        clearInterval(tokenCheckTimeoutRef.current);
        tokenCheckTimeoutRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated]);

  const performLogout = useCallback(async () => {
    isMountedRef.current = false;
    if (tokenCheckTimeoutRef.current) {
      clearInterval(tokenCheckTimeoutRef.current);
      tokenCheckTimeoutRef.current = null;
    }
    await logout();
    setTokens(null);
    router.replace('/(auth)/login');
  }, [logout, router]);

  return {
    user,
    tokens,
    isAuthenticated,
    isLoading,
    error,
    login,
    register,
    logout: performLogout,
    refreshToken,
    clearError,
    updateUser,
  };
};
