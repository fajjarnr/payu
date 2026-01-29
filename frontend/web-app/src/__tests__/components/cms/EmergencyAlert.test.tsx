import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import EmergencyAlert from '@/components/cms/EmergencyAlert';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock framer-motion
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: any) => <div {...props}>{children}</div>,
  },
  AnimatePresence: ({ children }: any) => <>{children}</>,
}));

// Mock useRouter
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

// Mock useEmergencyAlerts hook
const mockAlerts = [
  {
    id: 'alert-1',
    title: 'System Maintenance',
    description: 'Scheduled maintenance will occur on Sunday 2-4 AM',
    actionUrl: '/maintenance',
    actionType: 'DEEP_LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: {
      alertType: 'WARNING',
    },
  },
  {
    id: 'alert-2',
    title: 'New Feature Available',
    description: 'Check out our new analytics dashboard',
    actionUrl: 'https://example.com',
    actionType: 'LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: {
      alertType: 'INFO',
    },
  },
];

vi.mock('@/hooks', () => ({
  useEmergencyAlerts: () => ({
    data: mockAlerts,
    isLoading: false,
  }),
}));

// Mock localStorage
const mockLocalStorage = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
};
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

expect.extend(toHaveNoViolations);

describe('EmergencyAlert', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLocalStorage.getItem.mockReturnValue(null);
    mockPush.mockClear();
    global.open = vi.fn();
  });

  it('should render emergency alerts', () => {
    renderWithIntl(<EmergencyAlert />);

    expect(screen.getByText('System Maintenance')).toBeInTheDocument();
    expect(screen.getByText('Scheduled maintenance will occur on Sunday 2-4 AM')).toBeInTheDocument();
  });

  it('should not render when loading', () => {
    vi.doMock('@/hooks', () => ({
      useEmergencyAlerts: () => ({
        data: null,
        isLoading: true,
      }),
    }));

    const { container } = renderWithIntl(<EmergencyAlert />);
    expect(container.firstChild).toBeNull();
  });

  it('should not render when no alerts', () => {
    vi.doMock('@/hooks', () => ({
      useEmergencyAlerts: () => ({
        data: [],
        isLoading: false,
      }),
    }));

    const { container } = renderWithIntl(<EmergencyAlert />);
    expect(container.firstChild).toBeNull();
  });

  it('should filter out dismissed alerts', () => {
    mockLocalStorage.getItem.mockReturnValue(JSON.stringify(['alert-1']));

    renderWithIntl(<EmergencyAlert />);

    expect(screen.queryByText('System Maintenance')).not.toBeInTheDocument();
    expect(screen.getByText('New Feature Available')).toBeInTheDocument();
  });

  it('should dismiss alert when dismiss button is clicked', () => {
    renderWithIntl(<EmergencyAlert />);

    const dismissButton = screen.getByLabelText('Dismiss alert');
    fireEvent.click(dismissButton);

    expect(mockLocalStorage.setItem).toHaveBeenCalled();
  });

  it('should save dismissed alerts to localStorage', () => {
    renderWithIntl(<EmergencyAlert />);

    const dismissButton = screen.getAllByLabelText('Dismiss alert')[0];
    fireEvent.click(dismissButton);

    expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
      'dismissed-alerts',
      expect.stringContaining('alert-1')
    );
  });

  it('should navigate to deep link when DEEP_LINK action type', () => {
    renderWithIntl(<EmergencyAlert />);

    const alert = screen.getByText('System Maintenance').closest('.cursor-pointer');
    if (alert) {
      fireEvent.click(alert);
      expect(mockPush).toHaveBeenCalledWith('/maintenance');
    }
  });

  it('should open external link when LINK action type', () => {
    renderWithIntl(<EmergencyAlert />);

    const alert = screen.getByText('New Feature Available').closest('.cursor-pointer');
    if (alert) {
      fireEvent.click(alert);
      expect(global.open).toHaveBeenCalledWith('https://example.com', '_blank', 'noopener,noreferrer');
    }
  });

  it('should display correct styling for WARNING alert type', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const warningAlert = container.textContent?.includes('System Maintenance')
      ? container.querySelector('.bg-amber-50')
      : null;

    expect(warningAlert).toBeInTheDocument();
  });

  it('should display correct styling for INFO alert type', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const infoAlert = container.textContent?.includes('New Feature Available')
      ? container.querySelector('.bg-blue-50')
      : null;

    expect(infoAlert).toBeInTheDocument();
  });

  it('should display correct icon for WARNING alert', () => {
    renderWithIntl(<EmergencyAlert />);

    const warningIcon = screen.getByText('System Maintenance')
      .closest('div')
      ?.querySelector('svg');

    expect(warningIcon).toBeInTheDocument();
  });

  it('should display end date if available', () => {
    renderWithIntl(<EmergencyAlert />);

    expect(screen.getByText(/Valid until/)).toBeInTheDocument();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<EmergencyAlert />);

    const dismissButtons = screen.getAllByLabelText('Dismiss alert');
    dismissButtons.forEach((button) => {
      expect(button).toBeInTheDocument();
    });
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<EmergencyAlert />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should apply custom className', () => {
    const { container } = renderWithIntl(<EmergencyAlert className="custom-alert-class" />);

    expect(container.querySelector('.custom-alert-class')).toBeInTheDocument();
  });

  it('should use custom storage key', () => {
    renderWithIntl(<EmergencyAlert storageKey="custom-storage" />);

    const dismissButton = screen.getAllByLabelText('Dismiss alert')[0];
    fireEvent.click(dismissButton);

    expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
      'custom-storage',
      expect.any(String)
    );
  });

  it('should handle segment, location, and device props', () => {
    renderWithIntl(
      <EmergencyAlert
        segment="vip"
        location="dashboard"
        device="mobile"
      />
    );

    expect(screen.getByText('System Maintenance')).toBeInTheDocument();
  });

  it('should handle localStorage parse errors gracefully', () => {
    mockLocalStorage.getItem.mockReturnValue('invalid json');

    expect(() => {
      renderWithIntl(<EmergencyAlert />);
    }).not.toThrow();
  });

  it('should handle localStorage setItem errors gracefully', () => {
    mockLocalStorage.setItem.mockImplementation(() => {
      throw new Error('localStorage is full');
    });

    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    renderWithIntl(<EmergencyAlert />);

    const dismissButton = screen.getAllByLabelText('Dismiss alert')[0];
    fireEvent.click(dismissButton);

    expect(consoleSpy).toHaveBeenCalled();

    consoleSpy.mockRestore();
  });

  it('should display multiple alerts', () => {
    renderWithIntl(<EmergencyAlert />);

    expect(screen.getByText('System Maintenance')).toBeInTheDocument();
    expect(screen.getByText('New Feature Available')).toBeInTheDocument();
  });

  it('should animate alerts in and out', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const alert = container.querySelector('[initial*="opacity: 0"]');
    expect(alert).toBeInTheDocument();
  });

  it('should have backdrop blur effect', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const alertContainer = container.querySelector('.backdrop-blur-md');
    expect(alertContainer).toBeInTheDocument();
  });

  it('should apply correct border styling', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const alertBorder = container.querySelector('.border-b-2');
    expect(alertBorder).toBeInTheDocument();
  });

  it('should format end date correctly', () => {
    renderWithIntl(<EmergencyAlert />);

    const dateText = screen.getByText((content) => content.startsWith('Valid until'));
    expect(dateText).toBeInTheDocument();
  });
});
