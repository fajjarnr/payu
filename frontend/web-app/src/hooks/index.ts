'use client';

export { useLogout, useRefreshToken, useAuth } from './useAuth';
export { useSilentRefresh } from './useSilentRefresh';
export {
  useBalance,
  useReserveBalance,
  useCommitReservation,
  useReleaseReservation,
  useTransactionHistory
} from './useWallet';
export { useTransactions, useTransaction, useInitiateTransfer, useProcessQrisPayment, useCancelTransaction } from './useTransactions';
export { useWebSocket } from './useWebSocket';
export { useAnalyticsWebSocket, useUserMetrics, useSpendingTrends, useCashFlow } from './useAnalytics';
export {
  useActiveContent,
  useBanners,
  usePromos,
  useEmergencyAlerts,
  usePopups
} from './useCMS';
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
  useCheckPreApproval,
  usePayLaterPayment
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

// ── Cards ──
export {
  useCards,
  useCard,
  useCreateCard,
  useFreezeCard,
  useUnfreezeCard,
  useDeleteCard,
  useUpdateCard
} from './useCards';

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
  useSnapBiPayment
} from './usePartner';

// ── User ──
export { useUser, useUpdateUser } from './useUser';
