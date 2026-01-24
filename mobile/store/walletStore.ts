import { create } from 'zustand';
import { Wallet, Pocket } from '@/types';
import { walletService } from '@/services/wallet.service';

interface WalletState {
  primaryWallet: Wallet | null;
  pockets: Pocket[];
  balance: number;
  isLoading: boolean;
  error: string | null;

  // Actions
  loadWallet: () => Promise<void>;
  loadPockets: () => Promise<void>;
  createPocket: (data: {
    name: string;
    type: 'savings' | 'goals';
    initialBalance?: number;
  }) => Promise<void>;
  transferToPocket: (
    fromPocketId: string,
    toPocketId: string,
    amount: number,
    description?: string
  ) => Promise<void>;
  clearError: () => void;
}

export const useWalletStore = create<WalletState>((set, get) => ({
  primaryWallet: null,
  pockets: [],
  balance: 0,
  isLoading: false,
  error: null,

  loadWallet: async () => {
    set({ isLoading: true, error: null });

    try {
      const wallet = await walletService.getPrimaryWallet();

      set({
        primaryWallet: wallet,
        balance: wallet.balance,
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to load wallet',
        isLoading: false,
      });
    }
  },

  loadPockets: async () => {
    set({ isLoading: true, error: null });

    try {
      const wallets = await walletService.getWallets();

      const pockets: Pocket[] = wallets
        .filter((w) => w.pocketType !== 'primary')
        .map((w) => ({
          id: w.id,
          name: w.id, // In real app, this would be the pocket name
          balance: w.balance,
          type: w.pocketType as 'savings' | 'goals',
          color: '#10b981',
          icon: '💰',
        }));

      set({
        pockets,
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to load pockets',
        isLoading: false,
      });
    }
  },

  createPocket: async (data) => {
    set({ isLoading: true, error: null });

    try {
      const newPocket = await walletService.createPocket(data);

      const pocket: Pocket = {
        id: newPocket.id,
        name: data.name,
        balance: data.initialBalance || 0,
        type: data.type,
        color: '#10b981',
        icon: '💰',
      };

      set((state) => ({
        pockets: [...state.pockets, pocket],
        isLoading: false,
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to create pocket',
        isLoading: false,
      });
      throw error;
    }
  },

  transferToPocket: async (
    fromPocketId: string,
    toPocketId: string,
    amount: number,
    description?: string
  ) => {
    set({ isLoading: true, error: null });

    try {
      await walletService.transferToPocket(fromPocketId, toPocketId, amount, description);

      // Reload wallet and pockets
      await get().loadWallet();
      await get().loadPockets();
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
