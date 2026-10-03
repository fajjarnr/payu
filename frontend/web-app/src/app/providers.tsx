'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { ReactNode, useState } from 'react';
const createQueryClient = () => new QueryClient({
 defaultOptions: {
  queries: {
   staleTime: 1000 * 60 * 1,
   gcTime: 1000 * 60 * 10,
   // BUG-FE-026: Enable auto-refresh on reconnect/focus so balance stays fresh
   refetchOnWindowFocus: true,
   refetchOnReconnect: true,
   retry: (failureCount, error) => {
    // RATELOOP-001: never retry 4xx (429/validation/auth) — the server already
    // decided. Retry only transient 5xx/network failures, max once.
    if (isAxiosError(error)) {
      const status = error.response?.status;
      if (status !== undefined && status < 500) return false;
    }
    return failureCount < 1;
   },
   retryDelay: (attemptIndex) => {
    return Math.min(1000 * 2 ** attemptIndex, 30000);
   },
  },
  mutations: {
   // BUG-FE-027: Default retry 0 for all mutations to prevent double-debit
   // Financial mutations must never auto-retry on network errors
   retry: 0,
  },
 },
});

let browserQueryClient: QueryClient | undefined = undefined;

const getQueryClient = () => {
 if (typeof window === 'undefined') {
  return createQueryClient();
 }
 if (!browserQueryClient) {
  browserQueryClient = createQueryClient();
 }
 return browserQueryClient;
};

import { AntdProvider } from '@/components/AntdProvider';
import { useSilentRefresh } from '@/hooks/useSilentRefresh';
import { SessionBootstrap } from '@/components/SessionBootstrap';
import { useEffect } from 'react';
import { useAuthStore, useUIStore } from '@/stores';
import { createLocaleHref } from '@/lib/navigation';
import { useLocale } from 'next-intl';

function SilentRefreshRunner() {
  useSilentRefresh();
  return null;
}

/**
 * BUG-FE-010 FIX: Handle auth:session-expired events from the API interceptor
 * using Next.js router instead of hard window.location.href redirect.
 */
function AuthSessionExpiredHandler() {
  const logout = useAuthStore((state) => state.logout);
  const locale = useLocale();

  useEffect(() => {
    const handler = () => {
      logout();
      // Use replace to avoid polluting browser history
      window.location.replace(createLocaleHref('/login', locale));
    };
    window.addEventListener('auth:session-expired', handler);
    return () => window.removeEventListener('auth:session-expired', handler);
  }, [logout, locale]);

  return null;
}

/**
 * Bridges uiStore theme + next-intl into the antd ConfigProvider,
 * and syncs the `dark` class on <html> so Tailwind `dark:` variants work.
 */
function AntdShell({ children }: { children: ReactNode }) {
  const theme = useUIStore((s) => s.theme);
  const locale = useLocale();

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }, [theme]);

  return (
    <AntdProvider locale={locale} isDark={theme === 'dark'}>
      {children}
    </AntdProvider>
  );
}

export default function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => getQueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      <SessionBootstrap />
      <SilentRefreshRunner />
      <AuthSessionExpiredHandler />
      <AntdShell>{children}</AntdShell>
    </QueryClientProvider>
  );
}
