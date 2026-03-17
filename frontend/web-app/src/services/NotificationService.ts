import api from '@/lib/api';

// --- Interfaces matching backend NotificationResource (Quarkus JAX-RS) ---

// BUG-CROSS-058: Backend NotificationResponse has: id, userId, channel, recipient, title, body, status, createdAt, sentAt, readAt
// No 'type' field. No boolean 'read' — use readAt instead.
export interface Notification {
  id: string;
  userId: string;
  channel: NotificationChannel;
  recipient: string;
  title: string;
  body: string;
  status: string;
  createdAt: string;
  sentAt?: string;
  readAt?: string;
}

// BUG-CROSS-058: Backend SendNotificationRequest: userId, channel, recipient, title, body, templateId, data
// No 'type' field.
export interface SendNotificationRequest {
  userId: string;
  channel: NotificationChannel;
  recipient: string;
  title: string;
  body: string;
  templateId?: string;
  data?: Record<string, unknown>;
}

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

  // BUG-CROSS-057: Backend getByUser returns flat List<NotificationResponse> with @QueryParam("limit"), not paged
  /** GET /notifications/user/{userId} — Get user notifications */
  async getUserNotifications(userId: string, limit = 20): Promise<Notification[]> {
    const response = await api.get(`/notifications/user/${userId}`, { params: { limit } });
    return response.data;
  }

  /** POST /notifications/{id}/read — Mark notification as read */
  async markAsRead(notificationId: string): Promise<void> {
    await api.post(`/notifications/${notificationId}/read`);
  }
}

export default NotificationService.getInstance();
