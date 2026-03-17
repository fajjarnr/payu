import { describe, it, expect, vi, beforeEach } from 'vitest';
import NotificationService, {
  type Notification,
  type SendNotificationRequest,
} from '@/services/NotificationService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

// BUG-CROSS-058: Updated mock to match backend NotificationResponse (no type, no read boolean)
const mockNotification: Notification = {
  id: 'notif_001',
  userId: 'user_123',
  channel: 'PUSH',
  recipient: '+628123456789',
  title: 'Transfer Berhasil',
  body: 'Transfer sebesar Rp1.000.000 ke rekening 1234567890 berhasil.',
  status: 'SENT',
  createdAt: '2026-02-18T10:00:00Z',
  sentAt: '2026-02-18T10:00:00Z',
  readAt: undefined,
};

describe('NotificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('sendNotification', () => {
    it('should send a notification', async () => {
      // BUG-CROSS-058: Updated to match backend SendNotificationRequest (no type, has recipient/templateId)
      const request: SendNotificationRequest = {
        userId: 'user_123',
        channel: 'PUSH',
        recipient: '+628123456789',
        title: 'Transfer Berhasil',
        body: 'Transfer berhasil.',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockNotification });

      const result = await NotificationService.sendNotification(request);

      expect(api.post).toHaveBeenCalledWith('/notifications', request);
      expect(result.id).toBe('notif_001');
      expect(result.status).toBe('SENT');
    });
  });

  describe('getNotification', () => {
    it('should fetch notification by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockNotification });

      const result = await NotificationService.getNotification('notif_001');

      expect(api.get).toHaveBeenCalledWith('/notifications/notif_001');
      expect(result.title).toBe('Transfer Berhasil');
    });
  });

  // BUG-CROSS-057: getUserNotifications returns flat Notification[] with limit param (not paged)
  describe('getUserNotifications', () => {
    it('should fetch user notifications with default limit', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockNotification] });

      const result = await NotificationService.getUserNotifications('user_123');

      expect(api.get).toHaveBeenCalledWith('/notifications/user/user_123', {
        params: { limit: 20 },
      });
      expect(result).toHaveLength(1);
    });

    it('should support custom limit', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [] });

      await NotificationService.getUserNotifications('user_123', 10);

      expect(api.get).toHaveBeenCalledWith('/notifications/user/user_123', {
        params: { limit: 10 },
      });
    });
  });

  describe('markAsRead', () => {
    it('should mark notification as read', async () => {
      vi.mocked(api.post).mockResolvedValue({});

      await NotificationService.markAsRead('notif_001');

      expect(api.post).toHaveBeenCalledWith('/notifications/notif_001/read');
    });
  });
});
