import { notificationService } from '../notification.service';
import { apiClient } from '../api';
import { PushNotification, ApiResponse } from '@/types';
import * as Notifications from 'expo-notifications';

// Mock the apiClient
jest.mock('../api', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

// Mock expo-notifications
jest.mock('expo-notifications', () => ({
  setNotificationHandler: jest.fn(),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  getExpoPushTokenAsync: jest.fn(),
  scheduleNotificationAsync: jest.fn(),
  addNotificationReceivedListener: jest.fn(),
  addNotificationResponseReceivedListener: jest.fn(),
  setBadgeCountAsync: jest.fn(),
  dismissAllNotificationsAsync: jest.fn(),
}));

// Mock Platform
jest.mock('react-native', () => ({
  Platform: {
    OS: 'ios',
  },
}));

describe('notificationService', () => {
  const mockGet = apiClient.get as jest.MockedFunction<typeof apiClient.get>;
  const mockPost = apiClient.post as jest.MockedFunction<typeof apiClient.post>;
  const mockGetPermissionsAsync = Notifications.getPermissionsAsync as jest.MockedFunction<typeof Notifications.getPermissionsAsync>;
  const mockRequestPermissionsAsync = Notifications.requestPermissionsAsync as jest.MockedFunction<typeof Notifications.requestPermissionsAsync>;
  const mockGetExpoPushTokenAsync = Notifications.getExpoPushTokenAsync as jest.MockedFunction<typeof Notifications.getExpoPushTokenAsync>;
  const mockScheduleNotificationAsync = Notifications.scheduleNotificationAsync as jest.MockedFunction<typeof Notifications.scheduleNotificationAsync>;
  const mockAddNotificationReceivedListener = Notifications.addNotificationReceivedListener as jest.MockedFunction<typeof Notifications.addNotificationReceivedListener>;
  const mockAddNotificationResponseReceivedListener = Notifications.addNotificationResponseReceivedListener as jest.MockedFunction<typeof Notifications.addNotificationResponseReceivedListener>;
  const mockSetBadgeCountAsync = Notifications.setBadgeCountAsync as jest.MockedFunction<typeof Notifications.setBadgeCountAsync>;
  const mockDismissAllNotificationsAsync = Notifications.dismissAllNotificationsAsync as jest.MockedFunction<typeof Notifications.dismissAllNotificationsAsync>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('requestPermissions', () => {
    it('should return true when permission is already granted', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'granted' } as any);

      const result = await notificationService.requestPermissions();

      expect(mockGetPermissionsAsync).toHaveBeenCalledTimes(1);
      expect(mockRequestPermissionsAsync).not.toHaveBeenCalled();
      expect(result).toBe(true);
    });

    it('should request permission when not granted', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'undetermined' } as any);
      mockRequestPermissionsAsync.mockResolvedValueOnce({ status: 'granted' } as any);

      const result = await notificationService.requestPermissions();

      expect(mockGetPermissionsAsync).toHaveBeenCalledTimes(1);
      expect(mockRequestPermissionsAsync).toHaveBeenCalledTimes(1);
      expect(result).toBe(true);
    });

    it('should return false when permission is denied', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'denied' } as any);
      mockRequestPermissionsAsync.mockResolvedValueOnce({ status: 'denied' } as any);

      const result = await notificationService.requestPermissions();

      expect(result).toBe(false);
    });

    it('should handle permission request error', async () => {
      mockGetPermissionsAsync.mockRejectedValueOnce(new Error('Permission error'));

      await expect(notificationService.requestPermissions()).rejects.toThrow('Permission error');
    });
  });

  describe('getExpoPushToken', () => {
    it('should get push token successfully', async () => {
      const mockToken = { data: 'ExponentPushToken[xxxxxxxxxxxxxxxx]' };
      mockGetExpoPushTokenAsync.mockResolvedValueOnce(mockToken as any);

      const result = await notificationService.getExpoPushToken();

      expect(mockGetExpoPushTokenAsync).toHaveBeenCalledWith({
        projectId: 'payu-digital-banking',
      });
      expect(result).toBe(mockToken.data);
    });

    it('should handle token retrieval error', async () => {
      mockGetExpoPushTokenAsync.mockRejectedValueOnce(new Error('Token error'));

      await expect(notificationService.getExpoPushToken()).rejects.toThrow('Token error');
    });
  });

  describe('registerPushToken', () => {
    it('should register push token successfully', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'granted' } as any);
      mockGetExpoPushTokenAsync.mockResolvedValueOnce({ data: 'ExponentPushToken[xxx]' } as any);
      mockPost.mockResolvedValueOnce({ data: {} });

      await notificationService.registerPushToken();

      expect(mockGetPermissionsAsync).toHaveBeenCalledTimes(1);
      expect(mockGetExpoPushTokenAsync).toHaveBeenCalledTimes(1);
      expect(mockPost).toHaveBeenCalledWith('/notifications/register', {
        token: 'ExponentPushToken[xxx]',
        platform: 'ios',
      });
    });

    it('should not register when permission is denied', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'denied' } as any);
      mockRequestPermissionsAsync.mockResolvedValueOnce({ status: 'denied' } as any);

      await notificationService.registerPushToken();

      expect(mockGetExpoPushTokenAsync).not.toHaveBeenCalled();
      expect(mockPost).not.toHaveBeenCalled();
    });

    it('should handle missing push token', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'granted' } as any);
      mockGetExpoPushTokenAsync.mockResolvedValueOnce({ data: undefined } as any);

      await notificationService.registerPushToken();

      expect(mockPost).not.toHaveBeenCalled();
    });

    it('should handle registration API error', async () => {
      mockGetPermissionsAsync.mockResolvedValueOnce({ status: 'granted' } as any);
      mockGetExpoPushTokenAsync.mockResolvedValueOnce({ data: 'ExponentPushToken[xxx]' } as any);
      mockPost.mockRejectedValueOnce(new Error('Registration failed'));

      await expect(notificationService.registerPushToken()).rejects.toThrow('Registration failed');
    });
  });

  describe('getNotifications', () => {
    const mockNotifications: PushNotification[] = [
      {
        id: 'notif-1',
        title: 'Transfer Successful',
        body: 'You have successfully transferred Rp 100,000',
        data: { transactionId: 'txn-1' },
        readAt: undefined,
        createdAt: '2024-01-15T10:00:00Z',
      },
      {
        id: 'notif-2',
        title: 'Top Up Received',
        body: 'You received Rp 500,000',
        data: { transactionId: 'txn-2' },
        readAt: '2024-01-15T11:00:00Z',
        createdAt: '2024-01-15T09:00:00Z',
      },
    ];

    it('should get notifications successfully', async () => {
      const apiResponse: ApiResponse<PushNotification[]> = {
        success: true,
        data: mockNotifications,
        message: 'Notifications retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await notificationService.getNotifications();

      expect(mockGet).toHaveBeenCalledWith('/notifications');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockNotifications);
    });

    it('should return empty array when no notifications', async () => {
      const apiResponse: ApiResponse<PushNotification[]> = {
        success: true,
        data: [],
        message: 'No notifications',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await notificationService.getNotifications();

      expect(result).toEqual([]);
    });

    it('should handle network errors', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network Error'));

      await expect(notificationService.getNotifications()).rejects.toThrow('Network Error');
    });

    it('should handle unauthorized access', async () => {
      const error = new Error('Unauthorized');
      (error as any).response = { status: 401 };
      mockGet.mockRejectedValueOnce(error);

      await expect(notificationService.getNotifications()).rejects.toThrow('Unauthorized');
    });
  });

  describe('markAsRead', () => {
    it('should mark notification as read successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await notificationService.markAsRead('notif-1');

      expect(mockPost).toHaveBeenCalledWith('/notifications/notif-1/read');
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle notification not found', async () => {
      const error = new Error('Notification not found');
      (error as any).response = { status: 404 };
      mockPost.mockRejectedValueOnce(error);

      await expect(notificationService.markAsRead('invalid-id')).rejects.toThrow('Notification not found');
    });

    it('should handle already read notification', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await notificationService.markAsRead('notif-1');

      expect(mockPost).toHaveBeenCalledWith('/notifications/notif-1/read');
    });
  });

  describe('markAllAsRead', () => {
    it('should mark all notifications as read successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await notificationService.markAllAsRead();

      expect(mockPost).toHaveBeenCalledWith('/notifications/read-all');
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle server error', async () => {
      const error = new Error('Internal Server Error');
      (error as any).response = { status: 500 };
      mockPost.mockRejectedValueOnce(error);

      await expect(notificationService.markAllAsRead()).rejects.toThrow('Internal Server Error');
    });

    it('should handle network error', async () => {
      mockPost.mockRejectedValueOnce(new Error('Network Error'));

      await expect(notificationService.markAllAsRead()).rejects.toThrow('Network Error');
    });
  });

  describe('sendLocalNotification', () => {
    it('should schedule local notification successfully', async () => {
      mockScheduleNotificationAsync.mockResolvedValueOnce('notification-id');

      await notificationService.sendLocalNotification('Test Title', 'Test Body');

      expect(mockScheduleNotificationAsync).toHaveBeenCalledWith({
        content: {
          title: 'Test Title',
          body: 'Test Body',
          data: undefined,
          sound: true,
        },
        trigger: null,
      });
    });

    it('should schedule notification with data', async () => {
      mockScheduleNotificationAsync.mockResolvedValueOnce('notification-id');

      const data = { transactionId: 'txn-1', type: 'transfer' };
      await notificationService.sendLocalNotification('Test', 'Body', data);

      expect(mockScheduleNotificationAsync).toHaveBeenCalledWith({
        content: {
          title: 'Test',
          body: 'Body',
          data,
          sound: true,
        },
        trigger: null,
      });
    });

    it('should handle scheduling error', async () => {
      mockScheduleNotificationAsync.mockRejectedValueOnce(new Error('Scheduling failed'));

      await expect(notificationService.sendLocalNotification('Test', 'Body'))
        .rejects.toThrow('Scheduling failed');
    });
  });

  describe('addNotificationListener', () => {
    it('should add notification received listener', () => {
      const mockHandler = jest.fn();
      const mockSubscription = { remove: jest.fn() };
      mockAddNotificationReceivedListener.mockReturnValueOnce(mockSubscription as any);

      const result = notificationService.addNotificationListener(mockHandler);

      expect(mockAddNotificationReceivedListener).toHaveBeenCalledWith(mockHandler);
      expect(result).toBe(mockSubscription);
    });
  });

  describe('addNotificationResponseListener', () => {
    it('should add notification response listener', () => {
      const mockHandler = jest.fn();
      const mockSubscription = { remove: jest.fn() };
      mockAddNotificationResponseReceivedListener.mockReturnValueOnce(mockSubscription as any);

      const result = notificationService.addNotificationResponseListener(mockHandler);

      expect(mockAddNotificationResponseReceivedListener).toHaveBeenCalledWith(mockHandler);
      expect(result).toBe(mockSubscription);
    });
  });

  describe('setBadgeCount', () => {
    it('should set badge count successfully', async () => {
      mockSetBadgeCountAsync.mockResolvedValueOnce(true);

      await notificationService.setBadgeCount(5);

      expect(mockSetBadgeCountAsync).toHaveBeenCalledWith(5);
      expect(mockSetBadgeCountAsync).toHaveBeenCalledTimes(1);
    });

    it('should set badge count to 0', async () => {
      mockSetBadgeCountAsync.mockResolvedValueOnce(true);

      await notificationService.setBadgeCount(0);

      expect(mockSetBadgeCountAsync).toHaveBeenCalledWith(0);
    });

    it('should handle badge count error', async () => {
      mockSetBadgeCountAsync.mockRejectedValueOnce(new Error('Badge error'));

      await expect(notificationService.setBadgeCount(5)).rejects.toThrow('Badge error');
    });
  });

  describe('dismissAllNotifications', () => {
    it('should dismiss all notifications successfully', async () => {
      mockDismissAllNotificationsAsync.mockResolvedValueOnce(undefined);

      await notificationService.dismissAllNotifications();

      expect(mockDismissAllNotificationsAsync).toHaveBeenCalledTimes(1);
    });

    it('should handle dismiss error', async () => {
      mockDismissAllNotificationsAsync.mockRejectedValueOnce(new Error('Dismiss failed'));

      await expect(notificationService.dismissAllNotifications()).rejects.toThrow('Dismiss failed');
    });
  });
});
