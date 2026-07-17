import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import PocketsPage from '@/app/[locale]/pockets/page';
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
  ButtonMotion: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/components/ui/skeleton', () => ({
  Skeleton: () => <div>Loading...</div>,
  SkeletonBalance: () => <div>Loading balance...</div>,
  SkeletonTransaction: () => <div>Loading transaction...</div>,
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/usePockets', () => ({
  usePockets: () => ({
    data: [
      { id: 'pocket_1', name: 'Tabungan Liburan', balance: 5000000, type: 'SAVINGS' },
    ],
    isLoading: false,
  }),
  usePocketsTotalBalance: () => ({
    data: 15000000,
    isLoading: false,
  }),
  useCreatePocket: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useCreditPocket: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDebitPocket: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useFreezePocket: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUnfreezePocket: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useClosePocket: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('PocketsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    renderWithIntl(<PocketsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    renderWithIntl(<PocketsPage />);
    expect(screen.getByText('Manajemen Kantong')).toBeInTheDocument();
  });

  it('should render main pocket section', () => {
    renderWithIntl(<PocketsPage />);
    expect(screen.getByText('Kantong Utama Cair')).toBeInTheDocument();
  });

  it('should render add pocket button', () => {
    renderWithIntl(<PocketsPage />);
    expect(screen.getByText('Tambah Kantong')).toBeInTheDocument();
  });
});
