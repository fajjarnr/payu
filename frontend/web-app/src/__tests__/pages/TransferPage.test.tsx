import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import TransferPage from '@/app/[locale]/transfer/page';

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

vi.mock('react-hook-form', () => ({
  useForm: () => ({
    register: () => ({}),
    handleSubmit: (fn: (...args: unknown[]) => void) => (e: Event) => { e?.preventDefault?.(); fn({}); },
    formState: { errors: {}, isSubmitting: false },
    watch: () => '',
    setValue: vi.fn(),
    reset: vi.fn(),
    control: {},
  }),
  useWatch: () => '',
}));

vi.mock('@/components/ui/popover', () => ({
  Popover: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  PopoverTrigger: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  PopoverContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/components/ui/calendar', () => ({
  Calendar: () => <div data-testid="calendar">Calendar</div>,
}));

describe('TransferPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<TransferPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render transfer type options', () => {
    render(<TransferPage />);
    const transferOptions = screen.getAllByText('Transfer Instan');
    expect(transferOptions.length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('BI-FAST').length).toBeGreaterThanOrEqual(1);
  });

  it('should render scheduling options', () => {
    render(<TransferPage />);
    expect(screen.getByText('Sekarang')).toBeInTheDocument();
    expect(screen.getByText('Terjadwal')).toBeInTheDocument();
    expect(screen.getByText('Berulang')).toBeInTheDocument();
  });
});
