/**
 * Unified card hook: TanStack Query for server state, Zustand for UI state.
 */
import { useCallback, useEffect } from 'react';
import { useCards as useCardsQuery, useCreateCard, useCardActions } from './useCardQuery';
import { useCardUIStore } from '@/store/cardUIStore';
import { Logger } from '@/utils/logger';

export const useCards = () => {
  const { data: cards = [], isLoading, error, refetch } = useCardsQuery();
  const { mutateAsync: createCardMutation, isPending: isCreating } = useCreateCard();
  const { freezeCard, unfreezeCard, isFreezing, isUnfreezing } = useCardActions();

  // UI state from Zustand (client-side only)
  const { selectedCardId, selectCard: setSelectedCardId } = useCardUIStore();

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
    cards,
    selectedCard,
    isLoading,
    isCreating,
    isFreezing,
    isUnfreezing,
    error: error ? (error as Error).message : null,

    loadCards: refresh,
    selectCard,
    createCard: createCardMutation,
    freezeCard,
    unfreezeCard,

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
