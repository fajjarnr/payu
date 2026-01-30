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

// Persist config for specific queries
const persistConfig = {
  buster: 'v1', // Cache version - increment to invalidate all caches
  maxAge: 1000 * 60 * 60 * 24 * 7, // 7 days
  persister: asyncStoragePersister,
  dehydrateOptions: {
    shouldDehydrateQuery: (query) => {
      // Only persist specific query types
      const persistableQueries = [
        'wallet',
        'wallets',
        'transactions',
        'cards',
        'user',
        'profile',
      ];
      return persistableQueries.some((key) =>
        query.queryKey[0]?.toString().startsWith(key)
      );
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
