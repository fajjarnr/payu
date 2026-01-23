import { useState } from 'react';
import { useWebSocket } from './useWebSocket';
import type { AnalyticsData } from '@/types';

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

  useWebSocket(`${process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080'}/ws/analytics/${accountId || ''}`, {
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
