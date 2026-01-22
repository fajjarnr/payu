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

export interface VerifyNikRequest {
  nik: string;
}

export interface DukcapilResponse {
  nik: string;
  fullName: string;
  dateOfBirth: string;
  placeOfBirth: string;
  gender: string;
  address: string;
  isValid: boolean;
}

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
    if (user.id) {
      localStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('accountId', user.id);
    }
    
    return user;
  }

  async verifyNik(request: VerifyNikRequest): Promise<DukcapilResponse> {
    const response = await api.post<DukcapilResponse>('/accounts/verify-nik', request);
    return response.data;
  }

  getUserFromStorage(): User | null {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  }

  getCurrentUser(): User | null {
    return this.getUserFromStorage();
  }
}

export default AccountService.getInstance();
