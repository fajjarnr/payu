'use client';

import { useEffect, useRef, useCallback } from 'react';
import { useIsAuthenticated } from '@/stores';
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
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | undefined>(undefined);
  const reconnectAttemptsRef = useRef(0);
  const isAuthenticated = useIsAuthenticated();

  // Store callbacks in refs to avoid bloated dependencies in connect (BUG-FE-034)
  const callbacksRef = useRef({ onMessage, onError, onClose, onOpen });
  useEffect(() => {
    callbacksRef.current = { onMessage, onError, onClose, onOpen };
  }, [onMessage, onError, onClose, onOpen]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (wsRef.current) {
      // 1000 indicates normal closure
      wsRef.current.close(1000, 'User intentional disconnect');
      wsRef.current = null;
    }
  }, []);

  const connect = useCallback(() => {
    if (!enabled || !isAuthenticated) return;

    if (wsRef.current) {
      wsRef.current.close(1000, 'Reconnecting');
    }
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    // WebSocket connection uses httpOnly cookie for authentication
    // The browser automatically includes cookies with the WebSocket request
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = (event) => {
      console.log('WebSocket connected');
      reconnectAttemptsRef.current = 0; // reset attempts
      callbacksRef.current.onOpen?.(event);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        callbacksRef.current.onMessage?.(data);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      callbacksRef.current.onError?.(error);
    };

    ws.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason);
      callbacksRef.current.onClose?.(event);
      
      // Prevent reconnecting if explicitly closed with 1000
      if (event.code !== 1000 && enabled && isAuthenticated) {
        const attempts = reconnectAttemptsRef.current;
        const maxRetries = 10;
        
        if (attempts >= maxRetries) {
          console.warn('Max WebSocket reconnect attempts reached');
          return;
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s... max 30s
        const backoffMs = Math.min(1000 * Math.pow(2, attempts), 30000);
        reconnectAttemptsRef.current = attempts + 1;

        // Call connect directly to get fresh closures and handlers (BUG-FE-030)
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log(`Reconnecting WebSocket after ${backoffMs}ms...`);
          connect();
        }, backoffMs);
      }
    };
  }, [url, isAuthenticated, enabled]);

  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    // Return a getter to always reflect the current underlying websocket (BUG-FE-031)
    get ws() { return wsRef.current; },
    disconnect,
    connect
  };
}
