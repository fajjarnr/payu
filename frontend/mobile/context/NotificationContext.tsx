import React, { createContext, useContext, useEffect, ReactNode } from 'react';
import { notificationService } from '@/services/notification.service';
import { PushNotification } from '@/types';

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
  const [notifications, setNotifications] = React.useState<PushNotification[]>([]);
  const [permissionGranted, setPermissionGranted] = React.useState(false);

  useEffect(() => {
    registerNotifications();
    setupNotificationListeners();
  }, []);

  const registerNotifications = async () => {
    const granted = await notificationService.requestPermissions();
    setPermissionGranted(granted);

    if (granted) {
      await notificationService.registerPushToken();
    }
  };

  const setupNotificationListeners = () => {
    const subscription = notificationService.addNotificationListener(
      (notification) => {
        console.log('Notification received:', notification);
      }
    );

    const responseSubscription =
      notificationService.addNotificationResponseListener((response) => {
        console.log('Notification tapped:', response);
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
    await notificationService.sendLocalNotification(title, body, data);
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
