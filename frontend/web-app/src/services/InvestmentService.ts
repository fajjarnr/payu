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

// BUG-CROSS-049: Backend createAccount takes no body — empty POST
export interface CreateAccountRequest {
  // No fields — backend expects empty POST
}

// BUG-CROSS-050: Backend BuyDepositRequest: accountId, amount, tenure (Integer)
export interface BuyDepositRequest {
  accountId: string;
  amount: number;
  tenure: number;
}

// BUG-CROSS-051: Backend BuyMutualFundRequest: accountId, fundCode, amount
export interface BuyMutualFundRequest {
  accountId: string;
  fundCode: string;
  amount: number;
}

// BUG-CROSS-052: Backend BuyGoldRequest: only amount
export interface BuyGoldRequest {
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

  // BUG-CROSS-049: Backend createAccount takes no body — empty POST
  /** POST /investments/accounts — Create investment account */
  async createAccount(): Promise<InvestmentAccount> {
    const response = await api.post('/investments/accounts');
    return response.data;
  }

  // BUG-CROSS-048: Backend getAccount uses /accounts/me, not /accounts/{userId}
  /** GET /investments/accounts/me — Get user investment account / portfolio */
  async getAccount(): Promise<InvestmentAccount> {
    const response = await api.get('/investments/accounts/me');
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

  // BUG-CROSS-048: Backend getGoldHoldings uses /gold/me, not /gold/{userId}
  /** GET /investments/gold/me — Get gold holdings */
  async getGoldHoldings(): Promise<GoldHolding> {
    const response = await api.get('/investments/gold/me');
    return response.data;
  }
}

export default InvestmentService.getInstance();
