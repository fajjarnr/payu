/** Performance monitoring: render timing, list scroll tracking, and benchmarks (P2-C5, P2-C6). */

const performanceMarks = new Map<string, number>();

export function performanceMarkStart(markName: string): void {
  performanceMarks.set(markName, Date.now());
}

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

export const PERFORMANCE_THRESHOLDS = {
  COMPONENT_RENDER: 16, // 60fps = ~16ms per frame
  LIST_SCROLL: 16,
  ANIMATION_FRAME: 16,

  API_CALL: 1000,
  SECURE_STORE_READ: 50,
  SECURE_STORE_WRITE: 50,

  LIST_INITIAL_RENDER: 500,
  LIST_ITEM_RENDER: 10,

  NAVIGATION_TRANSITION: 300,
} as const;

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

export interface PerformanceReport {
  markName: string;
  duration: number;
  threshold?: keyof typeof PERFORMANCE_THRESHOLDS;
  isSlow: boolean;
}

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

/** Tracks dropped frames during list scroll (FlashList/FlatList). */
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

export function createListScrollTracker(): ListScrollTracker {
  return new ListScrollTracker();
}

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

export interface BenchmarkResult {
  name: string;
  iterations: number;
  totalTime: number;
  avgTime: number;
  minTime: number;
  maxTime: number;
  opsPerSecond: number;
}

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
