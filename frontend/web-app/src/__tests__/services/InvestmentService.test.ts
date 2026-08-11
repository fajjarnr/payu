import { describe, it, expect, vi, beforeEach } from 'vitest';
import InvestmentService, {
  type InvestmentAccount,
  type BuyDepositRequest,
  type BuyMutualFundRequest,
  type BuyGoldRequest,
  type SellInvestmentRequest,
  type InvestmentOrder,
  type GoldHolding,
} from '@/services/InvestmentService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockAccount: InvestmentAccount = {
  id: 'inv_acc_001',
  userId: 'user_123',
  accountType: 'PREMIUM',
  balance: '50000000',
  currency: 'IDR',
  status: 'ACTIVE',
  createdAt: '2026-02-18T10:00:00Z',
};

const mockOrder: InvestmentOrder = {
  id: 'order_001',
  userId: 'user_123',
  type: 'GOLD',
  action: 'BUY',
  amount: '5000000',
  units: 5,
  status: 'COMPLETED',
  createdAt: '2026-02-18T10:00:00Z',
};

const mockGoldHolding: GoldHolding = {
  userId: 'user_123',
  totalWeightGrams: 10.5,
  currentValuePerGram: '1050000',
  totalValue: '11025000',
  holdings: [
    { purchaseDate: '2026-01-15', weightGrams: 5.0, purchasePrice: '1000000' },
    { purchaseDate: '2026-02-01', weightGrams: 5.5, purchasePrice: '1020000' },
  ],
};

describe('InvestmentService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // BUG-CROSS-049: createAccount takes no body
  describe('createAccount', () => {
    it('should create investment account', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockAccount });

      const result = await InvestmentService.createAccount();

      expect(api.post).toHaveBeenCalledWith('/investments/accounts');
      expect(result.id).toBe('inv_acc_001');
      expect(result.status).toBe('ACTIVE');
    });
  });

  // BUG-CROSS-048: getAccount uses /accounts/me
  describe('getAccount', () => {
    it('should fetch user investment account', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockAccount });

      const result = await InvestmentService.getAccount();

      expect(api.get).toHaveBeenCalledWith('/investments/accounts/me');
      expect(result.balance).toBe('50000000');
    });
  });

  // BUG-CROSS-050: BuyDepositRequest uses accountId, tenure (not userId, tenureMonths)
  describe('buyDeposit', () => {
    it('should buy a fixed deposit', async () => {
      const request: BuyDepositRequest = {
        accountId: 'acc_123',
        amount: '10000000',
        tenure: 12,
      };

      const depositOrder = { ...mockOrder, type: 'DEPOSIT' as const, action: 'BUY' as const };
      vi.mocked(api.post).mockResolvedValue({ data: depositOrder });

      const result = await InvestmentService.buyDeposit(request);

      expect(api.post).toHaveBeenCalledWith('/investments/deposits', request, {
        headers: { 'X-Idempotency-Key': expect.any(String) },
      });
      expect(result.status).toBe('COMPLETED');
    });
  });

  // BUG-CROSS-051: BuyMutualFundRequest uses accountId, fundCode (not userId, fundId)
  describe('buyMutualFund', () => {
    it('should buy mutual fund', async () => {
      const request: BuyMutualFundRequest = {
        accountId: 'acc_123',
        fundCode: 'fund_001',
        amount: '1000000',
      };

      const fundOrder = { ...mockOrder, type: 'MUTUAL_FUND' as const };
      vi.mocked(api.post).mockResolvedValue({ data: fundOrder });

      const result = await InvestmentService.buyMutualFund(request);

      expect(api.post).toHaveBeenCalledWith('/investments/mutual-funds', request, {
        headers: { 'X-Idempotency-Key': expect.any(String) },
      });
      expect(result.status).toBe('COMPLETED');
    });
  });

  // BUG-CROSS-052: BuyGoldRequest only has amount
  describe('buyGold', () => {
    it('should buy gold', async () => {
      const request: BuyGoldRequest = {
        amount: '5000000',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockOrder });

      const result = await InvestmentService.buyGold(request);

      expect(api.post).toHaveBeenCalledWith('/investments/gold', request, {
        headers: { 'X-Idempotency-Key': expect.any(String) },
      });
      expect(result.amount).toBe('5000000');
    });
  });

  describe('sell', () => {
    it('should sell an investment', async () => {
      const request: SellInvestmentRequest = {
        accountId: 'acc_123',
        transactionId: 'inv_001',
        amount: '3000000',
      };

      const sellOrder = { ...mockOrder, action: 'SELL' as const };
      vi.mocked(api.post).mockResolvedValue({ data: sellOrder });

      const result = await InvestmentService.sell(request);

      expect(api.post).toHaveBeenCalledWith('/investments/sell', request, {
        headers: { 'X-Idempotency-Key': expect.any(String) },
      });
      expect(result.action).toBe('SELL');
    });
  });

  // BUG-CROSS-048: getGoldHoldings uses /gold/me
  describe('getGoldHoldings', () => {
    it('should fetch gold holdings for a user', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockGoldHolding });

      const result = await InvestmentService.getGoldHoldings();

      expect(api.get).toHaveBeenCalledWith('/investments/gold/me');
      expect(result.totalWeightGrams).toBe(10.5);
      expect(result.holdings).toHaveLength(2);
      expect(result.totalValue).toBe('11025000');
    });
  });
});
