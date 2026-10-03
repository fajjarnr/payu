/** Cancels pending requests on unmount or app background to prevent memory leaks. */

import { apiClientInstance } from '@/services/api';
import { Logger } from '@/utils/logger';

// Track active abort controllers for cleanup
const activeControllers = new Set<AbortController>();

// Track mounted components
const mountedComponents = new WeakMap<object, boolean>();

export function createTrackedController(): AbortController {
  const controller = new AbortController();
  activeControllers.add(controller);
  return controller;
}

export function cleanupController(controller: AbortController): void {
  controller.abort();
  activeControllers.delete(controller);
}

export function cancelAllPendingRequests(): void {
  activeControllers.forEach((controller) => {
    controller.abort();
  });
  activeControllers.clear();

  // Also cancel via API client
  try {
    apiClientInstance.cancelAllRequests();
  } catch (error) {
    Logger.warn('RequestCleanup', 'Failed to cancel API client requests', { error });
  }
}

export function createMountTracker() {
  let mounted = true;

  return {
    isMounted: () => mounted,
    mount: () => { mounted = true; },
    unmount: () => { mounted = false; },
  };
}

export function useMountRef(): React.MutableRefObject<boolean> {
  const ref = React.useRef(true);
  return ref;
}

import React from 'react';

export function useCleanupOnUnmount(cleanupFn: () => void): void {
  React.useEffect(() => {
    return cleanupFn;
  }, [cleanupFn]);
}

export function useCancelOnBackground(cancelOnBackground: boolean = true): void {
  React.useEffect(() => {
    if (!cancelOnBackground) return;

    const subscription = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'background') {
        cancelAllPendingRequests();
      }
    });

    return () => {
      subscription.remove();
    };
  }, [cancelOnBackground]);
}

import { AppState } from 'react-native';

export function createDebounceable<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): T & { cancel: () => void } {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  const debounced = (...args: Parameters<T>) => {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }

    timeoutId = setTimeout(() => {
      fn(...args);
    }, delay);
  };

  (debounced as any).cancel = () => {
    if (timeoutId) {
      clearTimeout(timeoutId);
      timeoutId = null;
    }
  };

  return debounced as T & { cancel: () => void };
}

export function useDebouncedCallback<T extends (...args: any[]) => any>(
  fn: T,
  delay: number,
  deps: React.DependencyList = []
): T & { cancel: () => void } {
  const debouncedRef = React.useRef(createDebounceable(fn, delay));

  // Update the debounced function when deps change
  React.useEffect(() => {
    debouncedRef.current = createDebounceable(fn, delay);
    return () => {
      debouncedRef.current.cancel();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fn, delay, ...deps]);

  return debouncedRef.current;
}

export function createThrottleable<T extends (...args: any[]) => any>(
  fn: T,
  limit: number
): T & { cancel: () => void } {
  let inThrottle = false;
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  const throttled = (...args: Parameters<T>) => {
    if (!inThrottle) {
      fn(...args);
      inThrottle = true;

      timeoutId = setTimeout(() => {
        inThrottle = false;
      }, limit);
    }
  };

  (throttled as any).cancel = () => {
    if (timeoutId) {
      clearTimeout(timeoutId);
      timeoutId = null;
    }
    inThrottle = false;
  };

  return throttled as T & { cancel: () => void };
}

export function useThrottledCallback<T extends (...args: any[]) => any>(
  fn: T,
  limit: number,
  deps: React.DependencyList = []
): T & { cancel: () => void } {
  const throttledRef = React.useRef(createThrottleable(fn, limit));

  React.useEffect(() => {
    throttledRef.current = createThrottleable(fn, limit);
    return () => {
      throttledRef.current.cancel();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fn, limit, ...deps]);

  return throttledRef.current;
}
