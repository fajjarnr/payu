import { useEffect, useState } from 'react';
import * as Notifications from 'expo-notifications';
import { notificationService } from '@/services/notification.service';
import { PushNotification } from '@/types';

export const useNotifications = () => {
  const [notifications, setNotifications] = useState<PushNotification[]>([]);
  const [permissionGranted, setPermissionGranted] = useState(false);
  const [pushToken, setPushToken] = useState<string | undefined>();

  useEffect(() => {
    requestPermissions();
    registerForPushNotifications();

    const notificationListener =
      notificationService.addNotificationListener((notification) => {
        // Handle incoming notification
        console.log('Notification received:', notification);
      });

    const responseListener =
      notificationService.addNotificationResponseListener((response) => {
        // Handle notification tap
        console.log('Notification tapped:', response);
      });

    return () => {
      notificationListener.remove();
      responseListener.remove();
    };
  }, []);

  const requestPermissions = async () => {
    const granted = await notificationService.requestPermissions();
    setPermissionGranted(granted);
    return granted;
  };

  const registerForPushNotifications = async () => {
    try {
      await notificationService.registerPushToken();
      const token = await notificationService.getExpoPushToken();
      setPushToken(token);
    } catch (error) {
      console.error('Failed to register for push notifications:', error);
    }
  };

  const loadNotifications = async () => {
    const notifs = await notificationService.getNotifications();
    setNotifications(notifs);
  };

  const markAsRead = async (notificationId: string) => {
    await notificationService.markAsRead(notificationId);
    await loadNotifications();
  };

  const markAllAsRead = async () => {
    await notificationService.markAllAsRead();
    await loadNotifications();
  };

  const sendLocalNotification = async (
    title: string,
    body: string,
    data?: any
  ) => {
    await notificationService.sendLocalNotification(title, body, data);
  };

  return {
    notifications,
    permissionGranted,
    pushToken,
    requestPermissions,
    loadNotifications,
    markAsRead,
    markAllAsRead,
    sendLocalNotification,
  };
};
