'use client';

/* eslint-disable no-restricted-syntax -- display mapping of Money strings to chart numbers, not ledger arithmetic (ADR-0047) */

import { useQuery } from '@tanstack/react-query';
import AccountService, { type AccountBudget } from '@/services/AccountService';

export interface BudgetRow {
  id: string;
  category: string;
  limit: number;
  spent: number;
  remaining: number;
  percentage: number;
  status: 'safe' | 'warning' | 'danger' | 'exceeded';
}

export function toBudgetRow(b: AccountBudget): BudgetRow {
  const limit = Number(b.limitAmount);
  const spent = Number(b.currentSpent);
  const remaining = limit - spent;
  const percentage = limit > 0 ? (spent / limit) * 100 : 0;
  const warnAt = (b.warningThreshold ?? 0.8) * 100;
  const status =
    percentage >= 100 ? 'exceeded' : percentage >= warnAt ? 'warning' : 'safe';
  return { id: b.id, category: b.category, limit, spent, remaining, percentage, status };
}

export function useBudgets(accountId?: string) {
  return useQuery({
    queryKey: ['budgets', accountId],
    queryFn: async (): Promise<BudgetRow[]> => {
      const budgets = await AccountService.getBudgets(accountId!);
      return budgets.map(toBudgetRow);
    },
    enabled: !!accountId,
    staleTime: 60_000,
  });
}
