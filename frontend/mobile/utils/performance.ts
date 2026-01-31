/**
 * Performance Monitoring Utilities (P2-C5, P2-C6)
 *
 * Provides utilities for measuring and optimizing React Native app performance:
 * - Component render time measurement
 * - List scroll performance tracking
 * - Memory usage monitoring
 * - Frame rate monitoring
 */

// Performance markers for measuring operation durations
const performanceMarks = new Map<string, number>();

/**
 * Mark the start of a performance measurement
 *
 * @example
 * performanceMarkStart('transaction-list-render');
 * // ... do work ...
 * const duration = performanceMarkEnd('transaction-list-render');
 * console.log(`Render took ${duration}ms`);
 */
export function performanceMarkStart(markName: string): void {
  performanceMarks.set(markName, Date.now());
}

/**
 * Mark the end of a performance measurement and return duration
 *
 * @param markName - The name of the mark to end
 * @returns Duration in milliseconds, or -1 if mark not found
 */
export function performanceMarkEnd(markName: string): number {
  const startTime = performanceMarks.get(markName);
  if (startTime === undefined) {
    console.warn(`Performance mark "${markName}" not found`);
    return -1;
  }

  const duration = Date.now() - startTime;
  performanceMarks.delete(markName);
  return duration;
}

/**
 * Measure the execution time of an async function
 *
 * @example
 * const result = await measurePerformance('fetch-transactions', async () => {
 *   return await api.getTransactions();
 * });
 */
export async function measurePerformance<T>(
  markName: string,
  fn: () => Promise<T>
): Promise<T> {
  performanceMarkStart(markName);
  try {
    return await fn();
  } finally {
    performanceMarkEnd(markName);
  }
}

/**
 * Measure synchronous function execution time
 */
export function measureSyncPerformance<T>(
  markName: string,
  fn: () => T
): T {
  performanceMarkStart(markName);
  try {
    return fn();
  } finally {
    performanceMarkEnd(markName);
  }
}

/**
 * Performance thresholds for different operations (in milliseconds)
 */
export const PERFORMANCE_THRESHOLDS = {
  // UI rendering thresholds
  COMPONENT_RENDER: 16, // 60fps = ~16ms per frame
  LIST_SCROLL: 16,
  ANIMATION_FRAME: 16,

  // API operation thresholds
  API_CALL: 1000,
  SECURE_STORE_READ: 50,
  SECURE_STORE_WRITE: 50,

  // List operation thresholds
  LIST_INITIAL_RENDER: 500,
  LIST_ITEM_RENDER: 10,

  // Navigation thresholds
  NAVIGATION_TRANSITION: 300,
} as const;

/**
 * Check if a performance measurement exceeds threshold
 */
export function isPerformanceSlow(
  markName: string,
  duration: number,
  threshold: keyof typeof PERFORMANCE_THRESHOLDS
): boolean {
  const thresholdMs = PERFORMANCE_THRESHOLDS[threshold];
  const isSlow = duration > thresholdMs;

  if (isSlow) {
    console.warn(
      `[Performance Warning] ${markName} (${duration}ms) exceeded ${threshold} threshold (${thresholdMs}ms)`
    );
  }

  return isSlow;
}

/**
 * Performance report for batch operations
 */
export interface PerformanceReport {
  markName: string;
  duration: number;
  threshold?: keyof typeof PERFORMANCE_THRESHOLDS;
  isSlow: boolean;
}

/**
 * Generate a performance report for a measurement
 */
export function createPerformanceReport(
  markName: string,
  duration: number,
  threshold?: keyof typeof PERFORMANCE_THRESHOLDS
): PerformanceReport {
  const report: PerformanceReport = {
    markName,
    duration,
    isSlow: false,
  };

  if (threshold) {
    report.threshold = threshold;
    report.isSlow = isPerformanceSlow(markName, duration, threshold);
  }

  return report;
}

/**
 * Batch performance measurement for multiple operations
 *
 * @example
 * const reports = await measureBatchPerformance([
 *   ['load-user', () => loadUser()],
 *   ['load-wallet', () => loadWallet()],
 *   ['load-transactions', () => loadTransactions()],
 * ]);
 */
export async function measureBatchPerformance(
  operations: Array<[string, () => Promise<any>]>
): Promise<PerformanceReport[]> {
  const reports: PerformanceReport[] = [];

  for (const [name, fn] of operations) {
    await measurePerformance(name, fn);
    const duration = performanceMarkEnd(name);
    reports.push(createPerformanceReport(name, duration));
  }

  return reports;
}

/**
 * List scroll performance tracker
 * Use this to monitor FlashList/FlatList scroll performance
 */
export class ListScrollTracker {
  private frameCount = 0;
  private lastFrameTime = 0;
  private droppedFrames = 0;
  private isTracking = false;

