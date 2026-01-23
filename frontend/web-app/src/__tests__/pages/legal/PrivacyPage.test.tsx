import React from 'react';
import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import PrivacyPage from '@/app/[locale]/legal/privacy/page';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="dashboard-layout">{children}</div>,
}));

vi.mock('@/components/ui/Motion', () => ({
  PageTransition: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerItem: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

describe('PrivacyPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    render(<PrivacyPage />);
    expect(screen.getByText('Kebijakan Privasi')).toBeInTheDocument();
  });

  it('renders version information', () => {
    render(<PrivacyPage />);
    expect(screen.getByText(/Versi 1.0/)).toBeInTheDocument();
    expect(screen.getByText(/Januari 2026/)).toBeInTheDocument();
  });

  it('renders all section headings', () => {
    render(<PrivacyPage />);
    
    expect(screen.getByText('1. Pengumpulan Informasi')).toBeInTheDocument();
    expect(screen.getByText('2. Penggunaan Informasi')).toBeInTheDocument();
    expect(screen.getByText('3. Keamanan Data')).toBeInTheDocument();
    expect(screen.getByText('4. Berbagi Informasi')).toBeInTheDocument();
    expect(screen.getByText('5. Hak Pengguna')).toBeInTheDocument();
    expect(screen.getByText('6. Kepatuhan Regulasi')).toBeInTheDocument();
  });

  it('renders contact email in footer', () => {
    render(<PrivacyPage />);
    expect(screen.getByText(/privacy@payu.id/)).toBeInTheDocument();
  });

  it('uses DashboardLayout wrapper', () => {
    render(<PrivacyPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('renders introductory text for each section', () => {
    render(<PrivacyPage />);
    
    expect(screen.getByText(/Kami mengumpulkan informasi yang Anda berikan secara langsung/)).toBeInTheDocument();
    expect(screen.getByText(/Informasi yang dikumpulkan digunakan untuk menyediakan/)).toBeInTheDocument();
    expect(screen.getByText(/Kami menerapkan standar keamanan industri/)).toBeInTheDocument();
  });

  it('renders SVG icons', () => {
    render(<PrivacyPage />);
    const svgs = document.querySelectorAll('svg');
    expect(svgs.length).toBeGreaterThan(0);
  });

  it('mentions data encryption', () => {
    render(<PrivacyPage />);
    expect(screen.getByText(/dienkripsi/)).toBeInTheDocument();
  });

  it('mentions SSL/TLS protocols', () => {
    render(<PrivacyPage />);
    expect(screen.getByText(/SSL\/TLS/)).toBeInTheDocument();
  });
});
