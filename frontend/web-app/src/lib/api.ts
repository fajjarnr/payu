import axios, { isAxiosError } from 'axios';
import { toast } from 'sonner';

/**
 * PayU API Client — Secure BFF Proxy
 *
 * All requests go through the Next.js BFF proxy at /api/v1/[...path],
 * which reads the httpOnly cookie and attaches the Authorization header
 * server-side.  JWT tokens are NEVER accessible to client JavaScript.
 *
 * Auth flow (login/logout/refresh) uses separate BFF routes at /api/auth/*.
 */
const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // include httpOnly cookies in every request
});

// Export isAxiosError for type checking
export { isAxiosError };

// ── Response unwrapper: auto-extract ApiResponse.data wrapper ───────
// BUG-CROSS-003: Backend wraps responses in ApiResponse<T> = { success, data, message }.
// Frontend services expect response.data to be T directly, not the wrapper.
// This interceptor transparently unwraps so `response.data` always contains
// the inner payload regardless of whether the backend wraps it.
api.interceptors.response.use((response) => {
  const body = response.data;
  if (
    body &&
    typeof body === 'object' &&
    !Array.isArray(body) &&
    'success' in body &&
    'data' in body
  ) {
    response.data = body.data;
  }
  return response;
});

// ── 401 interceptor: transparent token refresh via BFF ──────────────
// BUG-FE-009 FIX: Encapsulated token refresh state to prevent global mutation issues
class TokenRefreshManager {
  private isRefreshing = false;
  private failedQueue: Array<{
    resolve: (value: unknown) => void;
    reject: (reason?: unknown) => void;
  }> = [];

  processQueue(error: unknown) {
    this.failedQueue.forEach(({ resolve, reject }) => {
      if (error) {
        reject(error);
      } else {
        resolve(undefined);
      }
    });
    this.failedQueue = [];
  }

  getIsRefreshing() { return this.isRefreshing; }
  setIsRefreshing(value: boolean) { this.isRefreshing = value; }

  addToQueue(resolve: (value: unknown) => void, reject: (reason?: unknown) => void) {
    this.failedQueue.push({ resolve, reject });
  }
}

const tokenRefreshManager = new TokenRefreshManager();

// Track retried requests to prevent infinite retry loops
const retriedRequests = new WeakSet<object>();

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !retriedRequests.has(originalRequest)) {
      // Queue concurrent requests while a refresh is in-flight
      if (tokenRefreshManager.getIsRefreshing()) {
        return new Promise((resolve, reject) => {
          tokenRefreshManager.addToQueue(resolve, reject);
        }).then(() => api(originalRequest));
      }

      retriedRequests.add(originalRequest);
      tokenRefreshManager.setIsRefreshing(true);

      try {
        // Ask BFF to rotate tokens (cookie → cookie, no JS exposure)
        const refreshRes = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
        });

        if (!refreshRes.ok) throw new Error('Refresh failed');

        tokenRefreshManager.processQueue(null);
        return api(originalRequest); // retry with fresh cookie
      } catch (refreshError) {
        tokenRefreshManager.processQueue(refreshError);
        if (typeof window !== 'undefined') {
          // Extract locale from current URL path for locale-aware redirect
          const pathLocale = window.location.pathname.match(/^\/(en|id)(\/|$)/);
          const locale = pathLocale ? pathLocale[1] : 'id';
          // BUG-FE-010 FIX: Dispatch event for graceful handling, with fallback redirect
          window.dispatchEvent(new CustomEvent('auth:session-expired', { detail: { locale } }));
          // Fallback: hard redirect if event is not handled within 100ms
          setTimeout(() => {
            // BUG-FE-010 FIX: Navigation handled by auth store, not hard redirect
          }, 100);
        }
        return Promise.reject(refreshError);
      } finally {
        tokenRefreshManager.setIsRefreshing(false);
      }
    }

    // RATELOOP-001: Never auto-retry 429. The server is explicitly asking us
    // to slow down; client-side retries (axios x3 + React Query) amplified one
    // throttled poll into a request storm with toast spam. Surface once, let
    // the query layer decide (4xx is not retried, see providers.tsx).
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers?.['retry-after'];
      const retrySeconds = retryAfter ? Math.max(1, Math.ceil(parseInt(retryAfter, 10) || 1)) : 1;
      toast.error(`Terlalu banyak permintaan, coba lagi dalam ${retrySeconds} detik`, {
        duration: 5000,
      });
    }

    return Promise.reject(error);
  },
);

export default api;
