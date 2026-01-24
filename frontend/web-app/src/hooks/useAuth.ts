import { useMutation, useQueryClient } from '@tanstack/react-query';
import AuthService from '@/services/AuthService';
import { useAuthStore } from '@/stores';
import type { LoginRequest, LoginResponse } from '@/types';

export const useLogin = () => {
  const queryClient = useQueryClient();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: (credentials: LoginRequest) => AuthService.login(credentials),
    onSuccess: async (data: LoginResponse) => {
      setAuth(
        data.access_token,
        data.refresh_token,
        { id: '', externalId: '', username: '', email: '', fullName: '', nik: '', kycStatus: 'PENDING', createdAt: '', updatedAt: '' },
        ''
      );
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
    mutationFn: () => {
      logout();
      return Promise.resolve();
    },
    onSuccess: () => {
      queryClient.clear();
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
  });
};

export const useRefreshToken = () => {
  const setToken = useAuthStore((state) => state.setToken);

  return useMutation({
    mutationFn: () => AuthService.refreshToken(),
    onSuccess: (token) => {
      setToken(token);
    }
  });
};

// Convenience hook to access auth state
export const useAuth = () => {
  return useAuthStore();
};
