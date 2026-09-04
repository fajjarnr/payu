import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import KycReviewsPage from '@/app/[locale]/backoffice/kyc/page';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/services', async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...actual,
    BackofficeService: {
      getKycReviews: vi.fn(async () => [
        { id: '1', status: 'PENDING', fullName: 'A', userId: 'u1', documentType: 'KTP', documentNumber: '1', createdAt: '2026-09-01T00:00:00Z' },
        { id: '2', status: 'PENDING', fullName: 'B', userId: 'u2', documentType: 'KTP', documentNumber: '2', createdAt: '2026-09-01T00:00:00Z' },
        { id: '3', status: 'APPROVED', fullName: 'C', userId: 'u3', documentType: 'KTP', documentNumber: '3', createdAt: '2026-09-01T00:00:00Z' },
      ]),
    },
  };
});

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return renderWithIntl(
    <QueryClientProvider client={qc}>
      <KycReviewsPage />
    </QueryClientProvider>
  );
}

describe('KycReviewsPage counter', () => {
  it('shows the live pending count instead of a hardcoded number', async () => {
    renderPage();
    expect(await screen.findByText('Tertunda: 2')).toBeInTheDocument();
  });
});
