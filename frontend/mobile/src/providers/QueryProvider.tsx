/**
 * QueryProvider - React Query Configuration for PayU Mobile App
 *
 * SECURITY POLICY: Cache Persistence Strategy
 * ================================================
 *
 * This provider configures React Query with selective persistence to AsyncStorage.
 * Sensitive data (financial, PII, auth) is EXCLUDED from AsyncStorage persistence
 * because AsyncStorage is NOT encrypted. Only non-sensitive reference data is persisted.
 *
 * Storage Security Levels:
 * ------------------------
 * 1. SecureStore (encrypted):
 *    - Auth tokens (payu_auth_tokens)
 *    - User data (payu_user)
 *    - Sensitive credentials (PIN, biometrics)
 *
 * 2. Memory-only (no persistence):
 *    - Wallet balances (SENSITIVE - financial data)
 *    - Transaction history (SENSITIVE - financial data)
 *    - User profile (SENSITIVE - PII)
 *    - Card details (SENSITIVE - financial data)
 *    - Auth session data (SENSITIVE - credentials)
 *
 * 3. AsyncStorage (unencrypted, limited persistence):
 *    - Bank lists (non-sensitive reference data)
 *    - Feature flags (non-sensitive)
 *    - UI preferences (non-sensitive)
 *    - Currency lists (non-sensitive reference data)
 *
 * Why This Approach?
 * ------------------
 * - AsyncStorage stores data in plaintext on device file system
 * - On rooted/jailbroken devices, AsyncStorage can be easily accessed
 * - Financial data in local storage violates PCI-DSS and OJK compliance
 * - Memory-only cache is cleared on app close (security best practice)
 *
 * Compliance References:
 * ----------------------
 * - PCI-DSS Requirement 3: Protect stored cardholder data
 * - OJK Regulation: Financial data encryption at rest
 * - PayU Security Policy P2-C2: Secure token storage
 *
 * @module QueryProvider
 * @version 2.0.0 - Security-hardened persistence
 */

import React, { ReactNode, useEffect, useState } from 'react';
import { QueryClient, QueryCache, MutationCache } from '@tanstack/react-query';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';
import { onlineManager, focusManager } from '@tanstack/react-query';
import { AppState, AppStateStatus, Platform } from 'react-native';
import { storage } from '@/utils/storage';
import { AUTH_CONFIG } from '@/constants/config';

// Online manager setup with NetInfo
onlineManager.setEventListener((setOnline) => {
  return NetInfo.addEventListener((state) => {
    setOnline(!!state.isConnected);
  });
});

// Focus manager for React Native
function onAppStateChange(status: AppStateStatus) {
  if (Platform.OS !== 'web') {
    focusManager.setFocused(status === 'active');
  }
}

// Create async storage persister
const asyncStoragePersister = createAsyncStoragePersister({
  storage: AsyncStorage,
  key: 'payu-query-cache',
  throttleTime: 1000,
  serialize: (data) => JSON.stringify(data),
  deserialize: (data) => JSON.parse(data),
});

// Sensitive query keys that must NOT be persisted to AsyncStorage (unencrypted storage)
// These contain financial/PII/auth data that should only exist in memory or SecureStore
const SENSITIVE_QUERY_KEYS = [
  // AUTHENTICATION - Never persist auth data (tokens in SecureStore only)
  'auth',        // Auth tokens and session data (MUST be in SecureStore only)
  'login',       // Login-related queries
  'register',    // Registration-related queries
  'token',       // Token-related queries
  'session',     // Session data
  'logout',      // Logout operations

  // FINANCIAL DATA - Never persist financial data
  'wallet',      // Wallet balances, pocket balances (financial data)
  'wallets',     // List of wallets with balances (financial data)
  'transactions', // Transaction history with amounts and recipients (financial data)
  'transaction', // Individual transaction details (financial data)
  'cards',       // Card numbers and details (financial data)
  'transfer',    // Transfer-related queries (financial data)
  'topup',       // Top-up related queries (financial data)
  'qris',        // QRIS payment data (financial data)
  'payment',     // Payment-related queries
  'balance',     // Balance queries

  // PII DATA - Never persist personal information
  'user',        // User PII (personal data)
  'profile',     // User profile data (personal data)
  'kyc',         // KYC verification data
  'identity',    // Identity documents
  'document',    // Document data

  // SECURITY DATA
  'pin',         // PIN-related data
  'password',    // Password-related data
  'biometric',   // Biometric data
  'security',    // Security settings
] as const;

