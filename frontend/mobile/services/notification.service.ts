import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { apiClient } from './api';
import { PushNotification, ApiResponse } from '@/types';

// Configure notification handler
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

export const notificationService = {
  async requestPermissions(): Promise<boolean> {
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;

    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }

    return finalStatus === 'granted';
  },

  async getExpoPushToken(): Promise<string | undefined> {
    const token = await Notifications.getExpoPushTokenAsync({
      projectId: 'payu-digital-banking',
    });
    return token.data;
  },

  async registerPushToken(): Promise<void> {
    const hasPermission = await this.requestPermissions();

    if (hasPermission) {
      const pushToken = await this.getExpoPushToken();

      if (pushToken) {
        await apiClient.post('/notifications/register', {
          token: pushToken,
          platform: Platform.OS,
        });
      }
    }
  },

  async getNotifications(): Promise<PushNotification[]> {
    const response = await apiClient.get<ApiResponse<PushNotification[]>>(
      '/notifications'
    );
    return response.data.data;
  },

  async markAsRead(notificationId: string): Promise<void> {
    await apiClient.post(`/notifications/${notificationId}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.post('/notifications/read-all');
  },

  async sendLocalNotification(title: string, body: string, data?: any): Promise<void> {
    await Notifications.scheduleNotificationAsync({
      content: {
        title,
        body,
        data,
        sound: true,
      },
      trigger: null,
    });
  },

  addNotificationListener(
    handler: (notification: Notifications.Notification) => void
  ): Notifications.Subscription {
    return Notifications.addNotificationReceivedListener(handler);
  },

  addNotificationResponseListener(
    handler: (response: Notifications.NotificationResponse) => void
  ): Notifications.Subscription {
    return Notifications.addNotificationResponseReceivedListener(handler);
  },

  async setBadgeCount(count: number): Promise<void> {
    await Notifications.setBadgeCountAsync(count);
  },

  async dismissAllNotifications(): Promise<void> {
    await Notifications.dismissAllNotificationsAsync();
  },
};
