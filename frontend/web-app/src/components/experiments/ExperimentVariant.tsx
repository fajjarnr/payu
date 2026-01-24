'use client';

import React, { ReactNode } from 'react';
import { useExperiment } from '@/hooks/useExperiment';

export interface ExperimentVariantProps {
  /**
   * The experiment key to fetch variant for
   */
  experimentKey: string;

  /**
   * Mapping of variant keys to React components
   */
  children: Record<string, ReactNode>;

  /**
   * Fallback component to show while loading
   */
  fallback?: ReactNode;

  /**
   * Fallback component if experiment fails to load
   */
  errorFallback?: ReactNode;

  /**
   * Default variant to use if none assigned
   */
  defaultVariant?: string;

  /**
   * Options for the useExperiment hook
   */
  experimentOptions?: Parameters<typeof useExperiment>[1];
}

/**
 * Component to render different UI based on A/B test variant
 *
 * @example
 * ```tsx
 * <ExperimentVariant experimentKey="checkout_flow_v2">
 *   {{ control: <OldCheckout />, variant_b: <NewCheckout /> }}
 * </ExperimentVariant>
 * ```
 */
export function ExperimentVariant({
  experimentKey,
  children,
  fallback = null,
  errorFallback = null,
  defaultVariant,
  experimentOptions,
}: ExperimentVariantProps) {
  const { variantKey, isLoading, isError } = useExperiment(
    experimentKey,
    experimentOptions
  );

  if (isLoading) {
    return <>{fallback}</>;
  }

  if (isError) {
    return <>{errorFallback}</>;
  }

  const effectiveVariant = variantKey || defaultVariant;
  const content = effectiveVariant ? children[effectiveVariant] : null;

  return <>{content || children.control || fallback}</>;
}

export default ExperimentVariant;
