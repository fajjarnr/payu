import { transactionService } from '../transaction.service';
import { apiClient } from '../api';
import { Transaction, TransferData, ApiResponse, PaginatedResponse, QRISData } from '@/types';

// Mock the apiClient
jest.mock('../api', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

describe('transactionService', () => {
  const mockGet = apiClient.get as jest.MockedFunction<typeof apiClient.get>;
  const mockPost = apiClient.post as jest.MockedFunction<typeof apiClient.post>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getTransactions', () => {
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
        type: 'topup',
        amount: 500000,
        description: 'Top up from bank',
        status: 'completed',
        createdAt: '2024-01-14T08:00:00Z',
        processedAt: '2024-01-14T08:05:00Z',
      },
    ];

    const mockPaginatedResponse: PaginatedResponse<Transaction> = {
      items: mockTransactions,
      total: 2,
      page: 1,
      pageSize: 10,
      hasMore: false,
    };

    it('should get transactions with default params', async () => {
      const apiResponse: ApiResponse<PaginatedResponse<Transaction>> = {
        success: true,
        data: mockPaginatedResponse,
        message: 'Transactions retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.getTransactions();

      expect(mockGet).toHaveBeenCalledWith('/transactions', { params: undefined });
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockPaginatedResponse);
    });

    it('should get transactions with pagination params', async () => {
      const apiResponse: ApiResponse<PaginatedResponse<Transaction>> = {
        success: true,
        data: mockPaginatedResponse,
        message: 'Transactions retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const params = { page: 2, pageSize: 20 };
      const result = await transactionService.getTransactions(params);

      expect(mockGet).toHaveBeenCalledWith('/transactions', { params });
      expect(result.items).toHaveLength(2);
    });

    it('should get transactions with filters', async () => {
      const apiResponse: ApiResponse<PaginatedResponse<Transaction>> = {
        success: true,
        data: {
          ...mockPaginatedResponse,
          items: mockTransactions.filter(t => t.type === 'transfer'),
        },
        message: 'Transactions retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const params = { type: 'transfer', status: 'completed' };
      const result = await transactionService.getTransactions(params);

      expect(mockGet).toHaveBeenCalledWith('/transactions', { params });
    });

    it('should handle empty transactions list', async () => {
      const apiResponse: ApiResponse<PaginatedResponse<Transaction>> = {
        success: true,
        data: {
          items: [],
          total: 0,
          page: 1,
          pageSize: 10,
          hasMore: false,
        },
        message: 'No transactions found',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.getTransactions();

      expect(result.items).toEqual([]);
      expect(result.total).toBe(0);
    });

    it('should handle network errors', async () => {
      const error = new Error('Network Error');
      mockGet.mockRejectedValueOnce(error);

      await expect(transactionService.getTransactions()).rejects.toThrow('Network Error');
    });
  });

  describe('getTransaction', () => {
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

    it('should get a specific transaction successfully', async () => {
      const apiResponse: ApiResponse<Transaction> = {
        success: true,
        data: mockTransaction,
        message: 'Transaction retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.getTransaction('txn-1');

      expect(mockGet).toHaveBeenCalledWith('/transactions/txn-1');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockTransaction);
    });

    it('should handle transaction not found', async () => {
      const error = new Error('Transaction not found');
      (error as any).response = { status: 404 };
      mockGet.mockRejectedValueOnce(error);

      await expect(transactionService.getTransaction('invalid-id')).rejects.toThrow('Transaction not found');
    });

    it('should handle unauthorized access to transaction', async () => {
      const error = new Error('Unauthorized');
      (error as any).response = { status: 403 };
      mockGet.mockRejectedValueOnce(error);

      await expect(transactionService.getTransaction('txn-1')).rejects.toThrow('Unauthorized');
    });
  });

  describe('transfer', () => {
    const transferData: TransferData = {
      amount: 100000,
      recipientAccount: '1234567890',
      recipientBank: 'BCA',
      description: 'Payment for services',
      fromPocket: 'wallet-1',
    };

    const mockTransaction: Transaction = {
      id: 'txn-transfer-1',
      userId: 'user-123',
      type: 'transfer',
      amount: 100000,
      description: 'Payment for services',
      status: 'completed',
      recipientName: 'Jane Doe',
      recipientAccount: '1234567890',
      fromPocket: 'wallet-1',
      createdAt: '2024-01-15T10:00:00Z',
      processedAt: '2024-01-15T10:01:00Z',
    };

    it('should transfer successfully', async () => {
      const apiResponse: ApiResponse<Transaction> = {
        success: true,
        data: mockTransaction,
        message: 'Transfer successful',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.transfer(transferData);

      expect(mockPost).toHaveBeenCalledWith('/transactions/transfer', transferData);
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockTransaction);
    });

    it('should handle scheduled transfer', async () => {
      const scheduledTransferData = {
        ...transferData,
        scheduleDate: '2024-01-20T10:00:00Z',
      };

      const apiResponse: ApiResponse<Transaction> = {
        success: true,
        data: { ...mockTransaction, status: 'pending' as const },
        message: 'Transfer scheduled',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.transfer(scheduledTransferData);

      expect(mockPost).toHaveBeenCalledWith('/transactions/transfer', scheduledTransferData);
      expect(result.status).toBe('pending');
    });

    it('should handle insufficient balance', async () => {
      const error = new Error('Insufficient balance');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.transfer(transferData)).rejects.toThrow('Insufficient balance');
    });

    it('should handle invalid recipient account', async () => {
      const error = new Error('Invalid recipient account');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.transfer({
        ...transferData,
        recipientAccount: 'invalid',
      })).rejects.toThrow('Invalid recipient account');
    });

    it('should handle daily limit exceeded', async () => {
      const error = new Error('Daily transfer limit exceeded');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.transfer({
        ...transferData,
        amount: 999999999,
      })).rejects.toThrow('Daily transfer limit exceeded');
    });
  });

  describe('topUp', () => {
    const amount = 500000;
    const paymentMethod = 'virtual_account_bca';

    const mockTransaction: Transaction = {
      id: 'txn-topup-1',
      userId: 'user-123',
      type: 'topup',
      amount: 500000,
      description: 'Top up via Virtual Account BCA',
      status: 'pending',
      createdAt: '2024-01-15T10:00:00Z',
    };

    it('should initiate top up successfully', async () => {
      const apiResponse: ApiResponse<Transaction> = {
        success: true,
        data: mockTransaction,
        message: 'Top up initiated',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.topUp(amount, paymentMethod);

      expect(mockPost).toHaveBeenCalledWith('/transactions/topup', { amount, paymentMethod });
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockTransaction);
    });

    it('should handle different payment methods', async () => {
      const apiResponse: ApiResponse<Transaction> = {
        success: true,
        data: { ...mockTransaction, paymentMethod: 'debit_card' },
        message: 'Top up initiated',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.topUp(100000, 'debit_card');

      expect(mockPost).toHaveBeenCalledWith('/transactions/topup', {
        amount: 100000,
        paymentMethod: 'debit_card',
      });
    });

    it('should handle invalid amount', async () => {
      const error = new Error('Minimum top up amount is 10000');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.topUp(1000, paymentMethod))
        .rejects.toThrow('Minimum top up amount is 10000');
    });

    it('should handle unsupported payment method', async () => {
      const error = new Error('Unsupported payment method');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.topUp(amount, 'invalid_method'))
        .rejects.toThrow('Unsupported payment method');
    });
  });

  describe('payQRIS', () => {
    const qrisData: QRISData = {
      merchantName: 'Warung Makan',
      amount: 25000,
      merchantId: 'MERCHANT123',
      terminalId: 'TERM001',
    };

    const mockTransaction: Transaction = {
      id: 'txn-qris-1',
      userId: 'user-123',
      type: 'qris',
      amount: 25000,
      description: 'Payment to Warung Makan',
      status: 'completed',
      createdAt: '2024-01-15T10:00:00Z',
      processedAt: '2024-01-15T10:00:05Z',
    };

    it('should pay QRIS successfully', async () => {
      const apiResponse: ApiResponse<Transaction> = {
        success: true,
        data: mockTransaction,
        message: 'QRIS payment successful',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.payQRIS(qrisData);

      expect(mockPost).toHaveBeenCalledWith('/transactions/qris', qrisData);
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockTransaction);
    });

    it('should handle invalid QRIS code', async () => {
      const error = new Error('Invalid QRIS code');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.payQRIS({
        ...qrisData,
        merchantId: 'INVALID',
      })).rejects.toThrow('Invalid QRIS code');
    });

    it('should handle expired QRIS code', async () => {
      const error = new Error('QRIS code expired');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.payQRIS(qrisData)).rejects.toThrow('QRIS code expired');
    });

    it('should handle insufficient balance for QRIS', async () => {
      const error = new Error('Insufficient balance');
      mockPost.mockRejectedValueOnce(error);

      await expect(transactionService.payQRIS({
        ...qrisData,
        amount: 999999999,
      })).rejects.toThrow('Insufficient balance');
    });
  });

  describe('getTransactionSummary', () => {
    const mockSummary = {
      totalIncome: 1500000,
      totalExpense: 800000,
      transactionCount: 25,
    };

    it('should get summary with default period', async () => {
      const apiResponse: ApiResponse<typeof mockSummary> = {
        success: true,
        data: mockSummary,
        message: 'Summary retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.getTransactionSummary();

      expect(mockGet).toHaveBeenCalledWith('/transactions/summary', { params: undefined });
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockSummary);
    });

    it('should get summary with specific period', async () => {
      const apiResponse: ApiResponse<typeof mockSummary> = {
        success: true,
        data: mockSummary,
        message: 'Summary retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.getTransactionSummary({ period: 'month' });

      expect(mockGet).toHaveBeenCalledWith('/transactions/summary', { params: { period: 'month' } });
    });

    it('should get summary for different periods', async () => {
      const apiResponse: ApiResponse<typeof mockSummary> = {
        success: true,
        data: { ...mockSummary, transactionCount: 100 },
        message: 'Summary retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await transactionService.getTransactionSummary({ period: 'year' });

      expect(mockGet).toHaveBeenCalledWith('/transactions/summary', { params: { period: 'year' } });
    });

    it('should handle summary retrieval error', async () => {
      const error = new Error('Failed to retrieve summary');
      mockGet.mockRejectedValueOnce(error);

      await expect(transactionService.getTransactionSummary()).rejects.toThrow('Failed to retrieve summary');
    });
  });
});
