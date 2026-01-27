'use client';

import { useEffect, useCallback, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import ABTestingService, {
  VariantAssignment,
} from '@/services/ABTestingService';
import { useExperimentContext } from '@/contexts/ExperimentContext';
import { useAuth } from './index';

export interface UseExperimentOptions {
  /**
   * Whether to automatically track impression on mount
   * @default true
   */
  trackImpression?: boolean;

  /**
   * Custom user ID (defaults to authenticated user ID)
   */
  userId?: string;

  /**
   * Custom device ID
   */
  deviceId?: string;

  /**
   * Whether to re-fetch on mount if cached
   * @default false
   */
  refetchOnMount?: boolean;

  /**
   * Callback when variant is assigned
   */
  onVariantAssigned?: (variantKey: string) => void;

  /**
   * Callback when experiment fails to load
   */
  onError?: (error: Error) => void;
}

export interface UseExperimentResult {
  /**
   * The assigned variant key
   */
  variantKey: string | null;

  /**
   * The full variant assignment details
   */
  assignment: VariantAssignment | null;

  /**
   * Whether the experiment is loading
   */
  isLoading: boolean;

  /**
   * Whether there was an error loading the experiment
   */
  isError: boolean;

  /**
   * The error object if there was an error
   */
  error: Error | null;

  /**
   * Track a conversion event for this experiment
   */
  trackConversion: (eventType: string, properties?: Record<string, unknown>) => Promise<void>;

  /**
   * Manually refetch the variant assignment
   */
  refetch: () => void;
}

/**
 * Hook to fetch and use A/B test variant assignments
 *
 * @example
 * ```tsx
 * const { variantKey, isLoading } = useExperiment('checkout_flow_v2');
 *
 * if (isLoading) return <LoadingSpinner />;
 *
 * return (
 *   <div>
 *     {variantKey === 'variant_b' ? <NewCheckout /> : <OldCheckout />}
 *   </div>
 * );
 * ```
 */
export function useExperiment(
  experimentKey: string,
  options: UseExperimentOptions = {}
): UseExperimentResult {
  const {
    trackImpression = true,
    userId: customUserId,
    deviceId,
    refetchOnMount = false,
    onVariantAssigned,
  } = options;

  const { user } = useAuth();
  const userId = customUserId || user?.id;

  const context = useExperimentContext();
  const hasTrackedImpression = useRef(false);

  // Check cache first
  const cachedVariant = ABTestingService.getCachedVariant(experimentKey);
  const contextVariant = context.getVariant(experimentKey);

  // Use cached variant if available
  const initialVariant = contextVariant || cachedVariant;

  // Fetch experiment metadata
  const {
    data: experiment,
    isLoading: isLoadingExperiment,
    isError: isExperimentError,
    error: experimentError,
  } = useQuery({
    queryKey: ['experiment', experimentKey],
    queryFn: () => ABTestingService.getExperimentByKey(experimentKey),
    enabled: !!experimentKey && !!userId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 2,
  });

  // Fetch or create variant assignment
  const {
    data: assignment,
    isLoading: isLoadingAssignment,
    isError: isAssignmentError,
    error: assignmentError,
    refetch,
  } = useQuery({
    queryKey: ['experiment-assignment', experimentKey, userId],
    queryFn: () =>
      ABTestingService.assignVariant(experimentKey, userId!, deviceId),
    enabled: !!experimentKey && !!userId && (!initialVariant || refetchOnMount),
    staleTime: 30 * 60 * 1000, // 30 minutes
    retry: 2,
  });

  const isLoading = isLoadingExperiment || isLoadingAssignment;
  const isError = isExperimentError || isAssignmentError;
  const error = experimentError || assignmentError;

  // Update context with variant
  useEffect(() => {
    if (assignment?.variantKey) {
      context.setVariant(experimentKey, assignment.variantKey);
      onVariantAssigned?.(assignment.variantKey);
    }
  }, [assignment, experimentKey, context, onVariantAssigned]);

  // Track impression on mount (once per session)
  useEffect(() => {
    if (
      !trackImpression ||
      !assignment?.experimentId ||
      !userId ||
      hasTrackedImpression.current
    ) {
      return;
    }

    hasTrackedImpression.current = true;

    // Track impression as a conversion event
    ABTestingService.trackConversion(
      assignment.experimentId,
      userId,
      'impression',
      deviceId,
      {
        variantKey: assignment.variantKey,
        experimentKey,
      }
    ).catch((err) => {
      console.error('Failed to track impression:', err);
    });
  }, [assignment, userId, deviceId, experimentKey, trackImpression]);

  // Track conversion function
  const trackConversion = useCallback(
    async (eventType: string, properties?: Record<string, unknown>) => {
      if (!assignment?.experimentId || !userId) {
        throw new Error('Cannot track conversion: no active assignment');
      }

      return ABTestingService.trackConversion(
        assignment.experimentId,
        userId,
        eventType,
        deviceId,
        properties
      );
    },
    [assignment, userId, deviceId]
  );

  // Determine which variant to use
  const variantKey = assignment?.variantKey || initialVariant || null;

  return {
    variantKey,
    assignment: assignment || null,
    isLoading,
    isError,
    error: error as Error | null,
    trackConversion,
    refetch,
  };
}

export default useExperiment;
