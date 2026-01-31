import { useCallback, useEffect } from 'react';
import { useCards as useCardsQuery, useCreateCard, useCardActions } from './useCardQuery';
import { useCardStore } from '@/store/cardStore';
import { Logger } from '@/utils/logger';

export const useCards = () => {
  const { data: cards = [], isLoading, error, refetch } = useCardsQuery();
  const { mutateAsync: createCardMutation } = useCreateCard();
  const { freezeCard, unfreezeCard } = useCardActions();
  const { selectedCardId, selectCard: setSelectedCardId } = useCardStore();

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
    cards,
    selectedCard,
    isLoading,
    error: error ? (error as Error).message : null,
    loadCards: refresh, // Alias for backward compatibility
    selectCard,
    createCard: createCardMutation,
    freezeCard,
    unfreezeCard,
    setSpendingLimit: async () => {}, // Not implemented in query mutation yet
    cancelCard: async () => {}, // Not implemented in query mutation yet
    clearError: () => {}, // React Query handles error state automatically
    refresh,
  };
};
