import api from '@/lib/api';

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  channel: NotificationChannel;
  title: string;
  body: string;
  data?: Record<string, unknown>;
  read: boolean;
  sentAt: string;
  readAt?: string;
}

export type NotificationType = 'TRANSACTION' | 'SECURITY' | 'PROMOTION' | 'SYSTEM' | 'REMINDER';
export type NotificationChannel = 'PUSH' | 'EMAIL' | 'SMS' | 'IN_APP';

export interface NotificationPreferences {
  pushEnabled: boolean;
  emailEnabled: boolean;
  smsEnabled: boolean;
  transactionAlerts: boolean;
  promotionAlerts: boolean;
  securityAlerts: boolean;
}

class NotificationService {
  private static instance: NotificationService;

  static getInstance(): NotificationService {
    if (!NotificationService.instance) {
      NotificationService.instance = new NotificationService();
    }
    return NotificationService.instance;
  }

  async getNotifications(page = 0, size = 20): Promise<{ content: Notification[]; totalElements: number }> {
    const response = await api.get('/notifications', { params: { page, size } });
    return response.data;
  }

  async getUnreadCount(): Promise<number> {
    const response = await api.get('/notifications/unread-count');
    return response.data.count;
  }

  async markAsRead(notificationId: string): Promise<void> {
    await api.put(`/notifications/${notificationId}/read`);
  }

  async markAllAsRead(): Promise<void> {
    await api.put('/notifications/read-all');
  }

  async getPreferences(): Promise<NotificationPreferences> {
    const response = await api.get('/notifications/preferences');
    return response.data;
  }

  async updatePreferences(preferences: Partial<NotificationPreferences>): Promise<NotificationPreferences> {
    const response = await api.put('/notifications/preferences', preferences);
    return response.data;
  }
}

export default NotificationService.getInstance();
