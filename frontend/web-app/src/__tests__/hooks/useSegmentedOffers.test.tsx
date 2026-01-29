import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useSegmentedOffers,
  useOffersBySegment,
  useVIPOffers
} from '@/hooks/useSegmentedOffers';
import SegmentationService from '@/services/SegmentationService';

// Mock SegmentationService
vi.mock('@/services/SegmentationService');

describe('useSegmentedOffers hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockOffersResponse = {
    offers: [
      {
        id: 'offer-1',
        title: 'Cashback Offer',
        description: 'Get 5% cashback',
        segmentId: 'segment-vip',
        segmentTier: 'VIP' as const,
        offerType: 'CASHBACK' as const,
        value: 5,
        currency: 'IDR',
        percentage: 5,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: ['Min transaction 100k'],
        imageUrl: 'https://example.com/offer.jpg',
        promoCode: 'CASHBACK5',
        minTransaction: 100000,
        maxReward: 50000,
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-2',
        title: 'Discount Offer',
        description: '10% discount',
        segmentId: 'segment-gold',
        segmentTier: 'GOLD' as const,
        offerType: 'DISCOUNT' as const,
        value: 10,
        percentage: 10,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: ['Max discount 50k'],
        imageUrl: 'https://example.com/offer2.jpg',
        minTransaction: 50000,
        maxReward: 50000,
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-3',
        title: 'Reward Points',
        description: 'Earn 2x points',
        segmentId: 'segment-silver',
        segmentTier: 'SILVER' as const,
        offerType: 'REWARD_POINTS' as const,
        value: 2,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: ['Valid for online transactions'],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-4',
        title: 'Free Transfer',
        description: 'Free BI-FAST transfer',
        segmentId: 'segment-platinum',
        segmentTier: 'PLATINUM' as const,
        offerType: 'FREE_TRANSFER' as const,
        value: 0,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: ['Unlimited transfers'],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-5',
        title: 'Inactive Offer',
        description: 'This offer is inactive',
        segmentId: 'segment-bronze',
        segmentTier: 'BRONZE' as const,
        offerType: 'CASHBACK' as const,
        value: 1,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: false,
        createdAt: '2024-01-01T00:00:00Z'
      }
    ],
    totalCount: 5,
    page: 0,
    size: 10
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useSegmentedOffers).toBeDefined();
  });

  it('should fetch segmented offers successfully', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(SegmentationService.getSegmentedOffers).toHaveBeenCalledWith('user-123', 0, 10);
    expect(result.current.isSuccess).toBe(true);
    expect(result.current.offers).toHaveLength(4); // Only active offers
    expect(result.current.totalCount).toBe(5);
  });

  it('should not fetch when userId is undefined', () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(SegmentationService.getSegmentedOffers).not.toHaveBeenCalled();
  });

  it('should filter offers by type correctly', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.cashbackOffers).toHaveLength(1);
    expect(result.current.cashbackOffers[0].offerType).toBe('CASHBACK');

    expect(result.current.discountOffers).toHaveLength(1);
    expect(result.current.discountOffers[0].offerType).toBe('DISCOUNT');

    expect(result.current.rewardOffers).toHaveLength(1);
    expect(result.current.rewardOffers[0].offerType).toBe('REWARD_POINTS');

    expect(result.current.freeTransferOffers).toHaveLength(1);
    expect(result.current.freeTransferOffers[0].offerType).toBe('FREE_TRANSFER');
  });

  it('should only return active offers', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.offers).toHaveLength(4);
    expect(result.current.offers.every((offer) => offer.isActive)).toBe(true);
  });

  it('should support pagination parameters', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers('user-123', 1, 20), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getSegmentedOffers).toHaveBeenCalledWith('user-123', 1, 20);
  });

  it('should handle fetch errors', async () => {
    const error = new Error('Failed to fetch offers');
    vi.mocked(SegmentationService.getSegmentedOffers).mockRejectedValue(error);

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('should provide invalidateOffers function', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    act(() => {
      result.current.invalidateOffers();
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ['segmented-offers', 'user-123']
    });
  });

  it('should respect stale time configuration', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const callCount = vi.mocked(SegmentationService.getSegmentedOffers).mock.calls.length;

    // Immediate refetch should use cache (staleTime: 2 minutes)
    const { result: result2 } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result2.current.isSuccess).toBe(true);
    });

    expect(vi.mocked(SegmentationService.getSegmentedOffers).mock.calls.length).toBe(callCount);
  });

  it('should return empty arrays when no offers available', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue({
      offers: [],
      totalCount: 0,
      page: 0,
      size: 10
    });

    const { result } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.offers).toEqual([]);
    expect(result.current.cashbackOffers).toEqual([]);
    expect(result.current.discountOffers).toEqual([]);
    expect(result.current.rewardOffers).toEqual([]);
    expect(result.current.freeTransferOffers).toEqual([]);
    expect(result.current.totalCount).toBe(0);
  });
});

