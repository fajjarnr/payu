/**
 * Client-side UI state for cards (selected card, view mode, details visibility).
 * Card data lives in TanStack Query; this store holds no server state.
 */
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface CardUIState {
  selectedCardId: string | null;
  cardViewMode: 'grid' | 'list';
  showCardDetails: boolean;

  selectCard: (cardId: string | null) => void;
  setCardViewMode: (mode: 'grid' | 'list') => void;
  toggleCardDetails: () => void;
  resetCardUI: () => void;
}

const defaults = {
  selectedCardId: null,
  cardViewMode: 'grid' as const,
  showCardDetails: false,
};

/** UI-only card store. For card data, use TanStack Query hooks. */
export const useCardUIStore = create<CardUIState>()(
  persist(
    (set, get) => ({
      selectedCardId: defaults.selectedCardId,
      cardViewMode: defaults.cardViewMode,
      showCardDetails: defaults.showCardDetails,

      selectCard: (cardId) => {
        set({ selectedCardId: cardId });
      },

      setCardViewMode: (mode) => {
        set({ cardViewMode: mode });
      },

      toggleCardDetails: () => {
        set({ showCardDetails: !get().showCardDetails });
      },

      resetCardUI: () => {
        set({
          selectedCardId: defaults.selectedCardId,
          cardViewMode: defaults.cardViewMode,
          showCardDetails: defaults.showCardDetails,
        });
      },
    }),
    {
      name: 'card-ui-storage',
      storage: createJSONStorage(() => AsyncStorage),
      // Only persist UI state (no sensitive card data)
      partialize: (state) => ({
        cardViewMode: state.cardViewMode,
        showCardDetails: state.showCardDetails,
        // Note: selectedCardId is not persisted to avoid issues when cards change
      }),
    }
  )
);

// Selectors for optimized re-renders
export const selectSelectedCardId = (state: CardUIState) => state.selectedCardId;
export const selectCardViewMode = (state: CardUIState) => state.cardViewMode;
export const selectShowCardDetails = (state: CardUIState) => state.showCardDetails;
