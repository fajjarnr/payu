import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useVIPStatus } from '@/hooks/useVIPStatus';
import { useUserSegment } from '@/hooks/useUserSegment';
import { useAuthStore } from '@/stores';

// Mock dependencies
vi.mock('@/hooks/useUserSegment');
vi.mock('@/stores');

describe('useVIPStatus hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockUser = {
    id: 'user-123',
    externalId: 'ext-123',
    username: 'testuser',
    email: 'test@example.com',
    fullName: 'Test User',
    nik: '1234567890123456',
    kycStatus: 'PENDING' as const,
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
    expect(useVIPStatus).toBeDefined();
  });

  it('should return VIP status for VIP tier users', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(true);
    expect(result.current.tier).toBe('VIP');
    expect(result.current.tierLabel).toBe('VIP');
    expect(result.current.tierColor).toBe('#10b981');
  });

  it('should return Diamond status for Diamond tier users', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'DIAMOND',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(true);
    expect(result.current.tier).toBe('DIAMOND');
    expect(result.current.tierLabel).toBe('Diamond');
    expect(result.current.tierColor).toBe('#b9f2ff');
  });

  it('should return Platinum status for Platinum tier users', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'PLATINUM',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(true);
    expect(result.current.tier).toBe('PLATINUM');
    expect(result.current.tierLabel).toBe('Platinum');
    expect(result.current.tierColor).toBe('#e5e4e2');
  });

  it('should return Gold status for Gold tier users', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'GOLD',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(false);
    expect(result.current.tier).toBe('GOLD');
    expect(result.current.tierLabel).toBe('Gold');
    expect(result.current.tierColor).toBe('#ffd700');
  });

  it('should return Silver status for Silver tier users', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'SILVER',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(false);
    expect(result.current.tier).toBe('SILVER');
    expect(result.current.tierLabel).toBe('Silver');
    expect(result.current.tierColor).toBe('#c0c0c0');
  });

  it('should return Bronze status for Bronze tier users', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'BRONZE',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(false);
    expect(result.current.tier).toBe('BRONZE');
    expect(result.current.tierLabel).toBe('Bronze');
    expect(result.current.tierColor).toBe('#cd7f32');
  });

  it('should return Standard status for users without a tier', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: null,
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(false);
    expect(result.current.tier).toBe(null);
    expect(result.current.tierLabel).toBe('Standard');
    expect(result.current.tierColor).toBe('#6b7280');
  });

  it('should provide correct benefits for VIP tier', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.benefits).toEqual([
      'Prioritas layanan pelanggan 24/7',
      'Bebas biaya transfer ke semua bank',
      'Limit transaksi tanpa batas',
      'Cashback khusus hingga 5%',
      'Akses eksklusif ke fitur investasi premium',
      'Personal relationship manager',
      'Invitation ke acara eksklusif'
    ]);
  });

  it('should provide correct benefits for Diamond tier', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'DIAMOND',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.benefits).toEqual([
      'Layanan pelanggan prioritas',
      'Bebas biaya transfer BI-FAST',
      'Limit transaksi tinggi',
      'Cashback hingga 3%',
      'Akses ke fitur investasi prioritas'
    ]);
  });

  it('should provide correct benefits for Gold tier', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'GOLD',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.benefits).toEqual([
      'Bebas biaya 20 transfer BI-FAST per bulan',
      'Cashback hingga 1.5%'
    ]);
  });

  it('should provide correct benefits for Bronze tier', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'BRONZE',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.benefits).toEqual(['Cashback hingga 0.5%']);
  });

  it('should provide empty benefits for users without tier', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: null,
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.benefits).toEqual([]);
  });

  it('should determine priority support correctly for VIP', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.prioritySupport).toBe(true);
  });

  it('should determine priority support correctly for Platinum', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'PLATINUM',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.prioritySupport).toBe(true);
  });

  it('should determine priority support correctly for Gold', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'GOLD',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.prioritySupport).toBe(false);
  });

  it('should determine exclusive offers correctly for VIP', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.exclusiveOffers).toBe(true);
  });

  it('should determine exclusive offers correctly for Diamond', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'DIAMOND',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.exclusiveOffers).toBe(true);
  });

  it('should determine exclusive offers correctly for Platinum', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'PLATINUM',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    // Platinum is a VIP tier (isVIP: true), so exclusiveOffers should be true
    expect(result.current.exclusiveOffers).toBe(true);
  });

  it('should determine higher limits correctly for Gold and above', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'GOLD',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.higherLimits).toBe(true);
  });

  it('should determine higher limits correctly for Silver', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'SILVER',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.higherLimits).toBe(false);
  });

  it('should determine fee waivers correctly for VIP', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.feeWaivers).toBe(true);
  });

  it('should determine fee waivers correctly for Platinum', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'PLATINUM',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.feeWaivers).toBe(true);
  });

  it('should determine fee waivers correctly for Gold', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'GOLD',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.feeWaivers).toBe(false);
  });

  it('should handle missing user', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: null
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: null,
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(false);
    expect(result.current.tier).toBe(null);
    expect(result.current.tierLabel).toBe('Standard');
    expect(result.current.benefits).toEqual([]);
  });

  it('should provide hasVIPAccess matching isVIP', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.hasVIPAccess).toBe(result.current.isVIP);
  });

  it('should return complete VIP status object', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current).toMatchObject({
      isVIP: true,
      tier: 'VIP',
      tierLabel: 'VIP',
      tierColor: '#10b981',
      hasVIPAccess: true,
      prioritySupport: true,
      exclusiveOffers: true,
      higherLimits: true,
      feeWaivers: true
    });

    expect(Array.isArray(result.current.benefits)).toBe(true);
    expect(result.current.benefits.length).toBeGreaterThan(0);
  });

  it('should update when user segment changes', () => {
    const mockUseUserSegment = useUserSegment as unknown as ReturnType<typeof vi.fn>;
    const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

    mockUseAuthStore.mockReturnValue({
      user: mockUser
    });

    // Start as VIP
    mockUseUserSegment.mockReturnValue({
      currentTier: 'VIP',
      isVIP: true,
      isLoading: false,
      isSuccess: true
    });

    const { result } = renderHook(() => useVIPStatus(), { wrapper });

    expect(result.current.isVIP).toBe(true);
    expect(result.current.tier).toBe('VIP');

    // Change to Gold
    mockUseUserSegment.mockReturnValue({
      currentTier: 'GOLD',
      isVIP: false,
      isLoading: false,
      isSuccess: true
    });

    // Rerender to get updated state
    const { result: newResult } = renderHook(() => useVIPStatus(), { wrapper });

    expect(newResult.current.isVIP).toBe(false);
    expect(newResult.current.tier).toBe('GOLD');
  });
});
