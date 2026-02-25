import { describe, it, expect, vi, beforeEach } from 'vitest';
import InvestmentService, {
  type InvestmentAccount,
  type CreateAccountRequest,
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
  balance: 50000000,
  currency: 'IDR',
  status: 'ACTIVE',
  createdAt: '2026-02-18T10:00:00Z',
};

const mockOrder: InvestmentOrder = {
  id: 'order_001',
  userId: 'user_123',
  type: 'GOLD',
  action: 'BUY',
  amount: 5000000,
  units: 5,
  status: 'COMPLETED',
  createdAt: '2026-02-18T10:00:00Z',
};

const mockGoldHolding: GoldHolding = {
  userId: 'user_123',
  totalWeightGrams: 10.5,
  currentValuePerGram: 1050000,
  totalValue: 11025000,
  holdings: [
    { purchaseDate: '2026-01-15', weightGrams: 5.0, purchasePrice: 1000000 },
    { purchaseDate: '2026-02-01', weightGrams: 5.5, purchasePrice: 1020000 },
  ],
};

describe('InvestmentService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createAccount', () => {
    it('should create investment account', async () => {
      const request: CreateAccountRequest = {
        userId: 'user_123',
        accountType: 'PREMIUM',
        currency: 'IDR',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockAccount });

      const result = await InvestmentService.createAccount(request);

      expect(api.post).toHaveBeenCalledWith('/investments/accounts', request);
      expect(result.id).toBe('inv_acc_001');
      expect(result.status).toBe('ACTIVE');
    });
  });

  describe('getAccount', () => {
    it('should fetch user investment account', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockAccount });

      const result = await InvestmentService.getAccount('user_123');

      expect(api.get).toHaveBeenCalledWith('/investments/accounts/user_123');
      expect(result.balance).toBe(50000000);
    });
  });

  describe('buyDeposit', () => {
    it('should buy a fixed deposit', async () => {
      const request: BuyDepositRequest = {
        userId: 'user_123',
        amount: 10000000,
        tenureMonths: 12,
        interestRate: 5.5,
      };

      const depositOrder = { ...mockOrder, type: 'DEPOSIT' as const, action: 'BUY' as const };
      vi.mocked(api.post).mockResolvedValue({ data: depositOrder });

      const result = await InvestmentService.buyDeposit(request);

      expect(api.post).toHaveBeenCalledWith('/investments/deposits', request);
      expect(result.status).toBe('COMPLETED');
    });
  });

  describe('buyMutualFund', () => {
    it('should buy mutual fund', async () => {
      const request: BuyMutualFundRequest = {
        userId: 'user_123',
        fundId: 'fund_001',
        amount: 1000000,
      };

      const fundOrder = { ...mockOrder, type: 'MUTUAL_FUND' as const };
      vi.mocked(api.post).mockResolvedValue({ data: fundOrder });

      const result = await InvestmentService.buyMutualFund(request);

      expect(api.post).toHaveBeenCalledWith('/investments/mutual-funds', request);
      expect(result.status).toBe('COMPLETED');
    });
  });

  describe('buyGold', () => {
    it('should buy gold', async () => {
      const request: BuyGoldRequest = {
        userId: 'user_123',
        weightGrams: 5,
        amount: 5000000,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockOrder });

      const result = await InvestmentService.buyGold(request);

      expect(api.post).toHaveBeenCalledWith('/investments/gold', request);
      expect(result.amount).toBe(5000000);
    });
  });

  describe('sell', () => {
    it('should sell an investment', async () => {
      const request: SellInvestmentRequest = {
        accountId: 'acc_123',
        transactionId: 'inv_001',
        amount: 3000000,
      };

      const sellOrder = { ...mockOrder, action: 'SELL' as const };
      vi.mocked(api.post).mockResolvedValue({ data: sellOrder });

      const result = await InvestmentService.sell(request);

      expect(api.post).toHaveBeenCalledWith('/investments/sell', request);
      expect(result.action).toBe('SELL');
    });
  });

  describe('getGoldHoldings', () => {
    it('should fetch gold holdings for a user', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockGoldHolding });

      const result = await InvestmentService.getGoldHoldings('user_123');

      expect(api.get).toHaveBeenCalledWith('/investments/gold/user_123');
      expect(result.totalWeightGrams).toBe(10.5);
      expect(result.holdings).toHaveLength(2);
      expect(result.totalValue).toBe(11025000);
    });
  });
});
