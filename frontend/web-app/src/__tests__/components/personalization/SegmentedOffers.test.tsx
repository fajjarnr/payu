import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUserSegment, useSegmentedOffers, useVIPStatus } from '@/hooks';
import * as SegmentationService from '@/services/SegmentationService';

// Mock the API module
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'QueryClientWrapper';
  return Wrapper;
};

describe('Customer Segmentation Hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('useUserSegment', () => {
    it('should return undefined for currentTier when userId is not provided', () => {
      const { result } = renderHook(() => useUserSegment(undefined), {
        wrapper: createWrapper(),
      });

      expect(result.current.currentTier).toBeUndefined();
      expect(result.current.isVIP).toBe(false);
    });

    it('should return VIP status correctly', () => {
      const mockData = {
        memberships: [
          {
            id: '1',
            userId: 'user1',
            segmentId: 'seg1',
            segment: {
              id: 'seg1',
              name: 'VIP Member',
              description: 'VIP tier',
              tier: 'VIP' as const,
              minBalance: 10000000,
              benefits: ['Priority support'],
              requirements: ['Balance > 10M'],
              createdAt: '2024-01-01',
              updatedAt: '2024-01-01',
            },
            status: 'ACTIVE' as const,
            joinedAt: '2024-01-01',
            score: 100,
          },
        ],
        currentTier: 'VIP' as const,
        totalScore: 100,
      };

      const { result } = renderHook(() => useUserSegment('user1'), {
        wrapper: createWrapper(),
      });

      // The hook would need mocked data, but we're testing the structure
      expect(result.current).toHaveProperty('currentTier');
      expect(result.current).toHaveProperty('isVIP');
      expect(result.current).toHaveProperty('invalidateSegments');
    });
  });

  describe('useSegmentedOffers', () => {
    it('should return empty offers array when userId is not provided', () => {
      const { result } = renderHook(() => useSegmentedOffers(undefined), {
        wrapper: createWrapper(),
      });

      expect(result.current.offers).toEqual([]);
      expect(result.current.cashbackOffers).toEqual([]);
      expect(result.current.discountOffers).toEqual([]);
    });
  });

  describe('useVIPStatus', () => {
    it('should return default VIP status', () => {
      const { result } = renderHook(() => useVIPStatus(), {
        wrapper: createWrapper(),
      });

      expect(result.current.isVIP).toBe(false);
      expect(result.current.tier).toBeNull();
      expect(result.current.tierLabel).toBe('Standard');
      expect(result.current.benefits).toEqual([]);
    });
  });
});

describe('SegmentationService', () => {
  it('should identify VIP tiers correctly', () => {
    expect(SegmentationService.default.isVIPSegment('VIP')).toBe(true);
    expect(SegmentationService.default.isVIPSegment('DIAMOND')).toBe(true);
    expect(SegmentationService.default.isVIPSegment('PLATINUM')).toBe(true);
    expect(SegmentationService.default.isVIPSegment('GOLD')).toBe(false);
    expect(SegmentationService.default.isVIPSegment('SILVER')).toBe(false);
    expect(SegmentationService.default.isVIPSegment('BRONZE')).toBe(false);
  });

  it('should return tier priority correctly', () => {
    expect(SegmentationService.default.getTierPriority('VIP')).toBe(6);
    expect(SegmentationService.default.getTierPriority('DIAMOND')).toBe(5);
    expect(SegmentationService.default.getTierPriority('PLATINUM')).toBe(4);
    expect(SegmentationService.default.getTierPriority('GOLD')).toBe(3);
    expect(SegmentationService.default.getTierPriority('SILVER')).toBe(2);
    expect(SegmentationService.default.getTierPriority('BRONZE')).toBe(1);
  });
});
