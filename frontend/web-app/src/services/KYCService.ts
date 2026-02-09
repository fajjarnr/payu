import api from '@/lib/api';

export interface KycSubmission {
  id: string;
  userId: string;
  status: KycStatus;
  level: KycLevel;
  submittedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

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

  async getKycStatus(): Promise<KycSubmission> {
    const response = await api.get('/kyc/status');
    return response.data;
  }

  async submitIdentityVerification(data: IdentityVerification): Promise<KycSubmission> {
    const formData = new FormData();
    formData.append('nik', data.nik);
    formData.append('fullName', data.fullName);
    formData.append('dateOfBirth', data.dateOfBirth);
    formData.append('address', data.address);
    formData.append('selfie', data.selfieFile);
    formData.append('idCard', data.idCardFile);
    const response = await api.post('/kyc/verify', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  }

  async uploadDocument(type: DocumentType, file: File): Promise<KycDocument> {
    const formData = new FormData();
    formData.append('type', type);
    formData.append('file', file);
    const response = await api.post('/kyc/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  }

  async getDocuments(): Promise<KycDocument[]> {
    const response = await api.get('/kyc/documents');
    return response.data;
  }
}

export default KYCService.getInstance();
