import { create } from 'zustand';
import type { Money } from '@/lib/currency';

export interface WalletBalance {
  walletId: string;
  balance: Money;
  availableBalance: Money;
  currency: string;
  lastUpdated: string;
}

export interface RecentTransaction {
  id: string;
  type: 'credit' | 'debit';
  amount: Money;
  currency: string;
  description: string;
  status: 'completed' | 'pending' | 'failed';
  timestamp: string;
}

interface WalletState {
  balance: WalletBalance | null;
  recentTransactions: RecentTransaction[];
  isRefreshing: boolean;
  setBalance: (balance: WalletBalance) => void;
  setRecentTransactions: (transactions: RecentTransaction[]) => void;
  setRefreshing: (refreshing: boolean) => void;
  clearWallet: () => void;
}

/**
 * Wallet Store — Client-side cache for wallet state.
 *
 * NOTE: This store provides optimistic UI updates and local state caching.
 * The source of truth for wallet data is the server, managed via TanStack Query
 * in useWallet.ts hook. This store is for:
 * - Instant balance display without waiting for refetch
 * - Optimistic updates during transfers/top-ups
 * - Cross-component balance synchronization
 */
export const useWalletStore = create<WalletState>((set) => ({
  balance: null,
  recentTransactions: [],
  isRefreshing: false,

  setBalance: (balance) => set({ balance }),

  setRecentTransactions: (transactions) =>
    set({ recentTransactions: transactions }),

  setRefreshing: (refreshing) => set({ isRefreshing: refreshing }),

  clearWallet: () =>
    set({ balance: null, recentTransactions: [], isRefreshing: false }),
}));
