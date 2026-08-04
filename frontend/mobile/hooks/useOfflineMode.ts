import { useEffect, useState, useRef } from 'react';
import NetInfo from '@react-native-community/netinfo';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Transaction, TransferData, TopUpData, QRISPaymentData } from '@/types';
import { transactionService } from '@/services/transaction.service';
import { Logger } from '@/utils/logger';
import { generateIdempotencyKey, saveIdempotencyKey, removeIdempotencyKey } from '@/utils/idempotency';

const OFFLINE_QUEUE_KEY = '@payu:offline_queue';
const OFFLINE_CACHE_KEY = '@payu:offline_cache';
const QUEUE_VERSION_KEY = '@payu:offline_queue_version';

/**
 * Conflict resolution strategies
 */
export enum ConflictResolution {
  LAST_WRITE_WINS = 'last-write-wins',
  VERSION_BASED = 'version-based',
  MANUAL = 'manual',
}

/**
 * Offline queue item with enhanced metadata
 */
interface OfflineQueueItem {
  id: string;
  type: 'transfer' | 'topup' | 'qris';
  data: any;
  timestamp: number;
  idempotencyKey: string;
  retryCount: number;
  status: 'pending' | 'processing' | 'failed' | 'conflict';
  conflictData?: {
    localVersion: number;
    serverVersion?: number;
    reason: string;
  };
}

/**
 * Offline cache item
 */
interface OfflineCacheItem<T> {
  data: T;
  timestamp: number;
  version: number;
}

/**
 * Processing result for queue items
 */
interface QueueProcessResult {
  success: boolean;
  itemId: string;
  error?: string;
  conflict?: boolean;
}

/**
 * Enhanced offline mode hook with queue processing and conflict resolution
 *
 * Features:
 * - Automatic offline queue processing when connection restored
 * - Idempotency key integration for safe retries
 * - Conflict resolution strategies
 * - Configurable retry behavior
 */
