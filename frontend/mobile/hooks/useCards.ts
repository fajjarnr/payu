/**
 * useCards Hook - Unified Card Management Hook
 *
 * This hook provides a unified interface for card management using TanStack Query
 * for server state and Zustand for UI state (selectedCardId).
 *
 * STATE MANAGEMENT:
 * - Server State (cards data): Managed by TanStack Query (useCardsQuery)
 * - UI State (selectedCardId): Managed by useCardUIStore (Zustand)
 * - Actions (create, freeze, unfreeze): Handled by TanStack Query mutations
 *
 * This pattern eliminates duplication between Zustand and React Query by clearly
 * separating server state from UI state.
 */
import { useCallback, useEffect } from 'react';
import { useCards as useCardsQuery, useCreateCard, useCardActions } from './useCardQuery';
import { useCardUIStore } from '@/store/cardUIStore';
import { Logger } from '@/utils/logger';

export const useCards = () => {
  // Server state from TanStack Query
  const { data: cards = [], isLoading, error, refetch } = useCardsQuery();
  const { mutateAsync: createCardMutation, isPending: isCreating } = useCreateCard();
  const { freezeCard, unfreezeCard, isFreezing, isUnfreezing } = useCardActions();

  // UI state from Zustand (client-side only)
  const { selectedCardId, selectCard: setSelectedCardId } = useCardUIStore();

  // Derived state for selected card
  const selectedCard = cards.find(c => c.id === selectedCardId) || cards[0] || null;

  // Sync selected card if needed (e.g. initial load)
  useEffect(() => {
    if (!selectedCardId && cards.length > 0) {
      setSelectedCardId(cards[0].id);
    }
  }, [cards, selectedCardId, setSelectedCardId]);

  const selectCard = useCallback((cardId: string) => {
    setSelectedCardId(cardId);
  }, [setSelectedCardId]);

  const refresh = useCallback(async () => {
    Logger.debug('Cards', 'Refreshing cards');
    await refetch();
  }, [refetch]);

  return {
    // Server state
    cards,
    selectedCard,
    isLoading,
    isCreating,
    isFreezing,
    isUnfreezing,
    error: error ? (error as Error).message : null,

    // Actions
    loadCards: refresh,
    selectCard,
    createCard: createCardMutation,
    freezeCard,
    unfreezeCard,

    // Placeholder for future implementations
    setSpendingLimit: async () => {
      Logger.warn('Cards', 'setSpendingLimit not implemented');
    },
    cancelCard: async () => {
      Logger.warn('Cards', 'cancelCard not implemented');
    },
    clearError: () => {
      // React Query handles error state automatically
    },
    refresh,
  };
};
