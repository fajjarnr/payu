import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PartnerService, type Partner } from '@/services/PartnerService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('PartnerService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('register', () => {
    it('should register a new partner successfully', async () => {
      const mockRequest = {
        name: 'Test Partner',
        email: 'partner@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
        publicKey: 'ssh-rsa AAAAB3NzaC1yc2E...',
      };

      const mockPartner: Partner = {
        id: 1,
        name: 'Test Partner',
        email: 'partner@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
        clientId: 'client_123',
        clientSecret: 'secret_456',
        publicKey: 'ssh-rsa AAAAB3NzaC1yc2E...',
        active: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.register(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/partners', mockRequest);
      expect(result).toEqual(mockPartner);
      expect(result.active).toBe(true);
      expect(result.clientId).toBeDefined();
      expect(result.clientSecret).toBeDefined();
    });

    it('should register partner without optional publicKey', async () => {
      const mockRequest = {
        name: 'Simple Partner',
        email: 'simple@example.com',
        type: 'PAYMENT_GATEWAY',
        phone: '+628987654321',
      };

      const mockPartner: Partner = {
        id: 2,
        name: 'Simple Partner',
        email: 'simple@example.com',
        type: 'PAYMENT_GATEWAY',
        phone: '+628987654321',
        active: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.register(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/partners', mockRequest);
      expect(result.publicKey).toBeUndefined();
    });

    it('should register partner with different types', async () => {
      const partnerTypes = ['MERCHANT', 'PAYMENT_GATEWAY', 'BILLER', 'WALLET_PROVIDER'];

      for (const type of partnerTypes) {
        const mockRequest = {
          name: `Partner ${type}`,
          email: `${type.toLowerCase()}@example.com`,
          type: type,
          phone: '+628123456789',
        };

        const mockPartner: Partner = {
          id: Math.floor(Math.random() * 1000),
          name: `Partner ${type}`,
          email: `${type.toLowerCase()}@example.com`,
          type: type,
          phone: '+628123456789',
          active: true,
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

        const result = await PartnerService.register(mockRequest);

        expect(result.type).toBe(type);
      }
    });

    it('should handle registration errors', async () => {
      const mockRequest = {
        name: 'Error Partner',
        email: 'error@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
      };

      const mockError = new Error('Email already registered');
      vi.mocked(api.post).mockRejectedValue(mockError);

      await expect(PartnerService.register(mockRequest)).rejects.toThrow('Email already registered');
    });

    it('should handle validation errors', async () => {
      const mockRequest = {
        name: '',
        email: 'invalid-email',
        type: 'MERCHANT',
        phone: '+628123456789',
      };

      const mockError = new Error('Validation failed: Invalid email format');
      vi.mocked(api.post).mockRejectedValue(mockError);

      await expect(PartnerService.register(mockRequest)).rejects.toThrow('Validation failed');
    });
  });

  describe('getProfile', () => {
    it('should fetch partner profile by ID', async () => {
      const mockPartner: Partner = {
        id: 123,
        name: 'Existing Partner',
        email: 'existing@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
        clientId: 'client_abc123',
        clientSecret: 'secret_xyz789',
        publicKey: 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC...',
        active: true,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.getProfile(123);

      expect(api.get).toHaveBeenCalledWith('/partners/123');
      expect(result).toEqual(mockPartner);
      expect(result.clientId).toBe('client_abc123');
      expect(result.clientSecret).toBe('secret_xyz789');
    });

    it('should fetch partner profile without credentials', async () => {
      const mockPartner: Partner = {
        id: 456,
        name: 'Partner Without Credentials',
        email: 'no-creds@example.com',
        type: 'BILLER',
        phone: '+628987654321',
        active: true,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.getProfile(456);

      expect(result.clientId).toBeUndefined();
      expect(result.clientSecret).toBeUndefined();
      expect(result.publicKey).toBeUndefined();
    });

    it('should fetch inactive partner profile', async () => {
      const mockPartner: Partner = {
        id: 789,
        name: 'Inactive Partner',
        email: 'inactive@example.com',
        type: 'PAYMENT_GATEWAY',
        phone: '+628555555555',
        active: false,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.getProfile(789);

      expect(result.active).toBe(false);
    });

    it('should handle partner not found error', async () => {
      const mockError = new Error('Partner not found');
      vi.mocked(api.get).mockRejectedValue(mockError);

      await expect(PartnerService.getProfile(999)).rejects.toThrow('Partner not found');
    });

    it('should handle network errors', async () => {
      const mockError = new Error('Network error');
      vi.mocked(api.get).mockRejectedValue(mockError);

      await expect(PartnerService.getProfile(1)).rejects.toThrow('Network error');
    });
  });

  describe('Data transformation', () => {
    it('should correctly transform API response to Partner type', async () => {
      const apiResponse = {
        data: {
          id: 100,
          name: 'Transform Partner',
          email: 'transform@example.com',
          type: 'MERCHANT',
          phone: '+628123456789',
          clientId: 'client_transform',
          clientSecret: 'secret_transform',
          publicKey: 'ssh-rsa test-key',
          active: true,
        },
      };

      vi.mocked(api.get).mockResolvedValue(apiResponse);

      const result = await PartnerService.getProfile(100);

      expect(typeof result.id).toBe('number');
      expect(typeof result.active).toBe('boolean');
      expect(result.name).toBe('Transform Partner');
    });

    it('should handle partner with all fields', async () => {
      const mockRequest = {
        name: 'Full Partner',
        email: 'full@example.com',
        type: 'WALLET_PROVIDER',
        phone: '+628777777777',
        publicKey: 'ssh-rsa full-public-key',
      };

      const mockPartner: Partner = {
        id: 200,
        name: 'Full Partner',
        email: 'full@example.com',
        type: 'WALLET_PROVIDER',
        phone: '+628777777777',
        clientId: 'client_full',
        clientSecret: 'secret_full',
        publicKey: 'ssh-rsa full-public-key',
        active: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.register(mockRequest);

      expect(result.id).toBeDefined();
      expect(result.clientId).toBeDefined();
      expect(result.clientSecret).toBeDefined();
      expect(result.publicKey).toBe(mockRequest.publicKey);
    });
  });

  describe('Partner types', () => {
    it('should handle all partner types correctly', async () => {
      const partnerConfigs = [
        {
          type: 'MERCHANT',
          email: 'merchant@example.com',
          expectedType: 'MERCHANT',
        },
        {
          type: 'PAYMENT_GATEWAY',
          email: 'pg@example.com',
          expectedType: 'PAYMENT_GATEWAY',
        },
        {
          type: 'BILLER',
          email: 'biller@example.com',
          expectedType: 'BILLER',
        },
        {
          type: 'WALLET_PROVIDER',
          email: 'wallet@example.com',
          expectedType: 'WALLET_PROVIDER',
        },
      ];

      for (const config of partnerConfigs) {
        const mockRequest = {
          name: `${config.type} Partner`,
          email: config.email,
          type: config.type,
          phone: '+628123456789',
        };

        const mockPartner: Partner = {
          id: Math.floor(Math.random() * 1000),
          name: `${config.type} Partner`,
          email: config.email,
          type: config.expectedType as any,
          phone: '+628123456789',
          active: true,
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

        const result = await PartnerService.register(mockRequest);

        expect(result.type).toBe(config.expectedType);
      }
    });
  });

  describe('Edge cases', () => {
    it('should handle partner ID as string', async () => {
      const mockPartner: Partner = {
        id: 0,
        name: 'Zero ID Partner',
        email: 'zero@example.com',
        type: 'MERCHANT',
        phone: '+628000000000',
        active: true,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.getProfile(0);

      expect(result.id).toBe(0);
    });

    it('should handle partner with very long name', async () => {
      const longName = 'A'.repeat(200);
      const mockRequest = {
        name: longName,
        email: 'longname@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
      };

      const mockPartner: Partner = {
        id: 1,
        name: longName,
        email: 'longname@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
        active: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.register(mockRequest);

      expect(result.name).toHaveLength(200);
    });

    it('should handle partner with special characters in phone number', async () => {
      const mockRequest = {
        name: 'Special Phone Partner',
        email: 'special@example.com',
        type: 'BILLER',
        phone: '+62-812-3456-7890',
      };

      const mockPartner: Partner = {
        id: 1,
        name: 'Special Phone Partner',
        email: 'special@example.com',
        type: 'BILLER',
        phone: '+62-812-3456-7890',
        active: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

      const result = await PartnerService.register(mockRequest);

      expect(result.phone).toBe('+62-812-3456-7890');
    });
  });

  describe('API integration', () => {
    it('should call correct endpoint for registration', async () => {
      const mockRequest = {
        name: 'Endpoint Test',
        email: 'endpoint@example.com',
        type: 'MERCHANT',
        phone: '+628123456789',
      };

      const mockPartner: Partner = {
        id: 1,
        ...mockRequest,
        active: true,
      } as any;

      vi.mocked(api.post).mockResolvedValue({ data: mockPartner });

      await PartnerService.register(mockRequest);

      expect(api.post).toHaveBeenCalledTimes(1);
      expect(api.post).toHaveBeenCalledWith('/partners', mockRequest);
    });

    it('should call correct endpoint for profile retrieval', async () => {
      const mockPartner: Partner = {
        id: 999,
        name: 'Endpoint Profile',
        email: 'profile@example.com',
        type: 'PAYMENT_GATEWAY',
        phone: '+628123456789',
        active: true,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPartner });

      await PartnerService.getProfile(999);

      expect(api.get).toHaveBeenCalledTimes(1);
      expect(api.get).toHaveBeenCalledWith('/partners/999');
    });
  });
});
