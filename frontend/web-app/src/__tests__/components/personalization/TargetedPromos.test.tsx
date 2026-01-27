import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TargetedPromos from '@/components/personalization/TargetedPromos';
import { QuickPromoBanner } from '@/components/personalization/TargetedPromos';

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

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'QueryClientWrapper';
  return Wrapper;
};

describe('TargetedPromos', () => {
  it('should render promo items', () => {
    const { container } = render(
      <TargetedPromos maxPromos={2} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('Special Cashback');
    expect(container.textContent).toContain('Free Transfer');
  });

  it('should filter by offer type when specified', () => {
    const { container } = render(
      <TargetedPromos offerType="CASHBACK" maxPromos={2} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('Special Cashback');
    expect(container.textContent).not.toContain('Free Transfer');
  });

  it('should render promo code when available', () => {
    const { container } = render(
      <TargetedPromos offerType="CASHBACK" maxPromos={1} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('VIP5');
  });
});

describe('QuickPromoBanner', () => {
  it('should render top cashback offer', () => {
    const { container } = render(
      <QuickPromoBanner />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('Special Cashback');
    expect(container.textContent).toContain('5%');
    expect(container.textContent).toContain('Klaim');
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
