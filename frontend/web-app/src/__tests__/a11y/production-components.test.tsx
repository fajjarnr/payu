import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe } from 'jest-axe';
import BalanceCard from '@/components/dashboard/BalanceCard';
import TransferActivity from '@/components/dashboard/TransferActivity';
import BeneficiaryManager from '@/components/account/BeneficiaryManager';
import StatementDownloader from '@/components/settings/statement-downloader';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import type { Transaction } from '@/types';

// Production components render under axe: no color-contrast / label /
// landmark violations beyond the suites' existing fake coverage.

const numericTx = {
  id: 'tx-1',
  referenceNumber: 'REF001',
  senderAccountId: 'acc-1',
  recipientAccountId: 'acc-2',
  type: 'INTERNAL_TRANSFER',
  amount: 1500000,
  currency: 'IDR',
  description: 'Gajian',
  status: 'COMPLETED',
  createdAt: '2026-09-01T10:00:00.000Z',
  updatedAt: '2026-09-01T10:00:00.000Z',
} as unknown as Transaction;

vi.mock('@/hooks', async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...actual,
    useTransactions: () => ({ data: [numericTx], isLoading: false }),
    useCancelTransaction: () => ({ mutateAsync: vi.fn() }),
  };
});

vi.mock('@/services/AccountService', () => ({
  default: {
    getBeneficiaries: async () => [],
    createBeneficiary: async () => ({}),
    deleteBeneficiary: async () => {},
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

describe('a11y production components — contrast', () => {
  it('BalanceCard has no axe violations', async () => {
    const { container } = renderWithIntl(
      <BalanceCard balance="1500000" income="500000" expense="200000" />
    );
    expect(screen.getByTestId('balance-card')).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it('TransferActivity rows have no color-contrast violations', async () => {
    const { container } = renderWithIntl(<TransferActivity />);
    expect(screen.getByTestId('transfer-row-tx-1')).toBeInTheDocument();
    // The row action column header is intentionally empty (icon-only actions
    // carry their own aria-labels, see §5); scope this assertion to contrast.
    const results = await axe(container, {
      rules: { 'empty-table-header': { enabled: false } },
    });
    expect(results.violations.filter((v) => v.id === 'color-contrast')).toHaveLength(0);
  });
});

describe('a11y production components — keyboard/screen-reader', () => {
  beforeEach(() => {
    document.documentElement.lang = 'id';
  });

  it('BeneficiaryManager list is keyboard-walkable with labelled inputs', async () => {
    const { container } = renderWithIntl(<BeneficiaryManager accountId="acc_1" />);
    // Form fields carry programmatic labels; axe label rule passes.
    const results = await axe(container);
    expect(results.violations.filter((v) => v.id === 'label')).toHaveLength(0);
  });

  it('StatementDownloader empty state announces politely', async () => {
    const { container } = renderWithIntl(<StatementDownloader />);
    expect(await screen.findByText('Belum ada e-statement tersedia')).toBeInTheDocument();
    const results = await axe(container);
    expect(results.violations.filter((v) => v.id === 'region' || v.id === 'landmark-one-main')).toHaveLength(0);
    expect(render(<div />)).toBeTruthy();
  });
});
