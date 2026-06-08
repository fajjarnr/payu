'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useWebSocket } from './useWebSocket';
import AnalyticsService from '@/services/AnalyticsService';
import type { UserMetrics, SpendingAnalytics, CashFlowAnalysis } from '@/services/AnalyticsService';
import type { AnalyticsData } from '@/types';

/** REST hook: fetch user metrics (totalSpent, totalIncome, topCategories, etc.) */
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
  const now = new Date();
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
  const endDate = now.toISOString().split('T')[0];

  return useQuery<SpendingAnalytics>({
    queryKey: ['spending-trends', userId, startOfMonth, endDate],
    queryFn: () =>
      AnalyticsService.getSpendingTrends({
        userId: userId!,
        startDate: startOfMonth,
        endDate,
        granularity: 'MONTHLY',
      }),
    enabled: !!userId,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useCashFlow(userId: string | undefined) {
  const now = new Date();
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
  const endDate = now.toISOString().split('T')[0];

  return useQuery<CashFlowAnalysis>({
    queryKey: ['cash-flow', userId, startOfMonth, endDate],
    queryFn: () =>
      AnalyticsService.getCashFlowAnalysis({
        userId: userId!,
        startDate: startOfMonth,
        endDate,
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
