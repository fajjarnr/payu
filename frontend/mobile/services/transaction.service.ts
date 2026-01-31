import { apiClient, apiClientInstance } from './api';
import {
  Transaction,
  TransferData,
  ApiResponse,
  PaginatedResponse,
  QRISData,
  TopUpData,
  QRISPaymentData,
} from '@/types';
import {
  generateIdempotencyKey,
  saveIdempotencyKey,
  removeIdempotencyKey,
} from '@/utils/idempotency';

export const transactionService = {
  async getTransactions(params?: {
    page?: number;
    pageSize?: number;
    type?: string;
    status?: string;
  }): Promise<PaginatedResponse<Transaction>> {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Transaction>>>(
      '/transactions',
      { params }
    );
    return response.data.data;
  },

  async getTransaction(id: string): Promise<Transaction> {
    const response = await apiClient.get<ApiResponse<Transaction>>(`/transactions/${id}`);
    return response.data.data;
  },

  /**
   * Transfer funds with idempotency support
   * Automatically generates idempotency key if not provided
   */
  async transfer(data: TransferData & { userId?: string }): Promise<Transaction> {
    const idempotencyKey =
      data.idempotencyKey || generateIdempotencyKey('transfer', data.userId);

    // Save idempotency key for recovery
    await saveIdempotencyKey(idempotencyKey, 'transfer', data.userId);

    try {
      const response = await apiClientInstance.postWithIdempotency<ApiResponse<Transaction>>(
        '/transactions/transfer',
        data,
        idempotencyKey
      );

      // Remove from storage after successful transfer
      await removeIdempotencyKey(idempotencyKey);

      return response.data;
    } catch (error) {
      // Keep idempotency key in storage for retry
      throw error;
    }
  },

  /**
   * Top up wallet with idempotency support
   * Automatically generates idempotency key if not provided
   */
  async topUp(data: TopUpData & { userId?: string }): Promise<Transaction> {
    const idempotencyKey =
      data.idempotencyKey || generateIdempotencyKey('topup', data.userId);

    // Save idempotency key for recovery
    await saveIdempotencyKey(idempotencyKey, 'topup', data.userId);

    try {
      const response = await apiClientInstance.postWithIdempotency<ApiResponse<Transaction>>(
        '/transactions/topup',
        data,
        idempotencyKey
      );

      // Remove from storage after successful topup
      await removeIdempotencyKey(idempotencyKey);

      return response.data;
    } catch (error) {
      // Keep idempotency key in storage for retry
      throw error;
    }
  },

  /**
   * Pay QRIS merchant with idempotency support
   * Automatically generates idempotency key if not provided
   */
  async payQRIS(data: QRISPaymentData & { userId?: string }): Promise<Transaction> {
    const idempotencyKey =
      data.idempotencyKey || generateIdempotencyKey('qris', data.userId);

    // Save idempotency key for recovery
    await saveIdempotencyKey(idempotencyKey, 'qris', data.userId);

    try {
      const response = await apiClientInstance.postWithIdempotency<ApiResponse<Transaction>>(
        '/transactions/qris',
        data,
        idempotencyKey
      );

      // Remove from storage after successful payment
      await removeIdempotencyKey(idempotencyKey);

      return response.data;
    } catch (error) {
      // Keep idempotency key in storage for retry
      throw error;
    }
  },

  async getTransactionSummary(params?: {
    period?: 'week' | 'month' | 'year';
  }): Promise<{
    totalIncome: number;
    totalExpense: number;
    transactionCount: number;
  }> {
    const response = await apiClient.get<ApiResponse<any>>('/transactions/summary', {
      params,
    });
    return response.data.data;
  },
};
