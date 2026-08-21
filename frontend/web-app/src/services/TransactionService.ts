import api from '@/lib/api';
import { getFinancialMutationHeaders, idempotencyKeyFor } from '@/lib/utils';
import { assertUUID } from '@/lib/validation';
import type { TransactionType, TransactionStatus, TransferType, Transaction, TransactionFilters, Money } from '@/types';

// Re-export types for convenience
export type { TransactionType, TransactionStatus };
export type { TransferType };
// IMP-014: Re-export Transaction from centralized types/index.ts
export type { Transaction };

// BUG-CROSS-071: Backend InitiateTransferRequest has optional idempotencyKey and memo
export interface InitiateTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: Money;
  currency?: string;
  description: string;
  type?: TransactionType;
  transactionPin?: string;
  deviceId?: string;
  idempotencyKey?: string;
  memo?: string;
}

export interface InitiateTransferResponse {
  transactionId: string;
  referenceNumber: string;
  status: string;
  fee: Money;
  estimatedCompletionTime: string;
}

export interface ProcessQrisPaymentRequest {
  qrCode: string;
  amount: Money;
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

  async getAccountTransactions(accountId: string, page: number = 0, size: number = 20, filters?: TransactionFilters): Promise<Transaction[]> {
    // BUG-CROSS-002: Validate UUID format before sending to backend
    assertUUID(accountId, 'accountId');
    const response = await api.get<Transaction[]>(`/transactions/accounts/${accountId}`, {
      params: { 
        page, 
        size,
        ...filters
      }
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
    // BUG-FE-021: Add idempotency key for financial mutation
    const response = await api.post<ScheduledTransfer>('/scheduled-transfers', request, {
      headers: getFinancialMutationHeaders(),
    });
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
    const response = await api.put<ScheduledTransfer>(`/scheduled-transfers/${id}`, request, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('scheduled-transfer:update', id) }
    });
    return response.data;
  }

  async cancelScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>(`/scheduled-transfers/${id}/cancel`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('scheduled-transfer:cancel', id) }
    });
    return response.data;
  }

  async pauseScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>(`/scheduled-transfers/${id}/pause`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('scheduled-transfer:pause', id) }
    });
    return response.data;
  }

  async resumeScheduledTransfer(id: string): Promise<ScheduledTransfer> {
    const response = await api.post<ScheduledTransfer>(`/scheduled-transfers/${id}/resume`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('scheduled-transfer:resume', id) }
    });
    return response.data;
  }

  // === Split Bills (FE-GAP-008) ===

  async createSplitBill(request: CreateSplitBillRequest): Promise<SplitBill> {
    const response = await api.post<SplitBill>('/split-bills', request, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:create', request.creatorAccountId ?? '') }
    });
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
    const response = await api.put<SplitBill>(`/split-bills/${id}`, request, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:update', id) }
    });
    return response.data;
  }

  async cancelSplitBill(id: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${id}/cancel`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:cancel', id) }
    });
    return response.data;
  }

  async activateSplitBill(id: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${id}/activate`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:activate', id) }
    });
    return response.data;
  }

  async addParticipant(splitBillId: string, participant: SplitBillParticipant): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants`, participant, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:add-participant', splitBillId + ':' + participant.accountId) }
    });
    return response.data;
  }

  async acceptParticipation(splitBillId: string, participantId: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants/${participantId}/accept`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:accept', splitBillId + ':' + participantId) }
    });
    return response.data;
  }

  async declineParticipation(splitBillId: string, participantId: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants/${participantId}/decline`, {}, {
      headers: { 'X-Idempotency-Key': idempotencyKeyFor('split-bill:decline', splitBillId + ':' + participantId) }
    });
    return response.data;
  }

  async makeParticipantPayment(splitBillId: string, participantId: string, amount: Money): Promise<SplitBill> {
    // BUG-FE-021: Add idempotency key for financial mutation
    const response = await api.post<SplitBill>(`/split-bills/${splitBillId}/participants/${participantId}/payment`, { amount }, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async settleSplitBill(id: string): Promise<SplitBill> {
    const response = await api.post<SplitBill>(`/split-bills/${id}/settle`, null, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }
}

// === Scheduled Transfer Types ===

// BUG-CROSS-030: Align field names with backend ScheduledTransferResponse
export interface ScheduledTransfer {
  id: string;
  referenceNumber: string;
  senderAccountId: string;
  recipientAccountNumber: string;
  recipientAccountId?: string;
  transferType: 'INTERNAL_TRANSFER' | 'BANK_TRANSFER' | 'BI_FAST' | 'RTGS' | 'SKN';
  amount: Money;
  currency: string;
  description: string;
  scheduleType: 'ONE_TIME' | 'RECURRING_DAILY' | 'RECURRING_WEEKLY' | 'RECURRING_MONTHLY' | 'RECURRING_CUSTOM';
  startDate: string;
  endDate?: string;
  nextExecutionDate: string;
  frequencyDays?: number;
  dayOfMonth?: number;
  occurrenceCount?: number;
  executedCount?: number;
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'COMPLETED' | 'FAILED';
  failureReason?: string;
  lastTransactionId?: string;
  createdAt: string;
  updatedAt: string;
}

// BUG-CROSS-030: Align field names with backend CreateScheduledTransferRequest
export interface CreateScheduledTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: Money;
  currency?: string;
  description: string;
  transferType: 'INTERNAL_TRANSFER' | 'BANK_TRANSFER' | 'BI_FAST' | 'RTGS' | 'SKN';
  scheduleType: 'ONE_TIME' | 'RECURRING_DAILY' | 'RECURRING_WEEKLY' | 'RECURRING_MONTHLY' | 'RECURRING_CUSTOM';
  startDate: string;
  endDate?: string;
  frequencyDays?: number;
  dayOfMonth?: number;
  occurrenceCount?: number;
}

// === Split Bill Types ===

// BUG-CROSS-020: Align status with backend SplitStatus (removed phantom 'SETTLED')
export interface SplitBill {
  id: string;
  creatorAccountId: string;
  title: string;
  totalAmount: Money;
  currency: string;
  splitType: 'EQUAL' | 'CUSTOM' | 'PERCENTAGE';
  status: 'DRAFT' | 'ACTIVE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  participants: SplitBillParticipant[];
  totalPaid?: Money;
  remainingAmount?: Money;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

// BUG-CROSS-021: Align field names with backend: name→accountName, amount→amountOwed
export interface SplitBillParticipant {
  id?: string;
  accountId: string;
  accountNumber?: string;
  accountName: string;
  amountOwed: Money;
  amountPaid?: Money;
  remainingAmount?: Money;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'PARTIALLY_PAID' | 'SETTLED';
  settledAt?: string;
}

export interface CreateSplitBillRequest {
  creatorAccountId: string;
  title: string;
  totalAmount: Money;
  currency?: string;
  splitType: 'EQUAL' | 'CUSTOM' | 'PERCENTAGE';
  participants: Omit<SplitBillParticipant, 'id' | 'status' | 'settledAt' | 'amountPaid' | 'remainingAmount'>[];
}

export default TransactionService.getInstance();
