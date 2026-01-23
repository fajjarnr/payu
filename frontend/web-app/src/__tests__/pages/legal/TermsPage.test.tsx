import React from 'react';
import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import TermsPage from '@/app/legal/terms/page';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="dashboard-layout">{children}</div>,
}));

vi.mock('@/components/ui/Motion', () => ({
  PageTransition: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerItem: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

describe('TermsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    render(<TermsPage />);
    expect(screen.getByText('Syarat dan Ketentuan')).toBeInTheDocument();
  });

  it('renders version information', () => {
    render(<TermsPage />);
    expect(screen.getByText(/Versi 1.0/)).toBeInTheDocument();
    expect(screen.getByText(/Januari 2026/)).toBeInTheDocument();
  });

  it('renders all section headings', () => {
    render(<TermsPage />);
    
    expect(screen.getByText('1. Penerimaan Ketentuan')).toBeInTheDocument();
    expect(screen.getByText('2. Deskripsi Layanan')).toBeInTheDocument();
    expect(screen.getByText('3. Tanggung Jawab Pengguna')).toBeInTheDocument();
    expect(screen.getByText('4. Privasi dan Keamanan')).toBeInTheDocument();
    expect(screen.getByText('5. Batasan Tanggung Jawab')).toBeInTheDocument();
    expect(screen.getByText('6. Perubahan Ketentuan')).toBeInTheDocument();
  });

  it('renders contact email in footer', () => {
    render(<TermsPage />);
    expect(screen.getByText(/support@payu.id/)).toBeInTheDocument();
  });

  it('uses DashboardLayout wrapper', () => {
    render(<TermsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('renders introductory text for each section', () => {
    render(<TermsPage />);
    
    expect(screen.getByText(/Dengan mengakses dan menggunakan layanan PayU/)).toBeInTheDocument();
    expect(screen.getByText(/PayU menyediakan platform perbankan digital/)).toBeInTheDocument();
    expect(screen.getByText(/Pengguna bertanggung jawab untuk menjaga kerahasiaan/)).toBeInTheDocument();
  });

  it('renders SVG icons', () => {
    render(<TermsPage />);
    const svgs = document.querySelectorAll('svg');
    expect(svgs.length).toBeGreaterThan(0);
  });
});
