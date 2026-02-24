/**
 * Mutation Configuration Utilities
 *
 * BUG-FE-027 Fix: Centralized mutation retry configuration
 * - Financial mutations: NEVER auto-retry (retry: 0) to prevent double-debit
 * - Non-financial mutations: May retry on transient network errors
 *
 * @see providers.tsx - Global default is retry: 0 for all mutations
 */

import type { UseMutationOptions } from '@tanstack/react-query';

/**
 * Default retry delay calculation with exponential backoff
 * Max delay capped at 30 seconds
 */
export const defaultRetryDelay = (attemptIndex: number): number => {
  return Math.min(1000 * 2 ** attemptIndex, 30000);
};

/**
 * Mutation configuration for FINANCIAL operations
 * These mutations must NEVER auto-retry to prevent double-debit issues
 *
 * Examples: transfers, payments, wallet credits, loan applications,
 * reservation commits, escrow operations
 */
export const financialMutationConfig = {
  retry: 0,
  // Financial mutations should fail fast - no retry
} as const;

/**
 * Mutation configuration for NON-FINANCIAL operations
 * These mutations may safely retry on transient network errors
 *
 * Examples: user profile updates, notification reads, settings changes,
 * support tickets, KYC document uploads
 */
export const nonFinancialMutationConfig = {
  retry: 1,
  retryDelay: defaultRetryDelay,
} as const;

/**
 * Mutation configuration for READ-ONLY operations that use mutation
 * These are safe to retry as they don't modify financial state
 *
 * Examples: pre-approval checks, validation calls, lookup operations
 */
export const readOnlyMutationConfig = {
  retry: 2,
  retryDelay: defaultRetryDelay,
} as const;

/**
 * Type helper for creating typed mutation options
 */
export type MutationConfigType =
  | typeof financialMutationConfig
  | typeof nonFinancialMutationConfig
  | typeof readOnlyMutationConfig;

/**
 * Helper to merge mutation configs with custom options
 */
export function createMutationOptions<
  TData = unknown,
  TError = Error,
  TVariables = void,
  TContext = unknown,
>(
  config: MutationConfigType,
  options?: Partial<UseMutationOptions<TData, TError, TVariables, TContext>>
): UseMutationOptions<TData, TError, TVariables, TContext> {
  return {
    ...config,
    ...options,
  } as UseMutationOptions<TData, TError, TVariables, TContext>;
}

/**
 * Pre-configured mutation options for common use cases
 */
export const MutationPresets = {
  /**
   * Use for: Transfers, payments, wallet operations, reservations
   * Risk: Double-debit if retried
   */
  financial: financialMutationConfig,

  /**
   * Use for: Profile updates, preferences, non-critical updates
   * Risk: Low - safe to retry
   */
  nonFinancial: nonFinancialMutationConfig,

  /**
   * Use for: Validation checks, lookups, idempotent reads
   * Risk: None - pure read operations
   */
  readOnly: readOnlyMutationConfig,
} as const;
