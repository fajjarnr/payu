import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import TransactionService from '@/services/TransactionService';
import type { InitiateTransferRequest, ProcessQrisPaymentRequest } from '@/types';

export const useTransactions = (accountId: string | undefined, page = 0, size = 20) => {
  return useQuery({
    queryKey: ['transactions', accountId, page, size],
    queryFn: () => TransactionService.getAccountTransactions(accountId!, page, size),
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (error) => {
      console.error('QRIS payment failed:', error);
    }
  });
};
