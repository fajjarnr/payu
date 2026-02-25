'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import WalletService from '@/services/WalletService';
import type { ReserveBalanceRequest } from '@/services/WalletService';

export const useBalance = (accountId: string | undefined) => {
  return useQuery({
    queryKey: ['wallet-balance', accountId],
    queryFn: () => WalletService.getBalance(accountId!),
    enabled: !!accountId,
    staleTime: 30000,
    gcTime: 300000
  });
};

export const useReserveBalance = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ accountId, request }: { accountId: string; request: ReserveBalanceRequest }) =>
      WalletService.reserveBalance(accountId, request),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
    }
  });
};

export const useCommitReservation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reservationId: string) => WalletService.commitReservation(reservationId),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    }
  });
};

export const useReleaseReservation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reservationId: string) => WalletService.releaseReservation(reservationId),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
    }
  });
};

// XBUG-007: useCreditWallet removed — credit endpoint is internal-only and should not be called from frontend.

export const useTransactionHistory = (accountId: string | undefined, page = 0, size = 20) => {
  return useQuery({
    queryKey: ['wallet-transactions', accountId, page, size],
    queryFn: () => WalletService.getTransactionHistory(accountId!, page, size),
    enabled: !!accountId,
    staleTime: 60000,
    gcTime: 300000
  });
};
