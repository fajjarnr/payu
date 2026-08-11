import api from '@/lib/api';
import { getFinancialMutationHeaders } from '@/lib/utils';
import type { Money } from '@/lib/currency';

// --- Interfaces matching backend InvestmentController ---

export interface InvestmentAccount {
  id: string;
  userId: string;
  accountType: string;
  balance: Money;
  currency: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  createdAt: string;
}

// BUG-CROSS-049: Backend createAccount takes no body — empty POST
// Using type alias to satisfy @typescript-eslint/no-empty-object-type
// while still documenting the contract.
export type CreateAccountRequest = Record<string, never>;

// BUG-CROSS-050: Backend BuyDepositRequest: accountId, amount, tenure (Integer)
export interface BuyDepositRequest {
  accountId: string;
  amount: Money;
  tenure: number;
}

// BUG-CROSS-051: Backend BuyMutualFundRequest: accountId, fundCode, amount
export interface BuyMutualFundRequest {
  accountId: string;
  fundCode: string;
  amount: Money;
}

// BUG-CROSS-052: Backend BuyGoldRequest: only amount
export interface BuyGoldRequest {
  amount: Money;
}

export interface SellInvestmentRequest {
  accountId: string;
  transactionId: string;
  amount: Money;
}

export interface InvestmentOrder {
  id: string;
  userId: string;
  type: 'DEPOSIT' | 'MUTUAL_FUND' | 'GOLD';
  action: 'BUY' | 'SELL';
  amount: Money;
  units?: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  createdAt: string;
}

export interface GoldHolding {
  userId: string;
  totalWeightGrams: number;
  currentValuePerGram: Money;
  totalValue: Money;
  holdings: { purchaseDate: string; weightGrams: number; purchasePrice: Money }[];
}

// Legacy aliases for backward compatibility
export type InvestmentType = 'MUTUAL_FUND' | 'GOLD' | 'DEPOSIT' | 'BONDS';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export interface InvestmentProduct {
  id: string;
  name: string;
  type: InvestmentType;
  riskLevel: RiskLevel;
  minInvestment: Money;
  expectedReturn: number;
  currency: string;
  description: string;
  isActive: boolean;
}
export interface Portfolio {
  id: string;
  userId: string;
  totalValue: Money;
  totalInvested: Money;
  unrealizedPnl: Money;
  currency: string;
  holdings: Holding[];
}
export interface Holding {
  productId: string;
  productName: string;
  units: number;
  currentValue: Money;
  investedValue: Money;
  pnl: Money;
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
    const response = await api.post('/investments/deposits', request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  /** POST /investments/mutual-funds — Buy mutual fund */
  async buyMutualFund(request: BuyMutualFundRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/mutual-funds', request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  /** POST /investments/gold — Buy gold */
  async buyGold(request: BuyGoldRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/gold', request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  /** POST /investments/sell — Sell investment */
  async sell(request: SellInvestmentRequest): Promise<InvestmentOrder> {
    const response = await api.post('/investments/sell', request, {
      headers: getFinancialMutationHeaders(),
    });
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
