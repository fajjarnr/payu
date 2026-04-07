'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import TransactionService from '@/services/TransactionService';
import { MutationPresets } from '@/lib/mutation-config';
import type { InitiateTransferRequest, ProcessQrisPaymentRequest, TransactionFilters } from '@/types';

export const useTransactions = (accountId: string | undefined, page = 0, size = 20, filters?: TransactionFilters) => {
  return useQuery({
    queryKey: ['transactions', accountId, page, size, filters],
    queryFn: () => TransactionService.getAccountTransactions(accountId!, page, size, filters),
    enabled: !!accountId,
    staleTime: 60000,
    gcTime: 300000
  });
};

export const useTransaction = (transactionId: string | undefined) => {
  return useQuery({
    queryKey: ['transaction', transactionId],
    queryFn: () => TransactionService.getTransaction(transactionId!),
    enabled: !!transactionId,
    staleTime: 120000,
    gcTime: 300000
  });
};

export const useInitiateTransfer = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: InitiateTransferRequest) => TransactionService.initiateTransfer(request),
    ...MutationPresets.financial,
    // BUG-FE-013: Scope invalidation to avoid invalidating all accounts
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (error) => {
      console.error('Transfer failed:', error);
    }
  });
};

export const useProcessQrisPayment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ProcessQrisPaymentRequest) => TransactionService.processQrisPayment(request),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (error) => {
      console.error('QRIS payment failed:', error);
    }
  });
};

export const useCancelTransaction = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (transactionId: string) => TransactionService.cancelTransaction(transactionId),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (error) => {
      console.error('Cancel transaction failed:', error);
    }
  });
};