  startTracking(): void {
    this.isTracking = true;
    this.frameCount = 0;
    this.lastFrameTime = Date.now();
    this.droppedFrames = 0;
  }

  recordFrame(): void {
    if (!this.isTracking) return;

    const now = Date.now();
    const frameDelta = now - this.lastFrameTime;

    // Frame took longer than 16ms (60fps threshold)
    if (frameDelta > 16) {
      this.droppedFrames++;
    }

    this.frameCount++;
    this.lastFrameTime = now;
  }

  stopTracking(): {
    frameCount: number;
    droppedFrames: number;
    dropPercentage: number;
  } {
    this.isTracking = false;
    const dropPercentage =
      this.frameCount > 0 ? (this.droppedFrames / this.frameCount) * 100 : 0;

    const report = {
      frameCount: this.frameCount,
      droppedFrames: this.droppedFrames,
      dropPercentage,
    };

    if (dropPercentage > 10) {
      console.warn(
        `[ListScrollPerformance] ${dropPercentage.toFixed(1)}% frames dropped (${this.droppedFrames}/${this.frameCount})`
      );
    }

    return report;
  }
}

/**
 * Create a new list scroll tracker instance
 */
export function createListScrollTracker(): ListScrollTracker {
  return new ListScrollTracker();
}

/**
 * Memoization performance utilities
 */
export function createMemoizationCache<T extends (...args: any[]) => any>(
  fn: T,
  maxSize: number = 100
): T & { clear: () => void; size: () => number } {
  const cache = new Map<string, ReturnType<T>>();

  const memoized = (...args: Parameters<T>): ReturnType<T> => {
    const key = JSON.stringify(args);

    if (cache.has(key)) {
      return cache.get(key)!;
    }

    const result = fn(...args);
    cache.set(key, result);

    // Enforce max cache size
    if (cache.size > maxSize) {
      const firstKey = cache.keys().next().value;
      cache.delete(firstKey);
    }

    return result;
  };

  (memoized as any).clear = () => cache.clear();
  (memoized as any).size = () => cache.size;

  return memoized as T & { clear: () => void; size: () => number };
}

import * as React from 'react';

/**
 * Component render time measurement hook
 * Use this in React components to track render performance
 */
export function useRenderTime(componentName: string): void {
  const renderCount = React.useRef(0);
  const lastRenderTime = React.useRef<number>(Date.now());

  React.useEffect(() => {
    renderCount.current++;
    const now = Date.now();
    const timeSinceLastRender = now - lastRenderTime.current;
    lastRenderTime.current = now;

    if (timeSinceLastRender > PERFORMANCE_THRESHOLDS.COMPONENT_RENDER) {
      console.warn(
        `[RenderPerformance] ${componentName} render #${renderCount.current} took ${timeSinceLastRender}ms (threshold: ${PERFORMANCE_THRESHOLDS.COMPONENT_RENDER}ms)`
      );
    }
  });
}

/**
 * Performance benchmark result
 */
export interface BenchmarkResult {
  name: string;
  iterations: number;
  totalTime: number;
  avgTime: number;
  minTime: number;
  maxTime: number;
  opsPerSecond: number;
}

/**
 * Run a performance benchmark on a function
 *
 * @example
 * const result = await benchmark('memoized-calculation', () => {
 *   return expensiveCalculation();
 * }, 100);
 * console.log(result);
 */
export async function benchmark<T>(
  name: string,
  fn: () => T,
  iterations: number = 100
): Promise<BenchmarkResult> {
  const times: number[] = [];

  // Warmup
  for (let i = 0; i < Math.min(10, iterations); i++) {
    fn();
  }

  // Actual benchmark
  for (let i = 0; i < iterations; i++) {
    const start = Date.now();
    fn();
    const end = Date.now();
    times.push(end - start);
  }

  const totalTime = times.reduce((sum, time) => sum + time, 0);
  const avgTime = totalTime / iterations;
  const minTime = Math.min(...times);
  const maxTime = Math.max(...times);
  const opsPerSecond = 1000 / avgTime;

  return {
    name,
    iterations,
    totalTime,
    avgTime,
    minTime,
    maxTime,
    opsPerSecond,
  };
}

/**
 * Log a benchmark result in a readable format
 */
export function logBenchmarkResult(result: BenchmarkResult): void {
  console.log(`\n[Benchmark] ${result.name}`);
  console.log(`  Iterations: ${result.iterations}`);
  console.log(`  Total time: ${result.totalTime}ms`);
  console.log(`  Average: ${result.avgTime.toFixed(2)}ms`);
  console.log(`  Min: ${result.minTime}ms`);
  console.log(`  Max: ${result.maxTime}ms`);
  console.log(`  Ops/sec: ${result.opsPerSecond.toFixed(2)}`);
  console.log('');
}
