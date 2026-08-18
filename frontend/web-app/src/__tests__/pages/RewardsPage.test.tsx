import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import RewardsPage from '@/app/[locale]/rewards/page';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  wrapper.displayName = "TestWrapper";
  return wrapper;
};

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

vi.mock('@/components/ui/tabs', () => ({
  Tabs: ({ children, ...props }: { children: React.ReactNode }) => <div {...props}>{children}</div>,
  TabsList: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  TabsTrigger: ({ children, ...props }: { children: React.ReactNode; value: string }) => <button {...props}>{children}</button>,
  TabsContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/usePromotions', () => ({
  useLoyaltyPoints: () => ({
    data: { totalPoints: 15000, tier: 'GOLD' },
    isLoading: false,
  }),
  useCashbacks: () => ({
    data: [],
    isLoading: false,
  }),
  useReferralSummary: () => ({
    data: { referralCode: 'BUDI123', totalReferred: 5 },
    isLoading: false,
  }),
  useActivePromotions: () => ({
    data: [],
    isLoading: false,
  }),
}));

describe('RewardsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<RewardsPage />, { wrapper: createWrapper() });
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<RewardsPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Rewards & Gamifikasi')).toBeInTheDocument();
  });

  it('should render reward tabs', () => {
    render(<RewardsPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Poin Loyalty')).toBeInTheDocument();
    expect(screen.getByText('Cashback')).toBeInTheDocument();
    expect(screen.getByText('Referral')).toBeInTheDocument();
  });

  it('should render points balance', () => {
    render(<RewardsPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Saldo Poin')).toBeInTheDocument();
  });
});
