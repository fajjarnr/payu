import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import TransferPage from '@/app/[locale]/transfer/page';
import messages from '../../../messages/id.json';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector?: (s: Record<string, unknown>) => unknown) => {
    const state = {
      accountId: 'acc_123',
      user: { id: 'user_1', username: 'budi' },
      isAuthenticated: true,
    };
    return selector ? selector(state) : state;
  },
}));

vi.mock('@/stores/uiStore', () => ({
  useUIStore: (selector?: (s: Record<string, unknown>) => unknown) => {
    const state = { addToast: vi.fn() };
    return selector ? selector(state) : state;
  },
}));

vi.mock('@/hooks/useBeneficiaries', () => ({
  useBeneficiaries: () => ({ data: [], isLoading: false }),
}));

vi.mock('@/hooks/useTransactions', () => ({
  useInitiateTransfer: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));

function renderWithIntl(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="id" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

describe('TransferPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    renderWithIntl(<TransferPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render transfer type options', () => {
    renderWithIntl(<TransferPage />);
    const transferOptions = screen.getAllByText('Transfer Instan');
    expect(transferOptions.length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('BI-FAST').length).toBeGreaterThanOrEqual(1);
  });

  it('should render scheduling options', () => {
    renderWithIntl(<TransferPage />);
    expect(screen.getByText('Sekarang')).toBeInTheDocument();
    expect(screen.getByText('Terjadwal')).toBeInTheDocument();
    expect(screen.getByText('Berulang')).toBeInTheDocument();
  });
});
