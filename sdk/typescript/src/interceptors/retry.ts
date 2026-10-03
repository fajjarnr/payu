import { AxiosError } from 'axios';

export interface RetryConfig {
  maxRetries: number;
  retryDelay: number;
  retryableStatuses?: number[];
}

/**
 * Axios response interceptor that implements retry logic with exponential backoff.
 *
 * Retries requests that fail with certain HTTP status codes or network errors.
 */
export class RetryInterceptor {
  private readonly config: RetryConfig;
  private readonly retryableStatuses: number[];

  constructor(config: RetryConfig) {
    this.config = config;
    this.retryableStatuses = config.retryableStatuses || [
      408, // Request Timeout
      429, // Too Many Requests
      500, // Internal Server Error
      502, // Bad Gateway
      503, // Service Unavailable
      504  // Gateway Timeout
    ];
  }

  /**
   * Handle error and retry if appropriate.
   */
  async onError(error: AxiosError): Promise<any> {
    const config = error.config as any;

    // Check if retry is configured and allowed
    if (!config || this.shouldNotRetry(error)) {
      return Promise.reject(error);
    }

    config.retryCount = config.retryCount || 0;

    if (config.retryCount >= this.config.maxRetries) {
      return Promise.reject(error);
    }

    config.retryCount += 1;

    const delay = this.calculateDelay(config.retryCount);

    console.warn(
      `Retrying request (${config.retryCount}/${this.config.maxRetries}) after ${delay}ms: ` +
      `${config.method?.toUpperCase()} ${config.url}`
    );

    await this.sleep(delay);

    return config.adapter(config);
  }

  private shouldNotRetry(error: AxiosError): boolean {
    // Don't retry client errors (4xx) except specific statuses
    if (error.response) {
      const status = error.response.status;

      // Never retry auth errors
      if (status === 401 || status === 403) {
        return true;
      }

      // Only retry specific statuses
      return !this.retryableStatuses.includes(status);
    }

    // Retry network errors
    return false;
  }

  /**
   * Calculate delay with exponential backoff and jitter.
   */
  private calculateDelay(retryCount: number): number {
    const baseDelay = this.config.retryDelay;
    const exponentialDelay = baseDelay * Math.pow(2, retryCount - 1);
    const jitter = Math.random() * 100; // Add up to 100ms of jitter
    return Math.min(exponentialDelay + jitter, 10000); // Max 10 seconds
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
