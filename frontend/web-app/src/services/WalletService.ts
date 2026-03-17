import api from '@/lib/api';
import { getFinancialMutationHeaders } from '@/lib/utils';
import type { BalanceResponse, WalletTransaction, Pocket } from '@/types';

// IMP-014: Re-export types from centralized types/index.ts
export type { BalanceResponse, WalletTransaction, Pocket };

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

// IMP-014: WalletTransaction is now re-exported from '@/types'

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
    const response = await api.post<ReserveBalanceResponse>(`/wallets/${accountId}/reserve`, request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async commitReservation(reservationId: string): Promise<{ status: string; reservationId: string }> {
    const response = await api.post<{ status: string; reservationId: string }>(`/wallets/reservations/${reservationId}/commit`, null, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async releaseReservation(reservationId: string): Promise<{ status: string; reservationId: string }> {
    const response = await api.post<{ status: string; reservationId: string }>(`/wallets/reservations/${reservationId}/release`, null, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  // XBUG-007: credit() endpoint removed — this is an internal-only API
  // Direct wallet credits must go through backend services, not client-facing UI

  async getTransactionHistory(accountId: string, page: number = 0, size: number = 20): Promise<WalletTransaction[]> {
    const response = await api.get<WalletTransaction[]>(`/wallets/${accountId}/transactions`, {
      params: { page, size }
    });
    return response.data;
  }

  /** GET /wallets/{accountId}/ledger — Get ledger entries */
  async getLedgerEntries(accountId: string): Promise<LedgerEntry[]> {
    const response = await api.get<LedgerEntry[]>(`/wallets/${accountId}/ledger`);
    return response.data;
  }

  /** GET /wallets/{accountId}/ledger/transaction/{transactionId} */
  async getLedgerByTransaction(accountId: string, transactionId: string): Promise<LedgerEntry[]> {
    const response = await api.get<LedgerEntry[]>(`/wallets/${accountId}/ledger/transaction/${transactionId}`);
    return response.data;
  }

  // === Virtual Cards (FE-GAP-009) ===

  /** POST /cards — Create a virtual card */
  async createCard(request: CreateCardRequest): Promise<VirtualCard> {
    const response = await api.post<VirtualCard>('/cards', request);
    return response.data;
  }

  /** GET /cards — List all cards for an account */
  // BUG-CROSS-045: Backend CardController.getCards requires accountId as query param
  async listCards(accountId: string): Promise<VirtualCard[]> {
    const response = await api.get<VirtualCard[]>('/cards', { params: { accountId } });
    return response.data;
  }

  /** GET /cards/{cardId} — Get card details */
  async getCard(cardId: string): Promise<VirtualCard> {
    const response = await api.get<VirtualCard>(`/cards/${cardId}`);
    return response.data;
  }

  /** POST /cards/{cardId}/freeze — Freeze card */
  // BUG-CROSS-046: Backend returns ApiResponse<Void>, not VirtualCard
  async freezeCard(cardId: string): Promise<void> {
    await api.post(`/cards/${cardId}/freeze`);
  }

  /** POST /cards/{cardId}/unfreeze — Unfreeze card */
  // BUG-CROSS-046: Backend returns ApiResponse<Void>, not VirtualCard
  async unfreezeCard(cardId: string): Promise<void> {
    await api.post(`/cards/${cardId}/unfreeze`);
  }

  /** DELETE /cards/{cardId} — Delete/Close card */
  async deleteCard(cardId: string): Promise<{ status: string; cardId: string }> {
    const response = await api.delete<{ status: string; cardId: string }>(`/cards/${cardId}`);
    return response.data;
  }

  /** PUT /cards/{cardId} — Update card limits */
  async updateCard(cardId: string, request: UpdateCardRequest): Promise<VirtualCard> {
    const response = await api.put<VirtualCard>(`/cards/${cardId}`, request);
    return response.data;
  }

  // === Pockets (FE-GAP-010) ===

  /** POST /pockets — Create a pocket */
  async createPocket(request: CreatePocketRequest): Promise<Pocket> {
    const response = await api.post<Pocket>('/pockets', request);
    return response.data;
  }

  /** GET /pockets/{pocketId} — Get pocket details */
  async getPocket(pocketId: string): Promise<Pocket> {
    const response = await api.get<Pocket>(`/pockets/${pocketId}`);
    return response.data;
  }

  /** GET /pockets — List all pockets */
  async listPockets(): Promise<Pocket[]> {
    const response = await api.get<Pocket[]>('/pockets');
    return response.data;
  }

  /** GET /pockets/currency/{currency} — Get pocket by currency */
  async getPocketByCurrency(currency: string): Promise<Pocket> {
    const response = await api.get<Pocket>(`/pockets/currency/${currency}`);
    return response.data;
  }

  /** POST /pockets/{pocketId}/credit — Credit pocket */
  // BUG-CROSS-044: Backend PocketTransactionRequest uses { amount, referenceId }, returns Void
  async creditPocket(pocketId: string, amount: number, referenceId: string): Promise<void> {
    await api.post(`/pockets/${pocketId}/credit`, { amount, referenceId });
  }

  /** POST /pockets/{pocketId}/debit — Debit pocket */
  // BUG-CROSS-044: Backend PocketTransactionRequest uses { amount, referenceId }, returns Void
  async debitPocket(pocketId: string, amount: number, referenceId: string): Promise<void> {
    await api.post(`/pockets/${pocketId}/debit`, { amount, referenceId });
  }

  /** POST /pockets/{pocketId}/freeze — Freeze pocket */
  async freezePocket(pocketId: string): Promise<Pocket> {
    const response = await api.post<Pocket>(`/pockets/${pocketId}/freeze`);
    return response.data;
  }

  /** POST /pockets/{pocketId}/unfreeze — Unfreeze pocket */
  async unfreezePocket(pocketId: string): Promise<Pocket> {
    const response = await api.post<Pocket>(`/pockets/${pocketId}/unfreeze`);
    return response.data;
  }

  /** POST /pockets/{pocketId}/close — Close pocket */
  async closePocket(pocketId: string): Promise<Pocket> {
    const response = await api.post<Pocket>(`/pockets/${pocketId}/close`);
    return response.data;
  }

  /** GET /pockets/total-balance/{targetCurrency} — Get total balance */
  async getTotalPocketBalance(targetCurrency: string): Promise<{ totalBalance: number; currency: string }> {
    const response = await api.get(`/pockets/total-balance/${targetCurrency}`);
    return response.data;
  }
}

// === Card Types ===

// BUG-CROSS-047: Aligned VirtualCard interface with backend CardResponse
export interface VirtualCard {
  id: string;
  walletId: string;
  cardNumber: string;
  expiryDate: string;
  cardHolderName: string;
  status: 'ACTIVE' | 'FROZEN' | 'CANCELLED';
  dailyLimit: number;
  createdAt: string;
}

// BUG-CROSS-045: Aligned CreateCardRequest with backend CreateCardRequest
export interface CreateCardRequest {
  accountId: string;
  cardHolderName: string;
  dailyLimit?: number;
}

export interface UpdateCardRequest {
  dailyLimit?: number;
}

// === Pocket Types ===


// BUG-CROSS-043: Aligned CreatePocketRequest with backend — removed target/type, added description
export interface CreatePocketRequest {
  accountId: string;
  name: string;
  currency: string;
  description?: string;
}

// === Ledger Types ===

export interface LedgerEntry {
  id: string;
  accountId: string;
  transactionId: string;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export default WalletService.getInstance();
