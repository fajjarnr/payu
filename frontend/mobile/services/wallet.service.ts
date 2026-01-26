import { apiClient } from './api';
import { Wallet, ApiResponse } from '@/types';

export const walletService = {
  async getWallets(): Promise<Wallet[]> {
    const response = await apiClient.get<ApiResponse<Wallet[]>>('/wallets');
    return response.data.data;
  },

  async getWallet(walletId: string): Promise<Wallet> {
    const response = await apiClient.get<ApiResponse<Wallet>>(`/wallets/${walletId}`);
    return response.data.data;
  },

  async getPrimaryWallet(): Promise<Wallet> {
    const response = await apiClient.get<ApiResponse<Wallet>>('/wallets/primary');
    return response.data.data;
  },

  async createPocket(data: {
    name: string;
    type: 'savings' | 'goals';
    initialBalance?: number;
  }): Promise<Wallet> {
    const response = await apiClient.post<ApiResponse<Wallet>>('/wallets/pockets', data);
    return response.data.data;
  },

  async transferToPocket(
    fromPocketId: string,
    toPocketId: string,
    amount: number,
    description?: string
  ): Promise<void> {
    await apiClient.post('/wallets/internal-transfer', {
      fromPocketId,
      toPocketId,
      amount,
      description,
    });
  },
};
