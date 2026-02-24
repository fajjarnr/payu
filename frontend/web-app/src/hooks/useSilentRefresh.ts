'use client';

import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '@/stores';

/**
 * Proactive / Silent Token Refresh
 * ==================================
 * Schedules an automatic token refresh BEFORE the accessToken cookie expires,
 * ensuring the user is never kicked to the login page mid-session.
 *
 * Strategy:
 * - Refresh 2 minutes before the token expires (i.e., at t = expiresAt - 120s)
 * - Fall back to refreshing at 80% of the token's lifetime if expiresAt is unknown
 * - On browser tab focus (visibilitychange), check immediately if token is
 *   about to expire (< 3 minutes remaining) and refresh eagerly
 * - On refresh success, reschedule the next refresh based on the new expiresIn
 *   returned by the BFF (the store will be updated via useRefreshToken)
 *
 * Security note:
 * - We NEVER read or store the actual token (httpOnly cookie = invisible to JS)
 * - We only use the timestamp stored in Zustand (set during login/refresh) to
 *   know when to trigger the /api/auth/refresh BFF call
 *
 * References:
 * - OWASP ASVS 2.7.3: Silent re-authentication
 * - PCI-DSS 8.3.9: Session idle timeout handling
 */

// Refresh this many ms before the token actually expires
const REFRESH_MARGIN_MS = 2 * 60 * 1000; // 2 minutes
// If we don't know expiry, use this as default token lifetime
const DEFAULT_TOKEN_LIFETIME_MS = 15 * 60 * 1000; // 15 minutes
// Minimum time remaining before an eager refresh on tab focus
const EAGER_REFRESH_THRESHOLD_MS = 3 * 60 * 1000; // 3 minutes

export function useSilentRefresh() {
  const tokenExpiresAt = useAuthStore((state) => state.tokenExpiresAt);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const { setAuthenticated, setTokenExpiry, logout } = useAuthStore();

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  /**
   * Call the BFF refresh endpoint, update store, and reschedule.
   * Returns true on success, false on failure (session expired).
   */
  const doRefresh = useCallback(async (): Promise<boolean> => {
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      });

      if (!res.ok) {
        // refreshToken is expired — real session end
        logout();
        return false;
      }

      const data: { success: boolean; expiresIn?: number } = await res.json();
      const expiresIn = data.expiresIn ?? 900; // seconds
      setAuthenticated(true);
      setTokenExpiry(Date.now() + expiresIn * 1000);
      return true;
    } catch {
      // Network error — don't log out, retry will happen on next schedule
      return false;
    }
  }, [logout, setAuthenticated, setTokenExpiry]);

  /** Schedule the next proactive refresh */
  const scheduleRefresh = useCallback((expiresAt: number | null) => {
    clearTimer();

    if (!isAuthenticated) return;

    const now = Date.now();
    const effectiveExpiry = expiresAt ?? now + DEFAULT_TOKEN_LIFETIME_MS;
    const delay = Math.max(effectiveExpiry - now - REFRESH_MARGIN_MS, 0);

    timerRef.current = setTimeout(async () => {
      await doRefresh();
      // After refresh, the store's tokenExpiresAt will have been updated by
      // setTokenExpiry above \u2014 the useEffect watching tokenExpiresAt will
      // automatically reschedule the next refresh.
    }, delay);
  }, [clearTimer, doRefresh, isAuthenticated]);

  // Schedule/reschedule whenever tokenExpiresAt or isAuthenticated changes
  useEffect(() => {
    scheduleRefresh(tokenExpiresAt);
    return clearTimer;
  }, [tokenExpiresAt, isAuthenticated, scheduleRefresh, clearTimer]);

  // Eager refresh when the user returns to the tab
  useEffect(() => {
    if (typeof document === 'undefined') return;

    const handleVisibilityChange = async () => {
      if (document.visibilityState !== 'visible' || !isAuthenticated) return;

      const now = Date.now();
      const expiresAt = tokenExpiresAt ?? now + DEFAULT_TOKEN_LIFETIME_MS;
      const remaining = expiresAt - now;

      if (remaining < EAGER_REFRESH_THRESHOLD_MS) {
        // Token is about to expire \u2014 refresh now instead of waiting for the timer
        clearTimer();
        await doRefresh();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [isAuthenticated, tokenExpiresAt, clearTimer, doRefresh]);
}
