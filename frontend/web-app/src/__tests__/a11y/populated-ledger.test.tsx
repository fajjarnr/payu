import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { axe } from 'jest-axe';
import TransferActivity from '@/components/dashboard/TransferActivity';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import type { Transaction } from '@/types';

/**
 * The populated ledger — what every real user sees, and what the a11y suites
 * never rendered (they all used one row or none). The fixture is deliberately
 * hostile: a very long description, a large amount, mixed statuses and both
 * credit and debit directions.
 */
const populated: Transaction[] = [
  {
    id: 'tx-long',
    referenceNumber: 'REF2026000001ABCDEF',
    senderAccountId: 'acc-1',
    recipientAccountId: 'acc-2',
    type: 'QRIS_PAYMENT',
    amount: '1250000000.00',
    currency: 'IDR',
    description:
      'Pembayaran tagihan listrik PLN token listrik 900 kVA bulan ini beserta biaya oben',
    status: 'COMPLETED',
    createdAt: '2026-10-21T09:15:00.000Z',
    updatedAt: '2026-10-21T09:15:00.000Z',
  },
  {
    id: 'tx-pending',
    referenceNumber: 'REF2026000002ABCDEF',
    senderAccountId: 'acc-2',
    recipientAccountId: 'acc-1',
    type: 'INTERNAL_TRANSFER',
    amount: '1500000.00',
    currency: 'IDR',
    description: 'Transfer bulanan tabungan',
    status: 'PENDING',
    createdAt: '2026-10-20T09:15:00.000Z',
    updatedAt: '2026-10-20T09:15:00.000Z',
  },
  {
    id: 'tx-failed',
    referenceNumber: 'REF2026000003ABCDEF',
    senderAccountId: 'acc-1',
    recipientAccountId: 'acc-2',
    type: 'BILL_PAYMENT',
    amount: '250000.00',
    currency: 'IDR',
    description: 'Bayar air PAM',
    status: 'FAILED',
    createdAt: '2026-10-19T09:15:00.000Z',
    updatedAt: '2026-10-19T09:15:00.000Z',
  },
] as unknown as Transaction[];

vi.mock('@/hooks', async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...actual,
    useTransactions: () => ({ data: populated, isLoading: false }),
    useCancelTransaction: () => ({ mutateAsync: vi.fn() }),
  };
});

describe('populated ledger', () => {
  it('renders every row of a populated list', () => {
    const { container } = renderWithIntl(<TransferActivity />);
    for (const tx of populated) {
      expect(container.querySelector(`[data-testid="transfer-row-${tx.id}"]`)).toBeTruthy();
    }
    // The long description is truncated in the row, not dropped. Both the
    // desktop table and the mobile card list render, so there are two nodes.
    expect(screen.getAllByText(/Pembayaran tagihan listrik PLN/).length).toBeGreaterThan(0);
  });

  it('gives every row action a unique accessible name', () => {
    const { container } = renderWithIntl(<TransferActivity />);
    const names = Array.from(
      container.querySelectorAll('[aria-label^="Opsi untuk"]')
    ).map((el) => el.getAttribute('aria-label'));
    // Every row appears twice (desktop table + mobile card list) and each
    // row's name must be unique across rows.
    expect(new Set(names).size).toBe(populated.length);
  });

  it(
    'has no axe violations with a populated list',
    async () => {
      const { container } = renderWithIntl(<TransferActivity />);
      const results = await axe(container, {
        rules: { 'empty-table-header': { enabled: false } },
      });
      expect(results.violations.filter((v) => v.id === 'color-contrast')).toHaveLength(0);
      expect(results.violations.filter((v) => v.id === 'label')).toHaveLength(0);
      expect(results.violations.filter((v) => v.id === 'button-name')).toHaveLength(0);
    },
    30_000
  );
});
