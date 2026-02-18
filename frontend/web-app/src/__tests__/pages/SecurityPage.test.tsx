import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SecurityPage from '@/app/[locale]/security/page';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
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

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/useBiometrics', () => ({
  useBiometricRegistrations: () => ({
    data: [],
    isLoading: false,
  }),
  useRegisterBiometric: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useRevokeBiometric: () => ({ mutateAsync: vi.fn() }),
}));

describe('SecurityPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<SecurityPage />, { wrapper: createWrapper() });
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<SecurityPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Keamanan & Tata Kelola')).toBeInTheDocument();
  });

  it('should render biometric MFA section', () => {
    render(<SecurityPage />, { wrapper: createWrapper() });
    expect(screen.getByText('MFA Biometrik')).toBeInTheDocument();
  });

  it('should render device tokens section', () => {
    render(<SecurityPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Token Perangkat')).toBeInTheDocument();
  });

  it('should render authenticated sessions', () => {
    render(<SecurityPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Sesi Terautentikasi')).toBeInTheDocument();
  });

  it('should render panic protocol', () => {
    render(<SecurityPage />, { wrapper: createWrapper() });
    expect(screen.getByText('Protokol Panic.')).toBeInTheDocument();
  });
});
