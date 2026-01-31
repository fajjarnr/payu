import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Appearance } from 'react-native';

/**
 * Color Scheme Type
 */
export type ColorScheme = 'light' | 'dark' | 'system';

/**
 * Language Type
 */
export type Language = 'en' | 'id';

/**
 * UI State Interface
 *
 * This store manages CLIENT-SIDE ONLY state:
 * - Theme preferences (light/dark/system)
 * - Language preferences
 * - UI settings (show balance, notifications, etc.)
 *
 * SERVER DATA (wallet, transactions) should be fetched via React Query hooks:
 * - usePrimaryWallet() from @/src/hooks/useWalletQuery
 * - useInfiniteTransactions() from @/src/hooks/useTransactionQuery
 */
interface UIState {
  // Theme settings
  colorScheme: ColorScheme;
  isDark: boolean;

  // Language settings
  language: Language;

  // UI preferences
  showBalance: boolean;
  notificationsEnabled: boolean;
  biometricsEnabled: boolean;
  autoLockEnabled: boolean;
  autoLockTimeout: number; // in minutes

  // Actions
  setColorScheme: (scheme: ColorScheme) => void;
  setIsDark: (isDark: boolean) => void;
  toggleTheme: () => void;
  setLanguage: (language: Language) => void;
  setShowBalance: (show: boolean) => void;
  setNotificationsEnabled: (enabled: boolean) => void;
  setBiometricsEnabled: (enabled: boolean) => void;
  setAutoLockEnabled: (enabled: boolean) => void;
  setAutoLockTimeout: (timeout: number) => void;
  resetUI: () => void;
}

/**
 * Default values
 */
const defaults: Omit<UIState, 'actions'> = {
  colorScheme: 'system',
  isDark: Appearance.getColorScheme() === 'dark',
  language: 'en',
  showBalance: true,
  notificationsEnabled: true,
  biometricsEnabled: false,
  autoLockEnabled: true,
  autoLockTimeout: 5,
};

/**
 * UI Store
 *
 * Uses Zustand with persist middleware for client-side state.
 * This state is persisted to AsyncStorage but contains NO sensitive data.
 */
export const useUIStore = create<UIState>()(
  persist(
    (set, get) => ({
      // Initial state
      colorScheme: defaults.colorScheme,
      isDark: defaults.isDark,
      language: defaults.language,
      showBalance: defaults.showBalance,
      notificationsEnabled: defaults.notificationsEnabled,
      biometricsEnabled: defaults.biometricsEnabled,
      autoLockEnabled: defaults.autoLockEnabled,
      autoLockTimeout: defaults.autoLockTimeout,

      // Theme actions
      setColorScheme: (scheme: ColorScheme) => {
        set({ colorScheme: scheme });
        // Update isDark based on scheme
        if (scheme !== 'system') {
          set({ isDark: scheme === 'dark' });
        } else {
          set({ isDark: Appearance.getColorScheme() === 'dark' });
        }
      },

      setIsDark: (isDark: boolean) => {
        set({ isDark });
      },

      toggleTheme: () => {
        const { isDark } = get();
        set({ isDark: !isDark, colorScheme: !isDark ? 'dark' : 'light' });
      },

      // Language actions
      setLanguage: (language: Language) => {
        set({ language });
      },

      // UI preference actions
      setShowBalance: (show: boolean) => {
        set({ showBalance: show });
      },

      setNotificationsEnabled: (enabled: boolean) => {
        set({ notificationsEnabled: enabled });
      },

      setBiometricsEnabled: (enabled: boolean) => {
        set({ biometricsEnabled: enabled });
      },

      setAutoLockEnabled: (enabled: boolean) => {
        set({ autoLockEnabled: enabled });
      },

      setAutoLockTimeout: (timeout: number) => {
        set({ autoLockTimeout: timeout });
      },

      // Reset to defaults
      resetUI: () => {
        set({
          colorScheme: defaults.colorScheme,
          isDark: defaults.isDark,
          language: defaults.language,
          showBalance: defaults.showBalance,
          notificationsEnabled: defaults.notificationsEnabled,
          biometricsEnabled: defaults.biometricsEnabled,
          autoLockEnabled: defaults.autoLockEnabled,
          autoLockTimeout: defaults.autoLockTimeout,
        });
      },
    }),
    {
      name: 'ui-storage',
      storage: createJSONStorage(() => AsyncStorage),
      // Persist all UI settings (no sensitive data)
    }
  )
);

/**
 * Selectors for optimized re-renders
 */
export const selectColorScheme = (state: UIState) => state.colorScheme;
export const selectIsDark = (state: UIState) => state.isDark;
export const selectLanguage = (state: UIState) => state.language;
export const selectShowBalance = (state: UIState) => state.showBalance;
export const selectNotificationsEnabled = (state: UIState) => state.notificationsEnabled;
export const selectBiometricsEnabled = (state: UIState) => state.biometricsEnabled;
