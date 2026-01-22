import api from '@/lib/api';

export interface BalanceResponse {
  accountId: string;
  balance: number;
  availableBalance: number;
  reservedBalance: number;
  currency: string;
}

export interface ReserveBalanceRequest {
  amount: number;
  referenceId: string;
}

export interface ReserveBalanceResponse {
  reservationId: string;
  accountId: string;
  referenceId: string;
  status: string;
}

export interface CreditRequest {
  amount: number;
  referenceId: string;
  description?: string;
}

export interface WalletTransaction {
  id: string;
  walletId: string;
  referenceId: string;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export class WalletService {
  private static instance: WalletService;

  private constructor() {}

  static getInstance(): WalletService {
    if (!WalletService.instance) {
      WalletService.instance = new WalletService();
    }
    return WalletService.instance;
  }

  async getBalance(accountId: string): Promise<BalanceResponse> {
    const response = await api.get<BalanceResponse>(`/wallets/${accountId}/balance`);
    return response.data;
  }

  async reserveBalance(accountId: string, request: ReserveBalanceRequest): Promise<ReserveBalanceResponse> {
    const response = await api.post<ReserveBalanceResponse>(`/wallets/${accountId}/reserve`, request);
    return response.data;
  }

  async commitReservation(reservationId: string): Promise<{ status: string; reservationId: string }> {
    const response = await api.post<{ status: string; reservationId: string }>(`/wallets/reservations/${reservationId}/commit`);
    return response.data;
  }

  async releaseReservation(reservationId: string): Promise<{ status: string; reservationId: string }> {
    const response = await api.post<{ status: string; reservationId: string }>(`/wallets/reservations/${reservationId}/release`);
    return response.data;
  }

  async credit(accountId: string, request: CreditRequest): Promise<{ status: string; accountId: string }> {
    const response = await api.post<{ status: string; accountId: string }>(`/wallets/${accountId}/credit`, request);
    return response.data;
  }

  async getTransactionHistory(accountId: string, page: number = 0, size: number = 20): Promise<WalletTransaction[]> {
    const response = await api.get<WalletTransaction[]>(`/wallets/${accountId}/transactions`, {
      params: { page, size }
    });
    return response.data;
  }
}

export default WalletService.getInstance();
