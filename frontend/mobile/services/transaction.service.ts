import { apiClient } from './api';
import {
  Transaction,
  TransferData,
  ApiResponse,
  PaginatedResponse,
  QRISData,
} from '@/types';

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

  async transfer(data: TransferData): Promise<Transaction> {
    const response = await apiClient.post<ApiResponse<Transaction>>(
      '/transactions/transfer',
      data
    );
    return response.data.data;
  },

  async topUp(amount: number, paymentMethod: string): Promise<Transaction> {
    const response = await apiClient.post<ApiResponse<Transaction>>('/transactions/topup', {
      amount,
      paymentMethod,
    });
    return response.data.data;
  },

  async payQRIS(data: QRISData): Promise<Transaction> {
    const response = await apiClient.post<ApiResponse<Transaction>>(
      '/transactions/qris',
      data
    );
    return response.data.data;
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
