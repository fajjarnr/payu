import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import BillsPage from '@/app/[locale]/bills/page';

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

vi.mock('@/stores/uiStore', () => ({
  useUIStore: () => ({
    showToast: vi.fn(),
  }),
}));

vi.mock('@tanstack/react-query', () => ({
  useQuery: () => ({
    data: { content: [] },
    isLoading: false,
    error: null,
  }),
  useMutation: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('BillsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<BillsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<BillsPage />);
    expect(screen.getByText('Tagihan & Top-up')).toBeInTheDocument();
  });

  it('should render biller categories', () => {
    render(<BillsPage />);
    expect(screen.getByText('Kategori Layanan')).toBeInTheDocument();
  });

  it('should render biller options', () => {
    render(<BillsPage />);
    expect(screen.getByText('Pulsa')).toBeInTheDocument();
    expect(screen.getByText('Listrik (PLN)')).toBeInTheDocument();
    expect(screen.getByText('Air (PDAM)')).toBeInTheDocument();
  });

  it('should render recent activity section', () => {
    render(<BillsPage />);
    expect(screen.getByText('Aktivitas Terakhir')).toBeInTheDocument();
  });
});
