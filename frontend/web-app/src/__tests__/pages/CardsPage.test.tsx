import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import CardsPage from '@/app/[locale]/cards/page';

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

vi.mock('@/hooks/useCards', () => ({
  useCards: () => ({
    data: [
      {
        id: 'card_1',
        cardNumber: '****-****-****-1234',
        cardType: 'VIRTUAL',
        status: 'ACTIVE',
        dailyLimit: 10000000,
        expiryDate: '12/28',
      },
    ],
    isLoading: false,
  }),
  useFreezeCard: () => ({ mutateAsync: vi.fn() }),
  useUnfreezeCard: () => ({ mutateAsync: vi.fn() }),
  useCreateCard: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteCard: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateCard: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

describe('CardsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<CardsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<CardsPage />);
    expect(screen.getByText('Kartu Virtual')).toBeInTheDocument();
  });

  it('should render new card button', () => {
    render(<CardsPage />);
    expect(screen.getByText('Kartu Baru')).toBeInTheDocument();
  });

  it('should render card controls section', () => {
    render(<CardsPage />);
    expect(screen.getByText('Kontrol Operasional')).toBeInTheDocument();
  });

  it('should render card transaction toggles', () => {
    render(<CardsPage />);
    expect(screen.getByText('Transaksi Online')).toBeInTheDocument();
    expect(screen.getByText('Internasional')).toBeInTheDocument();
  });
});
