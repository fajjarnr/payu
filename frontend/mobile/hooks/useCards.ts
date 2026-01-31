import { useEffect, useCallback, useRef } from 'react';
import { useCardStore } from '@/store/cardStore';

export const useCards = () => {
  const isMountedRef = useRef(true);
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
    isMountedRef.current = true;

    const loadData = async () => {
      try {
        await loadCards();
      } catch (err) {
        if (isMountedRef.current) {
          console.error('Failed to load cards:', err);
        }
      }
    };

    loadData();

    return () => {
      isMountedRef.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Memoize refresh function
  const refresh = useCallback(async () => {
    if (!isMountedRef.current) return;

    try {
      await loadCards();
    } catch (err) {
      if (isMountedRef.current) {
        console.error('Failed to refresh cards:', err);
      }
    }
  }, [loadCards]);

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
    refresh,
  };
};
