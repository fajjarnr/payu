import React from 'react';
import { render } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';

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

export function renderWithIntl(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale={locale} messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

// Re-export everything from @testing-library/react
export * from '@testing-library/react';
