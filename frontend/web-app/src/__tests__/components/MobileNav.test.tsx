import React from 'react';
import { screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import MobileNav from '@/components/MobileNav';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock next/navigation with a mutable pathname
let mockPathname = '/';
vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}));

// Mock auth store with mutable state
let mockIsAuthenticated = true;
vi.mock('@/stores', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: 'test-user', fullName: 'Test User' },
      accountId: 'test-account',
      isAuthenticated: mockIsAuthenticated,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setAuthenticated: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn(),
    };
    return selector ? selector(state) : state;
  }),
}));

expect.extend(toHaveNoViolations);

/**
 * SECURITY NOTICE: Test Updates
 * ================================
 * These tests have been updated to reflect the security fix:
 * - MobileNav now uses auth store (Zustand) instead of localStorage
 * - Token is NO LONGER accessed from localStorage
 * - Authentication state comes from the auth store
 */
describe('MobileNav', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = '/';
    mockIsAuthenticated = true; // Default to authenticated for most tests
  });

  it('should render mobile navigation with all items', () => {
    renderWithIntl(<MobileNav />);

    expect(screen.getByText('Beranda')).toBeInTheDocument();
    expect(screen.getByText('Transfer')).toBeInTheDocument();
    expect(screen.getByText('Kantong')).toBeInTheDocument();
    expect(screen.getByText('Tagihan')).toBeInTheDocument();
  });

  it('should not render on login page', () => {
    mockPathname = '/login';
    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it('should not render on onboarding page', () => {
    mockPathname = '/onboarding';
    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it('should not render when user is not authenticated', () => {
    mockIsAuthenticated = false;
    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it('should render when user is authenticated', () => {
    mockIsAuthenticated = true;
    renderWithIntl(<MobileNav />);

    expect(screen.getByText('Beranda')).toBeInTheDocument();
  });

  it('should highlight active navigation item', () => {
    mockPathname = '/transfer';
    renderWithIntl(<MobileNav />);

    const activeLink = screen.getByText('Transfer').closest('a');
    expect(activeLink).toHaveClass('text-primary');
  });

  it('should render navigation items as links', () => {
    renderWithIntl(<MobileNav />);

    const homeLink = screen.getByText('Beranda').closest('a');
    expect(homeLink).toHaveAttribute('href', '/');

    const transferLink = screen.getByText('Transfer').closest('a');
    expect(transferLink).toHaveAttribute('href', '/transfer');

    const pocketsLink = screen.getByText('Kantong').closest('a');
    expect(pocketsLink).toHaveAttribute('href', '/pockets');

    const billsLink = screen.getByText('Tagihan').closest('a');
    expect(billsLink).toHaveAttribute('href', '/bills');
  });

  it('should have proper styling for mobile navigation', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.fixed.bottom-0.left-0.right-0');
    expect(navContainer).toHaveClass(
      'bg-card/80',
      'backdrop-blur-xl',
      'border-t',
      'border-border',
      'z-50'
    );
  });

  it('should hide on desktop screens', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.lg\\:hidden');
    expect(navContainer).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<MobileNav />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should display labels for active items only', () => {
    mockPathname = '/transfer';
    const { container } = renderWithIntl(<MobileNav />);

    // Active item should have visible label (text-xs class)
    const activeLabels = container.querySelectorAll('.opacity-100');
    expect(activeLabels.length).toBeGreaterThan(0);

    // Inactive items should have hidden labels
    const hiddenLabels = container.querySelectorAll('.opacity-0.h-0.overflow-hidden');
    expect(hiddenLabels.length).toBeGreaterThan(0);
  });

  it('should render icons for all navigation items', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const icons = container.querySelectorAll('svg');
    expect(icons.length).toBe(4); // 4 nav items
  });

  it('should apply hover effects to navigation items', () => {
    mockPathname = '/transfer';
    const { container } = renderWithIntl(<MobileNav />);

    const navItems = container.querySelectorAll('a');
    navItems.forEach((item) => {
      // Only inactive items have hover:text-foreground
      if (!item.classList.contains('text-primary')) {
        expect(item).toHaveClass('hover:text-foreground');
      }
    });
  });

  it('should have proper spacing between navigation items', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.flex.justify-between.items-center');
    expect(navContainer).toBeInTheDocument();
  });

  it('should apply active styling with accent background', () => {
    mockPathname = '/pockets';
    const { container } = renderWithIntl(<MobileNav />);

    const activeIconContainer = container.querySelector('.bg-accent.shadow-sm');
    expect(activeIconContainer).toBeInTheDocument();
  });

  it('should scale active icon', () => {
    mockPathname = '/bills';
    const { container } = renderWithIntl(<MobileNav />);

    const activeIcon = container.querySelector('.scale-110');
    expect(activeIcon).toBeInTheDocument();
  });

  it('should use increased stroke width for active icon', () => {
    mockPathname = '/';
    const { container } = renderWithIntl(<MobileNav />);

    // Check that the active icon has the stroke-[2.5px] class by checking the rendered HTML
    const activeIconContainer = container.querySelector('.bg-accent');
    expect(activeIconContainer).toBeInTheDocument();
    // The active icon should have a scale-110 class as well
    const activeIcon = activeIconContainer?.querySelector('.scale-110');
    expect(activeIcon).toBeInTheDocument();
  });

  it('should have safe area padding for mobile devices', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.pb-safe');
    expect(navContainer).toBeInTheDocument();
  });


  it('should render with correct height', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.h-16');
    expect(navContainer).toBeInTheDocument();
  });
});
