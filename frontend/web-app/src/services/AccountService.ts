import api from '@/lib/api';
import { getFinancialMutationHeaders } from '@/lib/utils';

export interface RegisterUserRequest {
  externalId: string;
  username: string;
  email: string;
  phoneNumber?: string;
  fullName: string;
  nik: string;
}

export type KycStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface User {
  id: string;
  externalId: string;
  username: string;
  email: string;
  phoneNumber?: string;
  fullName: string;
  nik: string;
  kycStatus: KycStatus;
  createdAt: string;
  updatedAt: string;
}

// BUG-CROSS-069: Backend VerifyNikRequest has: nik, fullName, birthPlace, birthDate
export interface VerifyNikRequest {
  nik: string;
  fullName: string;
  birthPlace: string;
  birthDate: string;
}

// BUG-CROSS-069: Backend VerifyNikResponse has: requestId, nik, verified, fullName, birthPlace, birthDate, gender, address, status, responseCode, responseMessage
export interface DukcapilResponse {
  requestId: string;
  nik: string;
  verified: boolean;
  fullName: string;
  birthPlace: string;
  birthDate: string;
  gender: string;
  address: string;
  status: string;
  responseCode: string;
  responseMessage: string;
}

/**
 * Account Service for PayU Digital Banking Platform
 *
 * SECURITY NOTICE: User Data Storage
 * ================================
 * This service does NOT store sensitive data in localStorage.
 * User profile data is managed through the auth store (Zustand) with persistence.
 * Tokens are managed exclusively via httpOnly cookies from the backend.
 *
 * Why this approach?
 * - Prevents XSS attacks from stealing data from localStorage
 * - httpOnly cookies are inaccessible to JavaScript
 * - Complies with PCI-DSS and OWASP security standards
 */
export class AccountService {
  private static instance: AccountService;

  private constructor() {}

  static getInstance(): AccountService {
    if (!AccountService.instance) {
      AccountService.instance = new AccountService();
    }
    return AccountService.instance;
  }

  async registerUser(request: RegisterUserRequest): Promise<User> {
    const response = await api.post<User>('/accounts/register', request);

    const user = response.data;
    // SECURITY: User data is stored via auth store, NOT localStorage
    // The calling component/hook should update the auth store with the user data
    return user;
  }

  async verifyNik(request: VerifyNikRequest): Promise<DukcapilResponse> {
    const response = await api.post<DukcapilResponse>('/accounts/verify-nik', request);
    return response.data;
  }

  // === Beneficiary CRUD (FEATURES A3) ===
  async getBeneficiaries(accountId: string): Promise<Beneficiary[]> {
    const response = await api.get<Beneficiary[]>(`/accounts/${accountId}/beneficiaries`);
    return response.data;
  }

  async createBeneficiary(accountId: string, request: BeneficiaryRequest): Promise<Beneficiary> {
    const response = await api.post<Beneficiary>(`/accounts/${accountId}/beneficiaries`, request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async updateBeneficiary(accountId: string, beneficiaryId: string, request: BeneficiaryRequest): Promise<Beneficiary> {
    const response = await api.put<Beneficiary>(`/accounts/${accountId}/beneficiaries/${beneficiaryId}`, request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async deleteBeneficiary(accountId: string, beneficiaryId: string): Promise<void> {
    await api.delete(`/accounts/${accountId}/beneficiaries/${beneficiaryId}`, {
      headers: getFinancialMutationHeaders(),
    });
  }

  // BUG-FE-025: Deprecated methods removed — use useAuthStore hook instead
}

export interface Beneficiary {
  id: string;
  bankCode: string;
  accountNumber: string;
  accountName: string;
  nickname?: string;
  status: string;
  verifiedAt?: string;
  createdAt?: string;
}

export interface BeneficiaryRequest {
  bankCode: string;
  accountNumber: string;
  nickname?: string;
}

export default AccountService.getInstance();
