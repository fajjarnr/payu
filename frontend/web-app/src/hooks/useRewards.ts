'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PromotionService from '@/services/PromotionService';
import type { ClaimPromotionRequest } from '@/services/PromotionService';

export function useActivePromotions() {
  return useQuery({
    queryKey: ['promotions'],
    queryFn: () => PromotionService.getActivePromotions(),
  });
}

export function useLoyaltyBalance(accountId: string) {
  return useQuery({
    queryKey: ['loyalty-balance', accountId],
    queryFn: () => PromotionService.getLoyaltyBalance(accountId),
    enabled: !!accountId,
  });
}

export function useLoyaltyPoints(accountId: string) {
  return useQuery({
    queryKey: ['loyalty-points', accountId],
    queryFn: () => PromotionService.getLoyaltyPoints(accountId),
    enabled: !!accountId,
  });
}

export function useCashbacks(accountId: string) {
  return useQuery({
    queryKey: ['cashbacks', accountId],
    queryFn: () => PromotionService.getCashbacks(accountId),
    enabled: !!accountId,
  });
}

export function useCashbackSummary(accountId: string) {
  return useQuery({
    queryKey: ['cashback-summary', accountId],
    queryFn: () => PromotionService.getCashback(accountId),
    enabled: !!accountId,
  });
}

export function useReferrals(accountId: string) {
  return useQuery({
    queryKey: ['referrals', accountId],
    queryFn: () => PromotionService.getReferrals(accountId),
    enabled: !!accountId,
  });
}

export function useReferralSummary(accountId: string) {
  return useQuery({
    queryKey: ['referral-summary', accountId],
    queryFn: () => PromotionService.getReferralSummary(accountId),
    enabled: !!accountId,
  });
}

export function useAccountRewards(accountId: string) {
  return useQuery({
    queryKey: ['rewards', accountId],
    queryFn: () => PromotionService.getAccountRewards(accountId),
    enabled: !!accountId,
  });
}

export function useRewardsSummary(accountId: string) {
  return useQuery({
    queryKey: ['rewards-summary', accountId],
    queryFn: () => PromotionService.getRewardsSummary(accountId),
    enabled: !!accountId,
  });
}

export function useClaimPromotion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ code, request }: { code: string; request: ClaimPromotionRequest }) =>
      PromotionService.claimPromotion(code, request),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['promotions'] });
      qc.invalidateQueries({ queryKey: ['rewards'] });
    },
  });
}
