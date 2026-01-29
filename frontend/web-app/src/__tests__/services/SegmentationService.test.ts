import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  SegmentationService,
  type CustomerSegment,
  type SegmentMembership,
  type SegmentedOffer,
  type UserSegmentsResponse,
  type SegmentedOffersResponse,
  type SegmentTier,
  type SegmentStatus,
} from '@/services/SegmentationService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('SegmentationService', () => {
  let service: SegmentationService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = SegmentationService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = SegmentationService.getInstance();
    const instance2 = SegmentationService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('User Segments', () => {
    describe('getUserSegments', () => {
      it('should fetch user segments successfully', async () => {
        const mockResponse: UserSegmentsResponse = {
          memberships: [
            {
              id: 'membership_1',
              userId: 'user_123',
              segmentId: 'segment_1',
              segment: {
                id: 'segment_1',
                name: 'Gold Tier',
                description: 'Premium customers with high balance',
                tier: 'GOLD',
                minBalance: 50000000,
                maxBalance: 100000000,
                benefits: ['Free transfers', 'Priority support', 'Cashback rewards'],
                requirements: ['Maintain minimum balance', 'Monthly transactions'],
                createdAt: '2024-01-01T10:00:00Z',
                updatedAt: '2024-01-01T10:00:00Z',
              },
              status: 'ACTIVE',
              joinedAt: '2024-01-01T10:00:00Z',
              validUntil: '2024-12-31T23:59:59Z',
              score: 850,
            },
          ],
          currentTier: 'GOLD',
          nextTier: 'PLATINUM',
          progressToNext: 15,
          totalScore: 850,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getUserSegments('user_123');

        expect(api.get).toHaveBeenCalledWith('/segments/user/user_123');
        expect(result).toEqual(mockResponse);
        expect(result.currentTier).toBe('GOLD');
        expect(result.memberships).toHaveLength(1);
      });

      it('should handle user with no segments', async () => {
        const mockResponse: UserSegmentsResponse = {
          memberships: [],
          currentTier: 'BRONZE',
          nextTier: 'SILVER',
          progressToNext: 50,
          totalScore: 300,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getUserSegments('user_456');

        expect(result.memberships).toEqual([]);
        expect(result.currentTier).toBe('BRONZE');
      });

      it('should handle user with max tier (no next tier)', async () => {
        const mockResponse: UserSegmentsResponse = {
          memberships: [
            {
              id: 'membership_vip',
              userId: 'user_vip',
              segmentId: 'segment_vip',
              segment: {
                id: 'segment_vip',
                name: 'VIP Tier',
                description: 'Exclusive VIP customers',
                tier: 'VIP',
                minBalance: 500000000,
                benefits: ['All perks', 'Dedicated manager', 'Exclusive events'],
                requirements: ['Invitation only'],
                createdAt: '2024-01-01T10:00:00Z',
                updatedAt: '2024-01-01T10:00:00Z',
              },
              status: 'ACTIVE',
              joinedAt: '2024-01-01T10:00:00Z',
              score: 1000,
            },
          ],
          currentTier: 'VIP',
          totalScore: 1000,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getUserSegments('user_vip');

        expect(result.currentTier).toBe('VIP');
        expect(result.nextTier).toBeUndefined();
        expect(result.progressToNext).toBeUndefined();
      });
    });

    describe('getSegmentById', () => {
      it('should fetch segment by ID', async () => {
        const mockSegment: CustomerSegment = {
          id: 'segment_123',
          name: 'Platinum Tier',
          description: 'High net worth individuals',
          tier: 'PLATINUM',
          minBalance: 100000000,
          maxBalance: 500000000,
          benefits: [
            'Unlimited free transfers',
            'Concierge service',
            'Airport lounge access',
            'Exclusive promotions',
          ],
          requirements: ['Minimum balance 100M', 'Monthly income verification'],
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockSegment });

        const result = await service.getSegmentById('segment_123');

        expect(api.get).toHaveBeenCalledWith('/segments/segment_123');
        expect(result.tier).toBe('PLATINUM');
        expect(result.benefits).toHaveLength(4);
      });
    });

    describe('getAllSegments', () => {
      it('should fetch all segments', async () => {
        const mockSegments: CustomerSegment[] = [
          {
            id: 'segment_1',
            name: 'Bronze Tier',
            description: 'Entry level customers',
            tier: 'BRONZE',
            minBalance: 0,
            maxBalance: 10000000,
            benefits: ['Basic banking'],
            requirements: ['Active account'],
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T10:00:00Z',
          },
          {
            id: 'segment_2',
            name: 'Silver Tier',
            description: 'Regular customers',
            tier: 'SILVER',
            minBalance: 10000000,
            maxBalance: 50000000,
            benefits: ['Free transfers', 'Mobile banking'],
            requirements: ['Minimum balance 10M'],
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T10:00:00Z',
          },
          {
            id: 'segment_3',
            name: 'Gold Tier',
            description: 'Premium customers',
            tier: 'GOLD',
            minBalance: 50000000,
            maxBalance: 100000000,
            benefits: ['Priority support', 'Cashback rewards'],
            requirements: ['Minimum balance 50M'],
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockSegments });

        const result = await service.getAllSegments();

        expect(api.get).toHaveBeenCalledWith('/segments');
        expect(result).toHaveLength(3);
        expect(result[0].tier).toBe('BRONZE');
        expect(result[1].tier).toBe('SILVER');
        expect(result[2].tier).toBe('GOLD');
      });

      it('should handle empty segments list', async () => {
        vi.mocked(api.get).mockResolvedValue({ data: [] });

        const result = await service.getAllSegments();

        expect(result).toEqual([]);
      });
    });
  });

  describe('Segmented Offers', () => {
    describe('getSegmentedOffers', () => {
      it('should fetch segmented offers for user with default pagination', async () => {
        const mockResponse: SegmentedOffersResponse = {
          offers: [
            {
              id: 'offer_1',
              title: 'Exclusive Gold Discount',
              description: 'Special discount for Gold tier customers',
              segmentId: 'segment_gold',
              segmentTier: 'GOLD',
              offerType: 'DISCOUNT',
              value: 20,
              currency: 'IDR',
              percentage: 20,
              validFrom: '2024-01-01T00:00:00Z',
              validUntil: '2024-12-31T23:59:59Z',
              terms: ['Minimum transaction 100K', 'Valid for selected merchants'],
              imageUrl: 'https://example.com/gold-discount.jpg',
              promoCode: 'GOLD20',
              minTransaction: 100000,
              maxReward: 50000,
              isActive: true,
              createdAt: '2024-01-01T10:00:00Z',
            },
          ],
          totalCount: 1,
          page: 0,
          size: 10,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getSegmentedOffers('user_123');

        expect(api.get).toHaveBeenCalledWith('/segments/user/user_123/offers', {
          params: { page: 0, size: 10 },
        });
        expect(result.offers).toHaveLength(1);
        expect(result.totalCount).toBe(1);
      });

      it('should fetch segmented offers with custom pagination', async () => {
        const mockResponse: SegmentedOffersResponse = {
          offers: [],
          totalCount: 0,
          page: 1,
          size: 20,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getSegmentedOffers('user_456', 1, 20);

        expect(api.get).toHaveBeenCalledWith('/segments/user/user_456/offers', {
          params: { page: 1, size: 20 },
        });
        expect(result.offers).toEqual([]);
      });

      it('should handle all offer types', async () => {
        const offerTypes = ['CASHBACK', 'DISCOUNT', 'REWARD_POINTS', 'FREE_TRANSFER', 'BONUS_INTEREST'];

        for (const type of offerTypes) {
          const mockResponse: SegmentedOffersResponse = {
            offers: [
              {
                id: `offer_${type}`,
                title: `${type} Offer`,
                description: `Test ${type} offer`,
                segmentId: 'segment_test',
                segmentTier: 'SILVER',
                offerType: type as any,
                value: 100,
                validFrom: '2024-01-01T00:00:00Z',
                validUntil: '2024-12-31T23:59:59Z',
                terms: [],
                isActive: true,
                createdAt: '2024-01-01T10:00:00Z',
              },
            ],
            totalCount: 1,
            page: 0,
            size: 10,
          };

          vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

          const result = await service.getSegmentedOffers('user_test');

          expect(result.offers[0].offerType).toBe(type);
        }
      });
    });

    describe('getOffersBySegment', () => {
      it('should fetch offers by segment ID', async () => {
        const mockResponse: SegmentedOffersResponse = {
          offers: [
            {
              id: 'offer_segment_1',
              title: 'Silver Exclusive',
              description: 'Offers for Silver tier',
              segmentId: 'segment_silver',
              segmentTier: 'SILVER',
              offerType: 'CASHBACK',
              value: 10,
              currency: 'IDR',
              validFrom: '2024-01-01T00:00:00Z',
              validUntil: '2024-12-31T23:59:59Z',
              terms: ['Valid for all transactions'],
              isActive: true,
              createdAt: '2024-01-01T10:00:00Z',
            },
          ],
          totalCount: 1,
          page: 0,
          size: 10,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getOffersBySegment('segment_silver');

        expect(api.get).toHaveBeenCalledWith('/segments/segment_silver/offers', {
          params: { page: 0, size: 10 },
        });
        expect(result.offers[0].segmentTier).toBe('SILVER');
      });
    });
  });

  describe('Segment Users', () => {
    describe('getSegmentUsers', () => {
      it('should fetch users in a segment with default pagination', async () => {
        const mockResponse = {
          users: [
            {
              userId: 'user_1',
              fullName: 'John Doe',
              score: 750,
              joinedAt: '2024-01-01T10:00:00Z',
            },
            {
              userId: 'user_2',
              fullName: 'Jane Smith',
              score: 820,
              joinedAt: '2024-01-02T10:00:00Z',
            },
          ],
          totalCount: 2,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getSegmentUsers('segment_123');

        expect(api.get).toHaveBeenCalledWith('/segments/segment_123/users', {
          params: { page: 0, size: 20 },
        });
        expect(result.users).toHaveLength(2);
        expect(result.totalCount).toBe(2);
      });

      it('should fetch users with custom pagination', async () => {
        const mockResponse = {
          users: [],
          totalCount: 0,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getSegmentUsers('segment_456', 1, 50);

        expect(api.get).toHaveBeenCalledWith('/segments/segment_456/users', {
          params: { page: 1, size: 50 },
        });
        expect(result.users).toEqual([]);
      });

      it('should handle users with different scores', async () => {
        const mockResponse = {
          users: [
            { userId: 'user_high', fullName: 'High Scorer', score: 950, joinedAt: '2024-01-01T10:00:00Z' },
            { userId: 'user_mid', fullName: 'Mid Scorer', score: 650, joinedAt: '2024-01-01T10:00:00Z' },
            { userId: 'user_low', fullName: 'Low Scorer', score: 350, joinedAt: '2024-01-01T10:00:00Z' },
          ],
          totalCount: 3,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getSegmentUsers('segment_scores');

        expect(result.users[0].score).toBeGreaterThan(result.users[1].score);
        expect(result.users[1].score).toBeGreaterThan(result.users[2].score);
      });
    });
  });

  describe('Helper Methods', () => {
    describe('isVIPSegment', () => {
      it('should return true for VIP tier', () => {
        expect(service.isVIPSegment('VIP')).toBe(true);
      });

      it('should return true for DIAMOND tier', () => {
        expect(service.isVIPSegment('DIAMOND')).toBe(true);
      });

      it('should return true for PLATINUM tier', () => {
        expect(service.isVIPSegment('PLATINUM')).toBe(true);
      });

      it('should return false for non-VIP tiers', () => {
        expect(service.isVIPSegment('BRONZE')).toBe(false);
        expect(service.isVIPSegment('SILVER')).toBe(false);
        expect(service.isVIPSegment('GOLD')).toBe(false);
      });
    });

    describe('getTierPriority', () => {
      it('should return correct priority for all tiers', () => {
        expect(service.getTierPriority('BRONZE')).toBe(1);
        expect(service.getTierPriority('SILVER')).toBe(2);
        expect(service.getTierPriority('GOLD')).toBe(3);
        expect(service.getTierPriority('PLATINUM')).toBe(4);
        expect(service.getTierPriority('DIAMOND')).toBe(5);
        expect(service.getTierPriority('VIP')).toBe(6);
      });

      it('should handle tier comparison', () => {
        const goldPriority = service.getTierPriority('GOLD');
        const platinumPriority = service.getTierPriority('PLATINUM');

        expect(platinumPriority).toBeGreaterThan(goldPriority);
      });

      it('should return 0 for invalid tier', () => {
        const invalidTier = 'INVALID' as SegmentTier;
        expect(service.getTierPriority(invalidTier)).toBe(0);
      });
    });
  });

  describe('Segment Tiers', () => {
    it('should handle all segment tiers', async () => {
      const tiers: SegmentTier[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND', 'VIP'];

      for (const tier of tiers) {
        const mockSegment: CustomerSegment = {
          id: `segment_${tier}`,
          name: `${tier} Segment`,
          description: `Description for ${tier}`,
          tier: tier,
          minBalance: 0,
          benefits: [],
          requirements: [],
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockSegment });

        const result = await service.getSegmentById(`segment_${tier}`);

        expect(result.tier).toBe(tier);
      }
    });
  });

  describe('Segment Status', () => {
    it('should handle all segment statuses', async () => {
      const statuses: SegmentStatus[] = ['ACTIVE', 'INACTIVE', 'PENDING'];

      for (const status of statuses) {
        const mockResponse: UserSegmentsResponse = {
          memberships: [
            {
              id: `membership_${status}`,
              userId: 'user_123',
              segmentId: `segment_${status}`,
              segment: {
                id: `segment_${status}`,
                name: `Segment ${status}`,
                description: `Description`,
                tier: 'GOLD',
                minBalance: 50000000,
                benefits: [],
                requirements: [],
                createdAt: '2024-01-01T10:00:00Z',
                updatedAt: '2024-01-01T10:00:00Z',
              },
              status: status,
              joinedAt: '2024-01-01T10:00:00Z',
              score: 500,
            },
          ],
          currentTier: 'GOLD',
          totalScore: 500,
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

        const result = await service.getUserSegments('user_123');

        expect(result.memberships[0].status).toBe(status);
      }
    });
  });

  describe('Data transformation', () => {
    it('should correctly transform segment data with balance ranges', async () => {
      const mockSegment: CustomerSegment = {
        id: 'segment_transform',
        name: 'Balance Tier',
        description: 'Segment with balance range',
        tier: 'SILVER',
        minBalance: 10000000,
        maxBalance: 50000000,
        benefits: ['Tier-based benefits'],
        requirements: ['Balance requirements'],
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: '2024-01-01T10:00:00Z',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockSegment });

      const result = await service.getSegmentById('segment_transform');

      expect(result.minBalance).toBe(10000000);
      expect(result.maxBalance).toBe(50000000);
    });

    it('should correctly transform offer data with all fields', async () => {
      const mockResponse: SegmentedOffersResponse = {
        offers: [
          {
            id: 'offer_full',
            title: 'Complete Offer',
            description: 'Offer with all fields',
            segmentId: 'segment_full',
            segmentTier: 'GOLD',
            offerType: 'CASHBACK',
            value: 50000,
            currency: 'IDR',
            percentage: 10,
            validFrom: '2024-01-01T00:00:00Z',
            validUntil: '2024-12-31T23:59:59Z',
            terms: ['Term 1', 'Term 2', 'Term 3'],
            imageUrl: 'https://example.com/offer.jpg',
            promoCode: 'GOLD50',
            minTransaction: 100000,
            maxReward: 50000,
            isActive: true,
            createdAt: '2024-01-01T10:00:00Z',
          },
        ],
        totalCount: 1,
        page: 0,
        size: 10,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

      const result = await service.getSegmentedOffers('user_full');

      expect(result.offers[0].terms).toHaveLength(3);
      expect(result.offers[0].promoCode).toBe('GOLD50');
      expect(result.offers[0].minTransaction).toBe(100000);
    });
  });

  describe('Error handling', () => {
    it('should handle user not found', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('User not found'));

      await expect(service.getUserSegments('invalid_user')).rejects.toThrow('User not found');
    });

    it('should handle segment not found', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Segment not found'));

      await expect(service.getSegmentById('invalid_segment')).rejects.toThrow('Segment not found');
    });

    it('should handle network errors', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network error'));

      await expect(service.getAllSegments()).rejects.toThrow('Network error');
    });

    it('should handle pagination errors', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Invalid pagination parameters'));

      await expect(service.getSegmentedOffers('user_123', -1, 0)).rejects.toThrow('Invalid pagination parameters');
    });
  });

  describe('Edge cases', () => {
    it('should handle segment without maxBalance', async () => {
      const mockSegment: CustomerSegment = {
        id: 'segment_unlimited',
        name: 'Unlimited Tier',
        description: 'Tier with no maximum balance',
        tier: 'VIP',
        minBalance: 1000000000,
        benefits: ['VIP benefits'],
        requirements: ['VIP requirements'],
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: '2024-01-01T10:00:00Z',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockSegment });

      const result = await service.getSegmentById('segment_unlimited');

      expect(result.maxBalance).toBeUndefined();
    });

    it('should handle segment membership without validUntil', async () => {
      const mockResponse: UserSegmentsResponse = {
        memberships: [
          {
            id: 'membership_permanent',
            userId: 'user_permanent',
            segmentId: 'segment_permanent',
            segment: {
              id: 'segment_permanent',
              name: 'Permanent Tier',
              description: 'Permanent membership',
              tier: 'VIP',
              minBalance: 1000000000,
              benefits: [],
              requirements: [],
              createdAt: '2024-01-01T10:00:00Z',
              updatedAt: '2024-01-01T10:00:00Z',
            },
            status: 'ACTIVE',
            joinedAt: '2024-01-01T10:00:00Z',
            score: 1000,
          },
        ],
        currentTier: 'VIP',
        totalScore: 1000,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

      const result = await service.getUserSegments('user_permanent');

      expect(result.memberships[0].validUntil).toBeUndefined();
    });
  });
});
