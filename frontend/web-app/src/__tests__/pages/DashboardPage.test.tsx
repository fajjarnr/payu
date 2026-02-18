import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Home from '@/app/[locale]/dashboard/page';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

// Common mocks
vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => {
    const map: Record<string, string> = {
      'common.user': 'User',
      futureTitle: 'Start Investing',
      futureDesc: 'Grow your wealth',
      startInvesting: 'Invest Now',
    };
    return map[key] || key;
  },
  useLocale: () => 'id',
}));

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

vi.mock('@/hooks', async (importOriginal) => {
  const actual = await importOriginal() as Record<string, unknown>;
  return {
    ...actual,
    useLogout: () => ({ mutateAsync: vi.fn(), isPending: false }),
    useBalance: () => ({
      data: { balance: 15000000, formattedBalance: 'Rp 15.000.000' },
      isLoading: false,
      error: null,
    }),
  };
});

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1', username: 'budi', fullName: 'Budi Santoso' },
    isAuthenticated: true,
  }),
}));

// Mock dashboard components
vi.mock('@/components/dashboard', () => ({
  BalanceCard: () => <div data-testid="balance-card">Balance Card</div>,
  QuickActions: () => <div data-testid="quick-actions">Quick Actions</div>,
  StatsCharts: () => <div data-testid="stats-charts">Stats Charts</div>,
  TransferActivity: () => <div data-testid="transfer-activity">Transfer Activity</div>,
  FinancialHealthScore: () => <div data-testid="financial-health">Financial Health</div>,
  SpendingInsights: () => <div data-testid="spending-insights">Spending Insights</div>,
  BudgetTracking: () => <div data-testid="budget-tracking">Budget Tracking</div>,
  InvestmentPerformance: () => <div data-testid="investment-performance">Investment Performance</div>,
}));
vi.mock('@/components/cms/BannerCarousel', () => ({
  default: () => <div data-testid="banner-carousel">Banners</div>,
}));
vi.mock('@/components/cms/PromoPopup', () => ({
  default: () => <div data-testid="promo-popup">Promo</div>,
}));
vi.mock('@/components/personalization/SegmentedOffers', () => ({
  default: () => <div data-testid="segmented-offers">Offers</div>,
}));
vi.mock('@/lib/a11y', () => ({
  SkipLink: () => <div>Skip</div>,
}));

vi.mock('@/lib/navigation', () => ({
  Link: ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  ),
}));

// Mock next/dynamic
vi.mock('next/dynamic', () => ({
  default: () => () => <div data-testid="dynamic-component">Dynamic</div>,
}));

vi.mock('@/components/ui/skeleton', () => ({
  Skeleton: () => <div>Loading...</div>,
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render balance card', () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByTestId('balance-card')).toBeInTheDocument();
  });

  it('should render quick actions', () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByTestId('quick-actions')).toBeInTheDocument();
  });

  it('should render investment CTA section', () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByText('Start Investing')).toBeInTheDocument();
  });
});
