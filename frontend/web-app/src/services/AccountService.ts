import api from '@/lib/api';

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

  // BUG-FE-025: Deprecated methods removed — use useAuthStore hook instead
}

export default AccountService.getInstance();
