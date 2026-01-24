import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TargetedPromos, QuickPromoBanner } from '@/components/personalization/TargetedPromos';

// Mock the hooks
vi.mock('@/hooks/useSegmentedOffers', () => ({
  useSegmentedOffers: () => ({
    offers: [
      {
        id: 'offer1',
        title: 'Special Cashback',
        description: 'Get 5% cashback on all transactions',
        segmentId: 'seg1',
        segmentTier: 'VIP',
        offerType: 'CASHBACK',
        value: 5,
        percentage: 5,
        validFrom: '2024-01-01',
        validUntil: '2024-12-31',
        terms: ['Min transaction Rp 100.000'],
        promoCode: 'VIP5',
        minTransaction: 100000,
        isActive: true,
        createdAt: '2024-01-01',
      },
      {
        id: 'offer2',
        title: 'Free Transfer',
        description: 'Free transfer to all banks',
        segmentId: 'seg1',
        segmentTier: 'VIP',
        offerType: 'FREE_TRANSFER',
        value: 0,
        validFrom: '2024-01-01',
        validUntil: '2024-12-31',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01',
      },
    ],
    cashbackOffers: [
      {
        id: 'offer1',
        title: 'Special Cashback',
        description: 'Get 5% cashback on all transactions',
        segmentId: 'seg1',
        segmentTier: 'VIP',
        offerType: 'CASHBACK',
        value: 5,
        percentage: 5,
        validFrom: '2024-01-01',
        validUntil: '2024-12-31',
        terms: ['Min transaction Rp 100.000'],
        promoCode: 'VIP5',
        minTransaction: 100000,
        isActive: true,
        createdAt: '2024-01-01',
      },
    ],
    discountOffers: [],
    rewardOffers: [],
    freeTransferOffers: [
      {
        id: 'offer2',
        title: 'Free Transfer',
        description: 'Free transfer to all banks',
        segmentId: 'seg1',
        segmentTier: 'VIP',
        offerType: 'FREE_TRANSFER',
        value: 0,
        validFrom: '2024-01-01',
        validUntil: '2024-12-31',
        terms: [],
        isActive: true,
        createdAt: '2024-01-01',
      },
    ],
    isLoading: false,
    error: null,
    totalCount: 2,
  }),
}));

vi.mock('@/stores', () => ({
  useAuthStore: () => ({
    user: { id: 'test-user', fullName: 'Test User' },
  }),
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('TargetedPromos', () => {
  it('should render promo items', () => {
    render(
      <TargetedPromos maxPromos={2} />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('Special Cashback');
    expect(screen.textContent).toContain('Free Transfer');
  });

  it('should filter by offer type when specified', () => {
    render(
      <TargetedPromos offerType="CASHBACK" maxPromos={2} />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('Special Cashback');
    expect(screen.textContent).not.toContain('Free Transfer');
  });

  it('should render promo code when available', () => {
    render(
      <TargetedPromos offerType="CASHBACK" maxPromos={1} />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('VIP5');
  });
});

describe('QuickPromoBanner', () => {
  it('should render top cashback offer', () => {
    render(
      <QuickPromoBanner />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('Special Cashback');
    expect(screen.textContent).toContain('5%');
    expect(screen.textContent).toContain('Klaim');
  });

  it('should not render when no offers available', () => {
    vi.clearAllMocks();
    vi.mock('@/hooks/useSegmentedOffers', () => ({
      useSegmentedOffers: () => ({
        offers: [],
        cashbackOffers: [],
        isLoading: false,
        error: null,
        totalCount: 0,
      }),
    }));

    const { container } = render(
      <QuickPromoBanner />,
      { wrapper: createWrapper() }
    );

    expect(container.firstChild).toBeNull();
  });
});
