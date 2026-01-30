import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  AccountService,
  type RegisterUserRequest,
  type User,
  type VerifyNikRequest,
  type DukcapilResponse,
} from '@/services/AccountService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('AccountService', () => {
  let service: AccountService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = AccountService.getInstance();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be a singleton', () => {
    const instance1 = AccountService.getInstance();
    const instance2 = AccountService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('registerUser', () => {
    it('should register user successfully and store in localStorage', async () => {
      const mockRequest: RegisterUserRequest = {
        externalId: 'ext_123',
        username: 'johndoe',
        email: 'john@example.com',
        phoneNumber: '+628123456789',
        fullName: 'John Doe',
        nik: '1234567890123456',
      };

      const mockUser: User = {
        id: 'user_123',
        externalId: 'ext_123',
        username: 'johndoe',
        email: 'john@example.com',
        phoneNumber: '+628123456789',
        fullName: 'John Doe',
        nik: '1234567890123456',
        kycStatus: 'PENDING',
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: '2024-01-01T10:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockUser });

      const result = await service.registerUser(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/accounts/register', mockRequest);
      expect(result).toEqual(mockUser);
      expect(localStorage.getItem('user')).toBe(JSON.stringify(mockUser));
      expect(localStorage.getItem('accountId')).toBe('user_123');
    });

    it('should register user without optional phoneNumber', async () => {
      const mockRequest: RegisterUserRequest = {
        externalId: 'ext_456',
        username: 'janedoe',
        email: 'jane@example.com',
        fullName: 'Jane Doe',
        nik: '9876543210987654',
      };

      const mockUser: User = {
        id: 'user_456',
        externalId: 'ext_456',
        username: 'janedoe',
        email: 'jane@example.com',
        fullName: 'Jane Doe',
        nik: '9876543210987654',
        kycStatus: 'PENDING',
        createdAt: '2024-01-01T11:00:00Z',
        updatedAt: '2024-01-01T11:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockUser });

      const result = await service.registerUser(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/accounts/register', mockRequest);
      expect(result).toEqual(mockUser);
      expect(localStorage.getItem('accountId')).toBe('user_456');
    });

    it('should handle API errors during registration', async () => {
      const mockRequest: RegisterUserRequest = {
        externalId: 'ext_789',
        username: 'erroruser',
        email: 'error@example.com',
        fullName: 'Error User',
        nik: '1111111111111111',
      };

      const mockError = new Error('Registration failed: Email already exists');
      vi.mocked(api.post).mockRejectedValue(mockError);

      await expect(service.registerUser(mockRequest)).rejects.toThrow('Registration failed: Email already exists');
      expect(localStorage.getItem('user')).toBeNull();
      expect(localStorage.getItem('accountId')).toBeNull();
    });

    it('should not store user in localStorage if response lacks id', async () => {
      const mockRequest: RegisterUserRequest = {
        externalId: 'ext_invalid',
        username: 'invalid',
        email: 'invalid@example.com',
        fullName: 'Invalid User',
        nik: '2222222222222222',
      };

      const invalidUser = { ...mockRequest } as unknown as User;

      vi.mocked(api.post).mockResolvedValue({ data: invalidUser });

      const result = await service.registerUser(mockRequest);

      expect(result).toEqual(invalidUser);
      expect(localStorage.getItem('user')).toBeNull();
      expect(localStorage.getItem('accountId')).toBeNull();
    });
  });

  describe('verifyNik', () => {
    it('should verify NIK successfully', async () => {
      const mockRequest: VerifyNikRequest = {
        nik: '1234567890123456',
      };

      const mockResponse: DukcapilResponse = {
        nik: '1234567890123456',
        fullName: 'John Doe',
        dateOfBirth: '1990-01-15',
        placeOfBirth: 'Jakarta',
        gender: 'LAKI-LAKI',
        address: 'Jl. Sudirman No. 1, Jakarta',
        isValid: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await service.verifyNik(mockRequest);

      expect(api.post).toHaveBeenCalledWith('/accounts/verify-nik', mockRequest);
      expect(result).toEqual(mockResponse);
      expect(result.isValid).toBe(true);
    });

    it('should handle invalid NIK', async () => {
      const mockRequest: VerifyNikRequest = {
        nik: '0000000000000000',
      };

      const mockResponse: DukcapilResponse = {
        nik: '0000000000000000',
        fullName: '',
        dateOfBirth: '',
        placeOfBirth: '',
        gender: '',
        address: '',
        isValid: false,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

      const result = await service.verifyNik(mockRequest);

      expect(result.isValid).toBe(false);
    });

    it('should handle NIK verification errors', async () => {
      const mockRequest: VerifyNikRequest = {
        nik: '9999999999999999',
      };

      const mockError = new Error('Dukcapil service unavailable');
      vi.mocked(api.post).mockRejectedValue(mockError);

      await expect(service.verifyNik(mockRequest)).rejects.toThrow('Dukcapil service unavailable');
    });
  });

  describe('getUserFromStorage', () => {
    it('should retrieve user from localStorage', () => {
      const mockUser: User = {
        id: 'user_123',
        externalId: 'ext_123',
        username: 'testuser',
        email: 'test@example.com',
        fullName: 'Test User',
        nik: '1234567890123456',
        kycStatus: 'VERIFIED',
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: '2024-01-01T10:00:00Z',
      };

      localStorage.setItem('user', JSON.stringify(mockUser));

      const result = service.getUserFromStorage();

      expect(result).toEqual(mockUser);
    });

    it('should return null when no user in localStorage', () => {
      const result = service.getUserFromStorage();

      expect(result).toBeNull();
    });

    it('should throw error for invalid JSON in localStorage', () => {
      localStorage.setItem('user', 'invalid json');

      expect(() => service.getUserFromStorage()).toThrow();
    });

    it('should return null for empty string in localStorage', () => {
      localStorage.setItem('user', '');

      const result = service.getUserFromStorage();

      expect(result).toBeNull();
    });
  });

  describe('getCurrentUser', () => {
    it('should return current user from storage', () => {
      const mockUser: User = {
        id: 'user_456',
        externalId: 'ext_456',
        username: 'currentuser',
        email: 'current@example.com',
        fullName: 'Current User',
        nik: '9876543210987654',
        kycStatus: 'PENDING',
        createdAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z',
      };

      localStorage.setItem('user', JSON.stringify(mockUser));

      const result = service.getCurrentUser();

      expect(result).toEqual(mockUser);
    });

    it('should return null when user not logged in', () => {
      const result = service.getCurrentUser();

      expect(result).toBeNull();
    });
  });

  describe('KYC Status handling', () => {
    it('should handle all KYC status types', async () => {
      const statuses: Array<'PENDING' | 'VERIFIED' | 'REJECTED'> = ['PENDING', 'VERIFIED', 'REJECTED'];

      for (const status of statuses) {
        const mockRequest: RegisterUserRequest = {
          externalId: `ext_${status}`,
          username: `user_${status.toLowerCase()}`,
          email: `${status.toLowerCase()}@example.com`,
          fullName: `${status} User`,
          nik: '1234567890123456',
        };

        const mockUser: User = {
          id: `user_${status.toLowerCase()}`,
          externalId: `ext_${status}`,
          username: `user_${status.toLowerCase()}`,
          email: `${status.toLowerCase()}@example.com`,
          fullName: `${status} User`,
          nik: '1234567890123456',
          kycStatus: status,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockUser });

        const result = await service.registerUser(mockRequest);

        expect(result.kycStatus).toBe(status);
      }
    });
  });

  describe('Data transformation', () => {
    it('should transform response data correctly', async () => {
      const mockRequest: RegisterUserRequest = {
        externalId: 'ext_transform',
        username: 'transformuser',
        email: 'transform@example.com',
        fullName: 'Transform User',
        nik: '5555555555555555',
      };

      const apiResponse = {
        data: {
          id: 'user_transform',
          externalId: 'ext_transform',
          username: 'transformuser',
          email: 'transform@example.com',
          fullName: 'Transform User',
          nik: '5555555555555555',
          kycStatus: 'PENDING' as const,
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        },
      };

      vi.mocked(api.post).mockResolvedValue(apiResponse);

      const result = await service.registerUser(mockRequest);

      expect(result).toEqual(apiResponse.data);
      expect(typeof result.id).toBe('string');
      expect(typeof result.kycStatus).toBe('string');
    });
  });
});
