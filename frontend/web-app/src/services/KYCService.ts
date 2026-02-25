import api from '@/lib/api';

// --- Interfaces matching backend kyc_router (FastAPI) ---

export interface StartKycRequest {
  userId: string;
  fullName: string;
  nik: string;
  dateOfBirth: string;
  address: string;
  phone?: string;
}

export interface UploadKtpRequest {
  verificationId: string;
  ktpImage: string; // base64 encoded
  nik: string;
}

export interface UploadSelfieRequest {
  verificationId: string;
  selfieImage: string; // base64 encoded
}

export interface KycSubmission {
  id: string;
  userId: string;
  status: KycStatus;
  level: KycLevel;
  submittedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
  verificationId?: string;
}

export interface KycVerificationResult {
  verificationId: string;
  userId: string;
  status: KycStatus;
  ktpVerified: boolean;
  selfieVerified: boolean;
  livenessScore?: number;
  faceMatchScore?: number;
  ocrData?: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface KycHistory {
  userId: string;
  verifications: KycVerificationResult[];
}

// Legacy types preserved for backward compat
export interface KycDocument {
  id: string;
  type: DocumentType;
  fileName: string;
  status: 'PENDING' | 'VERIFIED' | 'REJECTED';
  uploadedAt: string;
}
export interface IdentityVerification {
  nik: string;
  fullName: string;
  dateOfBirth: string;
  address: string;
  selfieFile: File;
  idCardFile: File;
}

export type KycStatus = 'NOT_STARTED' | 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
export type KycLevel = 'BASIC' | 'STANDARD' | 'PREMIUM';
export type DocumentType = 'KTP' | 'SELFIE' | 'NPWP' | 'PROOF_OF_ADDRESS';

class KYCService {
  private static instance: KYCService;

  static getInstance(): KYCService {
    if (!KYCService.instance) {
      KYCService.instance = new KYCService();
    }
    return KYCService.instance;
  }

  /** POST /kyc/verify/start — Start KYC verification process */
  async startVerification(request: StartKycRequest): Promise<KycVerificationResult> {
    const response = await api.post('/kyc/verify/start', request);
    return response.data;
  }

  // BUG-FE-030: Max base64 image size (5MB encoded ≈ 6.67MB base64)
  private static readonly MAX_BASE64_SIZE = 7 * 1024 * 1024; // 7MB

  private validateImageSize(base64Image: string, fieldName: string): void {
    if (base64Image.length > KYCService.MAX_BASE64_SIZE) {
      const sizeMB = (base64Image.length / (1024 * 1024)).toFixed(1);
      throw new Error(
        `${fieldName} is too large (${sizeMB}MB). Maximum allowed is 5MB. Please resize the image before uploading.`
      );
    }
  }

  /** POST /kyc/verify/ktp — Upload and verify KTP document */
  async uploadKtp(request: UploadKtpRequest): Promise<KycVerificationResult> {
    this.validateImageSize(request.ktpImage, 'KTP image');
    const response = await api.post('/kyc/verify/ktp', request);
    return response.data;
  }

  /** POST /kyc/verify/selfie — Upload and verify selfie */
  async uploadSelfie(request: UploadSelfieRequest): Promise<KycVerificationResult> {
    this.validateImageSize(request.selfieImage, 'Selfie image');
    const response = await api.post('/kyc/verify/selfie', request);
    return response.data;
  }

  /** GET /kyc/verify/{verificationId} — Get verification status */
  async getVerificationStatus(verificationId: string): Promise<KycVerificationResult> {
    const response = await api.get(`/kyc/verify/${verificationId}`);
    return response.data;
  }

  /** GET /kyc/user/{userId} — Get user KYC history */
  async getUserKycHistory(userId: string): Promise<KycHistory> {
    const response = await api.get(`/kyc/user/${userId}`);
    return response.data;
  }
}

export default KYCService.getInstance();
