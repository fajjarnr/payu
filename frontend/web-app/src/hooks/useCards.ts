'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useFreezeCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (cardId: string) => WalletService.freezeCard(cardId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useUnfreezeCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (cardId: string) => WalletService.unfreezeCard(cardId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}
