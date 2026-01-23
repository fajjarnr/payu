import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '@/stores';
import type { PortfolioUpdate } from '@/types';

export interface UseWebSocketOptions {
  onMessage?: (data: PortfolioUpdate) => void;
  onError?: (error: Event) => void;
  onClose?: (event: CloseEvent) => void;
  onOpen?: (event: Event) => void;
  enabled?: boolean;
}

export function useWebSocket(url: string, options: UseWebSocketOptions = {}) {
  const { onMessage, onError, onClose, onOpen, enabled = true } = options;
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout>();
  const token = useAuthStore((state) => state.token);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
  }, []);

  const connect = useCallback(() => {
    if (!enabled || !token) return;

    const wsUrl = `${url}?token=${token}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = (event) => {
      console.log('WebSocket connected');
      onOpen?.(event);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage?.(data);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      onError?.(error);
    };

    ws.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason);
      onClose?.(event);

      if (event.code !== 1000 && enabled) {
        reconnectTimeoutRef.current = setTimeout(() => {
          if (enabled && token) {
            const newWs = new WebSocket(`${url}?token=${token}`);
            newWs.onopen = ws.onopen;
            newWs.onmessage = ws.onmessage;
            newWs.onerror = ws.onerror;
            newWs.onclose = ws.onclose;
            wsRef.current = newWs;
          }
        }, 3000);
      }
    };
  }, [url, token, enabled, onMessage, onError, onClose, onOpen]);

  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    ws: null as unknown as WebSocket,
    disconnect,
    connect
  };
}
