import { create } from 'zustand';
import { VirtualCard } from '@/types';
import { cardService } from '@/services/card.service';

interface CardState {
  cards: VirtualCard[];
  selectedCard: VirtualCard | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  loadCards: () => Promise<void>;
  selectCard: (cardId: string) => void;
  createCard: () => Promise<void>;
  freezeCard: (cardId: string) => Promise<void>;
  unfreezeCard: (cardId: string) => Promise<void>;
  setSpendingLimit: (cardId: string, limit: number) => Promise<void>;
  cancelCard: (cardId: string) => Promise<void>;
  clearError: () => void;
}

export const useCardStore = create<CardState>((set, get) => ({
  cards: [],
  selectedCard: null,
  isLoading: false,
  error: null,

  loadCards: async () => {
    set({ isLoading: true, error: null });

    try {
      const cards = await cardService.getCards();

      set({
        cards,
        selectedCard: cards[0] || null,
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to load cards',
        isLoading: false,
      });
    }
  },

  selectCard: (cardId: string) => {
    const { cards } = get();
    const card = cards.find((c) => c.id === cardId);

    set({ selectedCard: card || null });
  },

  createCard: async () => {
    set({ isLoading: true, error: null });

    try {
      const newCard = await cardService.createCard();

      set((state) => ({
        cards: [...state.cards, newCard],
        selectedCard: newCard,
        isLoading: false,
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to create card',
        isLoading: false,
      });
      throw error;
    }
  },

  freezeCard: async (cardId: string) => {
    set({ isLoading: true, error: null });

    try {
      await cardService.freezeCard(cardId);

      set((state) => ({
        cards: state.cards.map((c) =>
          c.id === cardId ? { ...c, status: 'frozen' } : c
        ),
        selectedCard:
          state.selectedCard?.id === cardId
            ? { ...state.selectedCard, status: 'frozen' as const }
            : state.selectedCard,
        isLoading: false,
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to freeze card',
        isLoading: false,
      });
      throw error;
    }
  },

  unfreezeCard: async (cardId: string) => {
    set({ isLoading: true, error: null });

    try {
      await cardService.unfreezeCard(cardId);

      set((state) => ({
        cards: state.cards.map((c) =>
          c.id === cardId ? { ...c, status: 'active' } : c
        ),
        selectedCard:
          state.selectedCard?.id === cardId
            ? { ...state.selectedCard, status: 'active' as const }
            : state.selectedCard,
        isLoading: false,
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to unfreeze card',
        isLoading: false,
      });
      throw error;
    }
  },

  setSpendingLimit: async (cardId: string, limit: number) => {
    set({ isLoading: true, error: null });

    try {
      await cardService.setSpendingLimit(cardId, limit);

      set((state) => ({
        cards: state.cards.map((c) =>
          c.id === cardId ? { ...c, spendingLimit: limit } : c
        ),
        selectedCard:
          state.selectedCard?.id === cardId
            ? { ...state.selectedCard, spendingLimit: limit }
            : state.selectedCard,
        isLoading: false,
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to set spending limit',
        isLoading: false,
      });
      throw error;
    }
  },

  cancelCard: async (cardId: string) => {
    set({ isLoading: true, error: null });

    try {
      await cardService.cancelCard(cardId);

      set((state) => ({
        cards: state.cards.filter((c) => c.id !== cardId),
        selectedCard:
          state.selectedCard?.id === cardId ? null : state.selectedCard,
        isLoading: false,
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to cancel card',
        isLoading: false,
      });
      throw error;
    }
  },

  clearError: () => set({ error: null }),
}));
