'use client';

import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useWebSocket } from './useWebSocket';
import AnalyticsService from '@/services/AnalyticsService';
import type { UserMetrics, SpendingAnalytics, CashFlowAnalysis } from '@/services/AnalyticsService';
import type { AnalyticsData } from '@/types';

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
  // WEB-OS-008: page JS cannot read the httpOnly session cookie, so the BFF
  // issues a short-lived ticket first and the handshake carries ?ticket=.
  const [ticketParam, setTicketParam] = useState<string | null>(null);

  useEffect(() => {
    if (!accountId) {
      setTicketParam(null);
      return;
    }
    let cancelled = false;
    fetch('/api/ws-ticket', { method: 'POST' })
      .then((res) => {
        if (!res.ok) throw new Error(`ws-ticket ${res.status}`);
        return res.json() as Promise<{ ticket: string }>;
      })
      .then((body) => {
        if (!cancelled) setTicketParam(`?ticket=${encodeURIComponent(body.ticket)}`);
      })
      .catch(() => {
        if (!cancelled) setTicketParam(null);
      });
    return () => {
      cancelled = true;
    };
  }, [accountId]);

  const handleUpdate = (message: { event_type?: string; data?: AnalyticsData }) => {
    // Server contract = DashboardEvent dump (WALLET_BALANCE_CHANGED).
    if (message?.event_type === 'WALLET_BALANCE_CHANGED' && message.data) {
      setAnalytics(message.data);
    }
  };

  const handleOpen = () => setIsConnected(true);
  const handleClose = () => setIsConnected(false);

  const wsBaseUrl = process.env.NEXT_PUBLIC_WS_URL || (typeof window !== 'undefined' ? `wss://${window.location.host}` : 'wss://localhost');

  useWebSocket(ticketParam ? `${wsBaseUrl}/ws/analytics/${accountId || ''}${ticketParam}` : '', {
    onMessage: handleUpdate,
    onOpen: handleOpen,
    onClose: handleClose,
    enabled: !!accountId && !!ticketParam
  });

  return {
    analytics,
    isConnected
  };
}
