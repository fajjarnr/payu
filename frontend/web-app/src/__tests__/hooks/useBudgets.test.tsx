import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { useBudgets } from '@/hooks/useBudgets';
import AccountService from '@/services/AccountService';

vi.mock('@/services/AccountService', () => ({
  default: {
    getBudgets: vi.fn(),
  },
}));

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useBudgets', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps backend budgets to widget rows with derived status', async () => {
    vi.mocked(AccountService.getBudgets).mockResolvedValue([
      { id: 'b1', category: 'Makanan', limitAmount: '1000000', currentSpent: '900000', warningThreshold: 0.8, active: true },
      { id: 'b2', category: 'Transport', limitAmount: '500000', currentSpent: '100000', warningThreshold: 0.8, active: true },
    ]);
    const { result } = renderHook(() => useBudgets('acc-1'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([
      expect.objectContaining({ id: 'b1', category: 'Makanan', limit: 1000000, spent: 900000, status: 'warning' }),
      expect.objectContaining({ id: 'b2', category: 'Transport', status: 'safe' }),
    ]);
  });

  it('stays idle without an account id', () => {
    const { result } = renderHook(() => useBudgets(undefined), { wrapper });
    expect(result.current.fetchStatus).toBe('idle');
    expect(AccountService.getBudgets).not.toHaveBeenCalled();
  });
});
