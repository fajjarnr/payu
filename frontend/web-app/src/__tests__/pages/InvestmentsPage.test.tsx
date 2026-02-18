import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import InvestmentsPage from '@/app/[locale]/investments/page';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock('@/components/ui/Motion', () => ({
  PageTransition: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerItem: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  ButtonMotion: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/useInvestments', () => ({
  useInvestmentAccount: () => ({
    data: { id: 'acc_1', balance: 50000000, status: 'ACTIVE' },
    isLoading: false,
  }),
  useGoldHoldings: () => ({
    data: { totalWeightGrams: 10.5, totalValue: 11025000, holdings: [] },
    isLoading: false,
  }),
}));

describe('InvestmentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<InvestmentsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<InvestmentsPage />);
    expect(screen.getByText('Manajemen Kekayaan')).toBeInTheDocument();
  });

  it('should render new investment button', () => {
    render(<InvestmentsPage />);
    expect(screen.getByTestId('new-investment-button')).toBeInTheDocument();
  });

  it('should render portfolio overview', () => {
    render(<InvestmentsPage />);
    expect(screen.getByText('Total Portofolio Bersih')).toBeInTheDocument();
  });

  it('should render risk profile', () => {
    render(<InvestmentsPage />);
    expect(screen.getByText('Profil Risiko')).toBeInTheDocument();
    expect(screen.getByText('Moderat-Agresif')).toBeInTheDocument();
  });

  it('should render product catalog', () => {
    render(<InvestmentsPage />);
    expect(screen.getByText('Katalog Produk Terpilih')).toBeInTheDocument();
    expect(screen.getByText('Emas Digital (XAU)')).toBeInTheDocument();
  });
});