// Non-sensitive query keys that CAN be persisted to AsyncStorage for offline performance
const NON_SENSITIVE_QUERY_KEYS = [
  'banks',        // Bank list (static reference data)
  'promo',        // Promotional content (public data)
  'features',     // Feature flags (public data)
  'settings',     // UI settings (non-sensitive preferences)
  'currencies',   // Currency list (static reference data)
  'categories',   // Transaction categories (static reference data)
] as const;

// Persist config - excludes sensitive data from AsyncStorage
const persistConfig = {
  buster: 'v2', // Cache version - increment to invalidate all caches
  maxAge: 1000 * 60 * 60 * 24 * 7, // 7 days
  persister: asyncStoragePersister,
  dehydrateOptions: {
    shouldDehydrateQuery: (query: any) => {
      const queryKeyString = query.queryKey[0]?.toString().toLowerCase() || '';

      // SECURITY: Exclude sensitive queries from AsyncStorage persistence
      // Sensitive data (financial, PII, auth) must only exist in memory or SecureStore
      const isSensitive = SENSITIVE_QUERY_KEYS.some((key) =>
        queryKeyString.includes(key.toLowerCase())
      );
      if (isSensitive) {
        return false; // Do NOT persist to AsyncStorage
      }

      // Only persist non-sensitive queries for offline performance
      const isNonSensitive = NON_SENSITIVE_QUERY_KEYS.some((key) =>
        queryKeyString.includes(key.toLowerCase())
      );

      return isNonSensitive;
    },
  },
};

interface QueryProviderProps {
  children: ReactNode;
}

export function QueryProvider({ children }: QueryProviderProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        queryCache: new QueryCache({
          onError: async (error: any, query) => {
            console.error(`Query error for [${query.queryKey}]:`, error);

            // Handle 401 errors globally
            if (error?.response?.status === 401) {
              await storage.remove(AUTH_CONFIG.TOKEN_KEY);
              await storage.remove(AUTH_CONFIG.USER_KEY);
              // Auth context will handle navigation
            }
          },
        }),
        mutationCache: new MutationCache({
          onError: (error: any, _variables, _context, mutation) => {
            console.error(
              `Mutation error for [${mutation.options.mutationKey}]:`,
              error
            );
          },
        }),
        defaultOptions: {
          queries: {
            // Offline-first configuration
            networkMode: 'offlineFirst',
            // Retry configuration
            retry: (failureCount, error: any) => {
              // Don't retry on 4xx errors (client errors)
              if (error?.response?.status >= 400 && error?.response?.status < 500) {
                return false;
              }
              // Retry up to 3 times for other errors
              return failureCount < 3;
            },
            retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
            // Stale time configuration
            staleTime: 1000 * 60 * 5, // 5 minutes
            // Cache time
            gcTime: 1000 * 60 * 60 * 24, // 24 hours
            // Refetch configuration
            refetchOnWindowFocus: false,
            refetchOnReconnect: true,
            refetchOnMount: 'always',
            // Error handling
            throwOnError: false,
          },
          mutations: {
            // Retry mutations only for network errors
            retry: (failureCount, error: any) => {
              // Only retry on network errors (no response)
              if (!error?.response && failureCount < 2) {
                return true;
              }
              return false;
            },
            networkMode: 'offlineFirst',
          },
        },
      })
  );

  useEffect(() => {
    // Subscribe to app state changes for focus management
    const subscription = AppState.addEventListener('change', onAppStateChange);

    return () => {
      subscription.remove();
    };
  }, []);

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={persistConfig}
      onSuccess={() => {
        console.log('Query cache restored successfully');
      }}
    >
      {children}
    </PersistQueryClientProvider>
  );
}

