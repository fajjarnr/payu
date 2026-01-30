import { act, renderHook } from '@testing-library/react-native';
import { useTransactionStore } from '../transactionStore';
import { transactionService } from '@/services/transaction.service';
import { Transaction, TransferData } from '@/types';

// Mock dependencies
jest.mock('@/services/transaction.service');

describe('transactionStore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset store state
    useTransactionStore.setState({
      transactions: [],
      isLoading: false,
      isLoadingMore: false,
      hasMore: true,
      page: 1,
      error: null,
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = useTransactionStore.getState();

      expect(state.transactions).toEqual([]);
      expect(state.isLoading).toBe(false);
      expect(state.isLoadingMore).toBe(false);
      expect(state.hasMore).toBe(true);
      expect(state.page).toBe(1);
      expect(state.error).toBeNull();
    });
  });

  describe('loadTransactions', () => {
    const mockTransactions: Transaction[] = [
      {
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
      },
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

    it('should set loading state when loading transactions', () => {
      (transactionService.getTransactions as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useTransactionStore());

      act(() => {
        result.current.loadTransactions();
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should update state on successful load', async () => {
      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: mockTransactions,
        hasMore: true,
        total: 25,
        page: 1,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions();
      });

      expect(transactionService.getTransactions).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
      });
      expect(result.current.transactions).toEqual(mockTransactions);
      expect(result.current.hasMore).toBe(true);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should reset page to 1 on refresh', async () => {
      useTransactionStore.setState({ page: 5 });
      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: mockTransactions,
        hasMore: true,
        total: 100,
        page: 1,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions(true);
      });

      expect(result.current.page).toBe(1);
    });

    it('should maintain current page when not refreshing', async () => {
      useTransactionStore.setState({ page: 3 });
      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: mockTransactions,
        hasMore: true,
        total: 60,
        page: 3,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions(false);
      });

      expect(result.current.page).toBe(3);
    });

    it('should handle hasMore false', async () => {
      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: mockTransactions.slice(0, 2),
        hasMore: false,
        total: 2,
        page: 1,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions();
      });

      expect(result.current.hasMore).toBe(false);
    });

    it('should handle error when loading fails', async () => {
      const errorMessage = 'Failed to fetch transactions';
      (transactionService.getTransactions as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions();
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.transactions).toEqual([]);
    });

    it('should use default error message when response is undefined', async () => {
      (transactionService.getTransactions as jest.Mock).mockRejectedValue(
        new Error('Network error')
      );

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions();
      });

      expect(result.current.error).toBe('Failed to load transactions');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle empty transactions array', async () => {
      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: [],
        hasMore: false,
        total: 0,
        page: 1,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions();
      });

      expect(result.current.transactions).toEqual([]);
      expect(result.current.hasMore).toBe(false);
    });
  });

  describe('loadMoreTransactions', () => {
    const existingTransactions: Transaction[] = [
      {
        id: 'txn-1',
        userId: 'user-123',
        type: 'transfer',
        amount: 100000,
        description: 'First transaction',
        status: 'completed',
        createdAt: '2024-01-15T10:00:00Z',
      },
    ];

    const newTransactions: Transaction[] = [
      {
        id: 'txn-2',
        userId: 'user-123',
        type: 'payment',
        amount: 50000,
        description: 'Second transaction',
        status: 'completed',
        createdAt: '2024-01-14T15:30:00Z',
      },
      {
        id: 'txn-3',
        userId: 'user-123',
        type: 'topup',
        amount: 500000,
        description: 'Third transaction',
        status: 'completed',
        createdAt: '2024-01-13T09:00:00Z',
      },
    ];

    it('should return early if already loading more', async () => {
      useTransactionStore.setState({ isLoadingMore: true });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(transactionService.getTransactions).not.toHaveBeenCalled();
    });

    it('should return early if no more transactions', async () => {
      useTransactionStore.setState({ hasMore: false });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(transactionService.getTransactions).not.toHaveBeenCalled();
    });

    it('should set isLoadingMore state', () => {
      useTransactionStore.setState({ hasMore: true, isLoadingMore: false });
      (transactionService.getTransactions as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useTransactionStore());

      act(() => {
        result.current.loadMoreTransactions();
      });

      expect(result.current.isLoadingMore).toBe(true);
    });

    it('should append new transactions to existing list', async () => {
      useTransactionStore.setState({
        transactions: existingTransactions,
        page: 1,
        hasMore: true,
      });

      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: newTransactions,
        hasMore: true,
        total: 25,
        page: 2,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(transactionService.getTransactions).toHaveBeenCalledWith({
        page: 2,
        pageSize: 20,
      });
      expect(result.current.transactions).toHaveLength(3);
      expect(result.current.transactions[0].id).toBe('txn-1');
      expect(result.current.transactions[1].id).toBe('txn-2');
      expect(result.current.transactions[2].id).toBe('txn-3');
      expect(result.current.page).toBe(2);
      expect(result.current.isLoadingMore).toBe(false);
    });

    it('should update hasMore when no more pages', async () => {
      useTransactionStore.setState({
        transactions: existingTransactions,
        page: 2,
        hasMore: true,
      });

      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: newTransactions.slice(0, 1),
        hasMore: false,
        total: 2,
        page: 3,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(result.current.hasMore).toBe(false);
      expect(result.current.page).toBe(3);
    });

    it('should handle error when loading more fails', async () => {
      useTransactionStore.setState({
        transactions: existingTransactions,
        page: 1,
        hasMore: true,
      });

      const errorMessage = 'Failed to load more transactions';
      (transactionService.getTransactions as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoadingMore).toBe(false);
      expect(result.current.transactions).toEqual(existingTransactions);
    });

    it('should use default error message when response is undefined', async () => {
      useTransactionStore.setState({
        transactions: existingTransactions,
        page: 1,
        hasMore: true,
      });

      (transactionService.getTransactions as jest.Mock).mockRejectedValue(
        new Error('Network error')
      );

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(result.current.error).toBe('Failed to load more transactions');
      expect(result.current.isLoadingMore).toBe(false);
    });
  });

  describe('transfer', () => {
    const transferData: TransferData = {
      amount: 100000,
      recipientAccount: '1234567890',
      recipientBank: 'BCA',
      description: 'Test transfer',
      fromPocket: 'pocket-1',
    };

    const mockTransaction: Transaction = {
      id: 'txn-new',
      userId: 'user-123',
      type: 'transfer',
      amount: 100000,
      description: 'Test transfer',
      status: 'completed',
      recipientName: 'John Doe',
      recipientAccount: '1234567890',
      fromPocket: 'pocket-1',
      createdAt: '2024-01-15T10:00:00Z',
      processedAt: '2024-01-15T10:01:00Z',
    };

    it('should set loading state when initiating transfer', () => {
      (transactionService.transfer as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useTransactionStore());

      act(() => {
        result.current.transfer(transferData);
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should add new transaction to the beginning of list on success', async () => {
      const existingTxn: Transaction = {
        id: 'txn-existing',
        userId: 'user-123',
        type: 'payment',
        amount: 50000,
        description: 'Existing transaction',
        status: 'completed',
        createdAt: '2024-01-14T10:00:00Z',
      };

      useTransactionStore.setState({ transactions: [existingTxn] });
      (transactionService.transfer as jest.Mock).mockResolvedValue(mockTransaction);

      const { result } = renderHook(() => useTransactionStore());

      let returnedTransaction: Transaction | undefined;

      await act(async () => {
        returnedTransaction = await result.current.transfer(transferData);
      });

      expect(transactionService.transfer).toHaveBeenCalledWith(transferData);
      expect(result.current.transactions).toHaveLength(2);
      expect(result.current.transactions[0]).toEqual(mockTransaction);
      expect(result.current.transactions[1]).toEqual(existingTxn);
      expect(result.current.isLoading).toBe(false);
      expect(returnedTransaction).toEqual(mockTransaction);
    });

    it('should handle transfer with scheduled date', async () => {
      const scheduledTransfer: TransferData = {
        ...transferData,
        scheduleDate: '2024-12-25',
      };

      const scheduledTransaction: Transaction = {
        ...mockTransaction,
        status: 'pending',
      };

      (transactionService.transfer as jest.Mock).mockResolvedValue(scheduledTransaction);

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.transfer(scheduledTransfer);
      });

      expect(transactionService.transfer).toHaveBeenCalledWith(scheduledTransfer);
      expect(result.current.transactions[0].status).toBe('pending');
    });

    it('should handle error when transfer fails', async () => {
      const errorMessage = 'Insufficient balance';
      (transactionService.transfer as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useTransactionStore());

      await expect(
        act(async () => {
          await result.current.transfer(transferData);
        })
      ).rejects.toBeDefined();

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.transactions).toHaveLength(0);
    });

    it('should use default error message when response is undefined', async () => {
      (transactionService.transfer as jest.Mock).mockRejectedValue(
        new Error('Network error')
      );

      const { result } = renderHook(() => useTransactionStore());

      await expect(
        act(async () => {
          await result.current.transfer(transferData);
        })
      ).rejects.toBeDefined();

      expect(result.current.error).toBe('Transfer failed');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle empty transactions list when adding new transaction', async () => {
      (transactionService.transfer as jest.Mock).mockResolvedValue(mockTransaction);

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.transfer(transferData);
      });

      expect(result.current.transactions).toHaveLength(1);
      expect(result.current.transactions[0]).toEqual(mockTransaction);
    });
  });

  describe('clearError', () => {
    it('should clear error state', () => {
      useTransactionStore.setState({ error: 'Some error' });

      const { result } = renderHook(() => useTransactionStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('pagination edge cases', () => {
    it('should handle multiple sequential page loads', async () => {
      const page1Data: Transaction[] = [
        {
          id: 'txn-1',
          userId: 'user-123',
          type: 'transfer',
          amount: 100000,
          description: 'Page 1',
          status: 'completed',
          createdAt: '2024-01-15T10:00:00Z',
        },
      ];

      const page2Data: Transaction[] = [
        {
          id: 'txn-2',
          userId: 'user-123',
          type: 'payment',
          amount: 50000,
          description: 'Page 2',
          status: 'completed',
          createdAt: '2024-01-14T10:00:00Z',
        },
      ];

      const page3Data: Transaction[] = [
        {
          id: 'txn-3',
          userId: 'user-123',
          type: 'topup',
          amount: 200000,
          description: 'Page 3',
          status: 'completed',
          createdAt: '2024-01-13T10:00:00Z',
        },
      ];

      (transactionService.getTransactions as jest.Mock)
        .mockResolvedValueOnce({
          items: page1Data,
          hasMore: true,
          total: 30,
          page: 1,
          pageSize: 10,
        })
        .mockResolvedValueOnce({
          items: page2Data,
          hasMore: true,
          total: 30,
          page: 2,
          pageSize: 10,
        })
        .mockResolvedValueOnce({
          items: page3Data,
          hasMore: false,
          total: 30,
          page: 3,
          pageSize: 10,
        });

      const { result } = renderHook(() => useTransactionStore());

      // Initial load
      await act(async () => {
        await result.current.loadTransactions();
      });

      expect(result.current.transactions).toHaveLength(1);
      expect(result.current.page).toBe(1);

      // Load more - page 2
      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(result.current.transactions).toHaveLength(2);
      expect(result.current.page).toBe(2);

      // Load more - page 3
      await act(async () => {
        await result.current.loadMoreTransactions();
      });

      expect(result.current.transactions).toHaveLength(3);
      expect(result.current.page).toBe(3);
      expect(result.current.hasMore).toBe(false);
    });

    it('should reset transactions on refresh', async () => {
      const oldTransactions: Transaction[] = [
        {
          id: 'txn-old',
          userId: 'user-123',
          type: 'transfer',
          amount: 100000,
          description: 'Old',
          status: 'completed',
          createdAt: '2024-01-10T10:00:00Z',
        },
      ];

      const newTransactions: Transaction[] = [
        {
          id: 'txn-new',
          userId: 'user-123',
          type: 'payment',
          amount: 50000,
          description: 'New',
          status: 'completed',
          createdAt: '2024-01-15T10:00:00Z',
        },
      ];

      useTransactionStore.setState({
        transactions: oldTransactions,
        page: 5,
        hasMore: false,
      });

      (transactionService.getTransactions as jest.Mock).mockResolvedValue({
        items: newTransactions,
        hasMore: true,
        total: 20,
        page: 1,
        pageSize: 20,
      });

      const { result } = renderHook(() => useTransactionStore());

      await act(async () => {
        await result.current.loadTransactions(true);
      });

      expect(result.current.transactions).toEqual(newTransactions);
      expect(result.current.page).toBe(1);
      expect(result.current.hasMore).toBe(true);
    });
  });
});
