import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cardService } from '@/services/card.service';
import { VirtualCard } from '@/types';
import { Logger } from '@/utils/logger';

export const CARD_KEYS = {
  all: ['cards'] as const,
  detail: (id: string) => [...CARD_KEYS.all, id] as const,
};

export function useCards() {
  return useQuery({
    queryKey: CARD_KEYS.all,
    queryFn: async () => {
      Logger.debug('useCards', 'Fetching cards');
      return cardService.getCards();
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useCreateCard() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => cardService.createCard(),
    onSuccess: (newCard) => {
      Logger.info('useCreateCard', 'Card created successfully');
      queryClient.setQueryData(CARD_KEYS.all, (old: VirtualCard[] | undefined) => {
        return old ? [...old, newCard] : [newCard];
      });
      queryClient.invalidateQueries({ queryKey: CARD_KEYS.all });
    },
    onError: (error) => {
      Logger.error('useCreateCard', 'Failed to create card', error);
    },
  });
}

export function useCardActions() {
  const queryClient = useQueryClient();

  const freezeMutation = useMutation({
    mutationFn: (cardId: string) => cardService.freezeCard(cardId),
    onSuccess: (_, cardId) => {
      queryClient.invalidateQueries({ queryKey: CARD_KEYS.all });
      Logger.info('useCardActions', 'Card frozen', { cardId });
    },
  });

  const unfreezeMutation = useMutation({
    mutationFn: (cardId: string) => cardService.unfreezeCard(cardId),
    onSuccess: (_, cardId) => {
      queryClient.invalidateQueries({ queryKey: CARD_KEYS.all });
      Logger.info('useCardActions', 'Card unfrozen', { cardId });
    },
  });

  return {
    freezeCard: freezeMutation.mutateAsync,
    unfreezeCard: unfreezeMutation.mutateAsync,
    isFreezing: freezeMutation.isPending,
    isUnfreezing: unfreezeMutation.isPending,
  };
}
