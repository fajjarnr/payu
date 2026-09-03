import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useTransactions,
  useTransaction,
  useInitiateTransfer,
  useProcessQrisPayment
} from '@/hooks/useTransactions';
import TransactionService from '@/services/TransactionService';
import type { Transaction, TransactionType } from '@/types';
import { asMoney } from '@/lib/currency';

// Mock TransactionService
vi.mock('@/services/TransactionService');

describe('useTransactions hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockTransactions: Transaction[] = [
    {
      id: 'txn-1',
      referenceNumber: 'REF-001',
      senderAccountId: 'account-1',
      recipientAccountId: 'account-2',
      type: 'INTERNAL_TRANSFER' as TransactionType,
      amount: asMoney('100000'),
      currency: 'IDR',
      description: 'Test transfer',
      status: 'COMPLETED',
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:00:00Z',
      completedAt: '2024-01-01T10:01:00Z'
    },
    {
      id: 'txn-2',
      referenceNumber: 'REF-002',
      senderAccountId: 'account-1',
      recipientAccountId: 'account-3',
      type: 'BILL_PAYMENT' as TransactionType,
      amount: asMoney('50000'),
      currency: 'IDR',
      description: 'Bill payment',
      status: 'PENDING',
      createdAt: '2024-01-02T10:00:00Z',
      updatedAt: '2024-01-02T10:00:00Z'
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useTransactions).toBeDefined();
  });

  it('should fetch transactions successfully', async () => {
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue(mockTransactions);

    const { result } = renderHook(() => useTransactions('account-1'), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(TransactionService.getAccountTransactions).toHaveBeenCalledWith('account-1', 0, 20, undefined);
    expect(result.current.isSuccess).toBe(true);
    expect(result.current.data).toEqual(mockTransactions);
  });

  it('should not fetch when accountId is undefined', () => {
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue(mockTransactions);

    const { result } = renderHook(() => useTransactions(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(TransactionService.getAccountTransactions).not.toHaveBeenCalled();
  });

  it('should support pagination parameters', async () => {
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue(mockTransactions);

    const { result } = renderHook(() => useTransactions('account-1', 1, 50), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(TransactionService.getAccountTransactions).toHaveBeenCalledWith('account-1', 1, 50, undefined);
  });

  it('should handle fetch errors', async () => {
    const error = new Error('Failed to fetch transactions');
    vi.mocked(TransactionService.getAccountTransactions).mockRejectedValue(error);

    const { result } = renderHook(() => useTransactions('account-1'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('should respect stale time configuration', async () => {
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue(mockTransactions);

    const { result } = renderHook(() => useTransactions('account-1'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const callCount = vi.mocked(TransactionService.getAccountTransactions).mock.calls.length;

    // Immediate refetch should use cache (staleTime: 60 seconds)
    const { result: result2 } = renderHook(() => useTransactions('account-1'), { wrapper });

    await waitFor(() => {
      expect(result2.current.isSuccess).toBe(true);
    });

    expect(vi.mocked(TransactionService.getAccountTransactions).mock.calls.length).toBe(callCount);
  });

  it('should return empty array when no transactions', async () => {
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue([]);

    const { result } = renderHook(() => useTransactions('account-1'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual([]);
  });
});

describe('useTransaction hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockTransaction: Transaction = {
    id: 'txn-1',
    referenceNumber: 'REF-001',
    senderAccountId: 'account-1',
    recipientAccountId: 'account-2',
    type: 'INTERNAL_TRANSFER' as TransactionType,
    amount: asMoney('100000'),
    currency: 'IDR',
    description: 'Test transfer',
    status: 'COMPLETED',
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-01-01T10:00:00Z',
    completedAt: '2024-01-01T10:01:00Z'
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useTransaction).toBeDefined();
  });

  it('should fetch transaction by ID successfully', async () => {
    vi.mocked(TransactionService.getTransaction).mockResolvedValue(mockTransaction);

    const { result } = renderHook(() => useTransaction('txn-1'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(TransactionService.getTransaction).toHaveBeenCalledWith('txn-1');
    expect(result.current.data).toEqual(mockTransaction);
  });

  it('should not fetch when transactionId is undefined', () => {
    vi.mocked(TransactionService.getTransaction).mockResolvedValue(mockTransaction);

    const { result } = renderHook(() => useTransaction(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(TransactionService.getTransaction).not.toHaveBeenCalled();
  });

  it('should handle errors', async () => {
    const error = new Error('Transaction not found');
    vi.mocked(TransactionService.getTransaction).mockRejectedValue(error);

    const { result } = renderHook(() => useTransaction('invalid-id'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });
});

describe('useInitiateTransfer hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockTransferRequest = {
    senderAccountId: 'account-1',
    recipientAccountNumber: '1234567890',
    amount: asMoney('100000'),
    description: 'Test transfer',
    type: 'INTERNAL_TRANSFER' as TransactionType,
    transactionPin: '123456'
  };

  const mockTransferResponse = {
    transactionId: 'txn-new',
    referenceNumber: 'REF-NEW',
    status: 'PENDING',
    fee: asMoney('5000'),
    estimatedCompletionTime: '2024-01-01T10:05:00Z'
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useInitiateTransfer).toBeDefined();
  });

  it('should initiate transfer successfully', async () => {
    vi.mocked(TransactionService.initiateTransfer).mockResolvedValue(mockTransferResponse);

    const { result } = renderHook(() => useInitiateTransfer(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync(mockTransferRequest);
    });

    expect(TransactionService.initiateTransfer).toHaveBeenCalledWith(mockTransferRequest);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockTransferResponse);
  });

  it('should invalidate wallet-balance query on success', async () => {
    vi.mocked(TransactionService.initiateTransfer).mockResolvedValue(mockTransferResponse);

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useInitiateTransfer(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync(mockTransferRequest);
    });

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['wallet-balance'] });
  });

  it('should invalidate transactions query on success', async () => {
    vi.mocked(TransactionService.initiateTransfer).mockResolvedValue(mockTransferResponse);

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useInitiateTransfer(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync(mockTransferRequest);
    });

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['transactions'] });
  });

  it('should handle transfer errors', async () => {
    const error = new Error('Insufficient balance');
    vi.mocked(TransactionService.initiateTransfer).mockRejectedValue(error);

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const { result } = renderHook(() => useInitiateTransfer(), { wrapper });

    let transferError: Error | null = null;
    try {
      await act(async () => {
        await result.current.mutateAsync(mockTransferRequest);
      });
    } catch (e) {
      transferError = e as Error;
    }

    expect(transferError).toBeTruthy();
    expect(consoleErrorSpy).toHaveBeenCalledWith('Transfer failed:', 'Insufficient balance');

    consoleErrorSpy.mockRestore();
  });

  it('should have loading state during mutation', async () => {
    vi.mocked(TransactionService.initiateTransfer).mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => resolve(mockTransferResponse), 100);
        })
    );

    const { result } = renderHook(() => useInitiateTransfer(), { wrapper });

    act(() => {
      result.current.mutate(mockTransferRequest);
    });

    await waitFor(() => {
      expect(result.current.isPending).toBe(true);
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });
});

