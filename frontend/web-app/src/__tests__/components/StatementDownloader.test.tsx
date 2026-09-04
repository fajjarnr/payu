import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StatementDownloader from '@/components/settings/statement-downloader';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector?: (s: Record<string, unknown>) => unknown) => {
    const state = { accountId: null, user: null };
    return selector ? selector(state) : state;
  },
}));

vi.mock('@/services/StatementService', () => ({
  default: {
    listStatements: async () => ({ content: [], last: true, totalPages: 0 }),
    generateAndDownload: async () => {},
    downloadStatementWithFilename: async () => {},
    formatPeriodType: (period: string) => period,
  },
}));

describe('StatementDownloader without account', () => {
  it('shows the empty state instead of spinning forever', async () => {
    renderWithIntl(<StatementDownloader />);
    expect(await screen.findByText('Belum ada e-statement tersedia')).toBeInTheDocument();
  });
});
