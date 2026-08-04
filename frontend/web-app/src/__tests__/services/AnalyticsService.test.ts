import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import AnalyticsService from '@/services/AnalyticsService';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('AnalyticsService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('maps user metrics and preserves Decimal values as strings', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        user_id: 'user-1',
        total_transactions: 2,
        total_amount: '9007199254740993.1234',
        average_transaction: '10.0000',
        last_transaction_date: null,
        account_age_days: 10,
        kyc_status: 'VERIFIED',
      },
    } as never);

    await expect(AnalyticsService.getUserMetrics('user-1')).resolves.toEqual({
      userId: 'user-1',
      totalTransactions: 2,
      totalAmount: '9007199254740993.1234',
      averageTransaction: '10.0000',
      lastTransactionDate: null,
      accountAgeDays: 10,
      kycStatus: 'VERIFIED',
    });
  });

  it('uses the backend request shape and maps spending trends', async () => {
    vi.mocked(api.post).mockResolvedValue({
      data: {
        period: '30 days',
        total_spending: '123456789012345.6789',
        spending_by_category: [{
          category: 'FOOD',
          amount: '100.0000',
          percentage: 80,
          transaction_count: 3,
          trend: 'stable',
        }],
        month_over_month_change: null,
        top_merchants: [],
      },
    } as never);

    await expect(AnalyticsService.getSpendingTrends({
      userId: 'user-1',
      periodDays: 30,
      groupBy: 'category',
    })).resolves.toEqual({
      period: '30 days',
      totalSpending: '123456789012345.6789',
      categories: [{
        category: 'FOOD',
        amount: '100.0000',
        percentage: 80,
        transactionCount: 3,
        trend: 'stable',
      }],
      monthOverMonthChange: null,
      topMerchants: [],
    });

    expect(api.post).toHaveBeenCalledWith('/analytics/spending/trends', {
      user_id: 'user-1',
      period_days: 30,
      group_by: 'category',
    });
  });

  it('maps cash flow Decimal values without converting them to numbers', async () => {
    vi.mocked(api.post).mockResolvedValue({
      data: {
        period: '30 days',
        income: '100000000000000.0000',
        expenses: '1.2500',
        net_cash_flow: '99999999999998.7500',
        income_by_source: [],
        expenses_by_category: [],
      },
    } as never);

    await expect(AnalyticsService.getCashFlowAnalysis({
      userId: 'user-1',
      periodDays: 30,
    })).resolves.toEqual({
      period: '30 days',
      income: '100000000000000.0000',
      expenses: '1.2500',
      netCashFlow: '99999999999998.7500',
      incomeBySource: [],
      expensesByCategory: [],
    });

    expect(api.post).toHaveBeenCalledWith('/analytics/cashflow', {
      user_id: 'user-1',
      period_days: 30,
    });
  });
});
