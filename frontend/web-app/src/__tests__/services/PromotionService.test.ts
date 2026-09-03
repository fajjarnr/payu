import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  PromotionService,
  type Promotion,
  type ClaimPromotionRequest,
  type Reward,
  type LoyaltyPoints,
  type Cashback,
  type Referral,
  type CreateLoyaltyPointsRequest,
  type RedeemLoyaltyPointsRequest,
  type CreateReferralRequest,
  type CompleteReferralRequest,
  type PromotionStatus,
  type RewardType,
} from '@/services/PromotionService';
import api from '@/lib/api';
import { asMoney } from '@/lib/currency';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('PromotionService', () => {
  let service: PromotionService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = PromotionService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = PromotionService.getInstance();
    const instance2 = PromotionService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('Promotions', () => {
    describe('getActivePromotions', () => {
      it('should fetch all active promotions', async () => {
        const mockPromotions: Promotion[] = [
          {
            id: 'promo_1',
            code: 'NEWYEAR2024',
            name: 'New Year Promo',
            description: 'Special discount for New Year',
            type: 'CASHBACK',
            value: 20,
            status: 'ACTIVE',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-12-31T23:59:59Z',
            maxClaims: 1000,
            currentClaims: 500,
            minTransactionAmount: 100000,
            categories: ['FOOD', 'TRAVEL'],
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockPromotions });

        const result = await service.getActivePromotions();

        expect(api.get).toHaveBeenCalledWith('/promotions');
        expect(result).toEqual(mockPromotions);
      });

      it('should handle empty promotions list', async () => {
        vi.mocked(api.get).mockResolvedValue({ data: [] });

        const result = await service.getActivePromotions();

        expect(result).toEqual([]);
      });

      it('should fetch promotions with all status types', async () => {
        const statuses: PromotionStatus[] = ['DRAFT', 'ACTIVE', 'INACTIVE', 'EXPIRED'];

        for (const status of statuses) {
          const mockPromotions: Promotion[] = [
            {
              id: `promo_${status}`,
              code: `${status}_CODE`,
              name: `${status} Promo`,
              description: `Test ${status} promotion`,
              type: 'CASHBACK',
              value: 10,
              status: status,
              startDate: '2024-01-01T00:00:00Z',
              endDate: '2024-12-31T23:59:59Z',
              currentClaims: 0,
              createdAt: '2024-01-01T10:00:00Z',
              updatedAt: '2024-01-01T10:00:00Z',
            },
          ];

          vi.mocked(api.get).mockResolvedValue({ data: mockPromotions });

          const result = await service.getActivePromotions();

          expect(result[0].status).toBe(status);
        }
      });
    });

    describe('getPromotion', () => {
      it('should fetch promotion by ID', async () => {
        const mockPromotion: Promotion = {
          id: 'promo_123',
          code: 'FLASHSALE',
          name: 'Flash Sale',
          description: 'Limited time flash sale',
          type: 'VOUCHER',
          value: 50000,
          status: 'ACTIVE',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-01-31T23:59:59Z',
          maxClaims: 100,
          currentClaims: 25,
          minTransactionAmount: 50000,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockPromotion });

        const result = await service.getPromotion('promo_123');

        expect(api.get).toHaveBeenCalledWith('/promotions/promo_123');
        expect(result).toEqual(mockPromotion);
      });
    });

    describe('getPromotionByCode', () => {
      it('should fetch promotion by code', async () => {
        const mockPromotion: Promotion = {
          id: 'promo_456',
          code: 'WELCOME10',
          name: 'Welcome Bonus',
          description: 'Welcome offer for new users',
          type: 'LOYALTY_POINTS',
          value: 1000,
          status: 'ACTIVE',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          currentClaims: 0,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockPromotion });

        const result = await service.getPromotionByCode('WELCOME10');

        expect(api.get).toHaveBeenCalledWith('/promotions/code/WELCOME10');
        expect(result.code).toBe('WELCOME10');
      });
    });

    describe('claimPromotion', () => {
      it('should claim promotion successfully', async () => {
        const mockRequest: ClaimPromotionRequest = {
          accountId: 'acc_123',
          transactionId: 'txn_123',
        };

        const mockReward: Reward = {
          id: 'reward_123',
          accountId: 'acc_123',
          promotionId: 'promo_123',
          promotionCode: 'FLASHSALE',
          type: 'CASHBACK',
          value: 10000,
          status: 'APPROVED',
          expiresAt: '2024-02-01T00:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockReward });

        const result = await service.claimPromotion('FLASHSALE', mockRequest);

        expect(api.post).toHaveBeenCalledWith('/promotions/FLASHSALE/claim', mockRequest);
        expect(result.status).toBe('APPROVED');
      });

      it('should claim promotion without transaction ID', async () => {
        const mockRequest: ClaimPromotionRequest = {
          accountId: 'acc_456',
        };

        const mockReward: Reward = {
          id: 'reward_456',
          accountId: 'acc_456',
          promotionId: 'promo_456',
          promotionCode: 'WELCOME10',
          type: 'LOYALTY_POINTS',
          value: 500,
          status: 'PENDING',
          expiresAt: '2024-02-01T00:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockReward });

        const result = await service.claimPromotion('WELCOME10', mockRequest);

        expect(result.status).toBe('PENDING');
      });
    });
  });

  describe('Loyalty Points', () => {
    describe('addLoyaltyPoints', () => {
      it('should add loyalty points to account', async () => {
        const mockRequest: CreateLoyaltyPointsRequest = {
          accountId: 'acc_123',
          points: 1000,
          description: 'Purchase reward',
          referenceId: 'txn_123',
        };

        const mockPoints: LoyaltyPoints = {
          id: 'points_123',
          accountId: 'acc_123',
          points: 1000,
          type: 'EARNED',
          description: 'Purchase reward',
          referenceId: 'txn_123',
          createdAt: '2024-01-01T10:00:00Z',
          expiresAt: '2025-01-01T00:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockPoints });

        const result = await service.addLoyaltyPoints(mockRequest);

        expect(api.post).toHaveBeenCalledWith('/loyalty-points', mockRequest);
        expect(result.type).toBe('EARNED');
        expect(result.points).toBe(1000);
      });

      it('should add loyalty points without reference ID', async () => {
        const mockRequest: CreateLoyaltyPointsRequest = {
          accountId: 'acc_456',
          points: 500,
          description: 'Bonus points',
        };

        const mockPoints: LoyaltyPoints = {
          id: 'points_456',
          accountId: 'acc_456',
          points: 500,
          type: 'EARNED',
          description: 'Bonus points',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockPoints });

        const result = await service.addLoyaltyPoints(mockRequest);

        expect(result.referenceId).toBeUndefined();
      });
    });

    describe('redeemLoyaltyPoints', () => {
      it('should redeem loyalty points', async () => {
        const mockRequest: RedeemLoyaltyPointsRequest = {
          accountId: 'acc_123',
          points: 500,
          description: 'Redeem for voucher',
        };

        const mockPoints: LoyaltyPoints = {
          id: 'points_redeem',
          accountId: 'acc_123',
          points: 500,
          type: 'REDEEMED',
          description: 'Redeem for voucher',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockPoints });

        const result = await service.redeemLoyaltyPoints(mockRequest);

        expect(api.post).toHaveBeenCalledWith('/loyalty-points/redeem', mockRequest);
        expect(result.type).toBe('REDEEMED');
      });
    });

    describe('getLoyaltyPoints', () => {
      it('should fetch loyalty points for account', async () => {
        const mockPoints: LoyaltyPoints[] = [
          {
            id: 'points_1',
            accountId: 'acc_123',
            points: 1000,
            type: 'EARNED',
            description: 'Purchase reward',
            createdAt: '2024-01-01T10:00:00Z',
            expiresAt: '2025-01-01T00:00:00Z',
          },
          {
            id: 'points_2',
            accountId: 'acc_123',
            points: 500,
            type: 'REDEEMED',
            description: 'Voucher redemption',
            createdAt: '2024-01-02T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockPoints });

        const result = await service.getLoyaltyPoints('acc_123');

        expect(api.get).toHaveBeenCalledWith('/loyalty-points/account/acc_123');
        expect(result).toHaveLength(2);
        expect(result[0].type).toBe('EARNED');
        expect(result[1].type).toBe('REDEEMED');
      });
    });

    describe('getLoyaltyBalance', () => {
      it('should fetch loyalty balance summary', async () => {
        const mockBalance = {
          accountId: 'acc_123',
          totalEarned: 5000,
          totalRedeemed: 2000,
          currentBalance: 3000,
          pointsExpiring: 1000,
          expiryDate: '2024-02-01T00:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockBalance });

        const result = await service.getLoyaltyBalance('acc_123');

        expect(api.get).toHaveBeenCalledWith('/loyalty-points/account/acc_123/balance');
        expect(result.currentBalance).toBe(3000);
        expect(result.pointsExpiring).toBe(1000);
      });
    });
  });

  describe('Cashback', () => {
    describe('getCashbacks', () => {
      it('should fetch cashbacks for account', async () => {
        const mockCashbacks: Cashback[] = [
          {
            id: 'cashback_1',
            accountId: 'acc_123',
            amount: asMoney('10000'),
            type: 'PERCENTAGE',
            referenceId: 'txn_123',
            merchantName: 'Tokopedia',
            status: 'CREDITED',
            createdAt: '2024-01-01T10:00:00Z',
            creditedAt: '2024-01-02T10:00:00Z',
          },
          {
            id: 'cashback_2',
            accountId: 'acc_123',
            amount: asMoney('5000'),
            type: 'FIXED',
            referenceId: 'txn_456',
            status: 'PENDING',
            createdAt: '2024-01-01T11:00:00Z',
            expiresAt: '2024-02-01T00:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockCashbacks });

        const result = await service.getCashbacks('acc_123');

        expect(api.get).toHaveBeenCalledWith('/cashbacks/account/acc_123');
        expect(result).toHaveLength(2);
        expect(result[0].type).toBe('PERCENTAGE');
        expect(result[1].type).toBe('FIXED');
      });

      it('should handle all cashback statuses', async () => {
        const statuses = ['PENDING', 'APPROVED', 'CREDITED', 'EXPIRED'];

        for (const status of statuses) {
          const mockCashbacks: Cashback[] = [
            {
              id: `cashback_${status}`,
              accountId: 'acc_123',
              amount: asMoney('1000'),
              type: 'FIXED',
              referenceId: 'txn_ref',
              status: status as Cashback['status'],
              createdAt: '2024-01-01T10:00:00Z',
            },
          ];

          vi.mocked(api.get).mockResolvedValue({ data: mockCashbacks });

          const result = await service.getCashbacks('acc_123');

          expect(result[0].status).toBe(status);
        }
      });
    });

    describe('getCashback', () => {
      it('should fetch cashback summary for account', async () => {
        const mockSummary = {
          accountId: 'acc_123',
          totalCashback: asMoney('150000'),
          pendingCashback: asMoney('25000'),
          creditedCashback: asMoney('100000'),
          expiredCashback: asMoney('25000'),
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockSummary });

        const result = await service.getCashback('acc_123');

        expect(api.get).toHaveBeenCalledWith('/cashbacks/account/acc_123/summary');
        expect(result.totalCashback).toBe('150000');
        expect(result.creditedCashback).toBe('100000');
      });
    });
  });

  describe('Referrals', () => {
    describe('createReferral', () => {
      it('should create new referral', async () => {
        const mockRequest: CreateReferralRequest = {
          referrerAccountId: 'acc_123',
          referrerReward: asMoney('50000'),
          refereeReward: asMoney('25000'),
          rewardType: 'CASHBACK',
          expiryDate: '2024-01-31T00:00:00Z',
        };

        const mockReferral: Referral = {
          id: 'ref_123',
          referrerAccountId: 'acc_123',
          referralCode: 'REF123ABC',
          referrerReward: asMoney('50000'),
          refereeReward: asMoney('25000'),
          rewardType: 'CASHBACK',
          status: 'PENDING',
          expiryDate: '2024-01-31T00:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockReferral });

        const result = await service.createReferral(mockRequest);

        expect(api.post).toHaveBeenCalledWith('/referrals', mockRequest);
        expect(result.status).toBe('PENDING');
        expect(result.referralCode).toBeDefined();
      });
    });

    describe('completeReferral', () => {
      it('should complete referral', async () => {
        const mockRequest: CompleteReferralRequest = {
          code: 'REF123ABC',
          refereeAccountId: 'acc_456',
        };

        const mockReferral: Referral = {
          id: 'ref_123',
          referrerAccountId: 'acc_123',
          refereeAccountId: 'acc_456',
          referralCode: 'REF123ABC',
          referrerReward: asMoney('50000'),
          refereeReward: asMoney('25000'),
          rewardType: 'CASHBACK',
          status: 'COMPLETED',
          completedAt: '2024-01-15T10:00:00Z',
          expiryDate: '2024-01-31T00:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockReferral });

        const result = await service.completeReferral(mockRequest);

        expect(api.post).toHaveBeenCalledWith('/referrals/complete', mockRequest);
        expect(result.status).toBe('COMPLETED');
        expect(result.refereeAccountId).toBe('acc_456');
      });
    });

    describe('getReferralByCode', () => {
      it('should fetch referral by code', async () => {
        const mockReferral: Referral = {
          id: 'ref_456',
          referrerAccountId: 'acc_789',
          referralCode: 'MYREFCODE',
          referrerReward: asMoney('75000'),
          refereeReward: asMoney('25000'),
          rewardType: 'CASHBACK',
          status: 'PENDING',
          expiryDate: '2024-02-01T00:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockReferral });

        const result = await service.getReferralByCode('MYREFCODE');

        expect(api.get).toHaveBeenCalledWith('/referrals/code/MYREFCODE');
        expect(result.referralCode).toBe('MYREFCODE');
      });
    });

    describe('getReferrals', () => {
      it('should fetch referrals for referrer', async () => {
        const mockReferrals: Referral[] = [
          {
            id: 'ref_1',
            referrerAccountId: 'acc_123',
            refereeAccountId: 'acc_456',
            referralCode: 'REF1',
            referrerReward: asMoney('50000'),
            refereeReward: asMoney('25000'),
            rewardType: 'CASHBACK',
            status: 'COMPLETED',
            completedAt: '2024-01-10T10:00:00Z',
            expiryDate: '2024-01-31T00:00:00Z',
            createdAt: '2024-01-01T10:00:00Z',
          },
          {
            id: 'ref_2',
            referrerAccountId: 'acc_123',
            referralCode: 'REF2',
            referrerReward: asMoney('50000'),
            refereeReward: asMoney('25000'),
            rewardType: 'CASHBACK',
            status: 'PENDING',
            expiryDate: '2024-02-01T00:00:00Z',
            createdAt: '2024-01-05T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockReferrals });

        const result = await service.getReferrals('acc_123');

        expect(api.get).toHaveBeenCalledWith('/referrals/referrer/acc_123');
        expect(result).toHaveLength(2);
        expect(result[0].status).toBe('COMPLETED');
        expect(result[1].status).toBe('PENDING');
      });
    });

    describe('getReferralSummary', () => {
      it('should fetch referral summary for referrer', async () => {
        const mockSummary = {
          referralCode: 'REF123ABC',
          totalReferrals: 10,
          completedReferrals: 7,
          pendingReferrals: 3,
          totalEarnings: asMoney('350000'),
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockSummary });

        const result = await service.getReferralSummary('acc_123');

        expect(api.get).toHaveBeenCalledWith('/referrals/referrer/acc_123/summary');
        expect(result.totalReferrals).toBe(10);
        expect(result.completedReferrals).toBe(7);
        expect(result.totalEarnings).toBe('350000');
      });
    });
  });

  describe('Reward Types', () => {
    it('should handle all reward types', async () => {
      const rewardTypes: RewardType[] = ['LOYALTY_POINTS', 'CASHBACK', 'VOUCHER'];

      for (const type of rewardTypes) {
        const mockPromotions: Promotion[] = [
          {
            id: `promo_${type}`,
            code: `${type}_CODE`,
            name: `${type} Promo`,
            description: `Test ${type} promotion`,
            type: type,
            value: 100,
            status: 'ACTIVE',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-12-31T23:59:59Z',
            currentClaims: 0,
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockPromotions });

        const result = await service.getActivePromotions();

        expect(result[0].type).toBe(type);
      }
    });
  });

  describe('Data transformation', () => {
    it('should correctly transform promotion data', async () => {
      const apiResponse = {
        data: {
          id: 'promo_transform',
          code: 'TRANSFORM',
          name: 'Transform Test',
          description: 'Testing data transformation',
          type: 'CASHBACK',
          value: 15.5,
          status: 'ACTIVE',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          maxClaims: 100,
          currentClaims: 25,
          minTransactionAmount: 50000,
          categories: ['FOOD', 'BEVERAGE'],
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T11:00:00Z',
        },
      };

      vi.mocked(api.get).mockResolvedValue(apiResponse);

      const result = await service.getPromotion('promo_transform');

      expect(result.value).toBe(15.5);
      expect(result.categories).toEqual(['FOOD', 'BEVERAGE']);
    });
  });

  describe('Error handling', () => {
    it('should handle promotion not found', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Promotion not found'));

      await expect(service.getPromotion('invalid_id')).rejects.toThrow('Promotion not found');
    });

    it('should handle invalid promo code', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Invalid promo code'));

      await expect(service.getPromotionByCode('INVALID')).rejects.toThrow('Invalid promo code');
    });

    it('should handle insufficient loyalty points', async () => {
      const mockRequest: RedeemLoyaltyPointsRequest = {
        accountId: 'acc_123',
        points: 10000,
        description: 'Redeem points',
      };

      vi.mocked(api.post).mockRejectedValue(new Error('Insufficient points balance'));

      await expect(service.redeemLoyaltyPoints(mockRequest)).rejects.toThrow('Insufficient points balance');
    });

    it('should handle expired referral code', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Referral code expired'));

      await expect(service.getReferralByCode('EXPIRED')).rejects.toThrow('Referral code expired');
    });
  });
});
