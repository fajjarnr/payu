import api from '@/lib/api';

// --- Interfaces matching backend InvestmentController ---

export interface InvestmentAccount {
  id: string;
  userId: string;
  accountType: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  createdAt: string;
}

export interface CreateAccountRequest {
  userId: string;
  accountType?: string;
  currency?: string;
}

export interface BuyDepositRequest {
  userId: string;
  amount: number;
  tenureMonths: number;
  interestRate?: number;
}

export interface BuyMutualFundRequest {
  userId: string;
  fundId: string;
  amount: number;
}

export interface BuyGoldRequest {
  userId: string;
  weightGrams: number;
  amount: number;
}

export interface SellInvestmentRequest {
  accountId: string;
  transactionId: string;
  amount: number;
}

export interface InvestmentOrder {
  id: string;
  userId: string;
  type: 'DEPOSIT' | 'MUTUAL_FUND' | 'GOLD';
  action: 'BUY' | 'SELL';
  amount: number;
  units?: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  createdAt: string;
}

export interface GoldHolding {
  userId: string;
  totalWeightGrams: number;
  currentValuePerGram: number;
  totalValue: number;
  holdings: { purchaseDate: string; weightGrams: number; purchasePrice: number }[];
}

// Legacy aliases for backward compatibility
export type InvestmentType = 'MUTUAL_FUND' | 'GOLD' | 'DEPOSIT' | 'BONDS';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
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

class InvestmentService {
  private static instance: InvestmentService;

  static getInstance(): InvestmentService {
    if (!InvestmentService.instance) {
      InvestmentService.instance = new InvestmentService();
    }
    return InvestmentService.instance;
  }

  /** POST /investments/accounts — Create investment account */
  async createAccount(request: CreateAccountRequest): Promise<InvestmentAccount> {
    const response = await api.post('/investments/accounts', request);
    return response.data;
  }

  /** GET /investments/accounts/{userId} — Get user investment account / portfolio */
  async getAccount(userId: string): Promise<InvestmentAccount> {
    const response = await api.get(`/investments/accounts/${userId}`);
    return response.data;
  }

  /** POST /investments/deposits — Buy fixed deposit */
  async buyDeposit(request: BuyDepositRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/deposits', request);
    return response.data;
  }

  /** POST /investments/mutual-funds — Buy mutual fund */
  async buyMutualFund(request: BuyMutualFundRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/mutual-funds', request);
    return response.data;
  }

  /** POST /investments/gold — Buy gold */
  async buyGold(request: BuyGoldRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/gold', request);
    return response.data;
  }

  /** POST /investments/sell — Sell investment */
  async sell(request: SellInvestmentRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/sell', request);
    return response.data;
  }

  /** GET /investments/gold/{userId} — Get gold holdings */
  async getGoldHoldings(userId: string): Promise<GoldHolding> {
    const response = await api.get(`/investments/gold/${userId}`);
    return response.data;
  }
}

export default InvestmentService.getInstance();
