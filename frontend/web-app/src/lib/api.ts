import axios, { isAxiosError, type InternalAxiosRequestConfig } from 'axios';
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

// IMP-004: Rate Limit Handling — Track retry state per request
interface RateLimitState {
  retryCount: number;
  maxRetries: number;
  baseDelay: number;
}

const rateLimitStates = new WeakMap<InternalAxiosRequestConfig, RateLimitState>();

// Get or initialize rate limit state for a request
const getRateLimitState = (config: InternalAxiosRequestConfig): RateLimitState => {
  if (!rateLimitStates.has(config)) {
    rateLimitStates.set(config, {
      retryCount: 0,
      maxRetries: 3,
      baseDelay: 1000, // 1 second base delay
    });
  }
  return rateLimitStates.get(config)!;
};

// Export isAxiosError for type checking
export { isAxiosError };

// ── Request interceptor: Initialize rate limit tracking ─────────────
api.interceptors.request.use((config) => {
  // Initialize rate limit state for new requests
  getRateLimitState(config);
  return config;
});

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

    // IMP-004 / WEB-IDM-001: Handle 429 Rate Limit with exponential backoff for idempotent requests only
    if (error.response?.status === 429) {
      const method = (originalRequest.method || 'GET').toUpperCase();
      const isIdempotentMethod = ['GET', 'HEAD', 'OPTIONS'].includes(method);
      const hasIdempotencyKey = Boolean(
        originalRequest.headers?.['X-Idempotency-Key'] || 
        originalRequest.headers?.['x-idempotency-key']
      );

      const state = getRateLimitState(originalRequest);
      const retryAfter = error.response.headers['retry-after'];

      // Parse Retry-After header (seconds)
      let delayMs: number;
      if (retryAfter) {
        const retrySeconds = parseInt(retryAfter, 10);
        delayMs = isNaN(retrySeconds) ? state.baseDelay : retrySeconds * 1000;
      } else {
        // Exponential backoff: 1s, 2s, 4s
        delayMs = state.baseDelay * Math.pow(2, state.retryCount);
      }

      // Show toast notification
      const retrySeconds = Math.ceil(delayMs / 1000);
      toast.error(`Terlalu banyak permintaan, coba lagi dalam ${retrySeconds} detik`, {
        duration: Math.min(delayMs, 5000),
      });

      // Check if we should retry (only if idempotent or has idempotency key)
      if ((isIdempotentMethod || hasIdempotencyKey) && state.retryCount < state.maxRetries) {
        state.retryCount++;

        // Wait for the delay then retry
        await new Promise(resolve => setTimeout(resolve, delayMs));
        return api(originalRequest);
      }
    }

    return Promise.reject(error);
  },
);

export default api;
