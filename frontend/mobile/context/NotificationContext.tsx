import React, { createContext, useContext, useEffect, ReactNode } from 'react';
import { notificationService } from '@/services/notification.service';
import { PushNotification } from '@/types';
import { Logger } from '@/utils/logger';

interface NotificationContextType {
  notifications: PushNotification[];
  permissionGranted: boolean;
  register: () => Promise<void>;
  sendLocalNotification: (title: string, body: string, data?: any) => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType>({
  notifications: [],
  permissionGranted: false,
  register: async () => {},
  sendLocalNotification: async () => {},
});

export const useNotificationContext = () => useContext(NotificationContext);

export const NotificationProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [notifications] = React.useState<PushNotification[]>([]);
  const [permissionGranted, setPermissionGranted] = React.useState(false);

  useEffect(() => {
    registerNotifications();
    setupNotificationListeners();
  }, []);

  const registerNotifications = async () => {
    try {
      const granted = await notificationService.requestPermissions();
      setPermissionGranted(granted);

      Logger.info('NotificationContext', `Permissions ${granted ? 'granted' : 'denied'}`);

      if (granted) {
        await notificationService.registerPushToken();
        Logger.info('NotificationContext', 'Push token registered successfully');
      }
    } catch (error) {
      Logger.error('NotificationContext', 'Failed to register notifications', error);
    }
  };

  const setupNotificationListeners = () => {
    const subscription = notificationService.addNotificationListener(
      (notification) => {
        Logger.debug('NotificationContext', 'Notification received', {
          title: notification.request.content.title,
        });
      }
    );

    const responseSubscription =
      notificationService.addNotificationResponseListener((response) => {
        Logger.debug('NotificationContext', 'Notification tapped', {
          actionIdentifier: response.actionIdentifier,
        });
      });

    return () => {
      subscription.remove();
      responseSubscription.remove();
    };
  };

  const sendLocalNotification = async (
    title: string,
    body: string,
    data?: any
  ) => {
    try {
      await notificationService.sendLocalNotification(title, body, data);
      Logger.debug('NotificationContext', 'Local notification sent', { title });
    } catch (error) {
      Logger.error('NotificationContext', 'Failed to send local notification', error, {
        title,
      });
    }
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        permissionGranted,
        register: registerNotifications,
        sendLocalNotification,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};
