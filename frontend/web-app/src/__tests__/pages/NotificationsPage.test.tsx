import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import NotificationsPage from '@/app/[locale]/notifications/page';

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

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: () => ({
    data: { content: [], totalElements: 0 },
    isLoading: false,
  }),
  useMarkNotificationRead: () => ({ mutateAsync: vi.fn() }),
}));

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<NotificationsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<NotificationsPage />);
    expect(screen.getByText('Kotak Masuk')).toBeInTheDocument();
  });

  it('should render filter tabs', () => {
    render(<NotificationsPage />);
    expect(screen.getByText('Semua')).toBeInTheDocument();
    expect(screen.getByText('Belum Dibaca')).toBeInTheDocument();
  });

  it('should render action buttons', () => {
    render(<NotificationsPage />);
    expect(screen.getByText('Tandai Semua Dibaca')).toBeInTheDocument();
  });

  it('should render empty state when no notifications', () => {
    render(<NotificationsPage />);
    expect(screen.getByText('Tidak ada notifikasi')).toBeInTheDocument();
  });
});
