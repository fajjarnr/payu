/**
 * Custom Hooks Index
 *
 * This file exports unified hooks that compose TanStack Query hooks
 * with Zustand UI state for a clean API.
 *
 * ARCHITECTURE:
 * - TanStack Query hooks (src/hooks/): Server state management
 * - Zustand stores (store/): UI state management
 * - These hooks: Composition layer combining both
 *
 * @see @/src/hooks/index.ts for TanStack Query hooks
 * @see @/store/index.ts for Zustand stores
 */

// Auth hook (unified)
export { useAuth } from './useAuth';

// Card hook (unified)
export { useCards } from './useCards';

// Other custom hooks
export { useAnalytics } from './useAnalytics';
export { useAppLock } from './useAppLock';
export { useBiometrics } from './useBiometrics';
export { useCamera } from './useCamera';
export { useCancellableEffect } from './useCancellableEffect';
export { useFeedback } from './useFeedback';
export { useNotifications } from './useNotifications';
export { useOfflineMode } from './useOfflineMode';

// Re-export TanStack Query hooks for convenience
export {
  // Auth
  useLogin,
  useRegister,
  useLogout,
  useRefreshToken,
  useRequestPasswordReset,
  useResetPassword,
  useChangePassword,
  useVerifyEmail,
  useAuthState,
  useInitializeAuth,
  authKeys,

  // Wallet
  useWallets,
  usePrimaryWallet,
  useWallet,
  useCreatePocket,
  useTransferToPocket,
  usePrefetchWallet,
  useRefreshWallets,
  walletKeys,

  // Transactions
  useTransactions,
  useInfiniteTransactions,
  useTransaction,
  useTransactionSummary,
  useCreateTransfer,
  useTopUp,
  usePayQRIS,
  usePrefetchTransaction,
  useRefreshTransactions,
  transactionKeys,
} from '@/src/hooks';

// Re-export card query hooks
export {
  useCards as useCardsQuery,
  useCreateCard,
  useCardActions,
  CARD_KEYS,
} from './useCardQuery';
