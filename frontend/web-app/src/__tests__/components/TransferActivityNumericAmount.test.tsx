import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import TransferActivity from '@/components/dashboard/TransferActivity';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import type { Transaction } from '@/types';

// Backend DECIMAL can arrive as a JSON number on the wire even though the
// domain type is Money (decimal string). Regression test for dashboard
// crash "e.replace is not a function".
vi.mock('@/hooks', async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
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
  return {
    ...actual,
    useTransactions: () => ({ data: [numericTx], isLoading: false }),
    useCancelTransaction: () => ({ mutateAsync: vi.fn() }),
  };
});

describe('TransferActivity numeric wire amount', () => {
  it('renders a numeric amount without crashing', () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.getByTestId('transfer-row-tx-1')).toHaveTextContent(/1\.500\.000/);
  });
});
