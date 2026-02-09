'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import AuthService from '@/services/AuthService';
import { useAuthStore } from '@/stores';
import type { LoginRequest, User } from '@/types';

export const useLogin = () => {
  const queryClient = useQueryClient();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: (credentials: LoginRequest) => AuthService.login(credentials),
    onSuccess: async () => {
      // Tokens are managed via httpOnly cookies by the backend
      // We only store user profile and account ID in the store
      const mockUser: User = {
        id: '',
        externalId: '',
        username: '',
        email: '',
        fullName: '',
        nik: '',
        kycStatus: 'PENDING',
        createdAt: '',
        updatedAt: ''
      };
      setAuth(mockUser, '');
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
        window.location.href = '/login';
      }
    },
  });
};

export const useRefreshToken = () => {
  const setAuthenticated = useAuthStore((state) => state.setAuthenticated);

  return useMutation({
    mutationFn: () => AuthService.refreshToken(),
    onSuccess: () => {
      // Tokens are managed via httpOnly cookies by the backend
      // We just update the authenticated state
      setAuthenticated(true);
    }
  });
};

// Convenience hook to access auth state
export const useAuth = () => {
  return useAuthStore();
};