export const useOfflineMode = (
  options?: {
    conflictResolution?: ConflictResolution;
    maxRetries?: number;
    autoProcess?: boolean;
  }
) => {
  const {
    conflictResolution = ConflictResolution.LAST_WRITE_WINS,
    maxRetries = 3,
    autoProcess = true,
  } = options || {};

  const [isOnline, setIsOnline] = useState(true);
  const [offlineQueue, setOfflineQueue] = useState<OfflineQueueItem[]>([]);
  const [hasPendingActions, setHasPendingActions] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  // Track processing to prevent duplicate processing
  const processingRef = useRef(false);
  const queueVersionRef = useRef(0);

  useEffect(() => {
    // Subscribe to network status updates
    const unsubscribe = NetInfo.addEventListener((state) => {
      const online = state.isConnected ?? false;
      setIsOnline(online);

      Logger.info('OfflineMode', `Network status changed: ${online ? 'online' : 'offline'}`);

      // When coming back online, process offline queue
      if (online && autoProcess && offlineQueue.length > 0 && !processingRef.current) {
        processOfflineQueue();
      }
    });

    // Load existing offline queue
    loadOfflineQueue();

    // Cleanup old queue items on mount
    cleanupOldQueueItems();

    return () => {
      unsubscribe();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * Load offline queue from persistent storage
   */
  const loadOfflineQueue = async () => {
    try {
      const queueJson = await AsyncStorage.getItem(OFFLINE_QUEUE_KEY);
      const versionJson = await AsyncStorage.getItem(QUEUE_VERSION_KEY);

      const queue = queueJson ? JSON.parse(queueJson) : [];
      const version = versionJson ? parseInt(versionJson, 10) : 0;

      setOfflineQueue(queue);
      setHasPendingActions(queue.filter((item: OfflineQueueItem) => item.status === 'pending').length > 0);
      queueVersionRef.current = version;

      Logger.debug('OfflineMode', `Loaded ${queue.length} items from offline queue`);
    } catch (error) {
      Logger.error('OfflineMode', 'Failed to load offline queue', error);
    }
  };

  /**
   * Save offline queue to persistent storage
   */
  const saveOfflineQueue = async (queue: OfflineQueueItem[]) => {
    try {
      await AsyncStorage.setItem(OFFLINE_QUEUE_KEY, JSON.stringify(queue));
      queueVersionRef.current += 1;
      await AsyncStorage.setItem(QUEUE_VERSION_KEY, queueVersionRef.current.toString());

      setHasPendingActions(queue.filter((item) => item.status === 'pending').length > 0);
    } catch (error) {
      Logger.error('OfflineMode', 'Failed to save offline queue', error);
    }
  };

  /**
   * Clean up old queue items (older than 7 days)
   */
  const cleanupOldQueueItems = async () => {
    try {
      const queueJson = await AsyncStorage.getItem(OFFLINE_QUEUE_KEY);
      if (!queueJson) return;

      const queue: OfflineQueueItem[] = JSON.parse(queueJson);
      const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;

      const validItems = queue.filter((item) => item.timestamp > sevenDaysAgo);

      if (validItems.length < queue.length) {
        await saveOfflineQueue(validItems);
        Logger.info('OfflineMode', `Cleaned up ${queue.length - validItems.length} old queue items`);
      }
    } catch (error) {
      Logger.error('OfflineMode', 'Failed to cleanup old queue items', error);
    }
  };

  /**
   * Add an item to the offline queue
   */
  const addToOfflineQueue = async (
    type: OfflineQueueItem['type'],
    data: TransferData | TopUpData | QRISPaymentData,
    userId?: string
  ): Promise<string> => {
    const idempotencyKey = generateIdempotencyKey(type, userId);

    const newItem: OfflineQueueItem = {
      id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      type,
      data,
      timestamp: Date.now(),
      idempotencyKey,
      retryCount: 0,
      status: 'pending',
    };

    // Save idempotency key for recovery
    await saveIdempotencyKey(idempotencyKey, type, userId);

    const updatedQueue = [...offlineQueue, newItem];
    setOfflineQueue(updatedQueue);
    await saveOfflineQueue(updatedQueue);

    Logger.info('OfflineMode', `Added ${type} to offline queue`, {
      itemId: newItem.id,
      idempotencyKey: idempotencyKey.substring(0, 20) + '...',
    });

    return newItem.id;
  };

  /**
   * Process a single queue item
   */
  const processQueueItem = async (item: OfflineQueueItem): Promise<QueueProcessResult> => {
    const { id, type, data, idempotencyKey, retryCount } = item;

    try {
      Logger.info('OfflineMode', `Processing ${type} from queue`, {
        itemId: id,
        retryCount,
      });

      let result: Transaction;

      switch (type) {
        case 'transfer':
          result = await transactionService.transfer({
            ...data,
            idempotencyKey,
          });
          break;

        case 'topup':
          result = await transactionService.topUp({
            ...data,
            idempotencyKey,
          });
          break;

        case 'qris':
          result = await transactionService.payQRIS({
            ...data,
            idempotencyKey,
          });
          break;

        default:
          throw new Error(`Unknown queue item type: ${type}`);
      }

      // Remove idempotency key after successful processing
      await removeIdempotencyKey(idempotencyKey);

      Logger.info('OfflineMode', `Successfully processed ${type} from queue`, {
        itemId: id,
        transactionId: result.id,
      });

      return {
        success: true,
        itemId: id,
      };
    } catch (error) {
      Logger.error('OfflineMode', `Failed to process ${type} from queue`, error, {
        itemId: id,
        retryCount,
      });

      // Check if this is a conflict error
      const isConflict = (error as any).response?.status === 409;

      if (isConflict) {
        return {
          success: false,
          itemId: id,
          error: 'Conflict detected',
          conflict: true,
        };
      }

      return {
        success: false,
        itemId: id,
        error: (error as Error).message,
      };
    }
  };

  /**
   * Resolve a conflict in the queue
   */
  const resolveConflict = async (
    itemId: string,
    resolution: 'local' | 'server' | 'abort'
  ): Promise<void> => {
    const queue = [...offlineQueue];
    const itemIndex = queue.findIndex((item) => item.id === itemId);

    if (itemIndex === -1) {
      Logger.warn('OfflineMode', `Cannot resolve conflict: item ${itemId} not found`);
      return;
    }

    const item = queue[itemIndex];

    if (resolution === 'abort') {
      // Remove item from queue
      queue.splice(itemIndex, 1);
      await removeIdempotencyKey(item.idempotencyKey);
    } else if (resolution === 'local') {
      // Retry with local data (last-write-wins)
      item.status = 'pending';
      item.retryCount = 0;
    } else if (resolution === 'server') {
      // Accept server version, remove from queue
      queue.splice(itemIndex, 1);
      await removeIdempotencyKey(item.idempotencyKey);
    }

    await saveOfflineQueue(queue);
    setOfflineQueue(queue);

    Logger.info('OfflineMode', `Conflict resolved for item ${itemId}: ${resolution}`);
  };

  /**
   * Process all pending items in the offline queue
   */
  const processOfflineQueue = async (): Promise<QueueProcessResult[]> => {
    // Prevent duplicate processing
    if (processingRef.current || isProcessing) {
      Logger.debug('OfflineMode', 'Queue processing already in progress');
      return [];
    }

    const pendingItems = offlineQueue.filter((item) => item.status === 'pending');

    if (pendingItems.length === 0) {
      Logger.debug('OfflineMode', 'No pending items to process');
      return [];
    }

    processingRef.current = true;
    setIsProcessing(true);

    Logger.info('OfflineMode', `Starting to process ${pendingItems.length} queue items`);

    const results: QueueProcessResult[] = [];
    const updatedQueue = [...offlineQueue];

    // Process items one by one
    for (const item of pendingItems) {
      const itemIndex = updatedQueue.findIndex((i) => i.id === item.id);

      if (itemIndex === -1) continue;

      // Mark as processing
      updatedQueue[itemIndex].status = 'processing';
      await saveOfflineQueue(updatedQueue);
      setOfflineQueue([...updatedQueue]);

      // Process the item
      const result = await processQueueItem(item);

      if (result.success) {
        // Remove from queue
        updatedQueue.splice(itemIndex, 1);
      } else if (result.conflict) {
        // Mark as conflicted
        updatedQueue[itemIndex].status = 'conflict';
        updatedQueue[itemIndex].conflictData = {
          localVersion: item.retryCount + 1,
          reason: result.error || 'Unknown conflict',
        };

        // Check if we should auto-resolve based on strategy
        if (conflictResolution === ConflictResolution.LAST_WRITE_WINS) {
          updatedQueue[itemIndex].status = 'pending';
          updatedQueue[itemIndex].retryCount += 1;

          // Check if max retries exceeded
          if (updatedQueue[itemIndex].retryCount >= maxRetries) {
            Logger.warn('OfflineMode', `Max retries exceeded for item ${item.id}`);
            // Keep as failed for manual intervention
            updatedQueue[itemIndex].status = 'failed';
          }
        }
      } else {
        // Processing failed, update retry count
        updatedQueue[itemIndex].status = 'pending';
        updatedQueue[itemIndex].retryCount += 1;

        // Check if max retries exceeded
        if (updatedQueue[itemIndex].retryCount >= maxRetries) {
          Logger.warn('OfflineMode', `Max retries exceeded for item ${item.id}`);
          updatedQueue[itemIndex].status = 'failed';
        }
      }

      results.push(result);
    }

    await saveOfflineQueue(updatedQueue);
    setOfflineQueue(updatedQueue);

    processingRef.current = false;
    setIsProcessing(false);

    const successCount = results.filter((r) => r.success).length;
    const failureCount = results.length - successCount;

    Logger.info('OfflineMode', `Queue processing complete: ${successCount} succeeded, ${failureCount} failed`);

    return results;
  };

  /**
   * Retry a specific failed queue item
   */
  const retryQueueItem = async (itemId: string): Promise<QueueProcessResult | null> => {
    const queue = [...offlineQueue];
    const itemIndex = queue.findIndex((item) => item.id === itemId);

    if (itemIndex === -1) {
      Logger.warn('OfflineMode', `Cannot retry: item ${itemId} not found`);
      return null;
    }

    const item = queue[itemIndex];
    item.status = 'pending';
    item.retryCount = 0;

    await saveOfflineQueue(queue);
    setOfflineQueue(queue);

    // Process this item immediately
    const result = await processQueueItem(item);

    if (result.success) {
      queue.splice(itemIndex, 1);
    } else {
      item.status = 'failed';
      item.retryCount += 1;
    }

    await saveOfflineQueue(queue);
    setOfflineQueue(queue);

    return result;
  };

  /**
   * Cache data for offline use
   */
  const cacheForOffline = async <T>(key: string, data: T): Promise<void> => {
    try {
      const cacheJson = await AsyncStorage.getItem(OFFLINE_CACHE_KEY);
      const cache: Record<string, OfflineCacheItem<T>> = cacheJson ? JSON.parse(cacheJson) : {};

      cache[key] = {
        data,
        timestamp: Date.now(),
        version: Date.now(),
      };

      await AsyncStorage.setItem(OFFLINE_CACHE_KEY, JSON.stringify(cache));

      Logger.debug('OfflineMode', `Cached data for key: ${key}`);
    } catch (error) {
      Logger.error('OfflineMode', 'Failed to cache data', error, { key });
    }
  };

  /**
   * Get cached data
   */
  const getCachedData = async <T>(key: string): Promise<OfflineCacheItem<T> | null> => {
    try {
      const cacheJson = await AsyncStorage.getItem(OFFLINE_CACHE_KEY);
      if (!cacheJson) return null;

      const cache: Record<string, OfflineCacheItem<T>> = JSON.parse(cacheJson);
      const cachedItem = cache[key];

      if (!cachedItem) return null;

      // Check if cache is stale (1 hour)
      const isStale = Date.now() - cachedItem.timestamp > 60 * 60 * 1000;

      if (isStale) {
        Logger.debug('OfflineMode', `Cached data is stale for key: ${key}`);
      }

      return cachedItem;
    } catch (error) {
      Logger.error('OfflineMode', 'Failed to get cached data', error, { key });
      return null;
    }
  };

  /**
   * Invalidate cached data
   */
  const invalidateCache = async (key?: string): Promise<void> => {
    try {
      if (key) {
        const cacheJson = await AsyncStorage.getItem(OFFLINE_CACHE_KEY);
        if (!cacheJson) return;

        const cache = JSON.parse(cacheJson);
        delete cache[key];

        await AsyncStorage.setItem(OFFLINE_CACHE_KEY, JSON.stringify(cache));

        Logger.debug('OfflineMode', `Invalidated cache for key: ${key}`);
      } else {
        await AsyncStorage.removeItem(OFFLINE_CACHE_KEY);
        Logger.debug('OfflineMode', 'Invalidated all cache');
      }
    } catch (error) {
      Logger.error('OfflineMode', 'Failed to invalidate cache', error, { key });
    }
  };

  /**
   * Clear the offline queue
   */
  const clearOfflineQueue = async (): Promise<void> => {
    setOfflineQueue([]);
    await saveOfflineQueue([]);

    // Clear all idempotency keys
    const { clearAllIdempotencyKeys } = await import('@/utils/idempotency');
    await clearAllIdempotencyKeys();

    Logger.info('OfflineMode', 'Offline queue cleared');
  };

  /**
   * Remove a specific item from the queue
   */
  const removeQueueItem = async (itemId: string): Promise<void> => {
    const queue = offlineQueue.filter((item) => item.id !== itemId);
    const removedItem = offlineQueue.find((item) => item.id === itemId);

    if (removedItem) {
      await removeIdempotencyKey(removedItem.idempotencyKey);
    }

    setOfflineQueue(queue);
    await saveOfflineQueue(queue);

    Logger.info('OfflineMode', `Removed item ${itemId} from queue`);
  };

  return {
    isOnline,
    offlineQueue,
    hasPendingActions,
    isProcessing,
    addToOfflineQueue,
    processOfflineQueue,
    retryQueueItem,
    resolveConflict,
    cacheForOffline,
    getCachedData,
    invalidateCache,
    clearOfflineQueue,
    removeQueueItem,
  };
};
