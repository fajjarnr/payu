import React from 'react';
import { render } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';

// Mock locale messages
const locale = 'id';
const messages = {
  dashboard: {
    financialHealthScore: 'Skor Kesehatan Finansial',
    financialHealthExcellent: 'Sangat Baik',
    financialHealthGood: 'Baik',
    financialHealthFair: 'Cukup',
    financialHealthPoor: 'Kurang',
    financialHealthVeryPoor: 'Sangat Kurang',
    spendingInsights: 'Wawasan Pengeluaran',
    spendingByCategory: 'Pengeluaran per Kategori',
    budgetTracking: 'Pelacakan Anggaran',
    budgetRemaining: 'Sisa',
    budgetUsed: 'Terpakai',
    budgetOver: 'Melebihi',
    quickActionsTitle: 'Aksi Cepat',
    quickActionsDragHint: 'Drag untuk mengatur ulang',
    viewAllAnalytics: 'Lihat Semua Analitik',
    manageBudgets: 'Kelola Anggaran',
  },
};

// Mock hooks at module level
vi.mock('@/hooks/useUserSegment', () => ({
  useUserSegment: () => ({
    currentTier: undefined,
    isVIP: false,
    currentMembership: undefined,
    progressToNext: undefined,
    nextTier: undefined,
    totalScore: 0,
  }),
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: 'test-user', fullName: 'Test User' },
      token: null,
      refreshToken: null,
      accountId: null,
      isAuthenticated: false,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setToken: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn(),
    };
    return selector ? selector(state) : state;
  }),
}));

const createQueryClient = () => {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
};

export function renderWithIntl(ui: React.ReactElement) {
  const queryClient = createQueryClient();

  return render(
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale={locale} messages={messages}>
        {ui}
      </NextIntlClientProvider>
    </QueryClientProvider>
  );
}

// Re-export everything from @testing-library/react
export * from '@testing-library/react';
