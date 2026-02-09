import api from '@/lib/api';

// --- Interfaces matching backend NotificationResource (Quarkus JAX-RS) ---

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

export interface SendNotificationRequest {
  userId: string;
  type: NotificationType;
  channel: NotificationChannel;
  title: string;
  body: string;
  data?: Record<string, unknown>;
}

export type NotificationType = 'TRANSACTION' | 'SECURITY' | 'PROMOTION' | 'SYSTEM' | 'REMINDER';
export type NotificationChannel = 'PUSH' | 'EMAIL' | 'SMS' | 'IN_APP';

// Kept for future backend implementation
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

  /** POST /notifications — Send a notification */
  async sendNotification(request: SendNotificationRequest): Promise<Notification> {
    const response = await api.post('/notifications', request);
    return response.data;
  }

  /** GET /notifications/{id} — Get a specific notification */
  async getNotification(id: string): Promise<Notification> {
    const response = await api.get(`/notifications/${id}`);
    return response.data;
  }

  /** GET /notifications/user/{userId} — Get user notifications */
  async getUserNotifications(userId: string, page = 0, size = 20): Promise<{ content: Notification[]; totalElements: number }> {
    const response = await api.get(`/notifications/user/${userId}`, { params: { page, size } });
    return response.data;
  }

  /** POST /notifications/{id}/read — Mark notification as read */
  async markAsRead(notificationId: string): Promise<void> {
    await api.post(`/notifications/${notificationId}/read`);
  }
}

export default NotificationService.getInstance();
