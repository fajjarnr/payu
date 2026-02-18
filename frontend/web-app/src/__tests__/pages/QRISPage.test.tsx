import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import QRISPage from '@/app/[locale]/qris/page';

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

describe('QRISPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<QRISPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<QRISPage />);
    expect(screen.getByText('Pembayaran QRIS')).toBeInTheDocument();
  });

  it('should render camera and upload actions', () => {
    render(<QRISPage />);
    expect(screen.getByText('Buka Kamera')).toBeInTheDocument();
    expect(screen.getByText('Unggah Foto')).toBeInTheDocument();
  });

  it('should render personal QR section', () => {
    render(<QRISPage />);
    expect(screen.getByText('QRIS Personal')).toBeInTheDocument();
    expect(screen.getByText('Tampilkan Kode Saya')).toBeInTheDocument();
  });

  it('should render recent activity', () => {
    render(<QRISPage />);
    expect(screen.getByText('Aktivitas Terakhir')).toBeInTheDocument();
  });

  it('should render security protocols section', () => {
    render(<QRISPage />);
    expect(screen.getByText('Protokol Keamanan')).toBeInTheDocument();
  });
});
