import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios';
import { API_CONFIG, AUTH_CONFIG } from '@/constants/config';
import { storage } from '@/utils/storage';
import { AuthTokens } from '@/types';

/**
 * Extended request config to support idempotency key
 */
interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
  idempotencyKey?: string;
  skipIdempotencyCheck?: boolean;
  _requestKey?: string;
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
          config.headers['Idempotency-Key'] = config.idempotencyKey;

          // Track pending idempotency keys for duplicate prevention
          if (!config.skipIdempotencyCheck) {
            if (this.pendingIdempotencyKeys.has(config.idempotencyKey)) {
              console.warn(`Duplicate idempotency key detected: ${config.idempotencyKey}`);
            }
            this.pendingIdempotencyKeys.add(config.idempotencyKey);
          }
        }

        // Generate request key for deduplication and abort controller support
        const requestKey = this.getRequestKey(config);
        config._requestKey = requestKey;

        // Cancel previous request if same request is pending (deduplication)
        if (this.pendingRequests.has(requestKey)) {
          const controller = this.pendingRequests.get(requestKey);
          controller?.abort();
        }

        // Create new abort controller for this request
        const controller = new AbortController();
        config.signal = controller.signal;
        this.pendingRequests.set(requestKey, controller);

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
        // Remove idempotency key from pending set on success
        const config = response.config as ExtendedAxiosRequestConfig;
        if (config.idempotencyKey) {
          this.pendingIdempotencyKeys.delete(config.idempotencyKey);
        }

        // Clean up pending request on success
        if (config._requestKey) {
          this.pendingRequests.delete(config._requestKey);
        }

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

        // If request was aborted, don't try to refresh token
        if (axios.isCancel(error) || error.name === 'CanceledError') {
          return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const tokens = await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);

            if (tokens?.refreshToken) {
              const response = await this.refreshToken(tokens.refreshToken);
              const newTokens = response.data;

              await storage.set(AUTH_CONFIG.TOKEN_KEY, newTokens);

              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`;
              }

              return this.client(originalRequest);
            }
          } catch {
            // Refresh failed, logout user
            await storage.remove(AUTH_CONFIG.TOKEN_KEY);
            await storage.remove(AUTH_CONFIG.USER_KEY);
            // Navigate to login (handled by auth context)
          }
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * Generate a unique key for a request based on method, url, and params
   */
  private getRequestKey(config: AxiosRequestConfig): string {
    return `${config.method}:${config.url}:${JSON.stringify(config.params || {})}`;
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
    this.pendingRequests.forEach((controller) => {
      controller.abort();
    });
    this.pendingRequests.clear();
  }

  /**
   * Make a request with idempotency key
   * This is a convenience method for financial operations
   */
  public async postWithIdempotency<T>(
    url: string,
    data: any,
    idempotencyKey: string,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.client.post<T>(url, data, {
      ...config,
      idempotencyKey,
    } as ExtendedAxiosRequestConfig);

    return response.data;
  }

  /**
   * Clear the pending idempotency keys set
   * Useful for testing or recovery scenarios
   */
  public clearPendingIdempotencyKeys(): void {
    this.pendingIdempotencyKeys.clear();
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
