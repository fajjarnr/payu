'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import FxService, { type ConvertCurrencyRequest, type FxConversionRequest } from '@/services/FxService';

export const useFxRate = (fromCurrency: string, toCurrency: string, enabled = true) => {
  return useQuery({
    queryKey: ['fx-rate', fromCurrency, toCurrency],
    queryFn: () => FxService.getCurrentRate(fromCurrency, toCurrency),
    enabled: enabled && !!fromCurrency && !!toCurrency && fromCurrency !== toCurrency,
    staleTime: 60000, // 1 minute
    gcTime: 300000, // 5 minutes
    refetchInterval: 60000, // Auto-refresh every minute
  });
};

export const useAllFxRates = (enabled = true) => {
  return useQuery({
    queryKey: ['fx-rates'],
    queryFn: () => FxService.getAllRates(),
    enabled,
    staleTime: 60000,
    gcTime: 300000,
    refetchInterval: 60000,
  });
};

export const useFxEstimate = () => {
  return useMutation({
    mutationFn: (request: ConvertCurrencyRequest) => FxService.estimateConversion(request),
    ...MutationPresets.readOnly,
  });
};

export const useFxConversion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: FxConversionRequest) => FxService.createConversion(request),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
      queryClient.invalidateQueries({ queryKey: ['fx-conversions'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (error) => {
      console.error('FX conversion failed:', error instanceof Error ? error.message : 'unknown');
    }
  });
};

export const useFxConversions = (enabled = true) => {
  return useQuery({
    queryKey: ['fx-conversions'],
    queryFn: () => FxService.getConversions(),
    enabled,
    staleTime: 30000,
    gcTime: 300000,
  });
};

export const useFxConversionDetail = (conversionId: string | undefined) => {
  return useQuery({
    queryKey: ['fx-conversion', conversionId],
    queryFn: () => FxService.getConversion(conversionId!),
    enabled: !!conversionId,
    staleTime: 120000,
    gcTime: 300000,
  });
};

export const useFxReverse = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (conversionId: string) => FxService.reverseConversion(conversionId),
    ...MutationPresets.financial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet-balance'] });
      queryClient.invalidateQueries({ queryKey: ['fx-conversions'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (error) => {
      console.error('FX reversal failed:', error instanceof Error ? error.message : 'unknown');
    }
  });
};
