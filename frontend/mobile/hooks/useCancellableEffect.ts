import { useEffect, useRef } from 'react';

/**
 * Custom hook for cancellable async operations in useEffect.
 * Prevents memory leaks by cancelling pending async operations on unmount.
 *
 * @param effect Async function to execute
 * @param deps Dependencies array
 *
 * @example
 * ```tsx
 * useCancellableEffect(async (isCancelled) => {
 *   const data = await fetchData();
 *   if (!isCancelled()) {
 *     setState(data);
 *   }
 * }, [dependency]);
 * ```
 */
export function useCancellableEffect(
  effect: (isCancelled: () => boolean) => Promise<void> | void,
  deps: React.DependencyList = []
) {
  const isCancelledRef = useRef(false);

  useEffect(() => {
    isCancelledRef.current = false;

    const cleanupOrPromise = effect(() => isCancelledRef.current);

    return () => {
      isCancelledRef.current = true;
      // Handle potential cleanup function returned by effect
      if (cleanupOrPromise && typeof cleanupOrPromise === 'function') {
        (cleanupOrPromise as () => void)();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}

/**
 * Custom hook for managing AbortController for fetch requests.
 * Automatically aborts requests on unmount.
 *
 * @returns AbortController and signal
 *
 * @example
 * ```tsx
 * const { abortController } = useAbortController();
 *
 * useEffect(() => {
 *   const fetchData = async () => {
 *     try {
 *       const response = await fetch(url, {
 *         signal: abortController.signal
 *       });
 *       // Handle response
 *     } catch (error) {
 *       if (error.name !== 'AbortError') {
 *         // Handle error
 *       }
 *     }
 *   };
 *
 *   fetchData();
 * }, []);
 * ```
 */
export function useAbortController() {
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    // Create new AbortController on mount
    abortControllerRef.current = new AbortController();

    return () => {
      // Abort on unmount
      abortControllerRef.current?.abort();
    };
  }, []);

  return {
    abortController: abortControllerRef.current,
    signal: abortControllerRef.current?.signal,
    abort: () => abortControllerRef.current?.abort(),
  };
}

/**
 * Custom hook for cancellable promise with timeout.
 *
 * @param promiseFn Function that returns a promise
 * @param timeout Timeout in milliseconds (default: 30000)
 * @returns Object with data, error, loading states
 *
 * @example
 * ```tsx
 * const { data, error, loading } = useCancellablePromise(
 *   () => apiService.getData(),
 *   10000
 * );
 * ```
 */
export function useCancellablePromise<T>(
  promiseFn: (signal?: AbortSignal) => Promise<T>,
  timeout: number = 30000
) {
  const [state, setState] = React.useState<{
    data: T | null;
    error: Error | null;
    loading: boolean;
  }>({
    data: null,
    error: null,
    loading: true,
  });

  const isMountedRef = useRef(true);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    isMountedRef.current = false;
    abortControllerRef.current?.abort();
  }, []);

  const execute = React.useCallback(async () => {
    setState({ data: null, error: null, loading: true });
    isMountedRef.current = true;

    // Create new AbortController for this request
    abortControllerRef.current = new AbortController();

    // Set timeout
    const timeoutId = setTimeout(() => {
      abortControllerRef.current?.abort();
    }, timeout);

    try {
      const result = await promiseFn(abortControllerRef.current.signal);
      clearTimeout(timeoutId);

      if (isMountedRef.current) {
        setState({ data: result, error: null, loading: false });
      }
    } catch (error) {
      clearTimeout(timeoutId);

      if (isMountedRef.current && error instanceof Error) {
        // Don't set error if it's an abort
        if (error.name !== 'AbortError') {
          setState({ data: null, error, loading: false });
        }
      }
    }
  }, [promiseFn, timeout]);

  // Retry function
  const retry = React.useCallback(() => {
    execute();
  }, [execute]);

  // Cancel function
  const cancel = React.useCallback(() => {
    abortControllerRef.current?.abort();
  }, []);

  return {
    ...state,
    execute,
    retry,
    cancel,
  };
}

import React from 'react';
