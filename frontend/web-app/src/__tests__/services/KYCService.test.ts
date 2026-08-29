import { describe, it, expect, vi, beforeEach } from 'vitest';
import KYCService, {
  type StartKycRequest,
  type UploadKtpRequest,
  type UploadSelfieRequest,
  type KycVerificationResult,
  type KycHistory,
} from '@/services/KYCService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockVerification: KycVerificationResult = {
  verificationId: 'ver_001',
  userId: 'user_123',
  status: 'PENDING',
  ktpVerified: false,
  selfieVerified: false,
  livenessScore: undefined,
  faceMatchScore: undefined,
  ocrData: undefined,
  createdAt: '2026-02-18T10:00:00Z',
  updatedAt: '2026-02-18T10:00:00Z',
};

describe('KYCService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('startVerification', () => {
    it('should start KYC verification process', async () => {
      const request: StartKycRequest = {
        userId: 'user_123',
        fullName: 'Budi Santoso',
        nik: '3201010101010001',
        dateOfBirth: '1990-01-15',
        address: 'Jl. Sudirman No. 1, Jakarta',
        phone: '081234567890',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockVerification });

      const result = await KYCService.startVerification(request);

      expect(api.post).toHaveBeenCalledWith('/kyc/verify/start', request, expect.objectContaining({ headers: expect.objectContaining({ 'X-Idempotency-Key': expect.any(String) }) }));
      expect(result.verificationId).toBe('ver_001');
      expect(result.status).toBe('PENDING');
    });

    it('should work without optional phone', async () => {
      const request: StartKycRequest = {
        userId: 'user_123',
        fullName: 'Budi Santoso',
        nik: '3201010101010001',
        dateOfBirth: '1990-01-15',
        address: 'Jl. Sudirman No. 1',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockVerification });

      const result = await KYCService.startVerification(request);

      expect(api.post).toHaveBeenCalledWith('/kyc/verify/start', request, expect.objectContaining({ headers: expect.objectContaining({ 'X-Idempotency-Key': expect.any(String) }) }));
      expect(result.userId).toBe('user_123');
    });
  });

  describe('uploadKtp', () => {
    it('should upload and verify KTP document', async () => {
      const request: UploadKtpRequest = {
        verificationId: 'ver_001',
        ktpImage: 'base64encodedimage==',
        nik: '3201010101010001',
      };

      const verifiedResult = { ...mockVerification, ktpVerified: true, ocrData: { nik: '3201********0001' } };
      vi.mocked(api.post).mockResolvedValue({ data: verifiedResult });

      const result = await KYCService.uploadKtp(request);

      expect(api.post).toHaveBeenCalledWith('/kyc/verify/ktp', request, expect.objectContaining({ headers: expect.objectContaining({ 'X-Idempotency-Key': expect.any(String) }) }));
      expect(result.ktpVerified).toBe(true);
      expect(result.ocrData?.nik).toBe('3201********0001');
    });
  });

  describe('uploadSelfie', () => {
    it('should upload and verify selfie', async () => {
      const request: UploadSelfieRequest = {
        verificationId: 'ver_001',
        selfieImage: 'base64selfie==',
      };

      const verifiedResult = {
        ...mockVerification,
        selfieVerified: true,
        livenessScore: 0.95,
        faceMatchScore: 0.92,
      };
      vi.mocked(api.post).mockResolvedValue({ data: verifiedResult });

      const result = await KYCService.uploadSelfie(request);

      expect(api.post).toHaveBeenCalledWith('/kyc/verify/selfie', request, expect.objectContaining({ headers: expect.objectContaining({ 'X-Idempotency-Key': expect.any(String) }) }));
      expect(result.selfieVerified).toBe(true);
      expect(result.livenessScore).toBe(0.95);
      expect(result.faceMatchScore).toBe(0.92);
    });
  });

  describe('getVerificationStatus', () => {
    it('should fetch verification status by ID', async () => {
      const approvedResult = { ...mockVerification, status: 'APPROVED' as const, ktpVerified: true, selfieVerified: true };
      vi.mocked(api.get).mockResolvedValue({ data: approvedResult });

      const result = await KYCService.getVerificationStatus('ver_001');

      expect(api.get).toHaveBeenCalledWith('/kyc/verify/ver_001');
      expect(result.status).toBe('APPROVED');
      expect(result.ktpVerified).toBe(true);
      expect(result.selfieVerified).toBe(true);
    });
  });

  describe('getUserKycHistory', () => {
    it('should fetch user KYC history', async () => {
      const mockHistory: KycHistory = {
        userId: 'user_123',
        verifications: [
          mockVerification,
          { ...mockVerification, verificationId: 'ver_002', status: 'APPROVED' },
        ],
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockHistory });

      const result = await KYCService.getUserKycHistory('user_123');

      expect(api.get).toHaveBeenCalledWith('/kyc/user/user_123');
      expect(result.userId).toBe('user_123');
      expect(result.verifications).toHaveLength(2);
    });
  });
});
