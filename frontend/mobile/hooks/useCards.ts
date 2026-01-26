import { useEffect } from 'react';
import { useCardStore } from '@/store/cardStore';

export const useCards = () => {
  const {
    cards,
    selectedCard,
    isLoading,
    error,
    loadCards,
    selectCard,
    createCard,
    freezeCard,
    unfreezeCard,
    setSpendingLimit,
    cancelCard,
    clearError,
  } = useCardStore();

  useEffect(() => {
    loadCards();
  }, []);

  return {
    cards,
    selectedCard,
    isLoading,
    error,
    loadCards,
    selectCard,
    createCard,
    freezeCard,
    unfreezeCard,
    setSpendingLimit,
    cancelCard,
    clearError,
  };
};
