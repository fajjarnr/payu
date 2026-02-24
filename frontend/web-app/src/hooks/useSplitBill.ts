'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import TransactionService from '@/services/TransactionService';
import type { CreateSplitBillRequest, SplitBillParticipant } from '@/services/TransactionService';

export function useSplitBills(accountId: string) {
  return useQuery({
    queryKey: ['split-bills', accountId],
    queryFn: () => TransactionService.getAccountSplitBills(accountId),
    enabled: !!accountId,
  });
}

export function useSplitBill(id: string) {
  return useQuery({
    queryKey: ['split-bill', id],
    queryFn: () => TransactionService.getSplitBill(id),
    enabled: !!id,
  });
}

export function useCreateSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateSplitBillRequest) => TransactionService.createSplitBill(request),
    ...MutationPresets.financial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['split-bills'] }); },
  });
}

export function useUpdateSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<CreateSplitBillRequest> }) =>
      TransactionService.updateSplitBill(id, data),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['split-bill', vars.id] });
      qc.invalidateQueries({ queryKey: ['split-bills'] });
    },
  });
}

export function useCancelSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.cancelSplitBill(id),
    ...MutationPresets.financial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['split-bills'] }); },
  });
}

export function useActivateSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.activateSplitBill(id),
    ...MutationPresets.financial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['split-bills'] }); },
  });
}

export function useAddParticipant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, participant }: { id: string; participant: SplitBillParticipant }) =>
      TransactionService.addParticipant(id, participant),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['split-bill', vars.id] }); },
  });
}

export function useAcceptSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, participantId }: { id: string; participantId: string }) =>
      TransactionService.acceptParticipation(id, participantId),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['split-bill', vars.id] }); },
  });
}

export function useDeclineSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, participantId }: { id: string; participantId: string }) =>
      TransactionService.declineParticipation(id, participantId),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['split-bill', vars.id] }); },
  });
}

export function useSplitBillPayment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, participantId, amount }: { id: string; participantId: string; amount: number }) =>
      TransactionService.makeParticipantPayment(id, participantId, amount),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['split-bill', vars.id] }); },
  });
}

export function useSettleSplitBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.settleSplitBill(id),
    ...MutationPresets.financial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['split-bills'] }); },
  });
}
