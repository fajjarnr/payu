'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import AuthService from '@/services/AuthService';
import { useAuthStore, useWalletStore, useNotificationStore, useTransactionStore, useUIStore } from '@/stores';
import { createLocaleHref } from '@/lib/navigation';
import { useLocale } from 'next-intl';

export const useLogout = () => {
  const queryClient = useQueryClient();
  const logout = useAuthStore((state) => state.logout);
  const clearWallet = useWalletStore((state) => state.clearWallet);
  const setNotifications = useNotificationStore((state) => state.setNotifications);
  const resetFilters = useTransactionStore((state) => state.resetFilters);
  const clearToasts = useUIStore((state) => state.clearToasts);
  const locale = useLocale();

  return useMutation({
    mutationFn: async () => {
      // Clear httpOnly cookies via BFF
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
        .catch(() => { /* best-effort */ });
      logout();
    },
    ...MutationPresets.nonFinancial,
    onSuccess: () => {
      // BUG-FE-069: Clear all Zustand stores on logout
      clearWallet();
      setNotifications([]);
      resetFilters();
      clearToasts();

      // BUG-FE-100: Clear promo popup localStorage dismissals
      if (typeof window !== 'undefined') {
        const keysToRemove: string[] = [];
        for (let i = 0; i < localStorage.length; i++) {
          const key = localStorage.key(i);
          if (key && (key.startsWith('promo-popup-state-dismissed-') || key.startsWith('promo-popup-session'))) {
            keysToRemove.push(key);
          }
        }
        keysToRemove.forEach((key) => localStorage.removeItem(key));
      }

      queryClient.clear();
      if (typeof window !== 'undefined') {
        window.location.href = createLocaleHref('/login', locale);
      }
    },
  });
};

export const useRefreshToken = () => {
  const { setAuth, setAuthenticated, setTokenExpiry, logout } = useAuthStore();

  return useMutation({
    mutationFn: () => AuthService.refreshToken(),
    ...MutationPresets.nonFinancial,
    onSuccess: (data) => {
      // Tokens are managed via httpOnly cookies by the backend
      // Re-arm the expiry timer with the new token's lifetime
      const expiresIn = data?.expiresIn ?? 900;
      setTokenExpiry(Date.now() + expiresIn * 1000);
      if (data?.user) {
        const user = data.user;
        setAuth(user, user.accountId || user.id);
      } else {
        setAuthenticated(true);
      }
    },
    onError: (error) => {
      // Definitive rejection ends the session; transient errors keep it
      if ((error as Error & { status?: number })?.status === 401) logout();
    },
  });
};

// Convenience hook to access auth state
export const useAuth = () => {
  return useAuthStore();
};
