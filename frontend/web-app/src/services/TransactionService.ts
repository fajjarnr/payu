import api from '@/lib/api';
import type { TransactionType, TransactionStatus, TransferType } from '@/types';

// Re-export types for convenience
export type { TransactionType, TransactionStatus };
export type { TransferType };

export interface InitiateTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: number;
  currency?: string;
  description: string;
  type?: TransactionType;
  transactionPin?: string;
  deviceId?: string;
}

export interface InitiateTransferResponse {
  transactionId: string;
  referenceNumber: string;
  status: string;
  fee: number;
  estimatedCompletionTime: string;
}

export interface Transaction {
  id: string;
  referenceNumber: string;
  senderAccountId: string;
  recipientAccountId: string;
  type: TransactionType;
  amount: number;
  currency: string;
  description: string;
  status: TransactionStatus;
  failureReason?: string;
  metadata?: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export interface ProcessQrisPaymentRequest {
  qrCode: string;
  amount: number;
  accountId: string;
}

export class TransactionService {
  private static instance: TransactionService;

  private constructor() {}

  static getInstance(): TransactionService {
    if (!TransactionService.instance) {
      TransactionService.instance = new TransactionService();
    }
    return TransactionService.instance;
  }

  async initiateTransfer(request: InitiateTransferRequest): Promise<InitiateTransferResponse> {
    const response = await api.post<InitiateTransferResponse>('/transactions/transfer', request);
    return response.data;
  }

  async getTransaction(transactionId: string): Promise<Transaction> {
    const response = await api.get<Transaction>(`/transactions/${transactionId}`);
    return response.data;
  }

  async getAccountTransactions(accountId: string, page: number = 0, size: number = 20): Promise<Transaction[]> {
    const response = await api.get<Transaction[]>(`/transactions/accounts/${accountId}`, {
      params: { page, size }
    });
    return response.data;
  }

  async processQrisPayment(request: ProcessQrisPaymentRequest): Promise<void> {
    await api.post('/transactions/qris/pay', request);
  }
}

export default TransactionService.getInstance();
