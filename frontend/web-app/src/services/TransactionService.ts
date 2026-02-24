import api from '@/lib/api';
import { getFinancialMutationHeaders } from '@/lib/utils';
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
    const response = await api.post<InitiateTransferResponse>('/transactions/transfer', request, {
      headers: getFinancialMutationHeaders(),
    });
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
    await api.post('/transactions/qris/pay', request, {
      headers: getFinancialMutationHeaders(),
    });
  }

  /** POST /transactions/{transactionId}/cancel — Cancel pending transaction */
  async cancelTransaction(transactionId: string): Promise<{ status: string; transactionId: string }> {
    const response = await api.post<{ status: string; transactionId: string }>(`/transactions/${transactionId}/cancel`);
    return response.data;
  }

  // === Scheduled Transfers (FE-GAP-007) ===

  async createScheduledTransfer(request: CreateScheduledTransferRequest): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>('/scheduled-transfers', request);
    return response.data;
  }

  async getScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.get<ScheduledTransfer>(`/scheduled-transfers/${id}`);
    return response.data;
  }

  async getAccountScheduledTransfers(accountId: string): Promise<ScheduledTransfer[]> {
    const response = await api.get<ScheduledTransfer[]>(`/scheduled-transfers/accounts/${accountId}`);
    return response.data;
  }

  async updateScheduledTransfer(id: string, request: Partial<CreateScheduledTransferRequest>): Promise<ScheduledTransfer> {
    const response = await api.put<ScheduledTransfer>(`/scheduled-transfers/${id}`, request);
    return response.data;
  }

  async cancelScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>(`/scheduled-transfers/${id}/cancel`);
    return response.data;
  }

  async pauseScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>(`/scheduled-transfers/${id}/pause`);
    return response.data;
  }

  async resumeScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>(`/scheduled-transfers/${id}/resume`);
    return response.data;
  }

  // === Split Bills (FE-GAP-008) ===

  async createSplitBill(request: CreateSplitBillRequest): Promise<SplitBill> {
    const response = await api.post<SplitBill>('/split-bills', request);
    return response.data;
  }

  async getSplitBill(id: string): Promise<SplitBill> {
    const response = await api.get<SplitBill>(`/split-bills/${id}`);
    return response.data;
  }

  async getAccountSplitBills(accountId: string): Promise<SplitBill[]> {
    const response = await api.get<SplitBill[]>(`/split-bills/account/${accountId}`);
    return response.data;
  }

  async updateSplitBill(id: string, request: Partial<CreateSplitBillRequest>): Promise<SplitBill> {
    const response = await api.put<SplitBill>(`/split-bills/${id}`, request);
    return response.data;
  }

  async cancelSplitBill(id: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${id}/cancel`);
    return response.data;
  }

  async activateSplitBill(id: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${id}/activate`);
    return response.data;
  }

  async addParticipant(splitBillId: string, participant: SplitBillParticipant): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants`, participant);
    return response.data;
  }

  async acceptParticipation(splitBillId: string, participantId: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants/${participantId}/accept`);
    return response.data;
  }

  async declineParticipation(splitBillId: string, participantId: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants/${participantId}/decline`);
    return response.data;
  }

  async makeParticipantPayment(splitBillId: string, participantId: string, amount: number): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants/${participantId}/payment`, { amount });
    return response.data;
  }

  async settleSplitBill(id: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${id}/settle`);
    return response.data;
  }
}

// === Scheduled Transfer Types ===

export interface ScheduledTransfer {
  id: string;
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: number;
  currency: string;
  description: string;
  frequency: 'ONCE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
  startDate: string;
  endDate?: string;
  nextExecutionDate: string;
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'COMPLETED';
  createdAt: string;
  updatedAt: string;
}

export interface CreateScheduledTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: number;
  currency?: string;
  description: string;
  frequency: 'ONCE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
  startDate: string;
  endDate?: string;
}

// === Split Bill Types ===

export interface SplitBill {
  id: string;
  creatorAccountId: string;
  title: string;
  totalAmount: number;
  currency: string;
  splitType: 'EQUAL' | 'CUSTOM' | 'PERCENTAGE';
  status: 'DRAFT' | 'ACTIVE' | 'SETTLED' | 'CANCELLED';
  participants: SplitBillParticipant[];
  createdAt: string;
  updatedAt: string;
}

export interface SplitBillParticipant {
  id?: string;
  accountId: string;
  name: string;
  amount: number;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'PAID';
  paidAt?: string;
}

export interface CreateSplitBillRequest {
  creatorAccountId: string;
  title: string;
  totalAmount: number;
  currency?: string;
  splitType: 'EQUAL' | 'CUSTOM' | 'PERCENTAGE';
  participants: Omit<SplitBillParticipant, 'id' | 'status' | 'paidAt'>[];
}

export default TransactionService.getInstance();
