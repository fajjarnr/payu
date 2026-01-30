import React from 'react';
import { screen, fireEvent } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import PromoPopup from '@/components/cms/PromoPopup';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock framer-motion
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: React.HTMLAttributes<HTMLDivElement> & { children?: React.ReactNode }) => <div {...props}>{children}</div>,
  },
  AnimatePresence: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
}));

// Mock useRouter
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

// Mock usePopups hook
const mockPopups = [
  {
    id: 'popup-1',
    title: 'Special Offer',
    description: 'Get 20% off on your next purchase',
    imageUrl: 'https://example.com/popup1.jpg',
    actionUrl: '/offers/special',
    actionType: 'DEEP_LINK' as const,
    startDate: '2024-01-01',
    endDate: '2025-12-31',
    metadata: {},
  },
];

let mockPopupsData = mockPopups;
let mockIsLoading = false;

vi.mock('@/hooks', () => ({
  usePopups: () => ({
    data: mockPopupsData,
    isLoading: mockIsLoading,
  }),
}));

// Mock localStorage and sessionStorage
const mockLocalStorage = {
  getItem: vi.fn((..._args: unknown[]): string | null => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};

const mockSessionStorage = {
  getItem: vi.fn((..._args: unknown[]): string | null => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};

Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

Object.defineProperty(window, 'sessionStorage', {
  value: mockSessionStorage,
});

expect.extend(toHaveNoViolations);

describe('PromoPopup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    mockPopupsData = mockPopups;
    mockIsLoading = false;
    mockLocalStorage.getItem.mockReturnValue(null);
    mockSessionStorage.getItem.mockReturnValue(null);
    mockPush.mockClear();
    global.open = vi.fn();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should not render popup immediately', () => {
    renderWithIntl(<PromoPopup delay={2000} />);

    expect(screen.queryByText('Special Offer')).not.toBeInTheDocument();
  });

  it('should render popup after delay', () => {
    renderWithIntl(<PromoPopup delay={2000} />);

    vi.advanceTimersByTime(2000);

    expect(screen.getByText('Special Offer')).toBeInTheDocument();
  });

  it('should not render when loading', () => {
    mockIsLoading = true;
    mockPopupsData = [];

    renderWithIntl(<PromoPopup delay={0} />);

    expect(screen.queryByText('Special Offer')).not.toBeInTheDocument();

    // Reset
    mockIsLoading = false;
    mockPopupsData = mockPopups;
  });

  it('should not render when no popups', () => {
    mockPopupsData = [];

    renderWithIntl(<PromoPopup delay={0} />);

    expect(screen.queryByText('Special Offer')).not.toBeInTheDocument();
  });

  it('should filter out permanently dismissed popups', () => {
    mockLocalStorage.getItem.mockImplementation((_key: unknown) => {
      if (_key === 'promo-popup-state-dismissed-popup-1') return 'true';
      return null;
    });

    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    expect(screen.queryByText('Special Offer')).not.toBeInTheDocument();
  });

  it('should filter out popups shown this session', () => {
    mockSessionStorage.getItem.mockImplementation((_key: unknown) => {
      if (_key === 'promo-popup-session-shown-popup-1') return 'true';
      return null;
    });

    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    expect(screen.queryByText('Special Offer')).not.toBeInTheDocument();
  });

  it('should close popup when close button is clicked', () => {
    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    const closeButton = screen.getByLabelText('Close popup');
    fireEvent.click(closeButton);

    expect(mockSessionStorage.setItem).toHaveBeenCalled();
  });

  it('should permanently dismiss when "Don\'t Show Again" is clicked', () => {
    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    const dontShowButton = screen.getByText("Don't Show Again");
    fireEvent.click(dontShowButton);

    expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
      'promo-popup-state-dismissed-popup-1',
      'true'
    );
  });

  it('should navigate to deep link when action is clicked', () => {
    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    const claimButton = screen.getByText('Claim Now');
    fireEvent.click(claimButton);

    expect(mockPush).toHaveBeenCalledWith('/offers/special');
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should handle custom storage keys', () => {
    renderWithIntl(
      <PromoPopup
        delay={0}
        storageKey="custom-storage"
        sessionKey="custom-session"
      />
    );

    vi.advanceTimersByTime(100);

    const closeButton = screen.getByLabelText('Close popup');
    fireEvent.click(closeButton);

    expect(mockSessionStorage.setItem).toHaveBeenCalledWith(
      'custom-session',
      expect.any(String)
    );
  });

  it('should handle custom delay', () => {
    renderWithIntl(<PromoPopup delay={5000} />);

    vi.advanceTimersByTime(3000);
    expect(screen.queryByText('Special Offer')).not.toBeInTheDocument();

    vi.advanceTimersByTime(2000);
    expect(screen.getByText('Special Offer')).toBeInTheDocument();
  });

  it('should handle segment, location, and device props', () => {
    renderWithIntl(
      <PromoPopup
        delay={0}
        segment="vip"
        location="dashboard"
        device="mobile"
      />
    );

    vi.advanceTimersByTime(100);

    expect(screen.getByText('Special Offer')).toBeInTheDocument();
  });

  it('should display SPECIAL OFFER badge', () => {
    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    expect(screen.getByText('SPECIAL OFFER')).toBeInTheDocument();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<PromoPopup delay={0} />);

    vi.advanceTimersByTime(100);

    const closeButton = screen.getByLabelText('Close popup');
    expect(closeButton).toBeInTheDocument();
  });
});
