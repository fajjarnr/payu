import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import InvestmentsPage from '@/app/[locale]/investments/page';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock('@/components/ui/Motion', () => ({
  PageTransition: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerItem: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks', () => ({
  useInvestmentAccount: () => ({
    data: { id: 'acc_1', balance: 50000000, currency: 'IDR', status: 'ACTIVE' },
    isLoading: false,
    isError: false,
  }),
  useBuyDeposit: () => ({
    mutateAsync: vi.fn(),
  }),
  useSellInvestment: () => ({
    mutateAsync: vi.fn(),
  }),
  useCreateInvestmentAccount: () => ({
    mutateAsync: vi.fn(),
  }),
}));

describe('InvestmentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    renderWithIntl(<InvestmentsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    renderWithIntl(<InvestmentsPage />);
    expect(screen.getByText('Manajemen Kekayaan')).toBeInTheDocument();
  });

  it('should not render inactive investment actions', () => {
    renderWithIntl(<InvestmentsPage />);
    expect(screen.queryByTestId('new-investment-button')).not.toBeInTheDocument();
    expect(screen.queryByTestId('optimize-portfolio-button')).not.toBeInTheDocument();
  });

  it('should render portfolio overview', () => {
    renderWithIntl(<InvestmentsPage />);
    expect(screen.getByText('Saldo akun investasi')).toBeInTheDocument();
    expect(screen.getByTestId('portfolio-overview-card')).toHaveTextContent('50.000.000');
    expect(screen.queryByText('+Rp 12,4 Jt (8.2%)')).not.toBeInTheDocument();
    expect(screen.queryByText('Terjamin LPS')).not.toBeInTheDocument();
  });

  it('should show empty states when authoritative investment data is unavailable', () => {
    renderWithIntl(<InvestmentsPage />);
    expect(screen.getByTestId('investment-performance-empty')).toBeInTheDocument();
    expect(screen.getByTestId('investment-risk-empty')).toBeInTheDocument();
    expect(screen.getByTestId('investment-products-empty')).toBeInTheDocument();
    expect(screen.getByTestId('investment-advice-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('investment-product-0')).not.toBeInTheDocument();
    expect(screen.queryByText('Moderat-Agresif')).not.toBeInTheDocument();
  });
});
