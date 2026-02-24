'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import AuthService from '@/services/AuthService';
import { useAuthStore } from '@/stores';
import type { LoginRequest, User } from '@/types';
import { createLocaleHref } from '@/lib/navigation';
import { useLocale } from 'next-intl';

export const useLogin = () => {
  const queryClient = useQueryClient();
  const { setAuth, setTokenExpiry } = useAuthStore();

  return useMutation({
    mutationFn: (credentials: LoginRequest) => AuthService.login(credentials),
    onSuccess: async (response) => {
      // Tokens are managed via httpOnly cookies by the backend
      // We only store user profile and account ID in the store
      const user = response.data?.user;
      if (user) {
        setAuth(user, user.id);
      }
      // Track when the accessToken cookie will expire so useSilentRefresh
      // can proactively refresh before it expires (no token is exposed here)
      const expiresIn: number = response.data?.expiresIn ?? 900; // seconds
      setTokenExpiry(Date.now() + expiresIn * 1000);

      await queryClient.invalidateQueries({ queryKey: ['auth'] });
    },
    onError: (error) => {
      console.error('Login failed:', error);
    }
  });
};

export const useLogout = () => {
  const queryClient = useQueryClient();
  const logout = useAuthStore((state) => state.logout);
  const locale = useLocale();

  return useMutation({
    mutationFn: async () => {
      // Clear httpOnly cookies via BFF
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
        .catch(() => { /* best-effort */ });
      logout();
    },
    onSuccess: () => {
      queryClient.clear();
      if (typeof window !== 'undefined') {
        window.location.href = createLocaleHref('/login', locale);
      }
    },
  });
};

export const useRefreshToken = () => {
  const { setAuthenticated, setTokenExpiry } = useAuthStore();

  return useMutation({
    mutationFn: () => AuthService.refreshToken(),
    onSuccess: (data) => {
      // Tokens are managed via httpOnly cookies by the backend
      setAuthenticated(true);
      // Re-arm the expiry timer with the new token's lifetime
      const expiresIn = data?.expiresIn ?? 900;
      setTokenExpiry(Date.now() + expiresIn * 1000);
    }
  });
};

// Convenience hook to access auth state
export const useAuth = () => {
  return useAuthStore();
};
