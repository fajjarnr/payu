'use client';

export { useLogin, useLogout, useRefreshToken, useAuth } from './useAuth';
export {
  useBalance,
  useReserveBalance,
  useCommitReservation,
  useReleaseReservation,
  useCreditWallet,
  useTransactionHistory
} from './useWallet';
export { useTransactions, useTransaction, useInitiateTransfer, useProcessQrisPayment } from './useTransactions';
export { useWebSocket } from './useWebSocket';
export { useAnalyticsWebSocket } from './useAnalytics';
export {
  useActiveContent,
  useBanners,
  usePromos,
  useEmergencyAlerts,
  usePopups
} from './useCMS';
export { useExperiment } from './useExperiment';
export type { UseExperimentOptions, UseExperimentResult } from './useExperiment';
export { useUserSegment, useSegmentDetails, useAllSegments } from './useUserSegment';
export { useSegmentedOffers, useOffersBySegment, useVIPOffers } from './useSegmentedOffers';
export { useVIPStatus, type VIPStatus } from './useVIPStatus';
export {
  useFxRate,
  useAllFxRates,
  useFxEstimate,
  useFxConversion,
  useFxConversions,
  useFxConversion as useFxConversionById,
  useFxReverse
} from './useFx';

// ── Investment ──
export {
  useInvestmentAccount,
  useGoldHoldings,
  useCreateInvestmentAccount,
  useBuyDeposit,
  useBuyMutualFund,
  useBuyGold,
  useSellInvestment
} from './useInvestments';

// ── Lending ──
export {
  useCreditScore,
  useLoan,
  useRepaymentSchedule,
  usePayLater,
  usePayLaterTransactions,
  useActivePreApprovals,
  useApplyLoan,
  useActivatePayLater,
  useCheckPreApproval
} from './useLending';

// ── Notifications ──
export { useNotifications, useNotification, useMarkNotificationRead } from './useNotifications';

// ── Rewards & Promotions ──
export {
  useActivePromotions,
  useLoyaltyBalance,
  useLoyaltyPoints,
  useCashbacks,
  useCashbackSummary,
  useReferrals,
  useReferralSummary,
  useAccountRewards,
  useRewardsSummary,
  useClaimPromotion
} from './useRewards';

// ── Gamification ──
export {
  useGamificationSummary,
  useGamificationLevel,
  useGamificationBadges,
  useGamificationBadgeProgress,
  useGamificationStreak,
  useTodayCheckin,
  useCheckin,
  useRecordTransaction
} from './useGamification';

// ── Cards ──
export { useCards, useCard, useCreateCard, useFreezeCard, useUnfreezeCard } from './useCards';

// ── Pockets ──
export {
  usePockets,
  usePocket,
  usePocketsByCurrency,
  usePocketsTotalBalance,
  useCreatePocket,
  useCreditPocket,
  useDebitPocket,
  useFreezePocket,
  useUnfreezePocket,
  useClosePocket
} from './usePockets';

// ── Biometric ──
export {
  useBiometricChallenge,
  useBiometricRegistrations,
  useRegisterBiometric,
  useAuthenticateBiometric,
  useRevokeBiometric
} from './useBiometric';

// ── Scheduled Transfers ──
export {
  useScheduledTransfers,
  useScheduledTransfer,
  useCreateScheduledTransfer,
  useUpdateScheduledTransfer,
  useCancelScheduledTransfer,
  usePauseScheduledTransfer,
  useResumeScheduledTransfer
} from './useScheduledTransfers';

// ── Split Bill ──
export {
  useSplitBills,
  useSplitBill,
  useCreateSplitBill,
  useUpdateSplitBill,
  useCancelSplitBill,
  useActivateSplitBill,
  useAddParticipant,
  useAcceptSplitBill,
  useDeclineSplitBill,
  useSplitBillPayment,
  useSettleSplitBill
} from './useSplitBill';

// ── Support ──
export {
  useTrainingStatus,
  useSupportAgents,
  useSupportAgent,
  useCreateAgent,
  useUpdateAgentStatus,
  useTrainingModules,
  useMandatoryModules,
  useCreateModule,
  useUpdateModuleStatus,
  useAgentTrainings,
  useAgentTrainingStatus,
  useAssignTraining,
  useTickets,
  useCreateTicket
} from './useSupport';

// ── Compliance ──
export {
  useAuditReports,
  useAuditReport,
  useCreateAuditReport,
  useUserGdprAudits,
  useUserGdprAuditCount,
  useFailedAccessAudits,
  useCreateGdprAudit,
  useSearchGdprAudits,
  useDeleteGdprAudit
} from './useCompliance';

// ── Partner ──
export {
  usePartners,
  usePartner,
  useRegisterPartner,
  useUpdatePartner,
  useRegeneratePartnerKeys,
  useDeletePartner,
  usePartnerCertificates,
  useExpiringCertificates,
  useUploadCertificate,
  useGenerateCertificate,
  useRotateCertificate,
  useSnapBiAuthToken,
  useSnapBiPayment
} from './usePartner';

// ── User ──
export { useUser, useUpdateUser } from './useUser';
