import { renderHook, waitFor, act } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useTransactions,
  useInfiniteTransactions,
  useTransaction,
  useTransactionSummary,
  useCreateTransfer,
  useTopUp,
  usePayQRIS,
  useRefreshTransactions,
  transactionKeys,
} from '@/src/hooks/useTransactionQuery';
import { transactionService } from '@/services/transaction.service';
import { Transaction, TransferData } from '@/types';

// Mock dependencies
jest.mock('@/services/transaction.service');

describe('useTransactionQuery', () => {
  let queryClient: QueryClient;
  let wrapper: React.FC<{ children: React.ReactNode }>;

  const mockTransaction: Transaction = {
    id: 'txn-1',
    userId: 'user-123',
    type: 'transfer',
    amount: 100000,
    description: 'Transfer to friend',
    status: 'completed',
    recipientName: 'John Doe',
    recipientAccount: '1234567890',
    createdAt: '2024-01-15T10:00:00Z',
    processedAt: '2024-01-15T10:01:00Z',
  };

  const mockTransactions: Transaction[] = [
    mockTransaction,
    {
      id: 'txn-2',
      userId: 'user-123',
      type: 'payment',
      amount: 50000,
      description: 'QRIS Payment',
      category: 'Food',
      status: 'completed',
      createdAt: '2024-01-14T15:30:00Z',
      processedAt: '2024-01-14T15:30:30Z',
    },
    {
      id: 'txn-3',
      userId: 'user-123',
      type: 'topup',
      amount: 500000,
      description: 'Top up from bank',
      status: 'completed',
      createdAt: '2024-01-13T09:00:00Z',
      processedAt: '2024-01-13T09:05:00Z',
    },
  ];

  const mockPaginatedResponse = {
    items: mockTransactions,
    hasMore: true,
    total: 25,
    page: 1,
    pageSize: 20,
  };

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

  describe('transactionKeys', () => {
    it('should generate correct query keys', () => {
      expect(transactionKeys.all).toEqual(['transactions']);
      expect(transactionKeys.lists()).toEqual([['transactions'], 'list']);
      expect(transactionKeys.list({ type: 'transfer' })).toEqual([
        ['transactions'],
        'list',
        { filters: { type: 'transfer' } },
      ]);
      expect(transactionKeys.details()).toEqual([['transactions'], 'detail']);
      expect(transactionKeys.detail('txn-1')).toEqual([
        ['transactions'],
        'detail',
        'txn-1',
      ]);
      expect(transactionKeys.infinite()).toEqual([['transactions'], 'infinite']);
      expect(transactionKeys.summary()).toEqual([['transactions'], 'summary']);
    });
  });

  describe('useTransactions', () => {
    it('should fetch transactions with pagination successfully', async () => {
      (transactionService.getTransactions as jest.Mock).mockResolvedValue(
        mockPaginatedResponse
      );

      const { result } = renderHook(
        () => useTransactions({ page: 1, pageSize: 20 }),
        { wrapper }
      );

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(transactionService.getTransactions).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
      });
      expect(result.current.data).toEqual(mockPaginatedResponse);
    });

    it('should handle errors when fetching transactions fails', async () => {
      const errorMessage = 'Failed to fetch transactions';
      (transactionService.getTransactions as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      const { result } = renderHook(
        () => useTransactions({ page: 1 }),
        { wrapper }
      );

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toEqual(new Error(errorMessage));
    });
  });

  describe('useInfiniteTransactions', () => {
    it('should fetch first page of transactions successfully', async () => {
      (transactionService.getTransactions as jest.Mock).mockResolvedValue(
        mockPaginatedResponse
      );

      const { result } = renderHook(() => useInfiniteTransactions(), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(transactionService.getTransactions).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
      });
      expect(result.current.data?.pages[0]).toEqual(mockPaginatedResponse);
    });

    it('should fetch next page when fetchNextPage is called', async () => {
      const page2Response = {
        items: [mockTransactions[0]],
        hasMore: false,
        total: 4,
        page: 2,
        pageSize: 20,
      };

      (transactionService.getTransactions as jest.Mock)
        .mockResolvedValueOnce(mockPaginatedResponse)
        .mockResolvedValueOnce(page2Response);

      const { result } = renderHook(() => useInfiniteTransactions(), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      await act(async () => {
        await result.current.fetchNextPage();
      });

      await waitFor(() => expect(result.current.data?.pages).toHaveLength(2));

      expect(transactionService.getTransactions).toHaveBeenCalledWith({
        page: 2,
        pageSize: 20,
      });
    });

    it('should not fetch next page when hasMore is false', async () => {
      const noMoreResponse = {
        items: mockTransactions,
        hasMore: false,
        total: 3,
        page: 1,
        pageSize: 20,
      };

      (transactionService.getTransactions as jest.Mock).mockResolvedValue(
        noMoreResponse
      );

      const { result } = renderHook(() => useInfiniteTransactions(), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      await waitFor(() => expect(result.current.hasNextPage).toBe(false));

      await act(async () => {
        await result.current.fetchNextPage();
      });

      // Should only call once (initial fetch)
      expect(transactionService.getTransactions).toHaveBeenCalledTimes(1);
    });
  });

  describe('useTransaction', () => {
    it('should fetch specific transaction by ID successfully', async () => {
      (transactionService.getTransaction as jest.Mock).mockResolvedValue(
        mockTransaction
      );

      const { result } = renderHook(() => useTransaction('txn-1'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(transactionService.getTransaction).toHaveBeenCalledWith('txn-1');
      expect(result.current.data).toEqual(mockTransaction);
    });

    it('should not fetch when transactionId is empty', () => {
      const { result } = renderHook(() => useTransaction(''), { wrapper });

      expect(result.current.fetchStatus).toBe('idle');
      expect(transactionService.getTransaction).not.toHaveBeenCalled();
    });

    it('should handle errors when fetching transaction fails', async () => {
      const errorMessage = 'Transaction not found';
      (transactionService.getTransaction as jest.Mock).mockRejectedValue(
        new Error(errorMessage)
      );

      const { result } = renderHook(() => useTransaction('invalid-id'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toEqual(new Error(errorMessage));
    });
  });

  describe('useCreateTransfer', () => {
    const transferData: TransferData = {
      amount: 100000,
      recipientAccount: '1234567890',
      recipientBank: 'BCA',
      description: 'Test transfer',
      fromPocket: 'pocket-1',
    };

    it('should create transfer successfully', async () => {
      (transactionService.transfer as jest.Mock).mockResolvedValue(
        mockTransaction
      );

      const { result } = renderHook(() => useCreateTransfer(), { wrapper });

      await act(async () => {
        await result.current.mutate(transferData);
      });

      expect(transactionService.transfer).toHaveBeenCalledWith(transferData);
    });

    it('should invalidate wallet and transaction queries after mutation', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');
      (transactionService.transfer as jest.Mock).mockResolvedValue(
        mockTransaction
      );

      const { result } = renderHook(() => useCreateTransfer(), { wrapper });

      await act(async () => {
        await result.current.mutate(transferData);
      });

      await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalled());
    });
  });

  describe('useTopUp', () => {
    const topUpData = {
      amount: 500000,
      paymentMethod: 'bank_transfer',
    };

    it('should create top-up successfully', async () => {
      (transactionService.topUp as jest.Mock).mockResolvedValue(
        mockTransaction
      );

      const { result } = renderHook(() => useTopUp(), { wrapper });

      await act(async () => {
        await result.current.mutate(topUpData);
      });

      expect(transactionService.topUp).toHaveBeenCalledWith(topUpData);
    });
  });

  describe('usePayQRIS', () => {
    const qrisData = {
      amount: 50000,
      merchantName: 'Coffee Shop',
      qrisData: '00020101021226580016ID.CO.QRIS.WWW01189360052002000000000303UMI51440014ID.CO.QRIS.WWW0215ID10200200000000303UMI5204581253033605802ID5910Coffee Shop6007Jakarta6105101106304A1B2',
    };

    it('should create QRIS payment successfully', async () => {
      (transactionService.payQRIS as jest.Mock).mockResolvedValue(
        mockTransaction
      );

      const { result } = renderHook(() => usePayQRIS(), { wrapper });

      await act(async () => {
        await result.current.mutate(qrisData);
      });

      expect(transactionService.payQRIS).toHaveBeenCalledWith(qrisData);
    });
  });

  describe('useRefreshTransactions', () => {
    it('should refresh all transaction queries', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useRefreshTransactions(), {
        wrapper,
      });

      await act(async () => {
        await result.current.refresh();
      });

      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: transactionKeys.all,
      });
    });

    it('should refresh list with filters', async () => {
      const invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useRefreshTransactions(), {
        wrapper,
      });

      await act(async () => {
        await result.current.refreshList({ type: 'transfer' });
      });

      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: transactionKeys.list({ type: 'transfer' }),
      });
    });
  });
});
