import React from 'react';
import { screen, fireEvent, within } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import DashboardLayout from '@/components/DashboardLayout';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock next/navigation
vi.mock('next/navigation', () => ({
  usePathname: () => '/',
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
}));

// Mock child components
vi.mock('@/components/personalization', () => ({
  PersonalizedGreeting: ({ showTimeBased, showSegment }: { showTimeBased?: boolean; showSegment?: boolean }) => (
    <div data-show-time-based={showTimeBased} data-show-segment={showSegment}>
      PersonalizedGreeting
    </div>
  ),
}));

vi.mock('@/components/MobileNav', () => ({
  __esModule: true,
  default: () => <div data-testid="mobile-nav">MobileNav</div>,
}));

vi.mock('@/components/LanguageSwitcher', () => ({
  __esModule: true,
  default: () => <button data-testid="language-switcher">LanguageSwitcher</button>,
}));

expect.extend(toHaveNoViolations);

describe('DashboardLayout', () => {
  const defaultProps = {
    children: <div>Test Content</div>,
    username: 'Test User',
  };

  it('should render dashboard layout with all main elements', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    expect(screen.getAllByText('PayU')[0]).toBeInTheDocument();
    expect(screen.getByText('Test Content')).toBeInTheDocument();
    expect(screen.getByTestId('mobile-nav')).toBeInTheDocument();
    expect(screen.getByTestId('language-switcher')).toBeInTheDocument();
  });

  it('should render desktop sidebar with main menu items', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    // Check inside desktop sidebar
    const desktopSidebar = screen.getByLabelText('Sidebar Navigasi Desktop');
    const { getByText } = within(desktopSidebar);

    expect(getByText('Menu Utama')).toBeInTheDocument();

    expect(getByText('Beranda')).toBeInTheDocument();
    expect(getByText('Kantong')).toBeInTheDocument();
    expect(getByText('Transfer')).toBeInTheDocument();
    expect(getByText('Pembayaran QRIS')).toBeInTheDocument();
    expect(getByText('Tagihan & Top-up')).toBeInTheDocument();
    expect(getByText('Kartu Virtual')).toBeInTheDocument();
    expect(getByText('Investasi')).toBeInTheDocument();
    expect(getByText('Analitik Keuangan')).toBeInTheDocument();
  });

  it('should render desktop sidebar with other menu items', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    // Check inside desktop sidebar
    const desktopSidebar = screen.getByLabelText('Sidebar Navigasi Desktop');
    const { getByText } = within(desktopSidebar);

    expect(getByText('Lainnya')).toBeInTheDocument();
    expect(getByText('Keamanan & MFA')).toBeInTheDocument();
    expect(getByText('Pengaturan Akun')).toBeInTheDocument();
    expect(getByText('Bantuan & Support')).toBeInTheDocument();
  });

  it('should render header with search input', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    expect(screen.getByPlaceholderText('Cari apapun...')).toBeInTheDocument();
  });

  it('should render notification button with badge', () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);

    const notificationBadge = container.querySelector('.bg-destructive.rounded-full');
    expect(notificationBadge).toBeInTheDocument();
  });

  it('should render user profile button', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    expect(profileButton).toBeInTheDocument();
    expect(profileButton).toHaveAttribute('aria-haspopup', 'true');
  });

  it('should open mobile sidebar when menu button is clicked', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const menuButton = screen.getByLabelText('Buka menu navigasi');
    fireEvent.click(menuButton);

    // Mobile sidebar should be visible
    const mobileSidebar = screen.getByLabelText('Sidebar Navigasi Mobile');
    expect(mobileSidebar).toHaveClass('translate-x-0');
  });

  it('should close mobile sidebar when close button is clicked', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    // Open mobile sidebar
    const menuButton = screen.getByLabelText('Buka menu navigasi');
    fireEvent.click(menuButton);

    // Close mobile sidebar
    const closeButton = screen.getByLabelText('Tutup menu navigasi');
    fireEvent.click(closeButton);

    const mobileSidebar = screen.getByLabelText('Sidebar Navigasi Mobile');
    expect(mobileSidebar).toHaveClass('-translate-x-full');
  });

  it('should close mobile sidebar when overlay is clicked', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    // Open mobile sidebar
    const menuButton = screen.getByLabelText('Buka menu navigasi');
    fireEvent.click(menuButton);

    // Click overlay
    const overlay = document.querySelector('.bg-foreground\\/20.z-50.lg\\:hidden');
    if (overlay) {
      fireEvent.click(overlay);
    }

    const mobileSidebar = screen.getByLabelText('Sidebar Navigasi Mobile');
    expect(mobileSidebar).toHaveClass('-translate-x-full');
  });

  it('should call onLogout when logout button is clicked', () => {
    const onLogout = vi.fn();
    renderWithIntl(<DashboardLayout {...defaultProps} onLogout={onLogout} />);

    // Open user menu
    const profileButton = screen.getByLabelText('Menu profil pengguna');
    fireEvent.click(profileButton);

    // Click logout
    const logoutButton = screen.getByText('Keluar Sesi');
    fireEvent.click(logoutButton);

    expect(onLogout).toHaveBeenCalled();
  });

  it('should display username in profile dropdown', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} username="John Doe" />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    fireEvent.click(profileButton);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('should render PersonalizedGreeting with correct props', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const greeting = screen.getByText('PersonalizedGreeting');
    expect(greeting).toHaveAttribute('data-show-time-based', 'true');
    expect(greeting).toHaveAttribute('data-show-segment', 'true');
  });

  it('should have proper ARIA labels for accessibility', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    expect(screen.getByLabelText('Buka menu navigasi')).toBeInTheDocument();
    expect(screen.getByLabelText('Notifikasi')).toBeInTheDocument();
    expect(screen.getByLabelText('Menu profil pengguna')).toBeInTheDocument();
  });

  it('should update aria-expanded when mobile sidebar is toggled', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const menuButton = screen.getByLabelText('Buka menu navigasi');
    expect(menuButton).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(menuButton);
    expect(menuButton).toHaveAttribute('aria-expanded', 'true');
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should render sidebar navigation links with correct hrefs', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const homeLinks = screen.getAllByText('Beranda');
    expect(homeLinks[0].closest('a')).toHaveAttribute('href', '/');

    const pocketLinks = screen.getAllByText('Kantong');
    expect(pocketLinks[0].closest('a')).toHaveAttribute('href', '/pockets');

    const transferLinks = screen.getAllByText('Transfer');
    expect(transferLinks[0].closest('a')).toHaveAttribute('href', '/transfer');

    const qrisLinks = screen.getAllByText('Pembayaran QRIS');
    expect(qrisLinks[0].closest('a')).toHaveAttribute('href', '/qris');
  });

  it('should show notification badge indicator', () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);

    const notificationIndicator = container.querySelector('[aria-label="Notifikasi baru"]');
    expect(notificationIndicator).toBeInTheDocument();
  });

  it('should hide mobile navigation on desktop screens', () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);

    const desktopSidebar = container.querySelector('.hidden.lg\\:flex');
    expect(desktopSidebar).toBeInTheDocument();

    const mobileNav = screen.getByTestId('mobile-nav');
    expect(mobileNav).toBeInTheDocument();
  });

  it('should render search input with placeholder', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const searchInput = screen.getByPlaceholderText('Cari apapun...');
    expect(searchInput).toHaveAttribute('type', 'text');
  });

  it('should apply responsive classes to main content area', () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);

    const mainContent = container.querySelector('main');
    expect(mainContent).toHaveClass('flex-1', 'overflow-y-auto');
  });

  it('should render logo with PayU branding', () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);

    const logoElements = container.querySelectorAll('.text-2xl.font-bold.text-primary');
    expect(logoElements.length).toBeGreaterThan(0);
  });

  it('should render user dropdown with account section', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    fireEvent.click(profileButton);

    expect(screen.getByText('Akun')).toBeInTheDocument();
    expect(screen.getByText('Test User')).toBeInTheDocument();
  });
});
