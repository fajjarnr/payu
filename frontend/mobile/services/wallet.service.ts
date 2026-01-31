import { apiClient, apiClientInstance } from './api';
import { Wallet, ApiResponse } from '@/types';
import {
  generateIdempotencyKey,
  saveIdempotencyKey,
  removeIdempotencyKey,
} from '@/utils/idempotency';

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

  /**
   * Transfer between pockets with idempotency support
   * Automatically generates idempotency key
   */
  async transferToPocket(
    fromPocketId: string,
    toPocketId: string,
    amount: number,
    description?: string,
    userId?: string
  ): Promise<void> {
    const idempotencyKey = generateIdempotencyKey('pocket-transfer', userId);

    // Save idempotency key for recovery
    await saveIdempotencyKey(idempotencyKey, 'pocket-transfer', userId);

    try {
      await apiClientInstance.postWithIdempotency<ApiResponse<void>>(
        '/wallets/internal-transfer',
        {
          fromPocketId,
          toPocketId,
          amount,
          description,
        },
        idempotencyKey
      );

      // Remove from storage after successful transfer
      await removeIdempotencyKey(idempotencyKey);
    } catch (error) {
      // Keep idempotency key in storage for retry
      throw error;
    }
  },
};
