import api from '@/lib/api';

export interface SpendingAnalytics {
  period: string;
  totalSpent: number;
  totalIncome: number;
  netSavings: number;
  categories: CategoryBreakdown[];
  trend: TrendData[];
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

export type InsightType = 'SAVING_TIP' | 'SPENDING_ALERT' | 'GOAL_PROGRESS' | 'ANOMALY';

class AnalyticsService {
  private static instance: AnalyticsService;

  static getInstance(): AnalyticsService {
    if (!AnalyticsService.instance) {
      AnalyticsService.instance = new AnalyticsService();
    }
    return AnalyticsService.instance;
  }

  async getSpendingAnalytics(period: 'WEEKLY' | 'MONTHLY' | 'YEARLY'): Promise<SpendingAnalytics> {
    const response = await api.get('/analytics/spending', { params: { period } });
    return response.data;
  }

  async getCategoryBreakdown(month: string): Promise<CategoryBreakdown[]> {
    const response = await api.get('/analytics/categories', { params: { month } });
    return response.data;
  }

  async getInsights(): Promise<FinancialInsight[]> {
    const response = await api.get('/analytics/insights');
    return response.data;
  }

  async getTrends(months = 6): Promise<TrendData[]> {
    const response = await api.get('/analytics/trends', { params: { months } });
    return response.data;
  }
}

export default AnalyticsService.getInstance();