describe('useProcessQrisPayment hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockQrisRequest = {
    qrCode: '00020101021226580016ID.CO.QRIS.WWW01189360052002000000000303UMI51440014ID.CO.QRIS.WWW0215ID10200200000000303UMI5204581253033605802ID5910PayU Demo6007Jakarta6105101106304A1B2',
    amount: asMoney('50000'),
    accountId: 'account-1'
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useProcessQrisPayment).toBeDefined();
  });

  it('should process QRIS payment successfully', async () => {
    vi.mocked(TransactionService.processQrisPayment).mockResolvedValue(undefined);

    const { result } = renderHook(() => useProcessQrisPayment(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync(mockQrisRequest);
    });

    expect(TransactionService.processQrisPayment).toHaveBeenCalledWith(mockQrisRequest);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });

  it('should invalidate wallet-balance query on success', async () => {
    vi.mocked(TransactionService.processQrisPayment).mockResolvedValue(undefined);

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useProcessQrisPayment(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync(mockQrisRequest);
    });

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['wallet-balance'] });
  });

  it('should invalidate transactions query on success', async () => {
    vi.mocked(TransactionService.processQrisPayment).mockResolvedValue(undefined);

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useProcessQrisPayment(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync(mockQrisRequest);
    });

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['transactions'] });
  });

  it('should handle QRIS payment errors', async () => {
    const error = new Error('Invalid QR code');
    vi.mocked(TransactionService.processQrisPayment).mockRejectedValue(error);

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const { result } = renderHook(() => useProcessQrisPayment(), { wrapper });

    let qrisError: Error | null = null;
    try {
      await act(async () => {
        await result.current.mutateAsync(mockQrisRequest);
      });
    } catch (e) {
      qrisError = e as Error;
    }

    expect(qrisError).toBeTruthy();
    expect(consoleErrorSpy).toHaveBeenCalledWith('QRIS payment failed:', 'Invalid QR code');

    consoleErrorSpy.mockRestore();
  });

  it('should have loading state during mutation', async () => {
    vi.mocked(TransactionService.processQrisPayment).mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => resolve(undefined), 100);
        })
    );

    const { result } = renderHook(() => useProcessQrisPayment(), { wrapper });

    act(() => {
      result.current.mutate(mockQrisRequest);
    });

    await waitFor(() => {
      expect(result.current.isPending).toBe(true);
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });
});

