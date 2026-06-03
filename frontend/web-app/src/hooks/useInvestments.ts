'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import InvestmentService from '@/services/InvestmentService';
import type { BuyDepositRequest, BuyMutualFundRequest, BuyGoldRequest, SellInvestmentRequest } from '@/services/InvestmentService';

// BUG-CROSS-048: getAccount/getGoldHoldings now use /me endpoints (no userId param)
export function useInvestmentAccount() {
  return useQuery({
    queryKey: ['investment-account'],
    queryFn: () => InvestmentService.getAccount(),
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useGoldHoldings() {
  return useQuery({
    queryKey: ['gold-holdings'],
    queryFn: () => InvestmentService.getGoldHoldings(),
    staleTime: 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

// BUG-CROSS-049: createAccount takes no body
export function useCreateInvestmentAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => InvestmentService.createAccount(),
    ...MutationPresets.financial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['investment-account'] }); },
  });
}

// BUG-CROSS-050: BuyDepositRequest now uses accountId instead of userId
export function useBuyDeposit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: BuyDepositRequest) => InvestmentService.buyDeposit(request),
    ...MutationPresets.financial,
    onSuccess: () => { 
      qc.invalidateQueries({ queryKey: ['investment-account'] }); 
      qc.invalidateQueries({ queryKey: ['wallet-balance'] });
    },
  });
}

// BUG-CROSS-051: BuyMutualFundRequest now uses accountId instead of userId
export function useBuyMutualFund() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: BuyMutualFundRequest) => InvestmentService.buyMutualFund(request),
    ...MutationPresets.financial,
    onSuccess: () => { 
      qc.invalidateQueries({ queryKey: ['investment-account'] }); 
      qc.invalidateQueries({ queryKey: ['wallet-balance'] });
    },
  });
}

// BUG-CROSS-052: BuyGoldRequest only has amount now
export function useBuyGold() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: BuyGoldRequest) => InvestmentService.buyGold(request),
    ...MutationPresets.financial,
    onSuccess: () => { 
      qc.invalidateQueries({ queryKey: ['investment-account'] }); 
      qc.invalidateQueries({ queryKey: ['gold-holdings'] }); 
      qc.invalidateQueries({ queryKey: ['wallet-balance'] });
    },
  });
}

export function useSellInvestment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: SellInvestmentRequest) => InvestmentService.sell(request),
    ...MutationPresets.financial,
    onSuccess: (_, vars) => { 
      qc.invalidateQueries({ queryKey: ['investment-account', vars.accountId] }); 
      qc.invalidateQueries({ queryKey: ['gold-holdings', vars.accountId] });
      qc.invalidateQueries({ queryKey: ['wallet-balance'] });
    },
  });
}
