import { useEffect } from 'react';
import { useTransactionStore } from '@/store/transactionStore';
import { TransferData } from '@/types';

export const useTransactions = () => {
  const {
    transactions,
    isLoading,
    isLoadingMore,
    hasMore,
    error,
    loadTransactions,
    loadMoreTransactions,
    transfer,
    clearError,
  } = useTransactionStore();

  useEffect(() => {
    loadTransactions(true);
  }, []);

  return {
    transactions,
    isLoading,
    isLoadingMore,
    hasMore,
    error,
    loadTransactions,
    loadMoreTransactions,
    transfer,
    clearError,
  };
};

export const useTransfer = () => {
  const { transfer, isLoading, error, clearError } = useTransactionStore();

  const performTransfer = async (data: TransferData) => {
    return await transfer(data);
  };

  return {
    transfer: performTransfer,
    isLoading,
    error,
    clearError,
  };
};
