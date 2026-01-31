/**
 * Request Cleanup Utilities
 *
 * Provides utilities for managing and cancelling pending requests
 * to prevent memory leaks when components unmount or app goes to background.
 */

import { apiClientInstance } from '@/services/api';

// Track active abort controllers for cleanup
const activeControllers = new Set<AbortController>();

// Track mounted components
const mountedComponents = new WeakMap<object, boolean>();

/**
 * Create a tracked abort controller that will be automatically cleaned up
 *
 * @returns AbortController instance
 *
 * @example
 * ```tsx
 * useEffect(() => {
 *   const controller = createTrackedController();
 *   const signal = controller.signal;
 *
 *   fetch(url, { signal }).then(data => {
 *     if (!isComponentMounted()) return;
 *     setState(data);
 *   });
 *
 *   return () => cleanupController(controller);
 * }, []);
 * ```
 */
export function createTrackedController(): AbortController {
  const controller = new AbortController();
  activeControllers.add(controller);
  return controller;
}

/**
 * Cleanup a specific abort controller
 *
 * @param controller - The controller to cleanup
 */
export function cleanupController(controller: AbortController): void {
  controller.abort();
  activeControllers.delete(controller);
}

/**
 * Cancel all pending requests
 * Useful for app background, logout, or cleanup scenarios
 *
 * @example
 * ```tsx
 * // App state listener
 * AppState.addEventListener('change', (nextAppState) => {
 *   if (nextAppState === 'background') {
 *     cancelAllPendingRequests();
 *   }
 * });
 * ```
 */
export function cancelAllPendingRequests(): void {
  activeControllers.forEach((controller) => {
    controller.abort();
  });
  activeControllers.clear();

  // Also cancel via API client
  try {
    apiClientInstance.cancelAllRequests();
  } catch (error) {
    console.warn('Failed to cancel API client requests:', error);
  }
}

/**
 * Create a component mount tracker
 * Returns functions to check and update mount status
 *
 * @returns Object with isMounted, mount, and unmount functions
 *
 * @example
 * ```tsx
 * const { isMounted, mount, unmount } = createMountTracker();
 *
 * useEffect(() => {
 *   mount();
 *
 *   const fetchData = async () => {
 *     const data = await api.fetch();
 *     if (!isMounted()) return; // Don't update if unmounted
 *     setState(data);
 *   };
 *
 *   fetchData();
 *   return unmount;
 * }, []);
 * ```
 */
export function createMountTracker() {
  let mounted = true;

  return {
    isMounted: () => mounted,
    mount: () => { mounted = true; },
    unmount: () => { mounted = false; },
  };
}

/**
 * Create a ref-based mount tracker for use in components
 *
 * @returns React ref object with current property
 *
 * @example
 * ```tsx
 * const isMountedRef = useMountRef();
 *
 * useEffect(() => {
 *   return () => {
 *     isMountedRef.current = false;
 *   };
 * }, []);
 *
 * const fetchData = async () => {
 *   const data = await api.fetch();
 *   if (!isMountedRef.current) return;
 *   setState(data);
 * };
 * ```
 */
export function useMountRef(): React.MutableRefObject<boolean> {
  const ref = React.useRef(true);
  return ref;
}

import React from 'react';

/**
 * Hook to setup cleanup on component unmount
 *
 * @param cleanupFn - Function to call on unmount
 *
 * @example
 * ```tsx
 * useCleanupOnUnmount(() => {
 *   // Cancel any pending operations
 *   controller.abort();
 * });
 * ```
 */
export function useCleanupOnUnmount(cleanupFn: () => void): void {
  React.useEffect(() => {
    return cleanupFn;
  }, [cleanupFn]);
}

/**
 * Hook to setup request cancellation on app background
 *
 * @param cancelOnBackground - Whether to cancel on background (default: true)
 *
 * @example
 * ```tsx
 * useCancelOnBackground();
 * ```
 */
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

/**
 * Debounce function that can be cancelled
 *
 * @param fn - Function to debounce
 * @param delay - Delay in milliseconds
 * @returns Debounced function with cancel method
 *
 * @example
 * ```tsx
 * const debouncedSearch = useDebounceable((query) => {
 *   searchAPI(query);
 * }, 500);
 *
 * // In component
 * onChangeText={(text) => debouncedSearch(text)}
 *
 * // Cleanup
 * useEffect(() => {
 *   return () => debouncedSearch.cancel();
 * }, []);
 * ```
 */
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

/**
 * Hook for creating a debounced callback that auto-cancels on unmount
 */
export function useDebouncedCallback<T extends (...args: any[]) => any>(
  fn: T,
  delay: number,
  deps: React.DependencyList = []
): T & { cancel: () => void } {
  const debouncedRef = React.useRef(createDebounceable(fn, delay));

  // Update the debounced function when deps change
  React.useEffect(() => {
    debouncedRef.current = createDebounceable(fn, delay);
    // Cancel on unmount
    return () => {
      debouncedRef.current.cancel();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fn, delay, ...deps]);

  return debouncedRef.current;
}

/**
 * Throttle function that can be cancelled
 *
 * @param fn - Function to throttle
 * @param limit - Time limit in milliseconds
 * @returns Throttled function with cancel method
 */
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

/**
 * Hook for creating a throttled callback that auto-cancels on unmount
 */
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
