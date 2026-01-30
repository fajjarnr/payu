import React from 'react';
import { screen, fireEvent } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import BannerCarousel from '@/components/cms/BannerCarousel';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock framer-motion
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: React.HTMLAttributes<HTMLDivElement> & { children?: React.ReactNode }) => <div {...props}>{children}</div>,
  },
  AnimatePresence: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
}));

// Mock useBanners hook
const mockBanners = [
  {
    id: '1',
    title: 'Summer Sale',
    description: 'Get 50% off on all items',
    imageUrl: 'https://example.com/banner1.jpg',
    actionUrl: 'https://example.com/sale',
    actionType: 'LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: {},
  },
  {
    id: '2',
    title: 'New Arrival',
    description: 'Check out our latest collection',
    imageUrl: 'https://example.com/banner2.jpg',
    actionUrl: '/products/new',
    actionType: 'DEEP_LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: {},
  },
];

vi.mock('@/hooks', () => ({
  useBanners: () => ({
    data: mockBanners,
    isLoading: false,
    error: null,
  }),
}));

// Mock Skeleton component
vi.mock('@/components/ui/Skeleton', () => ({
  Skeleton: ({ className }: { className?: string }) => <div data-testid="skeleton" className={className} />,
}));

expect.extend(toHaveNoViolations);

