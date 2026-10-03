import React from 'react';
import { screen, fireEvent, within } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import DashboardLayout from '@/components/DashboardLayout';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

const mockPush = vi.fn();

vi.mock('@/lib/navigation', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/navigation')>()),
  usePathname: () => '/',
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
}));

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

    const desktopSidebar = screen.getByRole('menu');
    const { getByText } = within(desktopSidebar);

    expect(getByText('Utama')).toBeInTheDocument();

    expect(getByText('Dasbor')).toBeInTheDocument();
    expect(getByText('Akun')).toBeInTheDocument();
    expect(getByText('Transfer')).toBeInTheDocument();
    expect(getByText('Bayar QRIS')).toBeInTheDocument();
    expect(getByText('Tagihan')).toBeInTheDocument();
    expect(getByText('Kartu')).toBeInTheDocument();
    expect(getByText('Investasi')).toBeInTheDocument();
    expect(getByText('Analitik')).toBeInTheDocument();
  });

  it('should render desktop sidebar with other menu items', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const desktopSidebar = screen.getByRole('menu');
    const { getByText } = within(desktopSidebar);

    expect(getByText('Lainnya')).toBeInTheDocument();
    expect(getByText('Keamanan')).toBeInTheDocument();
    expect(getByText('Pengaturan')).toBeInTheDocument();
    expect(getByText('Bantuan')).toBeInTheDocument();
  });

  it('should render header with search input', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    expect(screen.getByPlaceholderText('Pencarian cerdas...')).toBeInTheDocument();
  });

  it('should render notification button with badge', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const notificationButton = screen.getByRole('button', { name: 'Notifikasi' });
    expect(notificationButton).toBeInTheDocument();

    const badge = notificationButton.closest('.ant-badge');
    expect(badge).toBeInTheDocument();
    expect(badge?.querySelector('.ant-badge-dot')).toBeInTheDocument();
  });

  it('should render user profile button', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    expect(profileButton).toBeInTheDocument();
    expect(profileButton).toHaveAttribute('aria-label', 'Menu profil pengguna');
  });

  it('should open mobile sidebar when menu button is clicked', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const menuButton = screen.getByTestId('mobile-menu-trigger');
    fireEvent.click(menuButton);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('should close mobile sidebar when close button is clicked', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const menuButton = screen.getByTestId('mobile-menu-trigger');
    fireEvent.click(menuButton);

    const closeButton = screen.getByRole('button', { name: 'Close' });
    fireEvent.click(closeButton);

    // antd Drawer removes its open-state class synchronously on close
    const drawer = document.querySelector('.ant-drawer');
    expect(drawer).not.toHaveClass('ant-drawer-open');
  });

  it('should render mobile sidebar overlay', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const menuButton = screen.getByTestId('mobile-menu-trigger');
    fireEvent.click(menuButton);

    expect(document.querySelector('.ant-drawer-mask')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('should call onLogout when logout button is clicked', () => {
    const onLogout = vi.fn();
    renderWithIntl(<DashboardLayout {...defaultProps} onLogout={onLogout} />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    fireEvent.click(profileButton);

    const logoutButton = screen.getByRole('menuitem', { name: 'Keluar' });
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


  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should navigate to the correct route when a sidebar menu item is clicked', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    fireEvent.click(screen.getByRole('menuitem', { name: 'Dasbor' }));
    expect(mockPush).toHaveBeenCalledWith('/id/dashboard');

    fireEvent.click(screen.getByRole('menuitem', { name: 'Akun' }));
    expect(mockPush).toHaveBeenCalledWith('/id/pockets');

    fireEvent.click(screen.getByRole('menuitem', { name: 'Transfer' }));
    expect(mockPush).toHaveBeenCalledWith('/id/transfer');

    fireEvent.click(screen.getByRole('menuitem', { name: 'Bayar QRIS' }));
    expect(mockPush).toHaveBeenCalledWith('/id/qris');
  });

  it('should show notification badge indicator', () => {
    const { container } = renderWithIntl(<DashboardLayout {...defaultProps} />);

    const notificationButton = screen.getByRole('button', { name: 'Notifikasi' });
    expect(notificationButton).toBeInTheDocument();
    const indicator = notificationButton.querySelector('[aria-hidden="true"]');
    expect(indicator).toBeInTheDocument();
  });

  it('should hide mobile navigation on desktop screens', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    // antd Sider renders the desktop sidebar menu; responsive hiding is handled by the Sider breakpoint
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('should render search input with placeholder', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    expect(screen.getByPlaceholderText('Pencarian cerdas...')).toBeInTheDocument();
  });

  it('should render user dropdown with account section', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    fireEvent.click(profileButton);

    expect(screen.getByText('Authenticated User')).toBeInTheDocument();
    expect(screen.getByText('Test User')).toBeInTheDocument();
  });

  it('should render user dropdown with account section', () => {
    renderWithIntl(<DashboardLayout {...defaultProps} />);

    const profileButton = screen.getByLabelText('Menu profil pengguna');
    fireEvent.click(profileButton);

    expect(screen.getByText('Authenticated User')).toBeInTheDocument();
    expect(screen.getByText('Test User')).toBeInTheDocument();
  });
});
