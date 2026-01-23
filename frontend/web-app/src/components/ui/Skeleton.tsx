'use client';

import React from 'react';
import clsx from 'clsx';

interface SkeletonProps {
  className?: string;
}

export const Skeleton = ({ className }: SkeletonProps) => (
  <div
    className={clsx(
      'animate-pulse bg-gray-100 dark:bg-gray-800 rounded',
      className
    )}
  />
);

export const SkeletonCard = () => (
  <div className="bg-card p-8 rounded-[2.5rem] border border-border shadow-sm space-y-6">
    <div className="flex justify-between items-start">
      <Skeleton className="h-12 w-12 rounded-xl" />
      <Skeleton className="h-8 w-20 rounded-full" />
    </div>
    <div className="space-y-3">
      <Skeleton className="h-3 w-32 rounded" />
      <Skeleton className="h-8 w-40 rounded" />
    </div>
  </div>
);

export const SkeletonText = ({ lines = 3, className }: { lines?: number; className?: string }) => (
  <div className={clsx('space-y-2', className)}>
    {Array.from({ length: lines }).map((_, i) => (
      <Skeleton
        key={i}
        className={clsx('h-4 rounded', i === lines - 1 ? 'w-3/4' : 'w-full')}
      />
    ))}
  </div>
);

export const SkeletonTransaction = () => (
  <div className="flex items-center justify-between p-4 space-x-4">
    <div className="flex items-center gap-4">
      <Skeleton className="h-12 w-12 rounded-2xl" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-32 rounded" />
        <Skeleton className="h-3 w-24 rounded" />
      </div>
    </div>
    <Skeleton className="h-6 w-20 rounded" />
  </div>
);

export const SkeletonChart = () => (
  <div className="h-[300px] flex items-end justify-between gap-3 px-4">
    {Array.from({ length: 12 }).map((_, i) => (
      <div key={i} className="flex-1 space-y-2">
        <Skeleton className="w-full h-full rounded-t-xl" />
      </div>
    ))}
  </div>
);

export const SkeletonBalance = () => (
  <div className="space-y-4">
    <div className="flex items-center gap-2 mb-2">
      <div className="h-2 w-2 bg-gray-300 dark:bg-gray-600 rounded-full animate-pulse" />
      <Skeleton className="h-3 w-24 rounded" />
    </div>
    <Skeleton className="h-16 w-64 rounded" />
  </div>
);

export const SkeletonStatsGrid = ({ count = 4 }: { count?: number }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
    {Array.from({ length: count }).map((_, i) => (
      <SkeletonCard key={i} />
    ))}
  </div>
);
