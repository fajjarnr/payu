import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CMSService, type Content, type ContentType, type ActionType } from '@/services/CMSService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('CMSService', () => {
  let service: CMSService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = CMSService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = CMSService.getInstance();
    const instance2 = CMSService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('getActiveContentByType', () => {
    it('should fetch active content by type without options', async () => {
      const mockContent: Content[] = [
        {
          id: 'content_1',
          contentType: 'BANNER',
          title: 'Promo Spesial',
          description: 'Dapatkan diskon 50%',
          imageUrl: 'https://example.com/banner1.jpg',
          actionUrl: 'https://example.com/promo',
          actionType: 'LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 10,
          status: 'ACTIVE',
          targetingRules: { segment: ['PREMIUM'] },
          metadata: { campaign: 'new_year' },
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
        {
          id: 'content_2',
          contentType: 'BANNER',
          title: 'Promo Biasa',
          description: 'Dapatkan diskon 10%',
          imageUrl: 'https://example.com/banner2.jpg',
          actionUrl: 'https://example.com/promo2',
          actionType: 'LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 5,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: {},
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockContent });

      const result = await service.getActiveContentByType('BANNER');

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/BANNER', {
        params: undefined,
      });
      expect(result).toHaveLength(2);
      expect(result[0].priority).toBeGreaterThan(result[1].priority);
      expect(result[0].title).toBe('Promo Spesial');
    });

    it('should fetch active content with targeting options', async () => {
      const mockContent: Content[] = [
        {
          id: 'content_3',
          contentType: 'PROMO',
          title: 'Promo Regional',
          description: 'Promo khusus Jakarta',
          imageUrl: 'https://example.com/promo_jkt.jpg',
          actionUrl: 'https://example.com/promo-jkt',
          actionType: 'DEEP_LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 8,
          status: 'ACTIVE',
          targetingRules: { location: ['JAKARTA'] },
          metadata: { region: 'JKT' },
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockContent });

      const options = {
        segment: 'PREMIUM',
        location: 'JAKARTA',
        device: 'MOBILE',
      };

      const result = await service.getActiveContentByType('PROMO', options);

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/PROMO', {
        params: options,
      });
      expect(result).toEqual(mockContent);
    });

    it('should handle empty content array', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [] });

      const result = await service.getActiveContentByType('ALERT');

      expect(result).toEqual([]);
    });

    it('should sort content by priority descending', async () => {
      const mockContent: Content[] = [
        {
          id: 'content_low',
          contentType: 'BANNER',
          title: 'Low Priority',
          description: 'Low priority banner',
          imageUrl: 'https://example.com/low.jpg',
          actionUrl: 'https://example.com/low',
          actionType: 'LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 1,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: {},
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
        {
          id: 'content_high',
          contentType: 'BANNER',
          title: 'High Priority',
          description: 'High priority banner',
          imageUrl: 'https://example.com/high.jpg',
          actionUrl: 'https://example.com/high',
          actionType: 'LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 100,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: {},
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
        {
          id: 'content_medium',
          contentType: 'BANNER',
          title: 'Medium Priority',
          description: 'Medium priority banner',
          imageUrl: 'https://example.com/medium.jpg',
          actionUrl: 'https://example.com/medium',
          actionType: 'LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 50,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: {},
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockContent });

      const result = await service.getActiveContentByType('BANNER');

      expect(result[0].priority).toBe(100);
      expect(result[1].priority).toBe(50);
      expect(result[2].priority).toBe(1);
    });
  });

  describe('getBanners', () => {
    it('should fetch banner content', async () => {
      const mockBanners: Content[] = [
        {
          id: 'banner_1',
          contentType: 'BANNER',
          title: 'Main Banner',
          description: 'Main promotional banner',
          imageUrl: 'https://example.com/main.jpg',
          actionUrl: '/promos/main',
          actionType: 'DEEP_LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 10,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: {},
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockBanners });

      const result = await service.getBanners();

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/BANNER', {
        params: undefined,
      });
      expect(result).toEqual(mockBanners);
    });

    it('should fetch banners with targeting options', async () => {
      const mockBanners: Content[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockBanners });

      const options = {
        segment: 'STANDARD',
        location: 'BANDUNG',
        device: 'WEB',
      };

      await service.getBanners(options);

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/BANNER', {
        params: options,
      });
    });
  });

  describe('getPromos', () => {
    it('should fetch promotional content', async () => {
      const mockPromos: Content[] = [
        {
          id: 'promo_1',
          contentType: 'PROMO',
          title: 'Flash Sale',
          description: 'Limited time offer',
          imageUrl: 'https://example.com/flash.jpg',
          actionUrl: '/promos/flash',
          actionType: 'DEEP_LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-01-31T23:59:59Z',
          priority: 20,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: {},
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'marketing',
          updatedBy: 'marketing',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockPromos });

      const result = await service.getPromos();

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/PROMO', {
        params: undefined,
      });
      expect(result).toEqual(mockPromos);
    });

    it('should fetch promos with device targeting', async () => {
      const mockPromos: Content[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockPromos });

      const options = {
        device: 'IOS',
      };

      await service.getPromos(options);

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/PROMO', {
        params: options,
      });
    });
  });

  describe('getAlerts', () => {
    it('should fetch alert content', async () => {
      const mockAlerts: Content[] = [
        {
          id: 'alert_1',
          contentType: 'ALERT',
          title: 'System Maintenance',
          description: 'Scheduled maintenance tonight',
          imageUrl: 'https://example.com/alert.jpg',
          actionUrl: '',
          actionType: 'DISMISS',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-01-02T00:00:00Z',
          priority: 100,
          status: 'ACTIVE',
          targetingRules: {},
          metadata: { severity: 'INFO' },
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'ops',
          updatedBy: 'ops',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockAlerts });

      const result = await service.getAlerts();

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/ALERT', {
        params: undefined,
      });
      expect(result).toEqual(mockAlerts);
    });

    it('should fetch alerts with segment targeting', async () => {
      const mockAlerts: Content[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockAlerts });

      const options = {
        segment: 'VIP',
      };

      await service.getAlerts(options);

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/ALERT', {
        params: options,
      });
    });
  });

  describe('getPopups', () => {
    it('should fetch popup content', async () => {
      const mockPopups: Content[] = [
        {
          id: 'popup_1',
          contentType: 'POPUP',
          title: 'Special Offer',
          description: 'Don\'t miss this offer',
          imageUrl: 'https://example.com/popup.jpg',
          actionUrl: '/promos/special',
          actionType: 'DEEP_LINK',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          priority: 15,
          status: 'ACTIVE',
          targetingRules: { showOnce: true },
          metadata: { frequency: 'once_per_session' },
          version: 1,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
          createdBy: 'marketing',
          updatedBy: 'marketing',
          active: true,
        },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockPopups });

      const result = await service.getPopups();

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/POPUP', {
        params: undefined,
      });
      expect(result).toEqual(mockPopups);
    });

    it('should fetch popups with all targeting options', async () => {
      const mockPopups: Content[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockPopups });

      const options = {
        segment: 'NEW_USER',
        location: 'SURABAYA',
        device: 'ANDROID',
      };

      await service.getPopups(options);

      expect(api.get).toHaveBeenCalledWith('/public/contents/type/POPUP', {
        params: options,
      });
    });
  });

  describe('Content types and action types', () => {
    it('should handle all content types', async () => {
      const contentTypes: ContentType[] = ['BANNER', 'PROMO', 'ALERT', 'POPUP'];

      for (const type of contentTypes) {
        const mockContent: Content[] = [];
        vi.mocked(api.get).mockResolvedValue({ data: mockContent });

        await service.getActiveContentByType(type);

        expect(api.get).toHaveBeenCalledWith(`/public/contents/type/${type}`, expect.any(Object));
      }
    });

    it('should handle all action types', async () => {
      const actionTypes: ActionType[] = ['LINK', 'DEEP_LINK', 'DISMISS'];

      for (const actionType of actionTypes) {
        const mockContent: Content[] = [
          {
            id: `content_${actionType}`,
            contentType: 'BANNER',
            title: `Test ${actionType}`,
            description: `Test ${actionType} action`,
            imageUrl: 'https://example.com/test.jpg',
            actionUrl: '/test',
            actionType: actionType,
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-12-31T23:59:59Z',
            priority: 1,
            status: 'ACTIVE',
            targetingRules: {},
            metadata: {},
            version: 1,
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T10:00:00Z',
            createdBy: 'admin',
            updatedBy: 'admin',
            active: true,
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockContent });

        const result = await service.getActiveContentByType('BANNER');

        expect(result[0].actionType).toBe(actionType);
      }
    });
  });

  describe('Data transformation', () => {
    it('should properly transform content data', async () => {
      const apiResponse = {
        data: [
          {
            id: 'content_transform',
            contentType: 'PROMO',
            title: 'Transform Test',
            description: 'Testing data transformation',
            imageUrl: 'https://example.com/transform.jpg',
            actionUrl: 'https://example.com/transform',
            actionType: 'LINK',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-12-31T23:59:59Z',
            priority: 5,
            status: 'ACTIVE',
            targetingRules: { test: true },
            metadata: { key: 'value' },
            version: 2,
            createdAt: '2024-01-01T10:00:00Z',
            updatedAt: '2024-01-01T11:00:00Z',
            createdBy: 'creator',
            updatedBy: 'updater',
            active: true,
          },
        ],
      };

      vi.mocked(api.get).mockResolvedValue(apiResponse);

      const result = await service.getActiveContentByType('PROMO');

      expect(result).toHaveLength(1);
      expect(result[0].targetingRules).toEqual({ test: true });
      expect(result[0].metadata).toEqual({ key: 'value' });
      expect(result[0].version).toBe(2);
    });
  });

  describe('Error handling', () => {
    it('should handle API errors gracefully', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('CMS service unavailable'));

      await expect(service.getBanners()).rejects.toThrow('CMS service unavailable');
    });

    it('should handle network errors', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network error'));

      await expect(service.getPromos()).rejects.toThrow('Network error');
    });
  });

  describe('Public endpoint behavior', () => {
    it('should use correct base URL for public endpoints', async () => {
      const mockContent: Content[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockContent });

      await service.getActiveContentByType('BANNER');

      expect(api.get).toHaveBeenCalledWith(expect.stringContaining('/public/contents'), expect.any(Object));
    });

    it('should not require authentication for public content', async () => {
      const mockContent: Content[] = [];

      vi.mocked(api.get).mockResolvedValue({ data: mockContent });

      await service.getAlerts();

      // Verify the endpoint starts with /public
      const callArgs = vi.mocked(api.get).mock.calls[0];
      expect(callArgs[0]).toMatch(/^\/public\//);
    });
  });
});
