import api from '@/lib/api';

export type PromotionStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'EXPIRED';
export type RewardType = 'LOYALTY_POINTS' | 'CASHBACK' | 'VOUCHER';

export interface Promotion {
  id: string;
  code: string;
  name: string;
  description: string;
  type: RewardType;
  value: number;
  status: PromotionStatus;
  startDate: string;
  endDate: string;
  maxClaims?: number;
  currentClaims: number;
  minTransactionAmount?: number;
  categories?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePromotionRequest {
  code: string;
  name: string;
  description: string;
  type: RewardType;
  value: number;
  startDate: string;
  endDate: string;
  maxClaims?: number;
  minTransactionAmount?: number;
  categories?: string[];
}

export interface UpdatePromotionRequest {
  name?: string;
  description?: string;
  value?: number;
  startDate?: string;
  endDate?: string;
  maxClaims?: number;
  minTransactionAmount?: number;
  categories?: string[];
}

export interface ClaimPromotionRequest {
  accountId: string;
  transactionId?: string;
}

export interface Reward {
  id: string;
  accountId: string;
  promotionId: string;
  promotionCode: string;
  type: RewardType;
  value: number;
  status: 'PENDING' | 'APPROVED' | 'REDEEMED' | 'EXPIRED';
  expiresAt: string;
  createdAt: string;
  redeemedAt?: string;
}

export interface LoyaltyPoints {
  id: string;
  accountId: string;
  points: number;
  type: 'EARNED' | 'REDEEMED' | 'EXPIRED';
  description: string;
  referenceId?: string;
  createdAt: string;
  expiresAt?: string;
}

export interface CreateLoyaltyPointsRequest {
  accountId: string;
  points: number;
  description: string;
  referenceId?: string;
}

export interface RedeemLoyaltyPointsRequest {
  accountId: string;
  points: number;
  description: string;
}

export interface LoyaltyBalanceResponse {
  accountId: string;
  totalEarned: number;
  totalRedeemed: number;
  currentBalance: number;
  pointsExpiring: number;
  expiryDate: string;
}

export interface Cashback {
  id: string;
  accountId: string;
  amount: number;
  type: 'PERCENTAGE' | 'FIXED';
  referenceId: string;
  merchantName?: string;
  status: 'PENDING' | 'APPROVED' | 'CREDITED' | 'EXPIRED';
  createdAt: string;
  creditedAt?: string;
  expiresAt?: string;
}

export interface CreateCashbackRequest {
  accountId: string;
  amount: number;
  type: 'PERCENTAGE' | 'FIXED';
  referenceId: string;
  merchantName?: string;
  expiresAt?: string;
}

export interface CashbackSummaryResponse {
  accountId: string;
  totalCashback: number;
  pendingCashback: number;
  creditedCashback: number;
  expiredCashback: number;
}

export interface Referral {
  id: string;
  referrerAccountId: string;
  refereeAccountId?: string;
  code: string;
  status: 'PENDING' | 'COMPLETED' | 'EXPIRED';
  rewardAmount: number;
  referralDate?: string;
  completedDate?: string;
  expiresAt: string;
  createdAt: string;
}

export interface CreateReferralRequest {
  referrerAccountId: string;
  rewardAmount: number;
  expiryDays: number;
}

export interface CompleteReferralRequest {
  code: string;
  refereeAccountId: string;
}

export interface ReferralSummaryResponse {
  referrerAccountId: string;
  totalReferrals: number;
  completedReferrals: number;
  pendingReferrals: number;
  totalEarnings: number;
}

export class PromotionService {
  private static instance: PromotionService;

  private constructor() {}

  static getInstance(): PromotionService {
    if (!PromotionService.instance) {
      PromotionService.instance = new PromotionService();
    }
    return PromotionService.instance;
  }

  async getActivePromotions(): Promise<Promotion[]> {
    const response = await api.get<Promotion[]>('/promotions');
    return response.data;
  }

  async getPromotion(id: string): Promise<Promotion> {
    const response = await api.get<Promotion>(`/promotions/${id}`);
    return response.data;
  }

  async getPromotionByCode(code: string): Promise<Promotion> {
    const response = await api.get<Promotion>(`/promotions/code/${code}`);
    return response.data;
  }

  async claimPromotion(code: string, request: ClaimPromotionRequest): Promise<Reward> {
    const response = await api.post<Reward>(`/promotions/${code}/claim`, request);
    return response.data;
  }

  async addLoyaltyPoints(request: CreateLoyaltyPointsRequest): Promise<LoyaltyPoints> {
    const response = await api.post<LoyaltyPoints>('/loyalty-points', request);
    return response.data;
  }

  async redeemLoyaltyPoints(request: RedeemLoyaltyPointsRequest): Promise<LoyaltyPoints> {
    const response = await api.post<LoyaltyPoints>('/loyalty-points/redeem', request);
    return response.data;
  }

  async getLoyaltyPoints(accountId: string): Promise<LoyaltyPoints[]> {
    const response = await api.get<LoyaltyPoints[]>(`/loyalty-points/account/${accountId}`);
    return response.data;
  }

  async getLoyaltyBalance(accountId: string): Promise<LoyaltyBalanceResponse> {
    const response = await api.get<LoyaltyBalanceResponse>(`/loyalty-points/account/${accountId}/balance`);
    return response.data;
  }

  async getCashbacks(accountId: string): Promise<Cashback[]> {
    const response = await api.get<Cashback[]>(`/cashbacks/account/${accountId}`);
    return response.data;
  }

  async getCashback(accountId: string): Promise<CashbackSummaryResponse> {
    const response = await api.get<CashbackSummaryResponse>(`/cashbacks/account/${accountId}/summary`);
    return response.data;
  }

  async createReferral(request: CreateReferralRequest): Promise<Referral> {
    const response = await api.post<Referral>('/referrals', request);
    return response.data;
  }

  async completeReferral(request: CompleteReferralRequest): Promise<Referral> {
    const response = await api.post<Referral>('/referrals/complete', request);
    return response.data;
  }

  async getReferralByCode(code: string): Promise<Referral> {
    const response = await api.get<Referral>(`/referrals/code/${code}`);
    return response.data;
  }

  async getReferrals(accountId: string): Promise<Referral[]> {
    const response = await api.get<Referral[]>(`/referrals/referrer/${accountId}`);
    return response.data;
  }

  async getReferralSummary(accountId: string): Promise<ReferralSummaryResponse> {
    const response = await api.get<ReferralSummaryResponse>(`/referrals/referrer/${accountId}/summary`);
    return response.data;
  }
}

export default PromotionService.getInstance();