describe('useOffersBySegment hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockSegmentOffers = {
    offers: [
      {
        id: 'offer-1',
        title: 'VIP Offer',
        description: 'Exclusive VIP offer',
        segmentId: 'segment-vip',
        segmentTier: 'VIP' as const,
        offerType: 'CASHBACK' as const,
        value: 10,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      }
    ],
    totalCount: 1,
    page: 0,
    size: 10
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useOffersBySegment).toBeDefined();
  });

  it('should fetch offers by segment successfully', async () => {
    vi.mocked(SegmentationService.getOffersBySegment).mockResolvedValue(mockSegmentOffers);

    const { result } = renderHook(() => useOffersBySegment('segment-vip'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getOffersBySegment).toHaveBeenCalledWith('segment-vip', 0, 10);
    expect(result.current.data).toEqual(mockSegmentOffers);
  });

  it('should not fetch when segmentId is undefined', () => {
    vi.mocked(SegmentationService.getOffersBySegment).mockResolvedValue(mockSegmentOffers);

    const { result } = renderHook(() => useOffersBySegment(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(SegmentationService.getOffersBySegment).not.toHaveBeenCalled();
  });

  it('should support pagination', async () => {
    vi.mocked(SegmentationService.getOffersBySegment).mockResolvedValue(mockSegmentOffers);

    const { result } = renderHook(() => useOffersBySegment('segment-vip', 2, 25), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getOffersBySegment).toHaveBeenCalledWith('segment-vip', 2, 25);
  });

  it('should handle errors', async () => {
    const error = new Error('Segment not found');
    vi.mocked(SegmentationService.getOffersBySegment).mockRejectedValue(error);

    const { result } = renderHook(() => useOffersBySegment('invalid-segment'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });
});

describe('useVIPOffers hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockAllOffers = {
    offers: [
      {
        id: 'offer-1',
        title: 'VIP Offer',
        description: 'VIP exclusive',
        segmentId: 'segment-vip',
        segmentTier: 'VIP' as const,
        offerType: 'CASHBACK' as const,
        value: 10,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-2',
        title: 'Diamond Offer',
        description: 'Diamond exclusive',
        segmentId: 'segment-diamond',
        segmentTier: 'DIAMOND' as const,
        offerType: 'DISCOUNT' as const,
        value: 15,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-3',
        title: 'Platinum Offer',
        description: 'Platinum exclusive',
        segmentId: 'segment-platinum',
        segmentTier: 'PLATINUM' as const,
        offerType: 'REWARD_POINTS' as const,
        value: 5,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-4',
        title: 'Gold Offer',
        description: 'Gold exclusive',
        segmentId: 'segment-gold',
        segmentTier: 'GOLD' as const,
        offerType: 'CASHBACK' as const,
        value: 3,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'offer-5',
        title: 'Inactive VIP Offer',
        description: 'Inactive VIP offer',
        segmentId: 'segment-vip',
        segmentTier: 'VIP' as const,
        offerType: 'CASHBACK' as const,
        value: 10,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: false,
        createdAt: '2024-01-01T00:00:00Z'
      }
    ],
    totalCount: 5,
    page: 0,
    size: 20
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useVIPOffers).toBeDefined();
  });

  it('should fetch and filter VIP tier offers', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockAllOffers);

    const { result } = renderHook(() => useVIPOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getSegmentedOffers).toHaveBeenCalledWith('user-123', 0, 20);
    expect(result.current.data).toHaveLength(3); // VIP, DIAMOND, PLATINUM only
    expect(result.current.data?.every((offer) => offer.isActive)).toBe(true);
    expect(result.current.data?.every((offer) => offer.segmentTier === 'VIP' || offer.segmentTier === 'DIAMOND' || offer.segmentTier === 'PLATINUM')).toBe(true);
  });

  it('should not fetch when userId is undefined', () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockAllOffers);

    const { result } = renderHook(() => useVIPOffers(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(SegmentationService.getSegmentedOffers).not.toHaveBeenCalled();
  });

  it('should exclude non-VIP tier offers', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockAllOffers);

    const { result } = renderHook(() => useVIPOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const hasGoldOffer = result.current.data?.some((offer) => offer.segmentTier === 'GOLD');
    expect(hasGoldOffer).toBe(false);
  });

  it('should exclude inactive offers', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockAllOffers);

    const { result } = renderHook(() => useVIPOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const hasInactiveOffer = result.current.data?.some((offer) => !offer.isActive);
    expect(hasInactiveOffer).toBe(false);
  });

  it('should return empty array when no VIP offers available', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue({
      offers: [
        {
          id: 'offer-1',
          title: 'Gold Offer',
          description: 'Gold offer',
          segmentId: 'segment-gold',
          segmentTier: 'GOLD' as const,
          offerType: 'CASHBACK' as const,
          value: 3,
          validFrom: '2024-01-01T00:00:00Z',
          validUntil: '2024-12-31T23:59:59Z',
          terms: [],
          isActive: true,
          createdAt: '2024-01-01T00:00:00Z'
        }
      ],
      totalCount: 1,
      page: 0,
      size: 20
    });

    const { result } = renderHook(() => useVIPOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual([]);
  });

  it('should handle errors', async () => {
    const error = new Error('Failed to fetch VIP offers');
    vi.mocked(SegmentationService.getSegmentedOffers).mockRejectedValue(error);

    const { result } = renderHook(() => useVIPOffers('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });
});

describe('Segmented offers hooks integration', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockOffersResponse = {
    offers: [
      {
        id: 'offer-1',
        title: 'Test Offer',
        description: 'Test',
        segmentId: 'segment-vip',
        segmentTier: 'VIP' as const,
        offerType: 'CASHBACK' as const,
        value: 5,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      }
    ],
    totalCount: 1,
    page: 0,
    size: 10
  };

  const mockSegmentOffers = {
    offers: [
      {
        id: 'offer-2',
        title: 'Segment Offer',
        description: 'Segment offer',
        segmentId: 'segment-gold',
        segmentTier: 'GOLD' as const,
        offerType: 'DISCOUNT' as const,
        value: 10,
        validFrom: '2024-01-01T00:00:00Z',
        validUntil: '2024-12-31T23:59:59Z',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01T00:00:00Z'
      }
    ],
    totalCount: 1,
    page: 0,
    size: 10
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should fetch user offers and segment offers independently', async () => {
    vi.mocked(SegmentationService.getSegmentedOffers).mockResolvedValue(mockOffersResponse);
    vi.mocked(SegmentationService.getOffersBySegment).mockResolvedValue(mockSegmentOffers);

    const { result: userOffers } = renderHook(() => useSegmentedOffers('user-123'), { wrapper });
    const { result: segmentOffers } = renderHook(() => useOffersBySegment('segment-gold'), {
      wrapper
    });

    await waitFor(() => {
      expect(userOffers.current.isSuccess).toBe(true);
      expect(segmentOffers.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getSegmentedOffers).toHaveBeenCalled();
    expect(SegmentationService.getOffersBySegment).toHaveBeenCalled();
  });
});
