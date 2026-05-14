'use client';

import { useEffect, useRef } from 'react';
import { useAuthStore } from '@/stores';

/**
 * SessionBootstrap — Reconciles server-side cookie session with client-side Zustand store.
 *
 * BUG-CROSS-035 FIX:
 * When a user returns to the app with a valid refresh cookie but an empty/stale Zustand store,
 * the middleware lets them through (cookie-based), but all client components render as
 * unauthenticated. This component detects the mismatch and triggers a refresh to populate
 * the store.
 *
 * BUG-FE-012 FIX:
 * By deferring store reads to useEffect (client-only), we avoid hydration mismatches between
 * the server render (no localStorage) and client render (with localStorage data).
 */
export function SessionBootstrap() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);
  const accountId = useAuthStore((state) => state.accountId);
  const { setAuth, setTokenExpiry, setAuthenticated } = useAuthStore();
  const bootstrapAttempted = useRef(false);

  useEffect(() => {
    // Only run once on mount
    if (bootstrapAttempted.current) return;

    // If the store already has auth data, nothing to do
    if (isAuthenticated && user && accountId) return;

    bootstrapAttempted.current = true;

    // Attempt to validate the existing cookie session via the BFF refresh endpoint
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
