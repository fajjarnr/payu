import {
  useQuery,
  useMutation,
  useQueryClient,
  useInfiniteQuery,
  UseQueryOptions,
} from '@tanstack/react-query';
import { transactionService } from '@/services/transaction.service';
import { Transaction, TransferData, QRISData, PaginatedResponse } from '@/types';

// Query keys
export const transactionKeys = {
  all: ['transactions'] as const,
  lists: () => [...transactionKeys.all, 'list'] as const,
  list: (filters: Record<string, any>) =>
    [...transactionKeys.lists(), { filters }] as const,
  details: () => [...transactionKeys.all, 'detail'] as const,
  detail: (id: string) => [...transactionKeys.details(), id] as const,
  infinite: () => [...transactionKeys.all, 'infinite'] as const,
  summary: () => [...transactionKeys.all, 'summary'] as const,
  summaryWithParams: (params: { period?: 'week' | 'month' | 'year' }) =>
    [...transactionKeys.summary(), params] as const,
};

// Types for mutations
interface TopUpData {
  amount: number;
  paymentMethod: string;
}

interface TransactionFilters {
  type?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}

const DEFAULT_PAGE_SIZE = 20;

/**
 * Hook to fetch transactions with pagination
 */
export function useTransactions(
  params?: {
    page?: number;
    pageSize?: number;
    type?: string;
    status?: string;
  },
  options?: UseQueryOptions<PaginatedResponse<Transaction>, Error>
) {
  return useQuery({
    queryKey: transactionKeys.list(params || {}),
    queryFn: () => transactionService.getTransactions(params),
    staleTime: 1000 * 60 * 2, // 2 minutes
    ...options,
  });
}

/**
 * Hook for infinite scroll transactions
 */
export function useInfiniteTransactions(
  filters?: TransactionFilters,
  pageSize: number = DEFAULT_PAGE_SIZE
) {
  return useInfiniteQuery({
    queryKey: transactionKeys.infinite(),
    queryFn: async ({ pageParam = 1 }) => {
      const response = await transactionService.getTransactions({
        page: pageParam,
        pageSize,
        ...filters,
      });
      return response;
    },
    getNextPageParam: (lastPage) => {
      if (!lastPage.hasMore) return undefined;
      return lastPage.page + 1;
    },
    getPreviousPageParam: (firstPage) => {
      if (firstPage.page <= 1) return undefined;
      return firstPage.page - 1;
    },
    initialPageParam: 1,
    staleTime: 1000 * 60 * 2,
  });
}

/**
 * Hook to fetch a specific transaction by ID
 */
export function useTransaction(
  transactionId: string,
  options?: UseQueryOptions<Transaction, Error>
) {
  return useQuery({
    queryKey: transactionKeys.detail(transactionId),
    queryFn: () => transactionService.getTransaction(transactionId),
    enabled: !!transactionId,
    staleTime: 1000 * 60 * 5, // 5 minutes - transactions don't change
    ...options,
  });
}

/**
 * Hook to fetch transaction summary
 */
export function useTransactionSummary(
  params?: { period?: 'week' | 'month' | 'year' },
  options?: UseQueryOptions<
    {
      totalIncome: number;
      totalExpense: number;
      transactionCount: number;
    },
    Error
  >
) {
  return useQuery({
    queryKey: transactionKeys.summaryWithParams(params || {}),
    queryFn: () => transactionService.getTransactionSummary(params),
    staleTime: 1000 * 60 * 5, // 5 minutes
    ...options,
  });
}

/**
 * Hook for creating a transfer with optimistic update
 */
export function useCreateTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['createTransfer'],
    mutationFn: (data: TransferData) => transactionService.transfer(data),
    onMutate: async (transferData) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: transactionKeys.lists() });
      await queryClient.cancelQueries({ queryKey: ['wallet'] });

      // Snapshot previous values
      const previousTransactions = queryClient.getQueryData(
        transactionKeys.list({})
      ) as PaginatedResponse<Transaction> | undefined;

      // Create optimistic transaction
      const optimisticTransaction: Transaction = {
        id: `temp-${Date.now()}`,
        userId: '',
        type: 'transfer',
        amount: transferData.amount,
        description: transferData.description,
        status: 'pending',
        fromPocket: transferData.fromPocket,
        recipientName: transferData.recipientAccount,
        recipientAccount: transferData.recipientAccount,
        createdAt: new Date().toISOString(),
      };

      // Optimistically add to list
      if (previousTransactions) {
        queryClient.setQueryData(
          transactionKeys.list({}),
          {
            ...previousTransactions,
            items: [optimisticTransaction, ...previousTransactions.items],
            total: previousTransactions.total + 1,
          }
        );
      }

      return { previousTransactions };
    },
    onError: (_err, _variables, context) => {
      // Rollback on error
      if (context?.previousTransactions) {
        queryClient.setQueryData(
          transactionKeys.list({}),
          context.previousTransactions
        );
      }
    },
    onSettled: () => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: transactionKeys.all });
      queryClient.invalidateQueries({ queryKey: ['wallet'] });
    },
  });
}

