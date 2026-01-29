import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useUserSegment,
  useSegmentDetails,
  useAllSegments
} from '@/hooks/useUserSegment';
import SegmentationService from '@/services/SegmentationService';

// Mock SegmentationService
vi.mock('@/services/SegmentationService');

describe('useUserSegment hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockUserSegmentsResponse = {
    memberships: [
      {
        id: 'membership-1',
        userId: 'user-123',
        segmentId: 'segment-vip',
        segment: {
          id: 'segment-vip',
          name: 'VIP Segment',
          description: 'VIP customers',
          tier: 'VIP' as const,
          minBalance: 10000000,
          benefits: ['Priority support', 'Free transfers'],
          requirements: ['Minimum balance 10M'],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z'
        },
        status: 'ACTIVE' as const,
        joinedAt: '2024-01-01T00:00:00Z',
        validUntil: '2025-01-01T00:00:00Z',
        score: 1000
      },
      {
        id: 'membership-2',
        userId: 'user-123',
        segmentId: 'segment-gold',
        segment: {
          id: 'segment-gold',
          name: 'Gold Segment',
          description: 'Gold customers',
          tier: 'GOLD' as const,
          minBalance: 5000000,
          benefits: ['Cashback rewards'],
          requirements: ['Minimum balance 5M'],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z'
        },
        status: 'INACTIVE' as const,
        joinedAt: '2023-01-01T00:00:00Z',
        score: 500
      }
    ],
    currentTier: 'VIP' as const,
    nextTier: undefined,
    progressToNext: undefined,
    totalScore: 1000
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
    expect(useUserSegment).toBeDefined();
  });

  it('should fetch user segments successfully', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(SegmentationService.getUserSegments).toHaveBeenCalledWith('user-123');
    expect(result.current.isSuccess).toBe(true);
    expect(result.current.currentTier).toBe('VIP');
    expect(result.current.totalScore).toBe(1000);
  });

  it('should not fetch when userId is undefined', () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(SegmentationService.getUserSegments).not.toHaveBeenCalled();
  });

  it('should extract current membership with ACTIVE status', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.currentMembership).toEqual(mockUserSegmentsResponse.memberships[0]);
    expect(result.current.currentMembership?.status).toBe('ACTIVE');
  });

  it('should determine VIP status correctly', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.isVIP).toBe(true);
    expect(result.current.currentTier).toBe('VIP');
  });

  it('should identify non-VIP users correctly', async () => {
    const nonVIPResponse = {
      ...mockUserSegmentsResponse,
      currentTier: 'BRONZE' as const,
      memberships: [
        {
          ...mockUserSegmentsResponse.memberships[0],
          segmentId: 'segment-bronze',
          segment: {
            ...mockUserSegmentsResponse.memberships[0].segment,
            id: 'segment-bronze',
            name: 'Bronze Segment',
            tier: 'BRONZE' as const,
            minBalance: 0
          }
        }
      ]
    };

    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(nonVIPResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.isVIP).toBe(false);
  });

  it('should provide next tier and progress information', async () => {
    const progressResponse = {
      ...mockUserSegmentsResponse,
      currentTier: 'GOLD' as const,
      nextTier: 'PLATINUM' as const,
      progressToNext: 75
    };

    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(progressResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.nextTier).toBe('PLATINUM');
    expect(result.current.progressToNext).toBe(75);
  });

  it('should handle fetch errors', async () => {
    const error = new Error('Failed to fetch user segments');
    vi.mocked(SegmentationService.getUserSegments).mockRejectedValue(error);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('should provide invalidateSegments function', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    act(() => {
      result.current.invalidateSegments();
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ['user-segments', 'user-123']
    });
  });

  it('should respect stale time configuration', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const callCount = vi.mocked(SegmentationService.getUserSegments).mock.calls.length;

    // Immediate refetch should use cache (staleTime: 5 minutes)
    const { result: result2 } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result2.current.isSuccess).toBe(true);
    });

    expect(vi.mocked(SegmentationService.getUserSegments).mock.calls.length).toBe(callCount);
  });

  it('should handle user with no memberships', async () => {
    const emptyResponse = {
      memberships: [],
      currentTier: undefined,
      nextTier: undefined,
      progressToNext: undefined,
      totalScore: 0
    };

    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(emptyResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.currentMembership).toBeUndefined();
    expect(result.current.currentTier).toBeUndefined();
    expect(result.current.isVIP).toBe(false);
  });
});

