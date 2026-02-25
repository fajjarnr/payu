'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import PromotionService from '@/services/PromotionService';

export function useGamificationSummary(userId: string) {
  return useQuery({
    queryKey: ['gamification-summary', userId],
    queryFn: () => PromotionService.getGamificationSummary(userId),
    enabled: !!userId,
  });
}

export function useGamificationLevel(userId: string) {
  return useQuery({
    queryKey: ['gamification-level', userId],
    queryFn: () => PromotionService.getGamificationLevel(userId),
    enabled: !!userId,
  });
}

export function useGamificationBadges(userId: string) {
  return useQuery({
    queryKey: ['gamification-badges', userId],
    queryFn: () => PromotionService.getBadges(userId),
    enabled: !!userId,
  });
}

export function useGamificationBadgeProgress(userId: string) {
  return useQuery({
    queryKey: ['gamification-badge-progress', userId],
    queryFn: () => PromotionService.getBadgeProgress(userId),
    enabled: !!userId,
  });
}

export function useGamificationStreak(userId: string) {
  return useQuery({
    queryKey: ['gamification-streak', userId],
    queryFn: () => PromotionService.getStreak(userId),
    enabled: !!userId,
  });
}

export function useTodayCheckin(userId: string) {
  return useQuery({
    queryKey: ['gamification-today', userId],
    queryFn: () => PromotionService.getTodayCheckin(userId),
    enabled: !!userId,
  });
}

export function useCheckin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, accountId }: { userId: string; accountId: string }) => PromotionService.checkin(accountId),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, { userId }) => {
      qc.invalidateQueries({ queryKey: ['gamification-summary', userId] });
      qc.invalidateQueries({ queryKey: ['gamification-streak', userId] });
      qc.invalidateQueries({ queryKey: ['gamification-today', userId] });
    },
  });
}

export function useRecordTransaction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, transactionId, amount }: { userId: string; transactionId: string; amount: number }) =>
      PromotionService.recordGamificationTransaction(transactionId, amount),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['gamification-summary', vars.userId] });
      qc.invalidateQueries({ queryKey: ['gamification-level', vars.userId] });
    },
  });
}
