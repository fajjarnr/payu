import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  TransactionService,
  type InitiateTransferRequest,
  type InitiateTransferResponse,
  type Transaction,
  type ProcessQrisPaymentRequest,
} from '@/services/TransactionService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('TransactionService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be a singleton', () => {
    const instance1 = TransactionService.getInstance();
    const instance2 = TransactionService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('initiateTransfer', () => {
    it('should initiate transfer successfully', async () => {
      const mockRequest: InitiateTransferRequest = {
        senderAccountId: 'acc_123',
        recipientAccountNumber: 'acc_456',
        amount: 100000,
        description: 'Test transfer',
        type: 'INTERNAL_TRANSFER',
        transactionPin: '123456',
        deviceId: 'device_123',
      };

      const mockResponse: InitiateTransferResponse = {
        transactionId: 'tx_123',
        referenceNumber: 'REF-2024-001',
        status: 'PENDING',
        fee: 0,
        estimatedCompletionTime: '2024-01-01T12:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await TransactionService.getInstance().initiateTransfer(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/transactions/transfer', mockRequest);
      expect(result).toEqual(mockResponse);
    });

    it('should initiate transfer with optional fields', async () => {
      const mockRequest: InitiateTransferRequest = {
        senderAccountId: 'acc_123',
        recipientAccountNumber: 'acc_456',
        amount: 50000,
        description: 'Minimal transfer',
      };

      const mockResponse: InitiateTransferResponse = {
        transactionId: 'tx_456',
        referenceNumber: 'REF-2024-002',
        status: 'PENDING',
        fee: 0,
        estimatedCompletionTime: '2024-01-01T12:30:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await TransactionService.getInstance().initiateTransfer(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/transactions/transfer', mockRequest);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getTransaction', () => {
    it('should fetch transaction by ID', async () => {
      const mockTransaction: Transaction = {
        id: 'tx_123',
        referenceNumber: 'REF-2024-001',
        senderAccountId: 'acc_123',
        recipientAccountId: 'acc_456',
        type: 'INTERNAL_TRANSFER',
        amount: 100000,
        currency: 'IDR',
        description: 'Test transfer',
        status: 'COMPLETED',
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: '2024-01-01T10:05:00Z',
        completedAt: '2024-01-01T10:05:00Z',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockTransaction });

      const result = await TransactionService.getInstance().getTransaction('tx_123');

      expect(api.get).toHaveBeenCalledWith('/transactions/tx_123');
      expect(result).toEqual(mockTransaction);
    });

    it('should fetch failed transaction with failure reason', async () => {
      const mockTransaction: Transaction = {
        id: 'tx_456',
        referenceNumber: 'REF-2024-002',
        senderAccountId: 'acc_123',
        recipientAccountId: 'acc_789',
        type: 'BIFAST_TRANSFER',
        amount: 50000,
        currency: 'IDR',
        description: 'Test BIFAST',
        status: 'FAILED',
        failureReason: 'Insufficient balance',
        createdAt: '2024-01-01T11:00:00Z',
        updatedAt: '2024-01-01T11:05:00Z',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockTransaction });

      const result = await TransactionService.getInstance().getTransaction('tx_456');

      expect(result.failureReason).toBe('Insufficient balance');
    });
  });

  describe('getAccountTransactions', () => {
    it('should fetch account transactions with default pagination', async () => {
      const mockTransactions: Transaction[] = [
        {
          id: 'tx_1',
          referenceNumber: 'REF-1',
          senderAccountId: 'acc_123',
          recipientAccountId: 'acc_456',
          type: 'INTERNAL_TRANSFER',
          amount: 100000,
          currency: 'IDR',
          description: 'Transfer 1',
          status: 'COMPLETED',
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:05:00Z',
          completedAt: '2024-01-01T10:05:00Z',
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockTransactions });

      const result = await TransactionService.getInstance().getAccountTransactions('acc_123');

      expect(api.get).toHaveBeenCalledWith('/transactions/accounts/acc_123', {
        params: { page: 0, size: 20 },
      });
      expect(result).toEqual(mockTransactions);
    });

    it('should fetch account transactions with custom pagination', async () => {
      const mockTransactions: Transaction[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockTransactions });

      const result = await TransactionService.getInstance().getAccountTransactions('acc_123', 1, 50);

      expect(api.get).toHaveBeenCalledWith('/transactions/accounts/acc_123', {
        params: { page: 1, size: 50 },
      });
      expect(result).toEqual(mockTransactions);
    });
  });

  describe('processQrisPayment', () => {
    it('should process QRIS payment successfully', async () => {
      const mockRequest: ProcessQrisPaymentRequest = {
        qrCode: '00020101021226570016ID.CO.QRIS.WWW01189360052002855280214ID10200000000303UMI51440014ID.CO.QRIS.WWW0215ID10200000000303UMI5204581253033605802ID5910Merchant6010Jakarta6105101106304ABCD',
        amount: 50000,
        accountId: 'acc_123',
      };

      vi.mocked(api.post).mockResolvedValue({ data: { success: true } });

      await TransactionService.getInstance().processQrisPayment(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/transactions/qris/pay', mockRequest);
    });

    it('should process QRIS payment with different amount', async () => {
      const mockRequest: ProcessQrisPaymentRequest = {
        qrCode: 'qris_code_string',
        amount: 150000,
        accountId: 'acc_456',
      };

      vi.mocked(api.post).mockResolvedValue({ data: { success: true } });

      await TransactionService.getInstance().processQrisPayment(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/transactions/qris/pay', mockRequest);
    });
  });
});
