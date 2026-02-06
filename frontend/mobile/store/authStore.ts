/**
 * Auth Store - Zustand
 *
 * DEPRECATED: This file is deprecated and will be removed in a future version.
 * Use TanStack Query hooks from '@/src/hooks/useAuthQuery' instead.
 *
 * MIGRATION GUIDE:
 * - For auth operations (login, logout, register): Use useLogin, useLogout, useRegister from '@/src/hooks/useAuthQuery'
 * - For auth state: Use useAuthState from '@/src/hooks/useAuthQuery'
 * - For token management: Tokens are automatically handled by the API layer
 *
 * SECURITY NOTE: Tokens are NEVER stored in Zustand or React Query cache.
 * They are stored ONLY in SecureStore (encrypted) by the auth service layer.
 *
 * This file is kept for backward compatibility only.
 */

import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { User } from '@/types';

/**
 * AuthUIState Interface
 *
 * This store now only manages UI-related auth state:
 * - lastLoginAttempt: timestamp for rate limiting UI feedback
 * - biometricPromptEnabled: UI preference for biometric prompt
 *
 * SERVER STATE (user, isAuthenticated) should be fetched via React Query:
 * - useAuthState() from '@/src/hooks/useAuthQuery'
 * - useInitializeAuth() for initialization
 */
interface AuthUIState {
  // UI State only
  lastLoginAttempt: number | null;
  biometricPromptEnabled: boolean;

  // Actions
  setLastLoginAttempt: (timestamp: number | null) => void;
  setBiometricPromptEnabled: (enabled: boolean) => void;
  resetAuthUI: () => void;
}

const defaults = {
  lastLoginAttempt: null,
  biometricPromptEnabled: true,
};

/**
 * useAuthStore - UI State Only
 *
 * This store only persists UI preferences related to auth.
 * For actual auth state (user, session), use TanStack Query hooks.
 *
 * @deprecated Use hooks from '@/src/hooks/useAuthQuery' instead
 */
export const useAuthStore = create<AuthUIState>()(
  persist(
    (set) => ({
      // Initial state - UI only
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
