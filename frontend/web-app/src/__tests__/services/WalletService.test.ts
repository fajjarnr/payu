import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  WalletService,
  type BalanceResponse,
  type ReserveBalanceRequest,
  type ReserveBalanceResponse,
  type CreditRequest,
  type WalletTransaction,
} from '@/services/WalletService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('WalletService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be a singleton', () => {
    const instance1 = WalletService.getInstance();
    const instance2 = WalletService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('getBalance', () => {
    it('should fetch balance for given account', async () => {
      const mockBalance: BalanceResponse = {
        accountId: 'acc_123',
        balance: 1000000,
        availableBalance: 900000,
        reservedBalance: 100000,
        currency: 'IDR',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockBalance });

      const result = await WalletService.getInstance().getBalance('acc_123');

      expect(api.get).toHaveBeenCalledWith('/wallets/acc_123/balance');
      expect(result).toEqual(mockBalance);
    });
  });

  describe('reserveBalance', () => {
    it('should reserve balance successfully', async () => {
      const mockRequest: ReserveBalanceRequest = {
        amount: 50000,
        referenceId: 'ref_123',
      };

      const mockResponse: ReserveBalanceResponse = {
        reservationId: 'res_123',
        accountId: 'acc_123',
        referenceId: 'ref_123',
        status: 'RESERVED',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await WalletService.getInstance().reserveBalance('acc_123', mockRequest);

      expect(api.post).toHaveBeenCalledWith('/wallets/acc_123/reserve', mockRequest);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('commitReservation', () => {
    it('should commit a reservation', async () => {
      const mockResponse = {
        status: 'COMMITTED',
        reservationId: 'res_123',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await WalletService.getInstance().commitReservation('res_123');

      expect(api.post).toHaveBeenCalledWith('/wallets/reservations/res_123/commit');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('releaseReservation', () => {
    it('should release a reservation', async () => {
      const mockResponse = {
        status: 'RELEASED',
        reservationId: 'res_123',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await WalletService.getInstance().releaseReservation('res_123');

      expect(api.post).toHaveBeenCalledWith('/wallets/reservations/res_123/release');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('credit', () => {
    it('should credit amount to wallet', async () => {
      const mockRequest: CreditRequest = {
        amount: 100000,
        referenceId: 'ref_123',
        description: 'Test credit',
      };

      const mockResponse = {
        status: 'CREDITED',
        accountId: 'acc_123',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await WalletService.getInstance().credit('acc_123', mockRequest);

      expect(api.post).toHaveBeenCalledWith('/wallets/acc_123/credit', mockRequest);
      expect(result).toEqual(mockResponse);
    });

    it('should credit amount without description', async () => {
      const mockRequest: CreditRequest = {
        amount: 50000,
        referenceId: 'ref_456',
      };

      const mockResponse = {
        status: 'CREDITED',
        accountId: 'acc_123',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await WalletService.getInstance().credit('acc_123', mockRequest);

      expect(api.post).toHaveBeenCalledWith('/wallets/acc_123/credit', mockRequest);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getTransactionHistory', () => {
    it('should fetch transaction history with default pagination', async () => {
      const mockTransactions = [
        {
          id: 'tx_1',
          walletId: 'wallet_123',
          referenceId: 'ref_1',
          type: 'CREDIT' as const,
          amount: 100000,
          balanceAfter: 1100000,
          description: 'Test transaction',
          createdAt: '2024-01-01T00:00:00Z',
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockTransactions });

      const result = await WalletService.getInstance().getTransactionHistory('acc_123');

      expect(api.get).toHaveBeenCalledWith('/wallets/acc_123/transactions', {
        params: { page: 0, size: 20 },
      });
      expect(result).toEqual(mockTransactions);
    });

    it('should fetch transaction history with custom pagination', async () => {
      const mockTransactions: WalletTransaction[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockTransactions });

      const result = await WalletService.getInstance().getTransactionHistory('acc_123', 1, 50);

      expect(api.get).toHaveBeenCalledWith('/wallets/acc_123/transactions', {
        params: { page: 1, size: 50 },
      });
      expect(result).toEqual(mockTransactions);
    });
  });
});