/**
 * Hook for top-up with optimistic update
 */
export function useTopUp() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['topUp'],
    mutationFn: (data: TopUpData) =>
      transactionService.topUp(data.amount, data.paymentMethod),
    onMutate: async (topUpData) => {
      await queryClient.cancelQueries({ queryKey: transactionKeys.lists() });
      await queryClient.cancelQueries({ queryKey: ['wallet'] });

      const previousTransactions = queryClient.getQueryData(
        transactionKeys.list({})
      ) as PaginatedResponse<Transaction> | undefined;

      const optimisticTransaction: Transaction = {
        id: `temp-${Date.now()}`,
        userId: '',
        type: 'topup',
        amount: topUpData.amount,
        description: `Top up via ${topUpData.paymentMethod}`,
        status: 'pending',
        createdAt: new Date().toISOString(),
      };

      if (previousTransactions) {
        queryClient.setQueryData(
          transactionKeys.list({}),
          {
            ...previousTransactions,
            items: [optimisticTransaction, ...previousTransactions.items],
            total: previousTransactions.total + 1,
          }
        );
      }

      return { previousTransactions };
    },
    onError: (_err, _variables, context) => {
      if (context?.previousTransactions) {
        queryClient.setQueryData(
          transactionKeys.list({}),
          context.previousTransactions
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: transactionKeys.all });
      queryClient.invalidateQueries({ queryKey: ['wallet'] });
    },
  });
}

/**
 * Hook for QRIS payment with optimistic update
 */
export function usePayQRIS() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['payQRIS'],
    mutationFn: (data: QRISData) => transactionService.payQRIS(data),
    onMutate: async (qrisData) => {
      await queryClient.cancelQueries({ queryKey: transactionKeys.lists() });
      await queryClient.cancelQueries({ queryKey: ['wallet'] });

      const previousTransactions = queryClient.getQueryData(
        transactionKeys.list({})
      ) as PaginatedResponse<Transaction> | undefined;

      const optimisticTransaction: Transaction = {
        id: `temp-${Date.now()}`,
        userId: '',
        type: 'qris',
        amount: qrisData.amount,
        description: `QRIS payment to ${qrisData.merchantName}`,
        status: 'pending',
        recipientName: qrisData.merchantName,
        createdAt: new Date().toISOString(),
      };

      if (previousTransactions) {
        queryClient.setQueryData(
          transactionKeys.list({}),
          {
            ...previousTransactions,
            items: [optimisticTransaction, ...previousTransactions.items],
            total: previousTransactions.total + 1,
          }
        );
      }

      return { previousTransactions };
    },
    onError: (_err, _variables, context) => {
      if (context?.previousTransactions) {
        queryClient.setQueryData(
          transactionKeys.list({}),
          context.previousTransactions
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: transactionKeys.all });
      queryClient.invalidateQueries({ queryKey: ['wallet'] });
    },
  });
}

/**
 * Hook to prefetch transaction data (useful for navigation)
 */
export function usePrefetchTransaction() {
  const queryClient = useQueryClient();

  return {
    prefetchTransactions: () => {
      queryClient.prefetchQuery({
        queryKey: transactionKeys.list({}),
        queryFn: () => transactionService.getTransactions(),
      });
    },
    prefetchTransaction: (transactionId: string) => {
      queryClient.prefetchQuery({
        queryKey: transactionKeys.detail(transactionId),
        queryFn: () => transactionService.getTransaction(transactionId),
      });
    },
  };
}

/**
 * Hook to refresh all transaction data
 */
export function useRefreshTransactions() {
  const queryClient = useQueryClient();

  return {
    refresh: async () => {
      await queryClient.invalidateQueries({ queryKey: transactionKeys.all });
    },
    refreshList: async (filters?: Record<string, any>) => {
      await queryClient.invalidateQueries({
        queryKey: transactionKeys.list(filters || {}),
      });
    },
  };
}
