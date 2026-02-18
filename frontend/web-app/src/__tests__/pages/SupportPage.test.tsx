import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import SupportPage from '@/app/[locale]/support/page';

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

vi.mock('@/hooks/useSupport', () => ({
  useTickets: () => ({
    data: [],
    isLoading: false,
  }),
  useCreateTicket: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useTrainingStatus: () => ({
    data: [],
    isLoading: false,
  }),
}));

describe('SupportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<SupportPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<SupportPage />);
    expect(screen.getByText('Terminal Bantuan')).toBeInTheDocument();
  });

  it('should render help channels', () => {
    render(<SupportPage />);
    expect(screen.getByText('Bantuan Langsung')).toBeInTheDocument();
    expect(screen.getByText('Protokol Email')).toBeInTheDocument();
    expect(screen.getByText('Panggilan Suara')).toBeInTheDocument();
  });

  it('should render knowledge repository', () => {
    render(<SupportPage />);
    expect(screen.getByText('Repositori Inteligensi')).toBeInTheDocument();
  });

  it('should render system status', () => {
    render(<SupportPage />);
    expect(screen.getByText('Gateway OK')).toBeInTheDocument();
    expect(screen.getByText('Backend OK')).toBeInTheDocument();
  });
});
