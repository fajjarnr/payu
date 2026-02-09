import { create } from 'zustand';

export type TransactionFilter = {
  type?: 'all' | 'credit' | 'debit';
  status?: 'all' | 'completed' | 'pending' | 'failed';
  dateFrom?: string;
  dateTo?: string;
  search?: string;
  minAmount?: number;
  maxAmount?: number;
};

interface TransactionState {
  filters: TransactionFilter;
  selectedTransactionId: string | null;
  isDetailOpen: boolean;
  setFilters: (filters: Partial<TransactionFilter>) => void;
  resetFilters: () => void;
  selectTransaction: (id: string | null) => void;
  setDetailOpen: (open: boolean) => void;
}

const defaultFilters: TransactionFilter = {
  type: 'all',
  status: 'all',
  search: '',
};

/**
 * Transaction Store — UI state for transaction list/detail views.
 *
 * Manages filter state, selected transaction, and detail panel visibility.
 * Transaction data itself is fetched via TanStack Query hooks.
 */
export const useTransactionStore = create<TransactionState>((set) => ({
  filters: { ...defaultFilters },
  selectedTransactionId: null,
  isDetailOpen: false,

  setFilters: (filters) =>
    set((state) => ({ filters: { ...state.filters, ...filters } })),

  resetFilters: () => set({ filters: { ...defaultFilters } }),

  selectTransaction: (id) =>
    set({ selectedTransactionId: id, isDetailOpen: id !== null }),

  setDetailOpen: (open) =>
    set({ isDetailOpen: open, selectedTransactionId: open ? undefined : null }),
}));
