/**
 * Composition layer combining TanStack Query hooks with Zustand UI state.
 */

export { useAuth } from './useAuth';

export { useCards } from './useCards';

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

  useWallets,
  usePrimaryWallet,
  useWallet,
  useCreatePocket,
  useTransferToPocket,
  usePrefetchWallet,
  useRefreshWallets,
  walletKeys,

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