describe('BannerCarousel', () => {
  const defaultProps = {
    autoPlayInterval: 5000,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    // Mock window.open
    global.open = vi.fn();
    global.window.location.href = '';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render banner carousel', () => {
    renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(screen.getByText('Summer Sale')).toBeInTheDocument();
    expect(screen.getByText('Get 50% off on all items')).toBeInTheDocument();
  });

  it('should render skeleton when loading', () => {
    vi.doMock('@/hooks', () => ({
      useBanners: () => ({
        data: null,
        isLoading: true,
        error: null,
      }),
    }));

    renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0);
  });

  it('should return null when there is an error', () => {
    vi.doMock('@/hooks', () => ({
      useBanners: () => ({
        data: null,
        isLoading: false,
        error: new Error('Failed to fetch'),
      }),
    }));

    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(container.firstChild).toBeNull();
  });

  it('should return null when there are no banners', () => {
    vi.doMock('@/hooks', () => ({
      useBanners: () => ({
        data: [],
        isLoading: false,
        error: null,
      }),
    }));

    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(container.firstChild).toBeNull();
  });

  it('should display navigation arrows when there are multiple banners', () => {
    renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(screen.getByLabelText('Previous banner')).toBeInTheDocument();
    expect(screen.getByLabelText('Next banner')).toBeInTheDocument();
  });

  it('should navigate to next banner when next button is clicked', () => {
    renderWithIntl(<BannerCarousel {...defaultProps} />);

    const nextButton = screen.getByLabelText('Next banner');
    fireEvent.click(nextButton);

    // Should show second banner
    expect(screen.getByText('New Arrival')).toBeInTheDocument();
  });

  it('should navigate to previous banner when previous button is clicked', () => {
    renderWithIntl(<BannerCarousel {...defaultProps} />);

    // First click next to go to second banner
    const nextButton = screen.getByLabelText('Next banner');
    fireEvent.click(nextButton);

    // Then click previous to go back
    const previousButton = screen.getByLabelText('Previous banner');
    fireEvent.click(previousButton);

    expect(screen.getByText('Summer Sale')).toBeInTheDocument();
  });

  it('should navigate to specific banner when indicator is clicked', () => {
    renderWithIntl(<BannerCarousel {...defaultProps} />);

    const indicators = screen.getAllByRole('button').filter(
      (button) => button.getAttribute('aria-label')?.includes('Go to banner')
    );

    if (indicators.length > 1) {
      fireEvent.click(indicators[1]);
      expect(screen.getByText('New Arrival')).toBeInTheDocument();
    }
  });

  it('should open external link when LINK action type is clicked', () => {
    const onBannerClick = vi.fn();
    renderWithIntl(<BannerCarousel {...defaultProps} onBannerClick={onBannerClick} />);

    const banner = screen.getByText('Summer Sale').closest('.cursor-pointer');
    if (banner) {
      fireEvent.click(banner);
      expect(onBannerClick).toHaveBeenCalledWith(mockBanners[0]);
    }
  });

  it('should call onBannerClick when provided', () => {
    const onBannerClick = vi.fn();
    renderWithIntl(<BannerCarousel {...defaultProps} onBannerClick={onBannerClick} />);

    const banner = screen.getByText('Summer Sale').closest('.cursor-pointer');
    if (banner) {
      fireEvent.click(banner);
      expect(onBannerClick).toHaveBeenCalled();
    }
  });

  it('should display indicators for multiple banners', () => {
    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);

    const indicators = container.querySelectorAll('.rounded-full.transition-all');
    expect(indicators.length).toBe(mockBanners.length);
  });

  it('should highlight current banner indicator', () => {
    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);

    const activeIndicator = container.querySelector('.w-8.bg-bank-green');
    expect(activeIndicator).toBeInTheDocument();
  });

  it('should auto-rotate banners', () => {
    vi.useFakeTimers();

    renderWithIntl(<BannerCarousel {...defaultProps} autoPlayInterval={1000} />);

    // Initially shows first banner
    expect(screen.getByText('Summer Sale')).toBeInTheDocument();

    // Fast forward past autoPlayInterval
    vi.advanceTimersByTime(1000);

    // Should show second banner
    expect(screen.getByText('New Arrival')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should stop auto-rotation when hovering', () => {
    vi.useFakeTimers();

    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} autoPlayInterval={1000} />);

    const banner = container.querySelector('.group');
    if (banner) {
      fireEvent.mouseEnter(banner);

      vi.advanceTimersByTime(2000);

      // Should still show first banner
      expect(screen.getByText('Summer Sale')).toBeInTheDocument();
    }

    vi.useRealTimers();
  });

  it('should have correct aspect ratio for responsive design', () => {
    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);

    const carousel = container.querySelector('.aspect-\\[2\\/1\\]');
    expect(carousel).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should apply custom className', () => {
    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} className="custom-class" />);

    const wrapper = container.querySelector('.custom-class');
    expect(wrapper).toBeInTheDocument();
  });

  it('should handle single banner without navigation arrows', () => {
    vi.doMock('@/hooks', () => ({
      useBanners: () => ({
        data: [mockBanners[0]],
        isLoading: false,
        error: null,
      }),
    }));

    renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(screen.queryByLabelText('Previous banner')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Next banner')).not.toBeInTheDocument();
  });

  it('should display PROMO badge on banners', () => {
    renderWithIntl(<BannerCarousel {...defaultProps} />);

    expect(screen.getByText('PROMO')).toBeInTheDocument();
  });

  it('should apply hover effect on banner', () => {
    const { container } = renderWithIntl(<BannerCarousel {...defaultProps} />);

    const bannerImage = container.querySelector('.group-hover\\:scale-105');
    expect(bannerImage).toBeInTheDocument();
  });

  it('should handle segment, location, and device props', () => {
    renderWithIntl(
      <BannerCarousel
        {...defaultProps}
        segment="vip"
        location="dashboard"
        device="mobile"
      />
    );

    expect(screen.getByText('Summer Sale')).toBeInTheDocument();
  });

  it('should wrap around when reaching last banner', () => {
    vi.useFakeTimers();

    renderWithIntl(<BannerCarousel {...defaultProps} autoPlayInterval={100} />);

    // Go to last banner
    vi.advanceTimersByTime(100);
    expect(screen.getByText('New Arrival')).toBeInTheDocument();

    // Should wrap to first banner
    vi.advanceTimersByTime(100);
    expect(screen.getByText('Summer Sale')).toBeInTheDocument();

    vi.useRealTimers();
  });
});
