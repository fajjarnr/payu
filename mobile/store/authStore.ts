import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import * as SecureStore from 'expo-secure-store';
import { User, AuthTokens } from '@/types';
import { storage } from '@/utils/storage';
import { authService } from '@/services/auth.service';
import { AUTH_CONFIG } from '@/constants/config';

interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (identifier: string, password: string) => Promise<void>;
  register: (data: {
    email: string;
    phoneNumber: string;
    fullName: string;
    password: string;
  }) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
  clearError: () => void;
  updateUser: (user: User) => void;
}

const secureStorage = {
  getItem: async (name: string): Promise<string | null> => {
    return await SecureStore.getItemAsync(name);
  },
  setItem: async (name: string, value: string): Promise<void> => {
    await SecureStore.setItemAsync(name, value);
  },
  removeItem: async (name: string): Promise<void> => {
    await SecureStore.deleteItemAsync(name);
  },
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      login: async (identifier: string, password: string) => {
        set({ isLoading: true, error: null });

        try {
          const response = await authService.login({
            identifier,
            password,
          });

          await storage.set(AUTH_CONFIG.TOKEN_KEY, response.tokens);
          await storage.set(AUTH_CONFIG.USER_KEY, response.user);

          set({
            user: response.user,
            tokens: response.tokens,
            isAuthenticated: true,
            isLoading: false,
          });
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Login failed',
            isLoading: false,
          });
          throw error;
        }
      },

      register: async (data) => {
        set({ isLoading: true, error: null });

        try {
          const response = await authService.register(data);

          await storage.set(AUTH_CONFIG.TOKEN_KEY, response.tokens);
          await storage.set(AUTH_CONFIG.USER_KEY, response.user);

          set({
            user: response.user,
            tokens: response.tokens,
            isAuthenticated: true,
            isLoading: false,
          });
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Registration failed',
            isLoading: false,
          });
          throw error;
        }
      },

      logout: async () => {
        try {
          await authService.logout();
        } catch (error) {
          console.error('Logout error:', error);
        } finally {
          await storage.remove(AUTH_CONFIG.TOKEN_KEY);
          await storage.remove(AUTH_CONFIG.USER_KEY);

          set({
            user: null,
            tokens: null,
            isAuthenticated: false,
          });
        }
      },

      refreshToken: async () => {
        const { tokens } = get();

        if (!tokens?.refreshToken) {
          throw new Error('No refresh token available');
        }

        try {
          const response = await authService.refreshToken(tokens.refreshToken);

          await storage.set(AUTH_CONFIG.TOKEN_KEY, response.tokens);

          set({
            tokens: response.tokens,
          });
        } catch (error) {
          await get().logout();
          throw error;
        }
      },

      clearError: () => set({ error: null }),

      updateUser: (user: User) => set({ user }),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => secureStorage),
      partialize: (state) => ({
        user: state.user,
        tokens: state.tokens,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
