'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import InvestmentService from '@/services/InvestmentService';
import type { BuyDepositRequest, BuyMutualFundRequest, BuyGoldRequest, SellInvestmentRequest, CreateAccountRequest } from '@/services/InvestmentService';

export function useInvestmentAccount(userId: string) {
  return useQuery({
    queryKey: ['investment-account', userId],
    queryFn: () => InvestmentService.getAccount(userId),
    enabled: !!userId,
  });
}

export function useGoldHoldings(userId: string) {
  return useQuery({
    queryKey: ['gold-holdings', userId],
    queryFn: () => InvestmentService.getGoldHoldings(userId),
    enabled: !!userId,
  });
}

export function useCreateInvestmentAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateAccountRequest) => InvestmentService.createAccount(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['investment-account', vars.userId] }); },
  });
}

export function useBuyDeposit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: BuyDepositRequest) => InvestmentService.buyDeposit(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['investment-account', vars.userId] }); },
  });
}

export function useBuyMutualFund() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: BuyMutualFundRequest) => InvestmentService.buyMutualFund(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['investment-account', vars.userId] }); },
  });
}

export function useBuyGold() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: BuyGoldRequest) => InvestmentService.buyGold(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['investment-account', vars.userId] }); },
  });
}

export function useSellInvestment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: SellInvestmentRequest) => InvestmentService.sell(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['investment-account', vars.userId] }); },
  });
}