describe('useSegmentDetails hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockSegment = {
    id: 'segment-vip',
    name: 'VIP Segment',
    description: 'VIP customers',
    tier: 'VIP' as const,
    minBalance: 10000000,
    benefits: ['Priority support', 'Free transfers', 'Higher limits'],
    requirements: ['Minimum balance 10M', 'Monthly transaction volume'],
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
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
    expect(useSegmentDetails).toBeDefined();
  });

  it('should fetch segment details successfully', async () => {
    vi.mocked(SegmentationService.getSegmentById).mockResolvedValue(mockSegment);

    const { result } = renderHook(() => useSegmentDetails('segment-vip'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getSegmentById).toHaveBeenCalledWith('segment-vip');
    expect(result.current.data).toEqual(mockSegment);
  });

  it('should not fetch when segmentId is undefined', () => {
    vi.mocked(SegmentationService.getSegmentById).mockResolvedValue(mockSegment);

    const { result } = renderHook(() => useSegmentDetails(undefined), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
    expect(SegmentationService.getSegmentById).not.toHaveBeenCalled();
  });

  it('should handle errors', async () => {
    const error = new Error('Segment not found');
    vi.mocked(SegmentationService.getSegmentById).mockRejectedValue(error);

    const { result } = renderHook(() => useSegmentDetails('invalid-id'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('should respect stale time configuration', async () => {
    vi.mocked(SegmentationService.getSegmentById).mockResolvedValue(mockSegment);

    const { result } = renderHook(() => useSegmentDetails('segment-vip'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const callCount = vi.mocked(SegmentationService.getSegmentById).mock.calls.length;

    // Immediate refetch should use cache (staleTime: 10 minutes)
    const { result: result2 } = renderHook(() => useSegmentDetails('segment-vip'), { wrapper });

    await waitFor(() => {
      expect(result2.current.isSuccess).toBe(true);
    });

    expect(vi.mocked(SegmentationService.getSegmentById).mock.calls.length).toBe(callCount);
  });
});

describe('useAllSegments hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockSegments = [
    {
      id: 'segment-bronze',
      name: 'Bronze',
      description: 'Basic tier',
      tier: 'BRONZE' as const,
      minBalance: 0,
      benefits: ['Basic features'],
      requirements: [],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    {
      id: 'segment-silver',
      name: 'Silver',
      description: 'Silver tier',
      tier: 'SILVER' as const,
      minBalance: 1000000,
      benefits: ['Enhanced features'],
      requirements: ['Minimum balance 1M'],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    {
      id: 'segment-gold',
      name: 'Gold',
      description: 'Gold tier',
      tier: 'GOLD' as const,
      minBalance: 5000000,
      benefits: ['Premium features'],
      requirements: ['Minimum balance 5M'],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    {
      id: 'segment-vip',
      name: 'VIP',
      description: 'VIP tier',
      tier: 'VIP' as const,
      minBalance: 10000000,
      benefits: ['VIP features'],
      requirements: ['Minimum balance 10M'],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    }
  ];

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
    expect(useAllSegments).toBeDefined();
  });

  it('should fetch all segments successfully', async () => {
    vi.mocked(SegmentationService.getAllSegments).mockResolvedValue(mockSegments);

    const { result } = renderHook(() => useAllSegments(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getAllSegments).toHaveBeenCalled();
    expect(result.current.data).toEqual(mockSegments);
    expect(result.current.data).toHaveLength(4);
  });

  it('should handle errors', async () => {
    const error = new Error('Failed to fetch segments');
    vi.mocked(SegmentationService.getAllSegments).mockRejectedValue(error);

    const { result } = renderHook(() => useAllSegments(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('should return empty array when no segments available', async () => {
    vi.mocked(SegmentationService.getAllSegments).mockResolvedValue([]);

    const { result } = renderHook(() => useAllSegments(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual([]);
  });

  it('should respect stale time configuration', async () => {
    vi.mocked(SegmentationService.getAllSegments).mockResolvedValue(mockSegments);

    const { result } = renderHook(() => useAllSegments(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const callCount = vi.mocked(SegmentationService.getAllSegments).mock.calls.length;

    // Immediate refetch should use cache (staleTime: 15 minutes)
    const { result: result2 } = renderHook(() => useAllSegments(), { wrapper });

    await waitFor(() => {
      expect(result2.current.isSuccess).toBe(true);
    });

    expect(vi.mocked(SegmentationService.getAllSegments).mock.calls.length).toBe(callCount);
  });
});

describe('User segment hooks integration', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockUserSegmentsResponse = {
    memberships: [
      {
        id: 'membership-1',
        userId: 'user-123',
        segmentId: 'segment-vip',
        segment: {
          id: 'segment-vip',
          name: 'VIP Segment',
          description: 'VIP customers',
          tier: 'VIP' as const,
          minBalance: 10000000,
          benefits: ['Priority support'],
          requirements: ['Minimum balance 10M'],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z'
        },
        status: 'ACTIVE' as const,
        joinedAt: '2024-01-01T00:00:00Z',
        score: 1000
      }
    ],
    currentTier: 'VIP' as const,
    totalScore: 1000
  };

  const mockAllSegments = [
    {
      id: 'segment-vip',
      name: 'VIP',
      description: 'VIP tier',
      tier: 'VIP' as const,
      minBalance: 10000000,
      benefits: ['VIP features'],
      requirements: ['Minimum balance 10M'],
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should fetch user segments and all segments independently', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);
    vi.mocked(SegmentationService.getAllSegments).mockResolvedValue(mockAllSegments);

    const { result: userSegments } = renderHook(() => useUserSegment('user-123'), { wrapper });
    const { result: allSegments } = renderHook(() => useAllSegments(), { wrapper });

    await waitFor(() => {
      expect(userSegments.current.isSuccess).toBe(true);
      expect(allSegments.current.isSuccess).toBe(true);
    });

    expect(SegmentationService.getUserSegments).toHaveBeenCalledWith('user-123');
    expect(SegmentationService.getAllSegments).toHaveBeenCalled();
  });

  it('should support invalidation workflow', async () => {
    vi.mocked(SegmentationService.getUserSegments).mockResolvedValue(mockUserSegmentsResponse);

    const { result } = renderHook(() => useUserSegment('user-123'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const initialCallCount = vi.mocked(SegmentationService.getUserSegments).mock.calls.length;

    // Invalidate and refetch
    await act(async () => {
      result.current.invalidateSegments();
    });

    await waitFor(() => {
      expect(
        vi.mocked(SegmentationService.getUserSegments).mock.calls.length
      ).toBeGreaterThan(initialCallCount);
    });
  });
});
