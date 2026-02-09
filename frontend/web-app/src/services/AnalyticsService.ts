import api from '@/lib/api';

// --- Interfaces matching backend analytics_router (FastAPI) ---

export interface UserMetrics {
  userId: string;
  totalTransactions: number;
  totalSpent: number;
  totalIncome: number;
  avgTransactionAmount: number;
  topCategories: CategoryBreakdown[];
  period: string;
}

export interface SpendingTrendsRequest {
  userId: string;
  startDate: string;
  endDate: string;
  granularity?: 'DAILY' | 'WEEKLY' | 'MONTHLY';
}

export interface SpendingAnalytics {
  period: string;
  totalSpent: number;
  totalIncome: number;
  netSavings: number;
  categories: CategoryBreakdown[];
  trend: TrendData[];
}

export interface CashFlowRequest {
  userId: string;
  startDate: string;
  endDate: string;
}

export interface CashFlowAnalysis {
  userId: string;
  inflow: number;
  outflow: number;
  netFlow: number;
  entries: { date: string; inflow: number; outflow: number; balance: number }[];
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
  percentage: number;
  transactionCount: number;
}

export interface TrendData {
  date: string;
  income: number;
  expense: number;
  balance: number;
}

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
    return response.data;
  }

  /** POST /analytics/spending/trends — Get spending trends */
  async getSpendingTrends(request: SpendingTrendsRequest): Promise<SpendingAnalytics> {
    const response = await api.post('/analytics/spending/trends', request);
    return response.data;
  }

  /** POST /analytics/cashflow — Get cash flow analysis */
  async getCashFlowAnalysis(request: CashFlowRequest): Promise<CashFlowAnalysis> {
    const response = await api.post('/analytics/cashflow', request);
    return response.data;
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
