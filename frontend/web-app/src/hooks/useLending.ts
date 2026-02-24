'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import LendingService from '@/services/LendingService';
import type { LoanApplicationRequest, PayLaterLimitRequest, PreApprovalCheckRequest } from '@/services/LendingService';

export function useCreditScore(userId: string) {
  return useQuery({
    queryKey: ['credit-score', userId],
    queryFn: () => LendingService.getCreditScore(userId),
    enabled: !!userId,
  });
}

export function useLoan(loanId: string) {
  return useQuery({
    queryKey: ['loan', loanId],
    queryFn: () => LendingService.getLoan(loanId),
    enabled: !!loanId,
  });
}

export function useRepaymentSchedule(loanId: string) {
  return useQuery({
    queryKey: ['repayment-schedule', loanId],
    queryFn: () => LendingService.getRepaymentSchedule(loanId),
    enabled: !!loanId,
  });
}

export function usePayLater(userId: string) {
  return useQuery({
    queryKey: ['paylater', userId],
    queryFn: () => LendingService.getPayLater(userId),
    enabled: !!userId,
  });
}

export function usePayLaterTransactions(userId: string) {
  return useQuery({
    queryKey: ['paylater-transactions', userId],
    queryFn: () => LendingService.getTransactionHistory(userId),
    enabled: !!userId,
  });
}

export function useActivePreApprovals(userId: string) {
  return useQuery({
    queryKey: ['pre-approvals', userId],
    queryFn: () => LendingService.getActivePreApprovals(userId),
    enabled: !!userId,
  });
}

export function useApplyLoan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: LoanApplicationRequest) => LendingService.applyLoan(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { 
      qc.invalidateQueries({ queryKey: ['loan'] }); 
      qc.invalidateQueries({ queryKey: ['credit-score', vars.userId] });
      qc.invalidateQueries({ queryKey: ['wallet-balance'] });
    },
  });
}

export function useActivatePayLater() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, request }: { userId: string; request: PayLaterLimitRequest }) =>
      LendingService.activatePayLater(userId, request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['paylater', vars.userId] }); },
  });
}

export function useCheckPreApproval() {
  return useMutation({
    mutationFn: (request: PreApprovalCheckRequest) => LendingService.checkPreApproval(request),
    ...MutationPresets.readOnly,
  });
}
