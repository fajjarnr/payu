import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import MobileNav from '@/components/MobileNav';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock next/navigation
vi.mock('next/navigation', () => ({
  usePathname: () => '/',
}));

// Mock localStorage
const mockLocalStorage = {
  getItem: vi.fn(() => 'test-token'),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};

Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

expect.extend(toHaveNoViolations);

describe('MobileNav', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLocalStorage.getItem.mockReturnValue('test-token');
  });

  it('should render mobile navigation with all items', () => {
    renderWithIntl(<MobileNav />);

    expect(screen.getByText('Beranda')).toBeInTheDocument();
    expect(screen.getByText('Transfer')).toBeInTheDocument();
    expect(screen.getByText('Kantong')).toBeInTheDocument();
    expect(screen.getByText('Tagihan')).toBeInTheDocument();
  });

  it('should not render on login page', () => {
    // Mock pathname to be /login
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/login',
    }));

    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it('should not render on onboarding page', () => {
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/onboarding',
    }));

    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it('should not render when user is not authenticated', () => {
    mockLocalStorage.getItem.mockReturnValue(null);

    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it('should render when user is authenticated', () => {
    mockLocalStorage.getItem.mockReturnValue('valid-token');

    renderWithIntl(<MobileNav />);

    expect(screen.getByText('Beranda')).toBeInTheDocument();
  });

  it('should highlight active navigation item', () => {
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/transfer',
    }));

    const { container } = renderWithIntl(<MobileNav />);

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
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/transfer',
    }));

    const { container } = renderWithIntl(<MobileNav />);

    // Active item should have visible label
    const activeLabel = container.querySelector('.text-\\[9px\\].font-bold.tracking-wider.opacity-100');
    expect(activeLabel).toBeInTheDocument();

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
    const { container } = renderWithIntl(<MobileNav />);

    const navItems = container.querySelectorAll('a');
    navItems.forEach((item) => {
      expect(item).toHaveClass('hover:text-foreground');
    });
  });

  it('should have proper spacing between navigation items', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.flex.justify-between.items-center');
    expect(navContainer).toBeInTheDocument();
  });

  it('should apply active styling with accent background', () => {
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/pockets',
    }));

    const { container } = renderWithIntl(<MobileNav />);

    const activeIconContainer = container.querySelector('.bg-accent.shadow-sm');
    expect(activeIconContainer).toBeInTheDocument();
  });

  it('should scale active icon', () => {
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/bills',
    }));

    const { container } = renderWithIntl(<MobileNav />);

    const activeIcon = container.querySelector('.scale-110');
    expect(activeIcon).toBeInTheDocument();
  });

  it('should use increased stroke width for active icon', () => {
    vi.doMock('next/navigation', () => ({
      usePathname: () => '/',
    }));

    const { container } = renderWithIntl(<MobileNav />);

    const activeIcon = container.querySelector('.stroke-\\[2\\.5px\\]');
    expect(activeIcon).toBeInTheDocument();
  });

  it('should have safe area padding for mobile devices', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.pb-safe');
    expect(navContainer).toBeInTheDocument();
  });

  it('should handle localStorage access errors gracefully', () => {
    mockLocalStorage.getItem.mockImplementation(() => {
      throw new Error('localStorage access denied');
    });

    expect(() => {
      renderWithIntl(<MobileNav />);
    }).not.toThrow();
  });

  it('should check for token with correct key', () => {
    renderWithIntl(<MobileNav />);

    expect(mockLocalStorage.getItem).toHaveBeenCalledWith('token');
  });

  it('should render with correct height', () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector('.h-16');
    expect(navContainer).toBeInTheDocument();
  });
});
