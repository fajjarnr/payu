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
  // BUG-AUTH-001: Shared lock to prevent concurrent refresh calls
  const isRefreshingRef = useRef(false);
  // BUG-AUTH-003: Use ref for isAuthenticated to avoid stale closures
  const isAuthenticatedRef = useRef(isAuthenticated);
  useEffect(() => { isAuthenticatedRef.current = isAuthenticated; }, [isAuthenticated]);

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
    // BUG-AUTH-001: Prevent two concurrent refresh requests
    if (isRefreshingRef.current) return false;
    isRefreshingRef.current = true;
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      });

      if (!res.ok) {
        // refreshToken is expired — real session end
        // BUG-AUTH-016 FIX: Only logout on 401, not on 503/network errors
        if (res.status === 401) { logout(); }
        return false;
      }

      const data: { success: boolean; expiresIn?: number } = await res.json();
      const expiresIn = data.expiresIn ?? 900; // seconds
      setAuthenticated(true);
      setTokenExpiry(Date.now() + expiresIn * 1000);
      return true;
    } catch (err) {
      console.error('[useSilentRefresh] Token refresh network error:', err);
      return false;
    } finally {
      isRefreshingRef.current = false;
    }
  }, [logout, setAuthenticated, setTokenExpiry]);

  // BUG-AUTH-004: Retry counter for exponential backoff on failure
  const retryAttemptsRef = useRef(0);
  const MAX_RETRY_ATTEMPTS = 5;

  /** Schedule the next proactive refresh */
  const scheduleRefresh = useCallback((expiresAt: number | null) => {
    clearTimer();

    if (!isAuthenticatedRef.current) return;

    const now = Date.now();
    const effectiveExpiry = expiresAt ?? now + DEFAULT_TOKEN_LIFETIME_MS;
    const delay = Math.max(effectiveExpiry - now - REFRESH_MARGIN_MS, 0);

    timerRef.current = setTimeout(async () => {
      const success = await doRefresh();
      if (success) {
        retryAttemptsRef.current = 0;
      } else if (isAuthenticatedRef.current && retryAttemptsRef.current < MAX_RETRY_ATTEMPTS) {
        // BUG-AUTH-004: Exponential backoff retry (2s, 4s, 8s, 16s, 32s)
        const backoffMs = Math.min(2000 * Math.pow(2, retryAttemptsRef.current), 32000);
        retryAttemptsRef.current += 1;
        timerRef.current = setTimeout(() => scheduleRefresh(null), backoffMs);
      }
    }, delay);
  }, [clearTimer, doRefresh]);

  // Schedule/reschedule whenever tokenExpiresAt or isAuthenticated changes
  useEffect(() => {
    // BUG-AUTH-002: Immediate refresh on mount if authenticated but tokenExpiresAt is null
    if (isAuthenticated && tokenExpiresAt === null) {
      doRefresh();
      return;
    }
    scheduleRefresh(tokenExpiresAt);
    return clearTimer;
  }, [tokenExpiresAt, isAuthenticated, scheduleRefresh, clearTimer, doRefresh]);

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
