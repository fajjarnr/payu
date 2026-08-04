import api from '@/lib/api';
import type { Money } from '@/lib/currency';

// Backend Decimal fields stay strings at the JSON boundary.

export interface UserMetrics {
  userId: string;
  totalTransactions: number;
  totalAmount: Money;
  averageTransaction: Money;
  lastTransactionDate: string | null;
  accountAgeDays: number;
  kycStatus: string | null;
}

export interface SpendingTrendsRequest {
  userId: string;
  periodDays?: number;
  groupBy?: 'category' | 'merchant' | 'day';
}

export interface SpendingAnalytics {
  period: string;
  totalSpending: Money;
  categories: CategoryBreakdown[];
  monthOverMonthChange: number | null;
  topMerchants: Array<Record<string, unknown>>;
}

export interface CashFlowRequest {
  userId: string;
  periodDays?: number;
}

export interface CashFlowAnalysis {
  period: string;
  income: Money;
  expenses: Money;
  netCashFlow: Money;
  incomeBySource: Array<Record<string, unknown>>;
  expensesByCategory: CategoryBreakdown[];
}

export interface CategoryBreakdown {
  category: string;
  amount: Money;
  percentage: number;
  transactionCount: number;
  trend: string;
}

type BackendSpendingPattern = {
  category: string;
  amount: Money;
  percentage: number;
  transaction_count: number;
  trend: string;
};

const mapSpendingPattern = (category: BackendSpendingPattern): CategoryBreakdown => ({
  category: category.category,
  amount: category.amount,
  percentage: category.percentage,
  transactionCount: category.transaction_count,
  trend: category.trend,
});

export interface FinancialInsight {
  id: string;
  type: InsightType;
  title: string;
  description: string;
  actionUrl?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  createdAt: string;
}

export interface RoboAdvisoryRequest {
  userId: string;
  riskProfile: 'CONSERVATIVE' | 'MODERATE' | 'AGGRESSIVE';
  investmentGoal?: string;
  horizon?: number;
}

export interface RoboAdvisoryResponse {
  userId: string;
  recommendations: { asset: string; allocation: number; rationale: string }[];
  expectedReturn: number;
  riskScore: number;
}

export interface FraudScoreRequest {
  userId: string;
  transactionId?: string;
  amount?: number;
  merchantName?: string;
}

export interface FraudDetectionResult {
  transactionId: string;
  fraudScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  factors: string[];
  recommendation: string;
}

export type InsightType = 'SAVING_TIP' | 'SPENDING_ALERT' | 'GOAL_PROGRESS' | 'ANOMALY';

class AnalyticsService {
  private static instance: AnalyticsService;

  static getInstance(): AnalyticsService {
    if (!AnalyticsService.instance) {
      AnalyticsService.instance = new AnalyticsService();
    }
    return AnalyticsService.instance;
  }

  /** GET /analytics/user/{userId}/metrics — Get user analytics metrics */
  async getUserMetrics(userId: string): Promise<UserMetrics> {
    const response = await api.get(`/analytics/user/${userId}/metrics`);
    const data = response.data;
    return {
      userId: data.user_id,
      totalTransactions: data.total_transactions,
      totalAmount: data.total_amount,
      averageTransaction: data.average_transaction,
      lastTransactionDate: data.last_transaction_date,
      accountAgeDays: data.account_age_days,
      kycStatus: data.kyc_status,
    };
  }

  /** POST /analytics/spending/trends — Get spending trends */
  async getSpendingTrends(request: SpendingTrendsRequest): Promise<SpendingAnalytics> {
    const response = await api.post('/analytics/spending/trends', {
      user_id: request.userId,
      period_days: request.periodDays ?? 30,
      group_by: request.groupBy ?? 'category',
    });
    const data = response.data;
    return {
      period: data.period,
      totalSpending: data.total_spending,
      categories: data.spending_by_category.map(mapSpendingPattern),
      monthOverMonthChange: data.month_over_month_change,
      topMerchants: data.top_merchants,
    };
  }

  /** POST /analytics/cashflow — Get cash flow analysis */
  async getCashFlowAnalysis(request: CashFlowRequest): Promise<CashFlowAnalysis> {
    const response = await api.post('/analytics/cashflow', {
      user_id: request.userId,
      period_days: request.periodDays ?? 30,
    });
    const data = response.data;
    return {
      period: data.period,
      income: data.income,
      expenses: data.expenses,
      netCashFlow: data.net_cash_flow,
      incomeBySource: data.income_by_source,
      expensesByCategory: data.expenses_by_category.map(mapSpendingPattern),
    };
  }

  /** GET /analytics/user/{userId}/recommendations — Get financial recommendations */
  async getRecommendations(userId: string): Promise<FinancialInsight[]> {
    const response = await api.get(`/analytics/user/${userId}/recommendations`);
    return response.data;
  }

  /** POST /analytics/robo-advisory — Get AI investment advice */
  async getRoboAdvisory(request: RoboAdvisoryRequest): Promise<RoboAdvisoryResponse> {
    const response = await api.post('/analytics/robo-advisory', request);
    return response.data;
  }

  /** POST /analytics/fraud/score — Calculate fraud score */
  async getFraudScore(request: FraudScoreRequest): Promise<FraudDetectionResult> {
    const response = await api.post('/analytics/fraud/score', request);
    return response.data;
  }

  /** GET /analytics/fraud/transaction/{transactionId} — Get transaction fraud score */
  async getTransactionFraudScore(transactionId: string): Promise<FraudDetectionResult> {
    const response = await api.get(`/analytics/fraud/transaction/${transactionId}`);
    return response.data;
  }

  /** GET /analytics/fraud/user/{userId}/high-risk — Get user high-risk transactions */
  async getUserHighRiskTransactions(userId: string): Promise<FraudDetectionResult[]> {
    const response = await api.get(`/analytics/fraud/user/${userId}/high-risk`);
    return response.data;
  }
}

export default AnalyticsService.getInstance();
