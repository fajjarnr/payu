import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import { useAuthStore } from '@/store/authStore';
import { storage } from '@/utils/storage';
import { AUTH_CONFIG } from '@/constants/config';

export const useAuth = () => {
  const router = useRouter();
  const {
    user,
    tokens,
    isAuthenticated,
    isLoading,
    error,
    login,
    register,
    logout,
    refreshToken,
    clearError,
    updateUser,
  } = useAuthStore();

  useEffect(() => {
    // Check for token expiry and refresh if needed
    const checkTokenExpiry = async () => {
      if (tokens?.expiresIn) {
        const expiryTime = new Date(tokens.expiresIn).getTime();
        const now = new Date().getTime();
        const timeUntilExpiry = expiryTime - now;

        if (timeUntilExpiry < AUTH_CONFIG.REFRESH_THRESHOLD) {
          try {
            await refreshToken();
          } catch (error) {
            console.error('Failed to refresh token:', error);
          }
        }
      }
    };

    if (isAuthenticated) {
      checkTokenExpiry();
    }
  }, [tokens, isAuthenticated]);

  const performLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

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
