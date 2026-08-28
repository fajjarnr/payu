'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import AccountService, { Beneficiary, BeneficiaryRequest } from '@/services/AccountService';

export function useBeneficiaries(accountId?: string) {
  return useQuery({
    queryKey: ['beneficiaries', accountId],
    queryFn: () => AccountService.getBeneficiaries(accountId!),
    enabled: !!accountId,
    staleTime: 30_000,
  });
}

export function useCreateBeneficiary(accountId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: BeneficiaryRequest) => AccountService.createBeneficiary(accountId, req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['beneficiaries', accountId] }),
  });
}

export function useDeleteBeneficiary(accountId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (beneficiaryId: string) => AccountService.deleteBeneficiary(accountId, beneficiaryId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['beneficiaries', accountId] }),
  });
}