describe('Transaction hooks integration', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockTransactions: Transaction[] = [
    {
      id: 'txn-1',
      referenceNumber: 'REF-001',
      senderAccountId: 'account-1',
      recipientAccountId: 'account-2',
      type: 'INTERNAL_TRANSFER' as TransactionType,
    amount: asMoney('100000'),
      currency: 'IDR',
      description: 'Test transfer',
      status: 'COMPLETED',
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:00:00Z'
    }
  ];

  const mockTransferResponse = {
    transactionId: 'txn-new',
    referenceNumber: 'REF-NEW',
    status: 'PENDING',
    fee: asMoney('5000'),
    estimatedCompletionTime: '2024-01-01T10:05:00Z'
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should refresh transaction list after initiating transfer', async () => {
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue(mockTransactions);
    vi.mocked(TransactionService.initiateTransfer).mockResolvedValue(mockTransferResponse);

    // First, fetch transactions
    const { result: transactionsResult } = renderHook(() => useTransactions('account-1'), {
      wrapper
    });

    await waitFor(() => {
      expect(transactionsResult.current.isSuccess).toBe(true);
    });

    const initialCallCount = vi.mocked(TransactionService.getAccountTransactions).mock.calls.length;

    // Then initiate transfer
    const { result: transferResult } = renderHook(() => useInitiateTransfer(), { wrapper });

    await act(async () => {
      await transferResult.current.mutateAsync({
        senderAccountId: 'account-1',
        recipientAccountNumber: '1234567890',
        amount: asMoney('50000'),
        description: 'Test',
        type: 'INTERNAL_TRANSFER' as TransactionType,
        transactionPin: '123456'
      });
    });

    // Transactions should be refetched after transfer
    await waitFor(() => {
      expect(vi.mocked(TransactionService.getAccountTransactions).mock.calls.length).toBeGreaterThan(
        initialCallCount
      );
    });
  });

  it('should handle complete transaction workflow', async () => {
    const mockTransaction: Transaction = {
      id: 'txn-1',
      referenceNumber: 'REF-001',
      senderAccountId: 'account-1',
      recipientAccountId: 'account-2',
      type: 'INTERNAL_TRANSFER' as TransactionType,
      amount: asMoney('100000'),
      currency: 'IDR',
      description: 'Test transfer',
      status: 'COMPLETED',
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:00:00Z'
    };

    vi.mocked(TransactionService.getTransaction).mockResolvedValue(mockTransaction);
    vi.mocked(TransactionService.getAccountTransactions).mockResolvedValue(mockTransactions);

    // Fetch transaction list
    const { result: listResult } = renderHook(() => useTransactions('account-1'), { wrapper });

    await waitFor(() => {
      expect(listResult.current.isSuccess).toBe(true);
    });

    // Fetch single transaction
    const { result: detailResult } = renderHook(() => useTransaction('txn-1'), { wrapper });

    await waitFor(() => {
      expect(detailResult.current.isSuccess).toBe(true);
    });

    expect(detailResult.current.data).toEqual(mockTransaction);
  });
});
