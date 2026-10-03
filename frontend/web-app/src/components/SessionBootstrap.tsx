'use client';

import { useEffect, useRef } from 'react';
import { useAuthStore } from '@/stores';

/**
 * Reconciles server-side cookie session with client-side Zustand store.
 *
 * BUG-CROSS-035: Detects mismatch when cookie session is valid but store is empty/stale.
 * BUG-FE-012: Defers store reads to useEffect to avoid hydration mismatches.
 */
export function SessionBootstrap() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);
  const accountId = useAuthStore((state) => state.accountId);
  const { setAuth, setTokenExpiry, setAuthenticated } = useAuthStore();
  const bootstrapAttempted = useRef(false);

  useEffect(() => {
    if (bootstrapAttempted.current) return;
    if (isAuthenticated && user && accountId) return;

    bootstrapAttempted.current = true;
    // Validate existing cookie session via BFF refresh endpoint
    // This is a lightweight check: if cookies are valid, we get a new token + user data
    const bootstrapSession = async () => {
      try {
        const res = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
        });

        if (res.ok) {
          const data = await res.json();
          const expiresIn = data.expiresIn ?? 900;
          setTokenExpiry(Date.now() + expiresIn * 1000);

          if (data.user) {
            const user = data.user;
            setAuth(user, user.accountId || user.id);
          } else {
            setAuthenticated(true);
          }
        }
        // If refresh fails (401/503), we don't have a valid session — leave store as-is
      } catch (err) {
        console.error('[SessionBootstrap] Session bootstrap failed:', err);
      }
    };

    bootstrapSession();
  }, [isAuthenticated, user, accountId, setAuth, setTokenExpiry, setAuthenticated]);

  return null;
}
