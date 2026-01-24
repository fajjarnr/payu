'use client';

import React, { ReactNode, useEffect } from 'react';
import { useExperiment } from '@/hooks/useExperiment';

export interface FeatureFlagProps {
  /**
   * The experiment key (used as feature flag)
   */
  experimentKey: string;

  /**
   * The variant key that enables the feature
   */
  enabledVariant: string;

  /**
   * Children to render when feature is enabled
   */
  children: ReactNode;

  /**
   * Fallback to render when feature is disabled
   */
  fallback?: ReactNode;

  /**
   * Whether to track impressions
   */
  trackImpression?: boolean;

  /**
   * Callback when feature is accessed (enabled or disabled)
   */
  onAccess?: (enabled: boolean, variantKey: string | null) => void;
}

/**
 * Feature flag wrapper component using A/B testing infrastructure
 *
 * @example
 * ```tsx
 * <FeatureFlag
 *   experimentKey="new_dashboard_enabled"
 *   enabledVariant="enabled"
 *   fallback={<OldDashboard />}
 * >
 *   <NewDashboard />
 * </FeatureFlag>
 * ```
 */
export function FeatureFlag({
  experimentKey,
  enabledVariant,
  children,
  fallback = null,
  trackImpression = true,
  onAccess,
}: FeatureFlagProps) {
  const { variantKey, isLoading } = useExperiment(experimentKey, {
    trackImpression,
  });

  useEffect(() => {
    if (!isLoading && onAccess) {
      const enabled = variantKey === enabledVariant;
      onAccess(enabled, variantKey);
    }
  }, [variantKey, isLoading, onAccess]);

  if (isLoading) {
    return <>{fallback}</>;
  }

  const isEnabled = variantKey === enabledVariant;

  return <>{isEnabled ? children : fallback}</>;
}

export default FeatureFlag;
