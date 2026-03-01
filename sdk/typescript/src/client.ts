import axios, { AxiosInstance, AxiosRequestConfig, AxiosError } from 'axios';
import { AuthInterceptor } from './interceptors/auth';
import { RetryInterceptor } from './interceptors/retry';
import { PayUError } from './errors';

/**
 * Configuration options for PayUClient.
 */
export interface PayUClientConfig {
  /** API Key from PayU Dashboard */
  apiKey: string;
  /** API Secret for HMAC signature */
  apiSecret: string;
  /** Environment: 'sandbox' or 'production' */
  environment?: 'sandbox' | 'production';
  /** Custom base URL (overrides environment) */
  baseUrl?: string;
  /** Request timeout in milliseconds */
  timeout?: number;
  /** Enable automatic retries */
  enableRetries?: boolean;
  /** Maximum retry attempts */
  maxRetries?: number;
}

/**
 * Main client for PayU Payment Gateway API.
 *
 * Provides authenticated access to all PayU endpoints with
 * automatic retry logic and error handling.
 */
export class PayUClient {
  private readonly httpClient: AxiosInstance;
  private readonly config: PayUClientConfig;

  // API resource clients (lazy loaded)
  private _payments?: any;
  private _transfers?: any;
  private _wallets?: any;
  private _transactions?: any;

  constructor(config: PayUClientConfig) {
    this.validateConfig(config);
    this.config = {
      environment: 'sandbox',
      timeout: 30000,
      enableRetries: true,
      maxRetries: 3,
      ...config
    };

    this.httpClient = this.createHttpClient();
    this.setupInterceptors();
  }

  /**
   * Get the base URL for the configured environment.
   */
  private getBaseUrl(): string {
    if (this.config.baseUrl) {
      return this.config.baseUrl;
    }

    return this.config.environment === 'production'
      ? 'https://api.payu.fajjjar.my.id'
      : 'https://sandbox-api.payu.fajjjar.my.id';
  }

  /**
   * Create and configure the Axios instance.
   */
  private createHttpClient(): AxiosInstance {
    return axios.create({
      baseURL: this.getBaseUrl(),
      timeout: this.config.timeout,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-PayU-SDK': 'typescript-1.0.0'
      }
    });
  }

  /**
   * Setup request/response interceptors.
   */
  private setupInterceptors(): void {
    // Auth interceptor
    const authInterceptor = new AuthInterceptor({
      apiKey: this.config.apiKey,
      apiSecret: this.config.apiSecret
    });

    this.httpClient.interceptors.request.use(
      authInterceptor.intercept.bind(authInterceptor),
      (error) => Promise.reject(error)
    );

    // Retry interceptor
    if (this.config.enableRetries) {
      const retryInterceptor = new RetryInterceptor({
        maxRetries: this.config.maxRetries || 3,
        retryDelay: 1000
      });

      this.httpClient.interceptors.response.use(
        (response) => response,
        retryInterceptor.onError.bind(retryInterceptor)
      );
    }

    // Error handling interceptor
    this.httpClient.interceptors.response.use(
      (response) => response,
      this.handleError.bind(this)
    );
  }

  /**
   * Handle API errors and transform to PayUError.
   */
  private handleError(error: AxiosError): Promise<never> {
    if (error.response) {
      // API returned an error response
      const status = error.response.status;
      const data = error.response.data as any;

      throw PayUError.fromApiResponse(status, data, error);
    } else if (error.request) {
      // Request was made but no response received
      throw new PayUError(
        'NETWORK_ERROR',
        'No response received from server',
        error
      );
    } else {
      // Something else happened
      throw new PayUError(
        'UNKNOWN_ERROR',
        error.message || 'An unknown error occurred',
        error
      );
    }
  }

  /**
   * Validate client configuration.
   */
  private validateConfig(config: PayUClientConfig): void {
    if (!config.apiKey) {
      throw new Error('API Key is required');
    }
    if (!config.apiSecret) {
      throw new Error('API Secret is required');
    }
  }

  /**
   * Get the underlying Axios instance for advanced usage.
   */
  getHttpClient(): AxiosInstance {
    return this.httpClient;
  }

  /**
   * Make a raw API request.
   */
  async request<T>(config: AxiosRequestConfig): Promise<T> {
    const response = await this.httpClient.request<T>(config);
    return response.data;
  }

  // Lazy-loaded API resources

  /**
   * Payments API
   */
  get payments(): any {
    if (!this._payments) {
      this._payments = new (require('./generated/api').PaymentsApi)(undefined, this.getBaseUrl(), this.httpClient);
    }
    return this._payments;
  }

  /**
   * Transfers API
   */
  get transfers(): any {
    if (!this._transfers) {
      this._transfers = new (require('./generated/api').TransfersApi)(undefined, this.getBaseUrl(), this.httpClient);
    }
    return this._transfers;
  }

  /**
   * Wallets API
   */
  get wallets(): any {
    if (!this._wallets) {
      this._wallets = new (require('./generated/api').WalletsApi)(undefined, this.getBaseUrl(), this.httpClient);
    }
    return this._wallets;
  }

  /**
   * Transactions API
   */
  get transactions(): any {
    if (!this._transactions) {
      this._transactions = new (require('./generated/api').TransactionsApi)(undefined, this.getBaseUrl(), this.httpClient);
    }
    return this._transactions;
  }
}
