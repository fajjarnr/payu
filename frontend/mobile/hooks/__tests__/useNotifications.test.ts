import { renderHook, waitFor, act } from '@testing-library/react-native';
import { useNotifications } from '../useNotifications';
import { notificationService } from '@/services/notification.service';

// Mock the notification service
jest.mock('@/services/notification.service', () => ({
  notificationService: {
    requestPermissions: jest.fn(),
    registerPushToken: jest.fn(),
    getExpoPushToken: jest.fn(),
    getNotifications: jest.fn(),
    markAsRead: jest.fn(),
    markAllAsRead: jest.fn(),
    sendLocalNotification: jest.fn(),
    addNotificationListener: jest.fn(),
    addNotificationResponseListener: jest.fn(),
  },
}));

// Mock expo-notifications
jest.mock('expo-notifications', () => ({
  setNotificationHandler: jest.fn(),
}));

describe('useNotifications', () => {
  const mockNotificationListener = { remove: jest.fn() };
  const mockResponseListener = { remove: jest.fn() };

  const mockNotifications = [
    {
      id: 'notif-1',
      title: 'Transaction Successful',
      body: 'Your transfer of Rp 100,000 was successful',
      data: { type: 'transaction', transactionId: 'tx-123' },
      readAt: null,
      createdAt: '2024-01-01T10:00:00Z',
    },
    {
      id: 'notif-2',
      title: 'New Promo',
      body: 'Check out our latest promotions!',
      data: { type: 'promo' },
      readAt: '2024-01-01T11:00:00Z',
      createdAt: '2024-01-01T09:00:00Z',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'log').mockImplementation();

    (notificationService.addNotificationListener as jest.Mock).mockReturnValue(mockNotificationListener);
    (notificationService.addNotificationResponseListener as jest.Mock).mockReturnValue(mockResponseListener);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should initialize with default state', () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotifications());

    expect(result.current.notifications).toEqual([]);
    expect(result.current.permissionGranted).toBe(false);
    expect(result.current.pushToken).toBeUndefined();
  });

  it('should request permissions on mount', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(true);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotifications());

    await waitFor(() => {
      expect(notificationService.requestPermissions).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(result.current.permissionGranted).toBe(true);
    });
  });

  it('should register for push notifications on mount', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(true);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);
    (notificationService.getExpoPushToken as jest.Mock).mockResolvedValue('expo-push-token-123');

    const { result } = renderHook(() => useNotifications());

    await waitFor(() => {
      expect(notificationService.registerPushToken).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(result.current.pushToken).toBe('expo-push-token-123');
    });
  });

  it('should set up notification listeners on mount', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    renderHook(() => useNotifications());

    await waitFor(() => {
      expect(notificationService.addNotificationListener).toHaveBeenCalled();
      expect(notificationService.addNotificationResponseListener).toHaveBeenCalled();
    });
  });

  it('should clean up listeners on unmount', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    const { unmount } = renderHook(() => useNotifications());

    await waitFor(() => {
      expect(notificationService.addNotificationListener).toHaveBeenCalled();
    });

    unmount();

    expect(mockNotificationListener.remove).toHaveBeenCalled();
    expect(mockResponseListener.remove).toHaveBeenCalled();
  });

  it('should request permissions manually', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(true);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotifications());

    // Wait for initial mount effects
    await waitFor(() => {
      expect(notificationService.requestPermissions).toHaveBeenCalled();
    });

    // Reset mock to test manual call
    (notificationService.requestPermissions as jest.Mock).mockClear();
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(true);

    let permissionResult: boolean | undefined;
    await act(async () => {
      permissionResult = await result.current.requestPermissions();
    });

    expect(permissionResult).toBe(true);
    expect(notificationService.requestPermissions).toHaveBeenCalled();
  });

  it('should load notifications', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);
    (notificationService.getNotifications as jest.Mock).mockResolvedValue(mockNotifications);

    const { result } = renderHook(() => useNotifications());

    await act(async () => {
      await result.current.loadNotifications();
    });

    expect(notificationService.getNotifications).toHaveBeenCalled();
    expect(result.current.notifications).toEqual(mockNotifications);
  });

  it('should mark notification as read', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);
    (notificationService.getNotifications as jest.Mock).mockResolvedValue(mockNotifications);

    const { result } = renderHook(() => useNotifications());

    await act(async () => {
      await result.current.markAsRead('notif-1');
    });

    expect(notificationService.markAsRead).toHaveBeenCalledWith('notif-1');
    expect(notificationService.getNotifications).toHaveBeenCalled();
  });

  it('should mark all notifications as read', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);
    (notificationService.getNotifications as jest.Mock).mockResolvedValue([]);

    const { result } = renderHook(() => useNotifications());

    await act(async () => {
      await result.current.markAllAsRead();
    });

    expect(notificationService.markAllAsRead).toHaveBeenCalled();
    expect(notificationService.getNotifications).toHaveBeenCalled();
  });

  it('should send local notification', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotifications());

    await act(async () => {
      await result.current.sendLocalNotification('Test Title', 'Test Body', { key: 'value' });
    });

    expect(notificationService.sendLocalNotification).toHaveBeenCalledWith(
      'Test Title',
      'Test Body',
      { key: 'value' }
    );
  });

  it('should handle push token registration error gracefully', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(true);
    (notificationService.registerPushToken as jest.Mock).mockRejectedValue(new Error('Registration failed'));

    renderHook(() => useNotifications());

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith('Failed to register for push notifications:', expect.any(Error));
    });

    consoleSpy.mockRestore();
  });

  it('should handle empty notifications list', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);
    (notificationService.getNotifications as jest.Mock).mockResolvedValue([]);

    const { result } = renderHook(() => useNotifications());

    await act(async () => {
      await result.current.loadNotifications();
    });

    expect(result.current.notifications).toEqual([]);
  });

  it('should handle sendLocalNotification without data parameter', async () => {
    (notificationService.requestPermissions as jest.Mock).mockResolvedValue(false);
    (notificationService.registerPushToken as jest.Mock).mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotifications());

    await act(async () => {
      await result.current.sendLocalNotification('Test Title', 'Test Body');
    });

    expect(notificationService.sendLocalNotification).toHaveBeenCalledWith(
      'Test Title',
      'Test Body',
      undefined
    );
  });
});
