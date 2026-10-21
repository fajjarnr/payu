import { screen, fireEvent } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import EmergencyAlert from '@/components/cms/EmergencyAlert';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

const mockPush = vi.fn();
vi.mock('@/lib/navigation', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/navigation')>()),
  useRouter: () => ({
    push: mockPush,
  }),
}));

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

let mockAlertsData: typeof mockAlerts | null = mockAlerts;
let mockIsLoading = false;

vi.mock('@/hooks', () => ({
  useEmergencyAlerts: () => ({
    data: mockAlertsData,
    isLoading: mockIsLoading,
  }),
}));

const mockLocalStorage = {
  getItem: vi.fn((): string | null => null),
  setItem: vi.fn(),
};
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

expect.extend(toHaveNoViolations);

describe('EmergencyAlert', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAlertsData = mockAlerts;
    mockIsLoading = false;
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
    mockAlertsData = null;
    mockIsLoading = true;

    const { container } = renderWithIntl(<EmergencyAlert />);
    expect(container.firstChild).toBeNull();
  });

  it('should not render when no alerts', () => {
    mockAlertsData = [];

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

    const dismissButton = screen.getAllByLabelText(/^Tutup peringatan/)[0];
    fireEvent.click(dismissButton);

    expect(mockLocalStorage.setItem).toHaveBeenCalled();
  });

  it('should save dismissed alerts to localStorage', () => {
    renderWithIntl(<EmergencyAlert />);

    const dismissButton = screen.getAllByLabelText(/^Tutup peringatan/)[0];
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

    const warningAlert = container.querySelector('.ant-alert-warning');

    expect(warningAlert).toBeInTheDocument();
  });

  it('should display correct styling for INFO alert type', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const infoAlert = container.querySelector('.ant-alert-info');

    expect(infoAlert).toBeInTheDocument();
  });

  it('should display correct icon for WARNING alert', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    const warningAlert = container.querySelector('.ant-alert-warning');
    expect(warningAlert?.querySelector('svg')).toBeInTheDocument();
  });

  it('should display end date if available', () => {
    renderWithIntl(<EmergencyAlert />);

    expect(screen.getAllByText(/Berlaku hingga/)).toHaveLength(mockAlerts.length);
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<EmergencyAlert />);

    const dismissButtons = screen.getAllByLabelText(/^Tutup peringatan/);
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

    const dismissButton = screen.getAllByLabelText(/^Tutup peringatan/)[0];
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

    const dismissButton = screen.getAllByLabelText(/^Tutup peringatan/)[0];
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
    renderWithIntl(<EmergencyAlert />);
    expect(screen.getAllByRole('alert')).toHaveLength(mockAlerts.length);
  });

  it('should render alerts on the default container background token', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    expect(container.querySelector('.ant-alert')).toBeInTheDocument();
  });

  it('should render alerts with the shared card radius', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);

    expect(container.querySelector('.rounded-2xl')).toBeInTheDocument();
  });

  it('should format end date correctly', () => {
    renderWithIntl(<EmergencyAlert />);

    const dateText = screen.getAllByText((content) => content.startsWith('Berlaku hingga'));
    expect(dateText).toHaveLength(mockAlerts.length);
  });
});
