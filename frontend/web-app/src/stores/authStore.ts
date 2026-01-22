import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '@/types';

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: User | null;
  accountId: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, refreshToken: string, user: User, accountId: string) => void;
  setUser: (user: User) => void;
  setToken: (token: string) => void;
  logout: () => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      refreshToken: null,
      user: null,
      accountId: null,
      isAuthenticated: false,

      setAuth: (token, refreshToken, user, accountId) => {
        set({
          token,
          refreshToken,
          user,
          accountId,
          isAuthenticated: !!token
        });
      },

      setUser: (user) => {
        set({ user });
      },

      setToken: (token) => {
        set({ token, isAuthenticated: !!token });
      },

      logout: () => {
        set({
          token: null,
          refreshToken: null,
          user: null,
          accountId: null,
          isAuthenticated: false
        });
      },

      clearAuth: () => {
        get().logout();
      }
    }),
    {
      name: 'payu-auth-storage',
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        user: state.user,
        accountId: state.accountId
      })
    }
  )
);
