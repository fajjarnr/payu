import { create } from 'zustand';

interface CardUiState {
  selectedCardId: string | null;
  selectCard: (cardId: string | null) => void;
}

export const useCardStore = create<CardUiState>((set) => ({
  selectedCardId: null,
  selectCard: (cardId) => set({ selectedCardId: cardId }),
}));
