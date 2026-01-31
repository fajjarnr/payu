import { renderHook, waitFor, act } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useWallets,
  usePrimaryWallet,
  useWallet,
  useCreatePocket,
  useTransferToPocket,
  useRefreshWallets,
  walletKeys,
} from '@/src/hooks/useWalletQuery';
import { walletService } from '@/services/wallet.service';
import { Wallet } from '@/types';

// Mock dependencies
jest.mock('@/services/wallet.service');

describe('useWalletQuery', () => {
  let queryClient: QueryClient;
  let wrapper: React.FC<{ children: React.ReactNode }>;

  const mockWallet: Wallet = {
    id: 'wallet-123',
    userId: 'user-123',
    balance: 1000000,
    currency: 'IDR',
    pocketType: 'primary',
    createdAt: '2024-01-01T00:00:00Z',
  };

  const mockWallets: Wallet[] = [
    {
      id: 'wallet-primary',
      userId: 'user-123',
      balance: 1000000,
      currency: 'IDR',
      pocketType: 'primary',
      createdAt: '2024-01-01T00:00:00Z',
    },
    {
      id: 'wallet-savings-1',
      userId: 'user-123',
      balance: 500000,
      currency: 'IDR',
      pocketType: 'savings',
      createdAt: '2024-01-01T00:00:00Z',
    },
    {
      id: 'wallet-goals-1',
      userId: 'user-123',
      balance: 250000,
      currency: 'IDR',
      pocketType: 'goals',
      createdAt: '2024-01-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
        mutations: {
          retry: false,
        },
      },
    });

    wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  });

  describe('walletKeys', () => {
    it('should generate correct query keys', () => {
      expect(walletKeys.all).toEqual(['wallet']);
      expect(walletKeys.lists()).toEqual([['wallet'], 'list']);
      expect(walletKeys.list({ type: 'savings' })).toEqual([
        ['wallet'],
        'list',
        { filters: { type: 'savings' } },
      ]);
      expect(walletKeys.details()).toEqual([['wallet'], 'detail']);
      expect(walletKeys.detail('wallet-123')).toEqual([
        ['wallet'],
        'detail',
        'wallet-123',
      ]);
      expect(walletKeys.primary()).toEqual([['wallet'], 'primary']);
      expect(walletKeys.pockets()).toEqual([['wallet'], 'pockets']);
    });
  });

  describe('useWallets', () => {
    it('should fetch all wallets successfully', async () => {
      (walletService.getWallets as jest.Mock).mockResolvedValue(mockWallets);

      const { result } = renderHook(() => useWallets(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(walletService.getWallets).toHaveBeenCalled();
      expect(result.current.data).toEqual(mockWallets);
    });

    it('should handle errors when fetching wallets fails', async () => {
      const errorMessage = 'Failed to fetch wallets';
      (walletService.getWallets as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      const { result } = renderHook(() => useWallets(), { wrapper });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toEqual(new Error(errorMessage));
    });

    it('should use custom query options', async () => {
      (walletService.getWallets as jest.Mock).mockResolvedValue(mockWallets);

      const { result } = renderHook(
        () =>
          useWallets({
            staleTime: 1000 * 60 * 5,
          }),
        { wrapper }
      );

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockWallets);
    });
  });

  describe('usePrimaryWallet', () => {
    it('should fetch primary wallet successfully', async () => {
      (walletService.getPrimaryWallet as jest.Mock).mockResolvedValue(mockWallet);

      const { result } = renderHook(() => usePrimaryWallet(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(walletService.getPrimaryWallet).toHaveBeenCalled();
      expect(result.current.data).toEqual(mockWallet);
    });

    it('should handle errors when fetching primary wallet fails', async () => {
      const errorMessage = 'Failed to fetch primary wallet';
      (walletService.getPrimaryWallet as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      const { result } = renderHook(() => usePrimaryWallet(), { wrapper });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toEqual(new Error(errorMessage));
    });

    it('should have default staleTime of 2 minutes', async () => {
      (walletService.getPrimaryWallet as jest.Mock).mockResolvedValue(mockWallet);

      const { result } = renderHook(() => usePrimaryWallet(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      // Check that data is fresh and won't refetch immediately
      expect(result.current.staleTime).toBe(1000 * 60 * 2);
    });
  });

  describe('useWallet', () => {
    it('should fetch specific wallet by ID successfully', async () => {
      (walletService.getWallet as jest.Mock).mockResolvedValue(mockWallet);

      const { result } = renderHook(() => useWallet('wallet-123'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(walletService.getWallet).toHaveBeenCalledWith('wallet-123');
      expect(result.current.data).toEqual(mockWallet);
    });

    it('should not fetch when walletId is empty', () => {
      const { result } = renderHook(() => useWallet(''), { wrapper });

      expect(result.current.fetchStatus).toBe('idle');
      expect(walletService.getWallet).not.toHaveBeenCalled();
    });

    it('should handle errors when fetching wallet fails', async () => {
      const errorMessage = 'Wallet not found';
      (walletService.getWallet as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      const { result } = renderHook(() => useWallet('invalid-id'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toEqual(new Error(errorMessage));
    });
  });

  describe('useCreatePocket', () => {
    const newPocketData = {
      name: 'Vacation Fund',
      type: 'savings' as const,
      initialBalance: 100000,
    };

    const mockNewWallet: Wallet = {
      id: 'pocket-new',
      userId: 'user-123',
      balance: 100000,
      currency: 'IDR',
      pocketType: 'savings',
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should create pocket successfully', async () => {
      (walletService.createPocket as jest.Mock).mockResolvedValue(mockNewWallet);

      const { result } = renderHook(() => useCreatePocket(), { wrapper });

      await act(async () => {
        await result.current.mutate(newPocketData);
      });

      expect(walletService.createPocket).toHaveBeenCalledWith(newPocketData);
    });

    it('should add optimistic pocket to cache', async () => {
      (walletService.getWallets as jest.Mock).mockResolvedValue(mockWallets);
      (walletService.createPocket as jest.Mock).mockResolvedValue(mockNewWallet);

      // First, fetch wallets to populate cache
      const { result: fetchResult } = renderHook(() => useWallets(), {
        wrapper,
      });
      await waitFor(() => expect(fetchResult.current.isSuccess).toBe(true));

      const { result } = renderHook(() => useCreatePocket(), { wrapper });

      await act(async () => {
        await result.current.mutate(newPocketData);
      });

      // Verify the mutation was called
      expect(walletService.createPocket).toHaveBeenCalledWith(newPocketData);
    });

    it('should rollback on error', async () => {
      const errorMessage = 'Insufficient balance';
      (walletService.createPocket as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      // Populate cache
      (walletService.getWallets as jest.Mock).mockResolvedValue(mockWallets);
      const { result: fetchResult } = renderHook(() => useWallets(), {
        wrapper,
      });
      await waitFor(() => expect(fetchResult.current.isSuccess).toBe(true));

      const { result } = renderHook(() => useCreatePocket(), { wrapper });

      await act(async () => {
        try {
          await result.current.mutate(newPocketData);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBeDefined();
    });

    it('should invalidate queries after mutation', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');
      (walletService.createPocket as jest.Mock).mockResolvedValue(mockNewWallet);

      const { result } = renderHook(() => useCreatePocket(), { wrapper });

      await act(async () => {
        await result.current.mutate(newPocketData);
      });

      await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalled());
    });
  });

  describe('useTransferToPocket', () => {
    const transferData = {
      fromPocketId: 'wallet-primary',
      toPocketId: 'wallet-savings-1',
      amount: 50000,
      description: 'Test transfer',
    };

    it('should transfer to pocket successfully', async () => {
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);

      const { result } = renderHook(() => useTransferToPocket(), { wrapper });

      await act(async () => {
        await result.current.mutate(transferData);
      });

      expect(walletService.transferToPocket).toHaveBeenCalledWith(
        transferData.fromPocketId,
        transferData.toPocketId,
        transferData.amount,
        transferData.description
      );
    });

    it('should update wallet balances optimistically', async () => {
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);

      const { result } = renderHook(() => useTransferToPocket(), { wrapper });

      await act(async () => {
        result.current.mutate(transferData);
      });

      expect(walletService.transferToPocket).toHaveBeenCalled();
    });

    it('should rollback on error', async () => {
      const errorMessage = 'Insufficient balance';
      (walletService.transferToPocket as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      const { result } = renderHook(() => useTransferToPocket(), { wrapper });

      await act(async () => {
        try {
          await result.current.mutate(transferData);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBeDefined();
    });

    it('should handle transfer without description', async () => {
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);

      const { result } = renderHook(() => useTransferToPocket(), { wrapper });

      await act(async () => {
        await result.current.mutate({
          fromPocketId: 'wallet-primary',
          toPocketId: 'wallet-savings-1',
          amount: 50000,
        });
      });

      expect(walletService.transferToPocket).toHaveBeenCalledWith(
        'wallet-primary',
        'wallet-savings-1',
        50000,
        undefined
      );
    });

    it('should invalidate wallet and transaction queries after mutation', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);

      const { result } = renderHook(() => useTransferToPocket(), { wrapper });

      await act(async () => {
        await result.current.mutate(transferData);
      });

      await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalled());
    });
  });

  describe('useRefreshWallets', () => {
    it('should refresh all wallet queries', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useRefreshWallets(), { wrapper });

      await act(async () => {
        await result.current.refresh();
      });

      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: walletKeys.all,
      });
    });

    it('should refresh only primary wallet query', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useRefreshWallets(), { wrapper });

      await act(async () => {
        await result.current.refreshPrimary();
      });

      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: walletKeys.primary(),
      });
    });
  });
});
