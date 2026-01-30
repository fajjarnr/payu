import {
  useQuery,
  useMutation,
  useQueryClient,
  UseQueryOptions,
} from '@tanstack/react-query';
import { walletService } from '@/services/wallet.service';
import { Wallet, Pocket } from '@/types';

// Query keys
export const walletKeys = {
  all: ['wallet'] as const,
  lists: () => [...walletKeys.all, 'list'] as const,
  list: (filters: Record<string, any>) =>
    [...walletKeys.lists(), { filters }] as const,
  details: () => [...walletKeys.all, 'detail'] as const,
  detail: (id: string) => [...walletKeys.details(), id] as const,
  primary: () => [...walletKeys.all, 'primary'] as const,
  pockets: () => [...walletKeys.all, 'pockets'] as const,
};

// Types for mutations
interface TransferToPocketData {
  fromPocketId: string;
  toPocketId: string;
  amount: number;
  description?: string;
}

interface CreatePocketData {
  name: string;
  type: 'savings' | 'goals';
  initialBalance?: number;
}

/**
 * Hook to fetch all wallets
 */
export function useWallets(options?: UseQueryOptions<Wallet[], Error>) {
  return useQuery({
    queryKey: walletKeys.lists(),
    queryFn: () => walletService.getWallets(),
    ...options,
  });
}

/**
 * Hook to fetch primary wallet
 */
export function usePrimaryWallet(options?: UseQueryOptions<Wallet, Error>) {
  return useQuery({
    queryKey: walletKeys.primary(),
    queryFn: () => walletService.getPrimaryWallet(),
    staleTime: 1000 * 60 * 2, // 2 minutes - wallet data changes frequently
    ...options,
  });
}

/**
 * Hook to fetch a specific wallet by ID
 */
export function useWallet(
  walletId: string,
  options?: UseQueryOptions<Wallet, Error>
) {
  return useQuery({
    queryKey: walletKeys.detail(walletId),
    queryFn: () => walletService.getWallet(walletId),
    enabled: !!walletId,
    staleTime: 1000 * 60 * 2,
    ...options,
  });
}

/**
 * Hook to create a new pocket with optimistic update
 */
export function useCreatePocket() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['createPocket'],
    mutationFn: (data: CreatePocketData) => walletService.createPocket(data),
    onMutate: async (newPocket) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: walletKeys.lists() });

      // Snapshot previous value
      const previousWallets = queryClient.getQueryData<Wallet[]>(
        walletKeys.lists()
      );

      // Optimistically update to the new value
      if (previousWallets) {
        const optimisticWallet: Wallet = {
          id: `temp-${Date.now()}`,
          userId: '',
          balance: newPocket.initialBalance || 0,
          currency: 'IDR',
          pocketType: newPocket.type,
          createdAt: new Date().toISOString(),
        };

        queryClient.setQueryData<Wallet[]>(walletKeys.lists(), [
          ...previousWallets,
          optimisticWallet,
        ]);
      }

      return { previousWallets };
    },
    onError: (_err, _newPocket, context) => {
      // Rollback on error
      if (context?.previousWallets) {
        queryClient.setQueryData(walletKeys.lists(), context.previousWallets);
      }
    },
    onSettled: () => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: walletKeys.lists() });
      queryClient.invalidateQueries({ queryKey: walletKeys.pockets() });
    },
  });
}

/**
 * Hook for internal pocket transfer with optimistic update
 */
export function useTransferToPocket() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['transferToPocket'],
    mutationFn: (data: TransferToPocketData) =>
      walletService.transferToPocket(
        data.fromPocketId,
        data.toPocketId,
        data.amount,
        data.description
      ),
    onMutate: async (transferData) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: walletKeys.lists() });
      await queryClient.cancelQueries({ queryKey: walletKeys.primary() });

      // Snapshot previous values
      const previousWallets = queryClient.getQueryData<Wallet[]>(
        walletKeys.lists()
      );
      const previousPrimary = queryClient.getQueryData<Wallet>(
        walletKeys.primary()
      );

      // Optimistically update wallet balances
      if (previousWallets) {
        queryClient.setQueryData<Wallet[]>(
          walletKeys.lists(),
          previousWallets.map((wallet) => {
            if (wallet.id === transferData.fromPocketId) {
              return { ...wallet, balance: wallet.balance - transferData.amount };
            }
            if (wallet.id === transferData.toPocketId) {
              return { ...wallet, balance: wallet.balance + transferData.amount };
            }
            return wallet;
          })
        );
      }

      return { previousWallets, previousPrimary };
    },
    onError: (_err, _variables, context) => {
      // Rollback on error
      if (context?.previousWallets) {
        queryClient.setQueryData(walletKeys.lists(), context.previousWallets);
      }
      if (context?.previousPrimary) {
        queryClient.setQueryData(walletKeys.primary(), context.previousPrimary);
      }
    },
    onSettled: () => {
      // Invalidate all wallet queries
      queryClient.invalidateQueries({ queryKey: walletKeys.all });
      // Also invalidate transactions as transfer creates a transaction
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });
}

/**
 * Hook to prefetch wallet data (useful for navigation)
 */
export function usePrefetchWallet() {
  const queryClient = useQueryClient();

  return {
    prefetchWallets: () => {
      queryClient.prefetchQuery({
        queryKey: walletKeys.lists(),
        queryFn: () => walletService.getWallets(),
        staleTime: 1000 * 60 * 5,
      });
    },
    prefetchWallet: (walletId: string) => {
      queryClient.prefetchQuery({
        queryKey: walletKeys.detail(walletId),
        queryFn: () => walletService.getWallet(walletId),
        staleTime: 1000 * 60 * 5,
      });
    },
  };
}

/**
 * Hook to refresh all wallet data
 */
export function useRefreshWallets() {
  const queryClient = useQueryClient();

  return {
    refresh: async () => {
      await queryClient.invalidateQueries({ queryKey: walletKeys.all });
    },
    refreshPrimary: async () => {
      await queryClient.invalidateQueries({ queryKey: walletKeys.primary() });
    },
  };
}
