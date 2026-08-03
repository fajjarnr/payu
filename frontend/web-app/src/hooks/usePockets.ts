'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import WalletService from '@/services/WalletService';
import type { CreatePocketRequest } from '@/services/WalletService';
import type { Money } from '@/types';

export function usePockets() {
  return useQuery({
    queryKey: ['pockets'],
    queryFn: () => WalletService.listPockets(),
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function usePocket(pocketId: string) {
  return useQuery({
    queryKey: ['pocket', pocketId],
    queryFn: () => WalletService.getPocket(pocketId),
    enabled: !!pocketId,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function usePocketsByCurrency(currency: string) {
  return useQuery({
    queryKey: ['pockets', 'currency', currency],
    queryFn: () => WalletService.getPocketByCurrency(currency),
    enabled: !!currency,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function usePocketsTotalBalance(currency: string = 'IDR') {
  return useQuery({
    queryKey: ['pockets', 'total-balance', currency],
    queryFn: () => WalletService.getTotalPocketBalance(currency),
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useCreatePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: CreatePocketRequest) => WalletService.createPocket(request),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}

export function useCreditPocket() {
  const qc = useQueryClient();
  return useMutation({
    // BUG-FE-070: 3rd param is `description`, not `currency`
    mutationFn: ({ pocketId, amount, description }: { pocketId: string; amount: Money; description?: string }) =>
      WalletService.creditPocket(pocketId, amount, description ?? ''),
    ...MutationPresets.financial,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pockets'] });
      qc.invalidateQueries({ queryKey: ['pocket'] });
    },
  });
}

export function useDebitPocket() {
  const qc = useQueryClient();
  return useMutation({
    // BUG-FE-070: 3rd param is `description`, not `currency`
    mutationFn: ({ pocketId, amount, description }: { pocketId: string; amount: Money; description?: string }) =>
      WalletService.debitPocket(pocketId, amount, description ?? ''),
    ...MutationPresets.financial,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pockets'] });
      qc.invalidateQueries({ queryKey: ['pocket'] });
    },
  });
}

export function useFreezePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (pocketId: string) => WalletService.freezePocket(pocketId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}

export function useUnfreezePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (pocketId: string) => WalletService.unfreezePocket(pocketId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}

export function useClosePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (pocketId: string) => WalletService.closePocket(pocketId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}
