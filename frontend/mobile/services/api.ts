import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios';
import { API_CONFIG, AUTH_CONFIG } from '@/constants/config';
import { storage } from '@/utils/storage';
import { Logger } from '@/utils/logger';
import { AuthTokens } from '@/types';

/**
 * Maximum number of retry attempts for failed requests
 */
const MAX_RETRY_ATTEMPTS = 3;

/**
 * Base delay for retry exponential backoff (ms)
 */
const RETRY_BASE_DELAY = 1000;

/**
 * Extended request config to support idempotency key and retry logic
 */
interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
  idempotencyKey?: string;
  skipIdempotencyCheck?: boolean;
  _requestKey?: string;
  _retryCount?: number;
  _retryable?: boolean;
}

/**
 * Check if an error should trigger a retry
 *
 * @param error - The axios error
 * @returns True if the error is retryable
 */
function isRetryableError(error: AxiosError): boolean {
  // Network errors
  if (!error.response) {
    return true;
  }

  // HTTP status codes that are safe to retry
  const retryableStatusCodes = [408, 429, 500, 502, 503, 504];
  return retryableStatusCodes.includes(error.response.status);
}

/**
 * Calculate delay for retry with exponential backoff
 *
 * @param attempt - Current attempt number (1-based)
 * @returns Delay in milliseconds
 */
function calculateRetryDelay(attempt: number): number {
  return RETRY_BASE_DELAY * Math.pow(2, attempt - 1);
}

/**
 * Delay execution for specified milliseconds
 *
 * @param ms - Milliseconds to delay
 * @returns Promise that resolves after delay
 */
function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

class ApiClient {
  private client: AxiosInstance;
  private pendingIdempotencyKeys: Set<string> = new Set();
  private pendingRequests: Map<string, AbortController> = new Map();

