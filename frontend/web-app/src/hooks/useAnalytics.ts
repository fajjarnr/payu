'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useWebSocket } from './useWebSocket';
import AnalyticsService from '@/services/AnalyticsService';
import type { UserMetrics, SpendingAnalytics, CashFlowAnalysis } from '@/services/AnalyticsService';
import type { AnalyticsData } from '@/types';

/** REST hook: fetch user metrics. */
export function useUserMetrics(userId: string | undefined) {
  return useQuery<UserMetrics>({
    queryKey: ['user-metrics', userId],
    queryFn: () => AnalyticsService.getUserMetrics(userId!),
    enabled: !!userId,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useSpendingTrends(userId: string | undefined) {
  return useQuery<SpendingAnalytics>({
    queryKey: ['spending-trends', userId, 30, 'category'],
    queryFn: () =>
      AnalyticsService.getSpendingTrends({
        userId: userId!,
        periodDays: 30,
        groupBy: 'category',
      }),
    enabled: !!userId,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useCashFlow(userId: string | undefined) {
  return useQuery<CashFlowAnalysis>({
    queryKey: ['cash-flow', userId, 30],
    queryFn: () =>
      AnalyticsService.getCashFlowAnalysis({
        userId: userId!,
        periodDays: 30,
      }),
    enabled: !!userId,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}



export function useAnalyticsWebSocket(accountId: string | undefined) {
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  const handleUpdate = (data: { type: string; data: AnalyticsData }) => {
    if (data.type === 'BALANCE_UPDATE' && data.data) {
      setAnalytics(data.data);
    }
  };

  const handleOpen = () => setIsConnected(true);
  const handleClose = () => setIsConnected(false);

  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || (typeof window !== 'undefined' ? `wss://${window.location.host}` : 'wss://localhost');

  useWebSocket(`${wsUrl}/ws/analytics/${accountId || ''}`, {
    onMessage: handleUpdate,
    onOpen: handleOpen,
    onClose: handleClose,
    enabled: !!accountId
  });

  return {
    analytics,
    isConnected
  };
}
