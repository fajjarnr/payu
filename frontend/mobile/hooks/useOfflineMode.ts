import { useEffect, useState } from 'react';
import NetInfo from '@react-native-community/netinfo';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Transaction } from '@/types';

const OFFLINE_QUEUE_KEY = '@payu:offline_queue';
const OFFLINE_CACHE_KEY = '@payu:offline_cache';

interface OfflineQueueItem {
  id: string;
  type: 'transfer' | 'payment' | 'bill_payment';
  data: any;
  timestamp: number;
}

interface OfflineCache {
  transactions: Transaction[];
  balance: number;
  timestamp: number;
}

export const useOfflineMode = () => {
  const [isOnline, setIsOnline] = useState(true);
  const [offlineQueue, setOfflineQueue] = useState<OfflineQueueItem[]>([]);
  const [hasPendingActions, setHasPendingActions] = useState(false);

  useEffect(() => {
    // Subscribe to network status updates
    const unsubscribe = NetInfo.addEventListener((state) => {
      const online = state.isConnected ?? false;
      setIsOnline(online);

      // When coming back online, process offline queue
      if (online && offlineQueue.length > 0) {
        processOfflineQueue();
      }
    });

    // Load existing offline queue
    loadOfflineQueue();

    return () => {
      unsubscribe();
    };
  }, []);

  const loadOfflineQueue = async () => {
    try {
      const queueJson = await AsyncStorage.getItem(OFFLINE_QUEUE_KEY);
      const queue = queueJson ? JSON.parse(queueJson) : [];
      setOfflineQueue(queue);
      setHasPendingActions(queue.length > 0);
    } catch (error) {
      console.error('Failed to load offline queue:', error);
    }
  };

  const saveOfflineQueue = async (queue: OfflineQueueItem[]) => {
    try {
      await AsyncStorage.setItem(OFFLINE_QUEUE_KEY, JSON.stringify(queue));
      setHasPendingActions(queue.length > 0);
    } catch (error) {
      console.error('Failed to save offline queue:', error);
    }
  };

  const addToOfflineQueue = async (
    type: OfflineQueueItem['type'],
    data: any
  ) => {
    const newItem: OfflineQueueItem = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      type,
      data,
      timestamp: Date.now(),
    };

    const updatedQueue = [...offlineQueue, newItem];
    setOfflineQueue(updatedQueue);
    await saveOfflineQueue(updatedQueue);

    return newItem.id;
  };

  const processOfflineQueue = async () => {
    if (!isOnline || offlineQueue.length === 0) return;

    // Process each item in the queue
    // This would integrate with your API services
    for (const item of offlineQueue) {
      try {
        // API call would go here based on item.type
        console.log('Processing offline item:', item);
      } catch (error) {
        console.error('Failed to process offline item:', item, error);
      }
    }

    // Clear processed items
    const updatedQueue = offlineQueue.filter((item) => {
      // Keep items that failed to process
      return false; // For now, clear all
    });

    setOfflineQueue(updatedQueue);
    await saveOfflineQueue(updatedQueue);
  };

  const cacheForOffline = async (key: string, data: any) => {
    try {
      const cacheJson = await AsyncStorage.getItem(OFFLINE_CACHE_KEY);
      const cache: Record<string, any> = cacheJson ? JSON.parse(cacheJson) : {};

      cache[key] = {
        data,
        timestamp: Date.now(),
      };

      await AsyncStorage.setItem(OFFLINE_CACHE_KEY, JSON.stringify(cache));
    } catch (error) {
      console.error('Failed to cache data:', error);
    }
  };

  const getCachedData = async (key: string) => {
    try {
      const cacheJson = await AsyncStorage.getItem(OFFLINE_CACHE_KEY);
      if (!cacheJson) return null;

      const cache: Record<string, any> = JSON.parse(cacheJson);
      const cachedItem = cache[key];

      if (!cachedItem) return null;

      // Check if cache is stale (1 hour)
      const isStale = Date.now() - cachedItem.timestamp > 60 * 60 * 1000;

      return {
        data: cachedItem.data,
        isStale,
      };
    } catch (error) {
      console.error('Failed to get cached data:', error);
      return null;
    }
  };

  const clearOfflineQueue = async () => {
    setOfflineQueue([]);
    await saveOfflineQueue([]);
  };

  return {
    isOnline,
    offlineQueue,
    hasPendingActions,
    addToOfflineQueue,
    processOfflineQueue,
    cacheForOffline,
    getCachedData,
    clearOfflineQueue,
  };
};
