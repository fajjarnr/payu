/**
 * Centralized Zustand store exports. Zustand holds UI/client state only; server state
 * lives in TanStack Query, and no store persists sensitive data such as tokens.
 * @see @/src/hooks/index.ts
 */

export {
  useUIStore,
  selectColorScheme,
  selectIsDark,
  selectLanguage,
  selectShowBalance,
  selectNotificationsEnabled,
  selectBiometricsEnabled,
  type ColorScheme,
  type Language,
} from './uiStore';

export {
  useCardUIStore,
  selectSelectedCardId,
  selectCardViewMode,
  selectShowCardDetails,
} from './cardUIStore';

// Auth UI State (deprecated, use @/src/hooks/useAuthQuery for auth state)
export {
  useAuthStore,
} from './authStore';
