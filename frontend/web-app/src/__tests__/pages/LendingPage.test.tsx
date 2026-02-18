import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import LendingPage from '@/app/[locale]/lending/page';

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

vi.mock('@/hooks/useLending', () => ({
  useCreditScore: () => ({
    data: { score: 750, category: 'GOOD', maxLoanAmount: 50000000 },
    isLoading: false,
  }),
  usePayLater: () => ({
    data: { isActive: false, limit: 0 },
    isLoading: false,
  }),
  usePayLaterTransactions: () => ({
    data: [],
    isLoading: false,
  }),
  useActivePreApprovals: () => ({
    data: [],
    isLoading: false,
  }),
}));

describe('LendingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<LendingPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<LendingPage />);
    expect(screen.getByText('Pinjaman & Kredit')).toBeInTheDocument();
  });

  it('should render lending tabs', () => {
    render(<LendingPage />);
    expect(screen.getByText('Pinjaman')).toBeInTheDocument();
    expect(screen.getByText('PayLater')).toBeInTheDocument();
  });

  it('should render credit score section', () => {
    render(<LendingPage />);
    expect(screen.getByText('Skor Kredit Anda')).toBeInTheDocument();
  });

  it('should render loan products', () => {
    render(<LendingPage />);
    expect(screen.getByText('Produk Pinjaman')).toBeInTheDocument();
  });
});