  constructor() {
    this.client = axios.create({
      baseURL: API_CONFIG.BASE_URL,
      timeout: API_CONFIG.TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      async (config: ExtendedAxiosRequestConfig) => {
        const tokens = await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);

        if (tokens?.accessToken) {
          config.headers.Authorization = `Bearer ${tokens.accessToken}`;
        }

        // Add idempotency key if provided
        if (config.idempotencyKey) {
          config.headers['X-Idempotency-Key'] = config.idempotencyKey;

          // Track pending idempotency keys for duplicate prevention
          if (!config.skipIdempotencyCheck) {
            if (this.pendingIdempotencyKeys.has(config.idempotencyKey)) {
              Logger.warn('API', 'Duplicate idempotency key detected', {
                keyFormat: config.idempotencyKey.split('::')[0],
              });
            }
            this.pendingIdempotencyKeys.add(config.idempotencyKey);
          }

          Logger.idempotency(config.method?.toUpperCase() || 'POST', config.idempotencyKey, {
            url: config.url,
          });
        }

        // Generate request key for deduplication and abort controller support
        const requestKey = this.getRequestKey(config);
        if (requestKey) {
          config._requestKey = requestKey;

          // Cancel previous read-only request if the same request is pending.
          if (this.pendingRequests.has(requestKey)) {
            const controller = this.pendingRequests.get(requestKey);
            controller?.abort();
          }

          // Mutations must never be cancelled by another request to the same URL.
          const controller = new AbortController();
          config.signal = controller.signal;
          this.pendingRequests.set(requestKey, controller);
        }

        // Log request
        Logger.apiRequest(
          config.method?.toUpperCase() || 'GET',
          config.url || '',
          config.params
        );

        return config;
      },
      (error) => {
        // Clean up pending request on error
        if (error.config?._requestKey) {
          this.pendingRequests.delete(error.config._requestKey);
        }
        return Promise.reject(error);
      }
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => {
        const config = response.config as ExtendedAxiosRequestConfig;

        // Remove idempotency key from pending set on success
        if (config.idempotencyKey) {
          this.pendingIdempotencyKeys.delete(config.idempotencyKey);
        }

        // Clean up pending request on success
        if (config._requestKey) {
          this.pendingRequests.delete(config._requestKey);
        }

        // Log response
        const duration = response.config.headers?.['request-duration']
          ? parseInt(response.config.headers['request-duration'] as string)
          : 0;
        Logger.apiResponse(
          config.method?.toUpperCase() || 'GET',
          config.url || '',
          response.status,
          duration
        );

        return response;
      },
      async (error: AxiosError) => {
        const originalRequest = error.config as ExtendedAxiosRequestConfig & {
          _retry?: boolean;
        };

        // Clean up pending request on error (unless it's an abort)
        if (originalRequest?._requestKey) {
          this.pendingRequests.delete(originalRequest._requestKey);
        }

        // Remove idempotency key from pending set on error
        if (originalRequest?.idempotencyKey) {
          this.pendingIdempotencyKeys.delete(originalRequest.idempotencyKey);
        }

        // If request was aborted, don't try to refresh token or retry
        if (axios.isCancel(error) || error.name === 'CanceledError') {
          Logger.debug('API', 'Request cancelled', { url: originalRequest?.url });
          return Promise.reject(error);
        }

        // Log API error with retry context
        const retryCount = (originalRequest?._retryCount || 0) + 1;
        Logger.apiError(
          originalRequest?.method?.toUpperCase() || 'GET',
          originalRequest?.url || '',
          error,
          {
            status: error.response?.status,
            retryCount,
            maxRetries: originalRequest?._retryable !== false ? MAX_RETRY_ATTEMPTS : 0,
          }
        );

        // Handle 401 - token refresh
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const tokens = await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);

            if (tokens?.refreshToken) {
              Logger.info('Auth', 'Attempting token refresh');

              const response = await this.refreshToken(tokens.refreshToken);
              const newTokens = response.data;

              await storage.set(AUTH_CONFIG.TOKEN_KEY, newTokens);

              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`;
              }

              Logger.info('Auth', 'Token refreshed successfully');

              return this.client(originalRequest);
            }
          } catch (refreshError) {
            Logger.error('Auth', 'Token refresh failed', refreshError);

            // Refresh failed, logout user
            await storage.remove(AUTH_CONFIG.TOKEN_KEY);
            await storage.remove(AUTH_CONFIG.USER_KEY);
            // Navigate to login (handled by auth context)
          }
        }

        // Handle retry for retryable errors
        if (
          originalRequest &&
          originalRequest._retryable !== false &&
          isRetryableError(error) &&
          retryCount <= MAX_RETRY_ATTEMPTS
        ) {
          originalRequest._retryCount = retryCount;

          const retryDelay = calculateRetryDelay(retryCount);

          Logger.retry(
            `${originalRequest.method?.toUpperCase() || 'GET'} ${originalRequest.url}`,
            retryCount,
            MAX_RETRY_ATTEMPTS,
            retryDelay
          );

          // Wait before retry
          await delay(retryDelay);

          // Retry the request
          return this.client(originalRequest);
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * Generate a unique key for a request based on method, url, and params
   */
  private getRequestKey(config: AxiosRequestConfig): string | undefined {
    const method = (config.method || 'GET').toUpperCase();
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      return undefined;
    }

    return JSON.stringify([method, config.url || '', config.params || {}]);
  }

  private async refreshToken(refreshToken: string) {
    return axios.post(`${API_CONFIG.BASE_URL}/auth/refresh`, {
      refreshToken,
    });
  }

  public getInstance() {
    return this.client;
  }

  /**
   * Cancel all pending requests
   * Useful for cleanup on app background or logout
   */
  public cancelAllRequests() {
    Logger.info('API', 'Cancelling all pending requests');
    this.pendingRequests.forEach((controller) => {
      controller.abort();
    });
    this.pendingRequests.clear();
  }

  /**
   * Make a request with idempotency key and retry support
   * This is the preferred method for financial operations
   *
   * @param url - The API endpoint URL
   * @param data - Request payload
   * @param idempotencyKey - Unique idempotency key for the operation
   * @param config - Optional axios config
   * @returns Promise resolving to the response data
   */
  public async postWithIdempotency<T>(
    url: string,
    data: any,
    idempotencyKey: string,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const startTime = Date.now();

    try {
      const response = await this.client.post<T>(url, data, {
        ...config,
        idempotencyKey,
        _retryable: true, // Enable retry for financial operations
        headers: {
          ...config?.headers,
          'request-duration': '0', // Placeholder, will be calculated
        },
      } as ExtendedAxiosRequestConfig);

      return response.data;
    } catch (error) {
      // Calculate duration for error logging
      const duration = Date.now() - startTime;
      Logger.apiError(
        'POST',
        url,
        error,
        {
          duration,
          idempotencyKeyFormat: idempotencyKey.split('::')[0],
        }
      );
      throw error;
    }
  }

  /**
   * Make a GET request with retry support
   *
   * @param url - The API endpoint URL
   * @param config - Optional axios config
   * @returns Promise resolving to the response data
   */
  public async getWithRetry<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.get<T>(url, {
      ...config,
      _retryable: true,
    } as ExtendedAxiosRequestConfig);

    return response.data;
  }

  /**
   * Make a POST request with retry support
   *
   * @param url - The API endpoint URL
   * @param data - Request payload
   * @param config - Optional axios config
   * @returns Promise resolving to the response data
   */
  public async postWithRetry<T>(url: string, data: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.post<T>(url, data, {
      ...config,
      _retryable: true,
    } as ExtendedAxiosRequestConfig);

    return response.data;
  }

  /**
   * Clear the pending idempotency keys set
   * Useful for testing or recovery scenarios
   */
  public clearPendingIdempotencyKeys(): void {
    this.pendingIdempotencyKeys.clear();
    Logger.debug('API', 'Pending idempotency keys cleared');
  }

  /**
   * Get the count of pending idempotency keys
   */
  public getPendingIdempotencyKeyCount(): number {
    return this.pendingIdempotencyKeys.size;
  }
}

export const apiClient = new ApiClient().getInstance();

// Export the ApiClient class instance for advanced usage
export const apiClientInstance = new ApiClient();

// Re-export the axios instance with idempotency support
export const apiClientWithIdempotency = apiClientInstance.getInstance();

// Type export for the extended config
export type { ExtendedAxiosRequestConfig };
