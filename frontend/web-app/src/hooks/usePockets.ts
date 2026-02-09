'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import WalletService from '@/services/WalletService';
import type { CreatePocketRequest } from '@/services/WalletService';

export function usePockets() {
  return useQuery({
    queryKey: ['pockets'],
    queryFn: () => WalletService.listPockets(),
  });
}

export function usePocket(pocketId: string) {
  return useQuery({
    queryKey: ['pocket', pocketId],
    queryFn: () => WalletService.getPocket(pocketId),
    enabled: !!pocketId,
  });
}

export function usePocketsByCurrency(currency: string) {
  return useQuery({
    queryKey: ['pockets', 'currency', currency],
    queryFn: () => WalletService.getPocketByCurrency(currency),
    enabled: !!currency,
  });
}

export function usePocketsTotalBalance(currency: string = 'IDR') {
  return useQuery({
    queryKey: ['pockets', 'total-balance', currency],
    queryFn: () => WalletService.getTotalPocketBalance(currency),
  });
}

export function useCreatePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: CreatePocketRequest) => WalletService.createPocket(request),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}

export function useCreditPocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ pocketId, amount, currency }: { pocketId: string; amount: number; currency: string }) =>
      WalletService.creditPocket(pocketId, amount, currency),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pockets'] });
      qc.invalidateQueries({ queryKey: ['pocket'] });
    },
  });
}

export function useDebitPocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ pocketId, amount, currency }: { pocketId: string; amount: number; currency: string }) =>
      WalletService.debitPocket(pocketId, amount, currency),
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}

export function useUnfreezePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (pocketId: string) => WalletService.unfreezePocket(pocketId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}

export function useClosePocket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (pocketId: string) => WalletService.closePocket(pocketId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pockets'] }); },
  });
}
