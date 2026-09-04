import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CustomerCasesPage from '@/app/[locale]/backoffice/customers/page';
import CMSPage from '@/app/[locale]/backoffice/cms/page';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/services', async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...actual,
    BackofficeService: {
      getCustomerCases: vi.fn(async () => [
        { id: '1', caseNumber: 'T-1', status: 'OPEN', priority: 'HIGH', subject: 'S1', userId: 'u1', createdAt: '2026-09-01T00:00:00Z' },
        { id: '2', caseNumber: 'T-2', status: 'RESOLVED', priority: 'LOW', subject: 'S2', userId: 'u2', createdAt: '2026-09-01T00:00:00Z' },
      ]),
    },
  };
});

vi.mock('@/hooks/useCMS', () => ({
  useActiveContent: (type: string) => {
    if (type === 'BANNER') {
      return { data: [{ id: 'b1', title: 'Promo Merdeka', description: 'Diskon', contentType: 'BANNER', status: 'ACTIVE', priority: 1, startDate: '2026-09-01', endDate: '2026-09-30', imageUrl: null }], isLoading: false, error: null };
    }
    if (type === 'PROMO') {
      return { data: [{ id: 'p1', title: 'Cashback', description: 'CB', contentType: 'PROMO', status: 'SCHEDULED', priority: 2, startDate: '2026-09-01', endDate: '2026-09-30', imageUrl: null }], isLoading: false, error: null };
    }
    return { data: [], isLoading: false, error: null };
  },
}));

function renderWithClient(node: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return renderWithIntl(<QueryClientProvider client={qc}>{node}</QueryClientProvider>);
}

describe('Backoffice honest counters', () => {
  it('shows live open ticket count instead of hardcoded 42', async () => {
    renderWithClient(<CustomerCasesPage />);
    expect(await screen.findByText('Open: 1')).toBeInTheDocument();
  });

  it('cms stats reflect loaded content across types', async () => {
    renderWithClient(<CMSPage />);
    expect(await screen.findByText('Promo Merdeka')).toBeInTheDocument();
    expect(screen.getByText('Cashback')).toBeInTheDocument();
  });
});
