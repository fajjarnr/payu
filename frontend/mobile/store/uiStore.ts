import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Appearance } from 'react-native';

export type ColorScheme = 'light' | 'dark' | 'system';

export type Language = 'en' | 'id';

/**
 * Client-side UI state (theme, language, UI settings). Server data (wallet,
 * transactions) comes from React Query hooks.
 */
interface UIState {
  colorScheme: ColorScheme;
  isDark: boolean;

  language: Language;

  showBalance: boolean;
  notificationsEnabled: boolean;
  biometricsEnabled: boolean;
  autoLockEnabled: boolean;
  autoLockTimeout: number; // in minutes

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

/** Persists client-side UI state to AsyncStorage; contains no sensitive data. */
export const useUIStore = create<UIState>()(
  persist(
    (set, get) => ({
      colorScheme: defaults.colorScheme,
      isDark: defaults.isDark,
      language: defaults.language,
      showBalance: defaults.showBalance,
      notificationsEnabled: defaults.notificationsEnabled,
      biometricsEnabled: defaults.biometricsEnabled,
      autoLockEnabled: defaults.autoLockEnabled,
      autoLockTimeout: defaults.autoLockTimeout,

      setColorScheme: (scheme: ColorScheme) => {
        set({ colorScheme: scheme });
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

      setLanguage: (language: Language) => {
        set({ language });
      },

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
