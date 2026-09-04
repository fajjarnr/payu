import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CampaignsPage from '@/app/[locale]/backoffice/campaigns/page';
import PartnersPage from '@/app/[locale]/backoffice/partners/page';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/hooks/useRewards', () => ({
  useActivePromotions: () => ({
    data: [
      { id: 'c1', code: 'A', name: 'Promo A', description: 'd', type: 'DISCOUNT', value: 10, status: 'ACTIVE', startDate: '2026-09-01', endDate: '2026-09-30', maxClaims: 100, currentClaims: 30, createdAt: '2026-09-01' },
      { id: 'c2', code: 'B', name: 'Promo B', description: 'd', type: 'CASHBACK', value: 5, status: 'DRAFT', startDate: '2026-09-01', endDate: '2026-09-30', currentClaims: 0, createdAt: '2026-09-01' },
    ],
    isLoading: false,
    error: null,
  }),
}));

vi.mock('@/hooks', async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...actual,
    usePartners: () => ({
      data: [
        { id: 1, name: 'Toko A', type: 'MERCHANT', active: true, publicKey: 'k' },
        { id: 2, name: 'Toko B', type: 'MERCHANT', active: true },
        { id: 3, name: 'Toko C', type: 'MERCHANT', active: false },
      ],
      isLoading: false,
    }),
  };
});

function renderWithClient(node: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return renderWithIntl(<QueryClientProvider client={qc}>{node}</QueryClientProvider>);
}

describe('Backoffice honest stats', () => {
  it('campaigns footer shows the live filtered count', async () => {
    renderWithClient(<CampaignsPage />);
    expect(await screen.findByText('Promo A')).toBeInTheDocument();
    expect(screen.getByText(/showing/i).closest('p')).toHaveTextContent('2');
  });

  it('partners stats derive from loaded data, not hardcoded hundreds', async () => {
    renderWithClient(<PartnersPage />);
    expect(await screen.findByText('Toko A')).toBeInTheDocument();
    expect(screen.queryByText('142')).not.toBeInTheDocument();
    expect(screen.queryByText('Rp 82B')).not.toBeInTheDocument();
  });
});
