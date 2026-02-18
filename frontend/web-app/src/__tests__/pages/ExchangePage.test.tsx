import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import ExchangePage from '@/app/[locale]/exchange/page';

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

vi.mock('@/stores/uiStore', () => ({
  useUIStore: () => ({ showToast: vi.fn() }),
}));

vi.mock('@/hooks/useFx', () => ({
  useFxRate: () => ({ data: { rate: 15750, inverseRate: 0.0000635 }, isLoading: false }),
  useFxEstimate: () => ({ mutateAsync: vi.fn() }),
  useFxConversion: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useFxConversions: () => ({ data: [], isLoading: false }),
}));

vi.mock('@/services/FxService', () => ({
  SUPPORTED_CURRENCIES: {
    IDR: { code: 'IDR', name: 'Indonesian Rupiah', symbol: 'Rp', flag: '🇮🇩', decimalPlaces: 0 },
    USD: { code: 'USD', name: 'US Dollar', symbol: '$', flag: '🇺🇸', decimalPlaces: 2 },
    EUR: { code: 'EUR', name: 'Euro', symbol: '€', flag: '🇪🇺', decimalPlaces: 2 },
    SGD: { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$', flag: '🇸🇬', decimalPlaces: 2 },
  },
}));

vi.mock('react-hook-form', () => ({
  useForm: () => ({
    register: () => ({}),
    handleSubmit: (fn: Function) => (e: Event) => { e?.preventDefault?.(); fn({}); },
    formState: { errors: {} },
    watch: () => 'IDR',
    setValue: vi.fn(),
    control: {},
  }),
  useWatch: () => 'IDR',
}));

describe('ExchangePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<ExchangePage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<ExchangePage />);
    expect(screen.getByText('Currency Exchange')).toBeInTheDocument();
  });

  it('should render exchange calculator section', () => {
    render(<ExchangePage />);
    expect(screen.getByText('Exchange Calculator')).toBeInTheDocument();
  });
});
