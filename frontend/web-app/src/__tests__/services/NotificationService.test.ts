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

const mockNotification: Notification = {
  id: 'notif_001',
  userId: 'user_123',
  type: 'TRANSACTION',
  channel: 'PUSH',
  title: 'Transfer Berhasil',
  body: 'Transfer sebesar Rp1.000.000 ke rekening 1234567890 berhasil.',
  data: { transactionId: 'tx_001', amount: 1000000 },
  read: false,
  sentAt: '2026-02-18T10:00:00Z',
  readAt: undefined,
};

describe('NotificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('sendNotification', () => {
    it('should send a notification', async () => {
      const request: SendNotificationRequest = {
        userId: 'user_123',
        type: 'TRANSACTION',
        channel: 'PUSH',
        title: 'Transfer Berhasil',
        body: 'Transfer berhasil.',
        data: { transactionId: 'tx_001' },
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockNotification });

      const result = await NotificationService.sendNotification(request);

      expect(api.post).toHaveBeenCalledWith('/notifications', request);
      expect(result.id).toBe('notif_001');
      expect(result.read).toBe(false);
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

  describe('getUserNotifications', () => {
    it('should fetch user notifications with default pagination', async () => {
      const mockResponse = {
        content: [mockNotification],
        totalElements: 1,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

      const result = await NotificationService.getUserNotifications('user_123');

      expect(api.get).toHaveBeenCalledWith('/notifications/user/user_123', {
        params: { page: 0, size: 20 },
      });
      expect(result.content).toHaveLength(1);
      expect(result.totalElements).toBe(1);
    });

    it('should support custom pagination', async () => {
      const mockResponse = { content: [], totalElements: 50 };
      vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

      await NotificationService.getUserNotifications('user_123', 2, 10);

      expect(api.get).toHaveBeenCalledWith('/notifications/user/user_123', {
        params: { page: 2, size: 10 },
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
