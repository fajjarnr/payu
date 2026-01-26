import { create } from 'zustand';
import { Transaction, TransferData } from '@/types';
import { transactionService } from '@/services/transaction.service';

interface TransactionState {
  transactions: Transaction[];
  isLoading: boolean;
  isLoadingMore: boolean;
  hasMore: boolean;
  page: number;
  error: string | null;

  // Actions
  loadTransactions: (refresh?: boolean) => Promise<void>;
  loadMoreTransactions: () => Promise<void>;
  transfer: (data: TransferData) => Promise<Transaction>;
  clearError: () => void;
}

export const useTransactionStore = create<TransactionState>((set, get) => ({
  transactions: [],
  isLoading: false,
  isLoadingMore: false,
  hasMore: true,
  page: 1,
  error: null,

  loadTransactions: async (refresh = false) => {
    set({
      isLoading: true,
      error: null,
      page: refresh ? 1 : get().page,
    });

    try {
      const response = await transactionService.getTransactions({
        page: 1,
        pageSize: 20,
      });

      set({
        transactions: response.items,
        hasMore: response.hasMore,
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to load transactions',
        isLoading: false,
      });
    }
  },

  loadMoreTransactions: async () => {
    const { isLoadingMore, hasMore, page, transactions } = get();

    if (isLoadingMore || !hasMore) return;

    set({ isLoadingMore: true });

    try {
      const response = await transactionService.getTransactions({
        page: page + 1,
        pageSize: 20,
      });

      set({
        transactions: [...transactions, ...response.items],
        hasMore: response.hasMore,
        page: page + 1,
        isLoadingMore: false,
      });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to load more transactions',
        isLoadingMore: false,
      });
    }
  },

  transfer: async (data: TransferData) => {
    set({ isLoading: true, error: null });

    try {
      const transaction = await transactionService.transfer(data);

      set((state) => ({
        transactions: [transaction, ...state.transactions],
        isLoading: false,
      }));

      return transaction;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Transfer failed',
        isLoading: false,
      });
      throw error;
    }
  },

  clearError: () => set({ error: null }),
}));
