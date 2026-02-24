'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import WalletService from '@/services/WalletService';
import type { CreateCardRequest } from '@/services/WalletService';

export function useCards() {
  return useQuery({
    queryKey: ['cards'],
    queryFn: () => WalletService.listCards(),
  });
}

export function useCard(cardId: string) {
  return useQuery({
    queryKey: ['card', cardId],
    queryFn: () => WalletService.getCard(cardId),
    enabled: !!cardId,
  });
}

export function useCreateCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateCardRequest) => WalletService.createCard(request),
    ...MutationPresets.financial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useFreezeCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (cardId: string) => WalletService.freezeCard(cardId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useUnfreezeCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (cardId: string) => WalletService.unfreezeCard(cardId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useDeleteCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (cardId: string) => WalletService.deleteCard(cardId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useUpdateCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cardId, data }: { cardId: string; data: import('@/services/WalletService').UpdateCardRequest }) =>
      WalletService.updateCard(cardId, data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}
