/**
 * Deprecated: use hooks from '@/src/hooks/useAuthQuery' instead. Kept for backward
 * compatibility; manages UI-only auth state. Security: tokens are stored only in
 * SecureStore, never in Zustand or the React Query cache.
 */

import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { User } from '@/types';

/**
 * UI-only auth state: lastLoginAttempt (rate-limit feedback) and biometricPromptEnabled.
 * Server state (user, isAuthenticated) comes from useAuthState/useInitializeAuth.
 */
interface AuthUIState {
  lastLoginAttempt: number | null;
  biometricPromptEnabled: boolean;

  setLastLoginAttempt: (timestamp: number | null) => void;
  setBiometricPromptEnabled: (enabled: boolean) => void;
  resetAuthUI: () => void;
}

const defaults = {
  lastLoginAttempt: null,
  biometricPromptEnabled: true,
};

/** UI-only auth store. For auth state, use TanStack Query hooks. */
export const useAuthStore = create<AuthUIState>()(
  persist(
    (set) => ({
      lastLoginAttempt: defaults.lastLoginAttempt,
      biometricPromptEnabled: defaults.biometricPromptEnabled,

      setLastLoginAttempt: (timestamp) => {
        set({ lastLoginAttempt: timestamp });
      },

      setBiometricPromptEnabled: (enabled) => {
        set({ biometricPromptEnabled: enabled });
      },

      resetAuthUI: () => {
        set({
          lastLoginAttempt: defaults.lastLoginAttempt,
          biometricPromptEnabled: defaults.biometricPromptEnabled,
        });
      },
    }),
    {
      name: 'auth-ui-storage',
      storage: createJSONStorage(() => AsyncStorage),
      // Only persist UI state (no sensitive data)
      partialize: (state) => ({
        lastLoginAttempt: state.lastLoginAttempt,
        biometricPromptEnabled: state.biometricPromptEnabled,
      }),
    }
  )
);
