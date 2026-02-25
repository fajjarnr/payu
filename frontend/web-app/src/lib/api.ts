import axios, { isAxiosError } from 'axios';

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

// ── No request interceptor ──────────────────────────────────────────
// Cookies are sent automatically by the browser; the BFF proxy converts
// the httpOnly cookie to a Bearer header on the server side.

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
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(undefined);
    }
  });
  failedQueue = [];
};

// Track retried requests to prevent infinite retry loops
const retriedRequests = new WeakSet<object>();

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !retriedRequests.has(originalRequest)) {
      // Queue concurrent requests while a refresh is in-flight
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      retriedRequests.add(originalRequest);
      isRefreshing = true;

      try {
        // Ask BFF to rotate tokens (cookie → cookie, no JS exposure)
        const refreshRes = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
        });

        if (!refreshRes.ok) throw new Error('Refresh failed');

        processQueue(null);
        return api(originalRequest); // retry with fresh cookie
      } catch (refreshError) {
        processQueue(refreshError);
        if (typeof window !== 'undefined') {
          // Extract locale from current URL path for locale-aware redirect
          const pathLocale = window.location.pathname.match(/^\/(en|id)(\/|$)/);
          const locale = pathLocale ? pathLocale[1] : 'id';
          window.location.href = `/${locale}/login`;
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export default api;
