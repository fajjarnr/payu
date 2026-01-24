import api from '@/lib/api';

export type SegmentTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND' | 'VIP';
export type SegmentStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

export interface CustomerSegment {
  id: string;
  name: string;
  description: string;
  tier: SegmentTier;
  minBalance: number;
  maxBalance?: number;
  benefits: string[];
  requirements: string[];
  createdAt: string;
  updatedAt: string;
}

export interface SegmentMembership {
  id: string;
  userId: string;
  segmentId: string;
  segment: CustomerSegment;
  status: SegmentStatus;
  joinedAt: string;
  validUntil?: string;
  score: number;
}

export interface SegmentedOffer {
  id: string;
  title: string;
  description: string;
  segmentId: string;
  segmentTier: SegmentTier;
  offerType: 'CASHBACK' | 'DISCOUNT' | 'REWARD_POINTS' | 'FREE_TRANSFER' | 'BONUS_INTEREST';
  value: number;
  currency?: string;
  percentage?: number;
  validFrom: string;
  validUntil: string;
  terms: string[];
  imageUrl?: string;
  promoCode?: string;
  minTransaction?: number;
  maxReward?: number;
  isActive: boolean;
  createdAt: string;
}

export interface UserSegmentsResponse {
  memberships: SegmentMembership[];
  currentTier: SegmentTier;
  nextTier?: SegmentTier;
  progressToNext?: number;
  totalScore: number;
}

export interface SegmentedOffersResponse {
  offers: SegmentedOffer[];
  totalCount: number;
  page: number;
  size: number;
}

export class SegmentationService {
  private static instance: SegmentationService;

  private constructor() {}

  static getInstance(): SegmentationService {
    if (!SegmentationService.instance) {
      SegmentationService.instance = new SegmentationService();
    }
    return SegmentationService.instance;
  }

  async getUserSegments(userId: string): Promise<UserSegmentsResponse> {
    const response = await api.get<UserSegmentsResponse>(`/segments/user/${userId}`);
    return response.data;
  }

  async getSegmentById(segmentId: string): Promise<CustomerSegment> {
    const response = await api.get<CustomerSegment>(`/segments/${segmentId}`);
    return response.data;
  }

  async getAllSegments(): Promise<CustomerSegment[]> {
    const response = await api.get<CustomerSegment[]>('/segments');
    return response.data;
  }

  async getSegmentedOffers(userId: string, page = 0, size = 10): Promise<SegmentedOffersResponse> {
    const response = await api.get<SegmentedOffersResponse>(`/segments/user/${userId}/offers`, {
      params: { page, size }
    });
    return response.data;
  }

  async getOffersBySegment(segmentId: string, page = 0, size = 10): Promise<SegmentedOffersResponse> {
    const response = await api.get<SegmentedOffersResponse>(`/segments/${segmentId}/offers`, {
      params: { page, size }
    });
    return response.data;
  }

  async getSegmentUsers(segmentId: string, page = 0, size = 20): Promise<{
    users: Array<{
      userId: string;
      fullName: string;
      score: number;
      joinedAt: string;
    }>;
    totalCount: number;
  }> {
    const response = await api.get(`/segments/${segmentId}/users`, {
      params: { page, size }
    });
    return response.data;
  }

  isVIPSegment(tier: SegmentTier): boolean {
    return tier === 'VIP' || tier === 'DIAMOND' || tier === 'PLATINUM';
  }

  getTierPriority(tier: SegmentTier): number {
    const priority: Record<SegmentTier, number> = {
      BRONZE: 1,
      SILVER: 2,
      GOLD: 3,
      PLATINUM: 4,
      DIAMOND: 5,
      VIP: 6
    };
    return priority[tier] || 0;
  }
}

export default SegmentationService.getInstance();