// Export a function to get queryClient instance for use outside React components
export function getQueryClientInstance() {
  // This is a placeholder - in practice, you should use the queryClient from the component
  throw new Error('Use useQueryClient hook inside React components');
}

/**
 * Security Verification Utility
 * ===============================
 *
 * Use this function in development/testing to verify that NO sensitive data
 * is being persisted to AsyncStorage.
 *
 * @example
 * ```ts
 * import { verifyAsyncStorageSecurity } from '@/providers/QueryProvider';
 *
 * // Run in development or after app initialization
 * if (__DEV__) {
 *   verifyAsyncStorageSecurity().then(report => {
 *     console.log('Security Report:', report);
 *   });
 * }
 * ```
 */
export async function verifyAsyncStorageSecurity(): Promise<{
  isSecure: boolean;
  violations: string[];
  persistedKeys: string[];
}> {
  const violations: string[] = [];
  const persistedKeys: string[] = [];

  try {
    // Get all keys from AsyncStorage
    const keys = await AsyncStorage.getAllKeys();

    // Check React Query cache specifically
    const queryCacheKey = 'payu-query-cache';
    if (keys.includes(queryCacheKey)) {
      const cacheData = await AsyncStorage.getItem(queryCacheKey);

      if (cacheData) {
        try {
          const parsedCache = JSON.parse(cacheData);
          const queries = parsedCache?.clientState?.queries || [];

          queries.forEach((query: any) => { // eslint-disable-line @typescript-eslint/no-explicit-any
            const queryKey = query?.queryKey?.[0]?.toString().toLowerCase() || '';
            persistedKeys.push(queryKey);

            // Check if sensitive data is persisted
            const isSensitive = SENSITIVE_QUERY_KEYS.some((key) =>
              queryKey.includes(key.toLowerCase())
            );

            if (isSensitive) {
              violations.push(
                `SECURITY VIOLATION: Sensitive query "${queryKey}" found in AsyncStorage!`
              );
            }
          });
        } catch (e) {
          violations.push('Failed to parse query cache for security check');
        }
      }
    }

    // Check for any other suspicious keys
    const suspiciousKeys = keys.filter((key) =>
      SENSITIVE_QUERY_KEYS.some((sensitive) =>
        key.toLowerCase().includes(sensitive.toLowerCase())
      )
    );

    suspiciousKeys.forEach((key) => {
      if (!violations.includes(`Suspicious key: ${key}`)) {
        violations.push(`Suspicious key in AsyncStorage: ${key}`);
      }
    });

    return {
      isSecure: violations.length === 0,
      violations,
      persistedKeys,
    };
  } catch (error) {
    return {
      isSecure: false,
      violations: [`Security check failed: ${error}`],
      persistedKeys: [],
    };
  }
}

/**
 * Development-only hook to log AsyncStorage contents for security auditing
 * WARNING: Only use in development - NEVER call in production
 */
export async function devLogAsyncStorageContents(): Promise<void> {
  if (!__DEV__) {
    console.warn('devLogAsyncStorageContents should only be called in development');
    return;
  }

  try {
    const keys = await AsyncStorage.getAllKeys();
    console.log('=== AsyncStorage Security Audit ===');
    console.log('Total keys:', keys.length);

    for (const key of keys) {
      const value = await AsyncStorage.getItem(key);
      const preview = value?.substring(0, 100);
      console.log(`Key: ${key} | Preview: ${preview}...`);
    }

    const securityReport = await verifyAsyncStorageSecurity();
    console.log('Security Report:', securityReport);

    if (!securityReport.isSecure) {
      console.error('SECURITY VIOLATIONS DETECTED:', securityReport.violations);
    } else {
      console.log('No security violations detected - AsyncStorage is clean');
    }
  } catch (error) {
    console.error('Failed to audit AsyncStorage:', error);
  }
}
