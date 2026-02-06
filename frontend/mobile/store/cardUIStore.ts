/**
 * Card UI Store - Zustand
 *
 * This store manages CLIENT-SIDE ONLY UI state for cards:
 * - selectedCardId: ID of the currently selected card in the UI
 *
 * SERVER STATE (cards data) should be fetched via React Query:
 * - useCards() from '@/hooks/useCardQuery'
 * - useCreateCard() for creating cards
 * - useCardActions() for freeze/unfreeze operations
 *
 * This separation eliminates duplication between Zustand and React Query
 * by clearly defining responsibilities:
 * - Zustand: UI state (selection, filters, view preferences)
 * - React Query: Server state (card data, mutations)
 */
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface CardUIState {
  // UI State only
  selectedCardId: string | null;
  cardViewMode: 'grid' | 'list';
  showCardDetails: boolean;

  // Actions
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

/**
 * useCardUIStore - Card UI State Only
 *
 * This store only persists UI preferences related to cards.
 * For card data, use TanStack Query hooks.
 */
export const useCardUIStore = create<CardUIState>()(
  persist(
    (set, get) => ({
      // Initial state - UI only
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
