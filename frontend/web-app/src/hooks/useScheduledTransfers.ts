'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import TransactionService from '@/services/TransactionService';
import type { CreateScheduledTransferRequest } from '@/services/TransactionService';

export function useScheduledTransfers(accountId: string) {
  return useQuery({
    queryKey: ['scheduled-transfers', accountId],
    queryFn: () => TransactionService.getAccountScheduledTransfers(accountId),
    enabled: !!accountId,
  });
}

export function useScheduledTransfer(id: string) {
  return useQuery({
    queryKey: ['scheduled-transfer', id],
    queryFn: () => TransactionService.getScheduledTransfer(id),
    enabled: !!id,
  });
}

export function useCreateScheduledTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateScheduledTransferRequest) =>
      TransactionService.createScheduledTransfer(request),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['scheduled-transfers'] }); },
  });
}

export function useUpdateScheduledTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<CreateScheduledTransferRequest> }) =>
      TransactionService.updateScheduledTransfer(id, data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['scheduled-transfer', vars.id] });
      qc.invalidateQueries({ queryKey: ['scheduled-transfers'] });
    },
  });
}

export function useCancelScheduledTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.cancelScheduledTransfer(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['scheduled-transfers'] }); },
  });
}

export function usePauseScheduledTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.pauseScheduledTransfer(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['scheduled-transfers'] }); },
  });
}

export function useResumeScheduledTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.resumeScheduledTransfer(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['scheduled-transfers'] }); },
  });
}
