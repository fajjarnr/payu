import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import VIPBadge from '@/components/personalization/VIPBadge';
import { VIPStatusIndicator } from '@/components/personalization/VIPBadge';

// Mock the hooks
vi.mock('@/hooks/useVIPStatus', () => ({
  useVIPStatus: () => ({
    isVIP: true,
    tier: 'VIP',
    tierLabel: 'VIP',
    tierColor: '#10b981',
    benefits: [
      'Prioritas layanan pelanggan 24/7',
      'Bebas biaya transfer ke semua bank',
      'Limit transaksi tanpa batas',
      'Cashback khusus hingga 5%',
      'Akses eksklusif ke fitur investasi premium',
      'Personal relationship manager',
      'Invitation ke acara eksklusif',
    ],
    hasVIPAccess: true,
    prioritySupport: true,
    exclusiveOffers: true,
    higherLimits: true,
    feeWaivers: true,
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

describe('VIPBadge', () => {
  it('should render VIP badge in default variant', () => {
    const { container } = render(
      <VIPBadge />,
      { wrapper: createWrapper() }
    );

    const badge = container.querySelector('.inline-flex');
    expect(badge).toBeTruthy();
  });

  it('should render VIP badge in card variant', () => {
    const { container } = render(
      <VIPBadge variant="card" />,
      { wrapper: createWrapper() }
    );

    const card = container.querySelector('.bg-gradient-to-br');
    expect(card).toBeTruthy();
  });

  it('should render VIP badge in inline variant', () => {
    const { container } = render(
      <VIPBadge variant="inline" />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('VIP');
  });

  it('should not show label when showLabel is false', () => {
    const { container } = render(
      <VIPBadge showLabel={false} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).not.toContain('VIP');
  });
});

describe('VIPStatusIndicator', () => {
  it('should render VIP status with tier', () => {
    const { container } = render(
      <VIPStatusIndicator showTier={true} showBenefits={false} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('STATUS MEMBER');
  });

  it('should render VIP status with benefits', () => {
    const { container } = render(
      <VIPStatusIndicator showTier={false} showBenefits={true} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('BENEFIT EKSKLUSIF');
  });
});
