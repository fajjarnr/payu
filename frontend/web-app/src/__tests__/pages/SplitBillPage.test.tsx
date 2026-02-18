import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import SplitBillPage from '@/app/[locale]/split-bill/page';

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
    user: { id: 'user_1', username: 'budi' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/useSplitBill', () => ({
  useSplitBills: () => ({ data: [], isLoading: false }),
  useSplitBill: () => ({ data: null, isLoading: false }),
  useCreateSplitBill: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useAcceptSplitBill: () => ({ mutateAsync: vi.fn() }),
  useDeclineSplitBill: () => ({ mutateAsync: vi.fn() }),
  useSplitBillPayment: () => ({ mutateAsync: vi.fn() }),
  useSettleSplitBill: () => ({ mutateAsync: vi.fn() }),
  useAddParticipant: () => ({ mutateAsync: vi.fn() }),
  useActivateSplitBill: () => ({ mutateAsync: vi.fn() }),
}));

vi.mock('@/services/TransactionService', () => ({
  SplitBillParticipant: {},
}));

describe('SplitBillPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<SplitBillPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<SplitBillPage />);
    expect(screen.getByText('Split Bill')).toBeInTheDocument();
  });

  it('should render create split bill button', () => {
    render(<SplitBillPage />);
    expect(screen.getByText('Split Bill Baru')).toBeInTheDocument();
  });
});
