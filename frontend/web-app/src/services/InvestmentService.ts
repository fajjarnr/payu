import api from '@/lib/api';

export interface InvestmentProduct {
  id: string;
  name: string;
  type: InvestmentType;
  riskLevel: RiskLevel;
  minInvestment: number;
  expectedReturn: number;
  currency: string;
  description: string;
  isActive: boolean;
}

export interface Portfolio {
  id: string;
  userId: string;
  totalValue: number;
  totalInvested: number;
  unrealizedPnl: number;
  currency: string;
  holdings: Holding[];
}

export interface Holding {
  productId: string;
  productName: string;
  units: number;
  currentValue: number;
  investedValue: number;
  pnl: number;
  pnlPercentage: number;
}

export interface InvestmentOrder {
  id: string;
  productId: string;
  type: 'BUY' | 'SELL';
  amount: number;
  units?: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  createdAt: string;
}

export type InvestmentType = 'MUTUAL_FUND' | 'GOLD' | 'DEPOSIT' | 'BONDS';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

class InvestmentService {
  private static instance: InvestmentService;

  static getInstance(): InvestmentService {
    if (!InvestmentService.instance) {
      InvestmentService.instance = new InvestmentService();
    }
    return InvestmentService.instance;
  }

  async getProducts(type?: InvestmentType): Promise<InvestmentProduct[]> {
    const response = await api.get('/investments/products', { params: { type } });
    return response.data;
  }

  async getPortfolio(): Promise<Portfolio> {
    const response = await api.get('/investments/portfolio');
    return response.data;
  }

  async buyProduct(productId: string, amount: number): Promise<InvestmentOrder> {
    const response = await api.post('/investments/orders', { productId, type: 'BUY', amount });
    return response.data;
  }

  async sellProduct(productId: string, units: number): Promise<InvestmentOrder> {
    const response = await api.post('/investments/orders', { productId, type: 'SELL', units });
    return response.data;
  }

  async getOrderHistory(page = 0, size = 20): Promise<{ content: InvestmentOrder[]; totalElements: number }> {
    const response = await api.get('/investments/orders', { params: { page, size } });
    return response.data;
  }
}

export default InvestmentService.getInstance();
