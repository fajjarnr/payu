import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import SettingsPage from '@/app/[locale]/settings/page';

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

vi.mock('@/components/settings/statement-downloader', () => ({
  StatementDownloader: () => <div data-testid="statement-downloader">Statement Downloader</div>,
}));

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<SettingsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<SettingsPage />);
    expect(screen.getByText('Ekosistem Akun')).toBeInTheDocument();
  });

  it('should render profile section', () => {
    render(<SettingsPage />);
    expect(screen.getByText('Profil Umum')).toBeInTheDocument();
    expect(screen.getByText('PENGGUNA PAYU')).toBeInTheDocument();
    expect(screen.getByText('Premium Member')).toBeInTheDocument();
  });

  it('should render credentials section', () => {
    render(<SettingsPage />);
    expect(screen.getByText('Kredensial Profil')).toBeInTheDocument();
  });

  it('should render system preferences', () => {
    render(<SettingsPage />);
    expect(screen.getByText('Preferensi Sistem')).toBeInTheDocument();
    expect(screen.getByText('Notifikasi Push')).toBeInTheDocument();
  });

  it('should render e-statement section', () => {
    render(<SettingsPage />);
    expect(screen.getByText('E-Statement')).toBeInTheDocument();
  });

  it('should render session actions', () => {
    render(<SettingsPage />);
    expect(screen.getByText('Sinkronisasi Profil')).toBeInTheDocument();
    expect(screen.getByText('Hapus Sesi')).toBeInTheDocument();
  });
});
