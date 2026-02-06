/**
 * Store Index - Centralized State Management Exports
 *
 * This file provides a centralized export for all Zustand stores.
 * Each store has a specific responsibility:
 *
 * - UI State (Zustand): Theme, language, view preferences, selections
 * - Server State (TanStack Query): User data, transactions, wallets, cards
 *
 * GUIDELINES:
 * 1. Use Zustand stores for UI/client state only
 * 2. Use TanStack Query hooks for server state (API data)
 * 3. Never duplicate server state in Zustand
 * 4. Never store sensitive data (tokens) in either store
 *
 * @see @/src/hooks/index.ts for TanStack Query hooks
 */

// UI State Stores
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
